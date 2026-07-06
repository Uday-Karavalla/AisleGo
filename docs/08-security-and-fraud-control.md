# AisleGo — Security and Fraud Control Plan

**Related docs:** `02-roles-and-permissions.md`, `04-architecture.md`, `05-database-er-diagram.md`

---

## 1. Authentication & Authorization Model

- **Authentication:** Spring Security with JWT access tokens (short-lived, ~15 minutes) and refresh tokens (longer-lived, rotated on use, ~30 days). Refresh tokens are stored server-side (Redis) with a revocation list so logout, password change, or a detected compromise can immediately invalidate all outstanding sessions for a user.
- **Token claims:** access tokens carry `userId`, `role`, and scope identifiers relevant to that role — `supermarketId` and/or `branchId` for supermarket-side roles, nothing extra for customers/delivery partners beyond their own `userId`. Scope claims are set at token issuance from the server-side staff/account record, never accepted from the client.
- **Authorization is two-layered on every request**, matching the scope model in `02-roles-and-permissions.md`:
  1. **Role check** — does this role have the capability at all (e.g. can a `BRANCH_MANAGER` accept orders)? Enforced declaratively via Spring Security method security (`@PreAuthorize`).
  2. **Scope check** — does the target resource belong to this actor's scope (e.g. is this order's `branchId` equal to the JWT's `branchId`)? Enforced in the service layer by scoping every repository query with the caller's scope identifiers rather than trusting path/body parameters — an ID in a URL is never sufficient on its own to authorize access to that record.
- **Password storage:** bcrypt with a strong work factor; OTP-based login is the preferred path for customers and delivery partners to reduce password-reuse risk.
- **Transport security:** TLS everywhere (enforced at the load balancer/ingress); HSTS on all web-facing domains; WebSocket connections upgrade over the same TLS-terminated connection and re-validate the JWT on connect and on reconnect.

## 2. Payment Handling

- **AisleGo never stores primary account numbers (PAN), CVV, or any raw card data**, full stop. This is a structural, non-negotiable constraint on the `PAYMENT` table (see `05-database-er-diagram.md`) — it holds only `gateway_payment_intent_id` and `gateway_token_reference`.
- All card/UPI entry happens through the **payment gateway's hosted fields or tokenization SDK**, so sensitive payment data never transits AisleGo's own backend or frontend code — it goes directly from the customer's browser/device to the gateway.
- Payment confirmation is authoritative via the **gateway's server-to-server webhook**, not the client's redirect callback alone (a client can be killed or lose connectivity mid-flow; the webhook is the source of truth for whether an order should transition to `PAYMENT_CONFIRMED`).
- Refunds are always issued back to the original payment method through the gateway's refund API — AisleGo never handles cash settlement for online payments.
- Cash-on-delivery/pickup, where enabled by a store, is tracked as a `PAYMENT.method = COD` record with its own reconciliation flow (delivery partner or branch confirms cash collected), kept clearly distinct from gateway-mediated payments in reporting.
- **Signature verification (Razorpay integration):** the concrete implementation of "card/UPI data never transits AisleGo's backend" above is that Razorpay's Checkout.js widget talks directly to Razorpay from the customer's browser — the AisleGo backend never receives a card number, UPI ID, or CVV at any point. It only ever sees three opaque identifiers coming back, and both are HMAC-SHA256 verified before an order is trusted as paid:
  - **Client callback verification:** when Checkout.js reports success, the frontend calls `POST /api/checkout/{orderId}/payment/verify` with `{gatewayOrderId, gatewayPaymentId, gatewaySignature}`. The backend recomputes the HMAC-SHA256 signature over `gatewayOrderId|gatewayPaymentId` using the Razorpay **key secret** (`razorpay-java`'s `Utils.verifyPaymentSignature`) and compares it to `gatewaySignature`; a mismatch cancels the order and releases its reserved inventory rather than confirming payment.
  - **Server-to-server webhook verification:** `POST /api/payments/webhook/razorpay` is called directly by Razorpay's servers (not the browser) and is authenticated independently — via a **separate webhook secret** (`RAZORPAY_WEBHOOK_SECRET`, distinct from the key secret) checked against the `X-Razorpay-Signature` header using `Utils.verifyWebhookSignature`. This is the defense-in-depth path for a customer who closes the browser before the client callback fires; it runs the same idempotent capture logic, so whichever of the two verification paths arrives first confirms the order and the other is a safe no-op.
  - Using two distinct secrets for the two paths means a leak of one does not let an attacker forge the other, and both verifications happen entirely server-side against values Razorpay itself signed — the backend never has to (and structurally cannot) trust an unsigned claim of "payment succeeded" from the client.

## 3. OTP Design (Pickup & Delivery)

- **Generation:** a 4–6 digit numeric OTP generated server-side at the moment an order reaches `READY_FOR_PICKUP` (pickup OTP) and again — or simultaneously — for the delivery leg (delivery OTP). Only the hash of each OTP is persisted (`pickup_otp_hash`, `delivery_otp_hash`); plaintext is transmitted once to the relevant display surface (branch dashboard for pickup, customer's tracking screen for delivery) and never logged.
- **Expiry windows:** pickup OTP is valid for a bounded window appropriate to how long a packed order should realistically wait for pickup (e.g. tens of minutes, configurable); delivery OTP is valid for a bounded window around the estimated delivery arrival, refreshed/extended automatically if the delivery is delayed for legitimate tracked reasons (traffic, distance) rather than left to expire on the customer.
- **Retry & lockout:** a limited number of verification attempts (e.g. 5) per OTP before the code is locked; a locked or expired OTP requires the issuing party (branch staff for pickup, customer app for delivery) to explicitly regenerate a new one, which invalidates the previous code. Repeated regeneration requests on the same order within a short time window are rate-limited and flagged as a fraud signal (§6).
- **Channel separation:** the delivery OTP is only ever shown to the customer (never to the delivery partner in advance), and the pickup OTP is only ever shown to branch staff — this ensures the physical possession handoff at each leg genuinely requires the receiving party to be present and coordinating with the OTP holder.

## 4. Atomic Inventory Reservation Strategy

Correctly reserving stock during checkout is one of the highest-risk correctness surfaces in the system — over-selling erodes trust with both the customer and the supermarket.

- **Reservation model:** `INVENTORY.reserved_quantity` is incremented within the same database transaction that validates cart contents and creates the order in `PLACED` state, using **optimistic locking** via the `INVENTORY.version` column — an `UPDATE ... WHERE id = ? AND version = ?` that fails (0 rows affected) if a concurrent request already modified the row, causing the checkout transaction to retry the reservation against fresh stock numbers or fail fast with `422 Insufficient Stock` if the retry still can't be satisfied.
- **Why optimistic over pessimistic (`SELECT FOR UPDATE`) by default:** checkout is a comparatively low-contention path per individual product/branch (many distinct products, not one hot row), so optimistic locking avoids holding row locks across the round-trip to the payment gateway. For narrow hot-spot cases (e.g. a flash-sale item with many concurrent buyers on one branch's stock row), the same code path can fall back to a short-lived `SELECT ... FOR UPDATE` scoped tightly around just the quantity check-and-decrement, not the whole checkout transaction.
- **Reservation vs. confirmation:** stock is **reserved** (held, not yet deducted from sellable count shown elsewhere) the moment checkout begins server-side validation, and only **confirmed** (durably deducted) once the payment gateway confirms payment via webhook. This two-step model is what makes the "payment failed" case safe.
- **What happens on payment failure or timeout:** a scheduled reconciliation job (and an immediate release on any explicit payment-failure webhook) releases any reservation whose parent order has not reached `PAYMENT_CONFIRMED` within a bounded timeout window (e.g. a few minutes) — `reserved_quantity` is decremented back, `stock_quantity` becomes available again, and the order transitions to a terminal `CANCELLED` (payment failed) state. This prevents a common failure mode where abandoned checkouts silently lock up real stock indefinitely.
- **Substitution/removal adjustments:** when a picker marks an item unavailable or a substitution is rejected, the reservation for that specific line item is released back to the branch's available stock as part of the same order-item state transition, not batched or delayed.

## 5. Idempotency & Audit Logging

- **Idempotency keys:** `POST /checkout` and `POST /payments/intents` require a client-generated `Idempotency-Key` header (see `06-api-specification.md` §10). The server persists the key against the resulting `orders.idempotency_key` / `payments.idempotency_key` (both unique-constrained) and, on a retried request bearing the same key, returns the original result rather than creating a duplicate order or duplicate charge. This protects against the very common case of a mobile client retrying a checkout request after a network timeout without knowing whether the first attempt succeeded.
- **Audit logging — what gets logged:** every sensitive state-changing action writes an immutable row to `AUDIT_LOG` (see `05-database-er-diagram.md`), including at minimum: order status transitions, refund/cancellation decisions (with actor and rationale), staff account creation/permission changes, supermarket/delivery-partner verification decisions, admin account suspensions, OTP regeneration events, and coupon/pricing changes. Each entry captures actor identity and role, action, entity type/ID, a before/after JSON snapshot, source IP, and timestamp.
- **Retention:** audit log entries are retained for a minimum regulatory- and dispute-driven window (e.g. 7 years for financial/transactional records, aligned to applicable tax and consumer-protection recordkeeping requirements; shorter operational-only entries such as OTP regeneration events may roll off sooner into cold storage after an active window, e.g. 1 year hot + longer-term cold archive). The audit log is append-only at the application layer (no update/delete code path exists) and is additionally protected by database-level revoke of `UPDATE`/`DELETE` grants on the table for all application roles except a narrowly-scoped archival job.
- **Who can read it:** platform administrators can query the full audit log; supermarket owners and branch managers can view (read-only) the subset of entries scoped to their own store/branch (see `02-roles-and-permissions.md` §3), never another tenant's.

## 6. Fraud Signals to Monitor

AisleGo's fraud-detection posture in Phase 0 is rule-based monitoring feeding the platform admin dispute queue (see `03-user-journeys.md`, Journey D); statistical/ML-based detection is a later-phase enhancement once sufficient transaction volume exists.

| Signal | What it might indicate | Example rule |
|---|---|---|
| Order velocity | Compromised payment method, bulk resale abuse | >N orders from one customer account within a short rolling window |
| Refund/complaint velocity | Chargeback fraud, serial "missing item" false claims | Customer's refund-approved rate significantly exceeds platform average |
| Address anomalies | Stolen payment method, drop-shipping fraud pattern | Delivery address changes frequently across orders paid with the same instrument; delivery address far outside any store's serviceable radius (spoofed) |
| Device/session anomalies | Account takeover, multi-accounting | Same device fingerprint driving many distinct customer accounts in a short window; login from a new device immediately followed by a high-value order |
| Delivery OTP anomalies | Fake delivery confirmation, delivery-partner collusion | Delivery OTP confirmed while the delivery partner's live location is materially far from the customer's delivery address at confirmation time |
| Pickup OTP anomalies | Store-side fraud, phantom order confirmation | Pickup OTP confirmed before the order reached `PACKING`/`READY_FOR_PICKUP` state, or with no corresponding picking activity logged |
| Substitution abuse | Store inflating basket value via forced substitutions | Substitution rate for a branch significantly above platform average, especially toward higher-priced items |
| Coupon abuse | Multi-accounting to farm promotional discounts | Same payment instrument or device applying a single-use coupon across many distinct customer accounts |
| Rapid account suspension evasion | Banned actor re-registering | New account signals (device, phone, payment instrument) matching a previously suspended account |

Flagged orders/accounts are routed into the admin dispute/fraud queue for human review rather than auto-actioned in Phase 0, except for hard-blocking rules (e.g. a suspended account cannot authenticate at all).

## 7. Server-Side Validation Discipline

- **Price and stock are never trusted from the client.** Every price shown client-side is illustrative; the server re-resolves current price and stock for every cart item both when adding to cart and, again, authoritatively, at checkout — this is what makes the `409`/`422` responses in `06-api-specification.md` §9 necessary and expected, not edge cases.
- **The single-supermarket-cart rule is validated server-side at every mutation** (add-to-cart and checkout), never assumed from client state — see `05-database-er-diagram.md` §3 for the full three-layer enforcement (application check, DB constraint, DB trigger).
- **Role/scope claims are always read from the verified JWT, never from request bodies or query parameters.** A request body containing a `branchId` is used only to identify *which* record is being acted on, never to establish *whether* the actor is allowed to act on it — that determination always comes from the token's own scope claims compared against the target record's actual owning branch/store, resolved server-side.

## 8. Supermarket Data Isolation (Tenant Isolation)

Strong isolation between supermarkets is a first-class requirement, not an incidental property of good code:

- **Row-level scoping by `supermarket_id`:** every repository/query method that touches tenant-owned data (products, inventory, orders, coupons, staff) requires a `supermarket_id` (and, where applicable, `branch_id`) as part of its query predicate, sourced from the caller's authenticated scope — never a global "fetch by ID" method that a scope check is bolted onto afterward. This is enforced by code convention and reviewed in code review; the modular monolith's schema-per-module organization (`04-architecture.md`) makes it straightforward to audit that no cross-tenant query paths exist.
- **No cross-tenant joins in application queries.** Any query that appears to need to join across two different supermarkets' data (which should essentially never happen for customer- or store-facing operations) is a signal requiring explicit review — the only legitimate cross-tenant views are platform-administrator analytics and audit queries, which run under a distinct, clearly-labeled admin-scoped code path rather than the standard tenant-scoped repositories.
- **The single-supermarket-order rule is itself a tenant-isolation mechanism**, not just a business rule — because an order can never span suppliers, there is no code path anywhere in Orders/Inventory/Payments that ever needs to reason about two supermarkets within the same transaction, which structurally eliminates an entire class of cross-tenant data leakage risk.
- **Object storage isolation:** product images, invoices, and verification documents are stored under tenant-prefixed paths/buckets with access-controlled, time-limited signed URLs — a supermarket's uploaded documents or a customer's invoice are never reachable via a guessable or shared static URL.
- **Future extraction safety:** because tenant scoping is enforced at the query/repository layer today rather than assumed from a shared process boundary, extracting a module (e.g. Payments) into its own service later (`04-architecture.md` §5) does not introduce a new tenant-isolation risk — the same scoped-query discipline carries over to that service's own data access layer.
