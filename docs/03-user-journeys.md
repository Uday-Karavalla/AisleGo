# AisleGo — User Journeys

**Related docs:** `01-PRD.md`, `02-roles-and-permissions.md`, `07-ui-screen-inventory.md`

Each journey is written as numbered steps with explicit decision points (branches marked `[Decision]`) and system-side notes (marked `(system: ...)`) explaining what happens behind the screen.

---

## Journey A — Customer: Discovery Through Rating

1. Customer opens the AisleGo PWA (fresh install or return visit).
2. App requests location permission.
   - **[Decision] Permission granted?**
     - Yes → proceed to step 3 using device geolocation.
     - No → customer is prompted to manually enter a delivery address or pincode; app falls back to that as the active location.
3. `(system: reverse-geocode coordinates → resolve serviceable area; query catalogue module for supermarkets with delivery coverage over that area)`
4. Customer sees a list of nearby, serviceable supermarkets, each showing: name, logo, rating, distance, estimated delivery time, and open/closed status.
5. Customer filters/sorts the list (e.g. "open now," "fastest delivery," "highest rated").
6. Customer taps a supermarket to open its storefront.
   - **[Decision] Is the store currently open / accepting orders?**
     - Closed → storefront is shown in browse-only mode; customer can still browse but cart/checkout actions are disabled with a "closed — opens at HH:MM" message.
     - Open → full storefront is active.
7. Customer searches or browses categories within that store's catalogue.
8. Customer applies filters (price range, in-stock only, offers).
9. Customer opens a product detail page: price, unit, stock availability, offer badges, and a substitution preference toggle ("allow substitution if unavailable").
10. Customer adds the product to cart with a chosen quantity.
    - **[Decision] Does the cart already contain items from a different supermarket?**
      - Yes → `(system: cart-service rejects the add with a 409 Conflict — see 06-api-specification.md)`. Customer sees a modal: *"Your cart has items from [Store A]. Adding from [Store B] requires starting a new cart."* Options: **Clear cart and add** or **Cancel**.
      - No → item is added normally; cart badge updates.
11. Customer repeats steps 7–10 until ready to check out, optionally saving items as favourites along the way.
12. Customer opens the cart, reviews line items, adjusts quantities, applies a coupon code.
    - `(system: coupon validated server-side against store/category eligibility and expiry before discount is shown)`
13. Customer proceeds to checkout: selects delivery address (or "pickup" as fulfillment type), and fulfillment timing (immediate / scheduled slot / pickup).
14. Customer selects a payment method (online payment via gateway, or cash-on-delivery/pickup if the store permits it) and confirms.
    - `(system: server re-validates price and stock for every line item at this instant — see 08-security-and-fraud-control.md §4 — then creates a payment intent with the gateway)`
    - **[Decision] Payment successful?**
      - No → order remains unplaced; customer sees a retry/alternate-method screen; any tentative inventory reservation is released.
      - Yes → order transitions to `Placed` → `Payment Confirmed`; `(system: publishes order-created event; branch receives new-order notification)`.
15. Customer is shown an order confirmation screen with an order ID and live status tracker.
16. Customer tracks order progress in real time as it moves through `Accepted by Store` → `Picking` → (optional `Substitution Approval`) → `Packing` → `Ready for Pickup` → `Delivery Partner Assigned` → `Picked Up` → `Out for Delivery` → `Delivered`.
    - **[Decision] Substitution required for an item?**
      - Yes → customer receives a push/in-app prompt showing the proposed substitute; customer approves, rejects (item removed, partial refund calculated), or the pre-set preference from step 9 auto-resolves it.
17. During `Out for Delivery`, customer sees the delivery partner's live location on a map and can call the delivery partner or the store directly.
18. Customer receives the order, confirms via delivery OTP shared with the delivery partner (see Journey C).
19. Customer downloads the digital invoice from the completed-order screen.
20. Customer rates the store, individual products, and the delivery experience as three separate ratings.
    - **[Decision] Is there an issue with the order (missing/damaged item)?**
      - Yes → customer files a report from the order detail screen, attaching photos; this creates a case routed to the store, and to platform admin if unresolved within SLA (see Journey D).
      - No → journey ends; order and invoice remain in order history for future "repeat order."

---

## Journey B — Supermarket: Onboarding Through First Order Fulfillment

1. Supermarket owner signs up on the AisleGo partner portal with business name, contact number, and email.
2. Owner submits verification documents (business registration, tax ID, bank account for settlements).
3. `(system: application enters "Pending Verification" status; a case is created in the platform admin queue)`
4. Platform administrator reviews the submitted documents.
   - **[Decision] Documents valid and business legitimate?**
     - No → application is rejected with a reason; owner can resubmit corrected documents.
     - Yes → business is marked **Verified**; owner gains access to the partner dashboard.
5. Owner creates one or more branches: address, delivery radius/area, operating hours, contact number per branch.
6. Owner (or a branch manager they invite) populates the catalogue: adds products via barcode scan or CSV/Excel bulk import, sets prices, assigns categories, uploads images.
7. Owner/branch manager sets initial per-branch inventory counts.
8. Owner configures store-level policies: accepted payment methods, substitution defaults, return window, staff roles.
9. Owner invites staff: assigns branch manager(s) to specific branches; branch managers in turn invite pickers.
10. `(system: storefront becomes visible in customer discovery results once branch has inventory, hours, and is within "open" status)`
11. **First order arrives** — branch receives a real-time notification (WebSocket + push) of a new order in `Payment Confirmed` state.
12. Branch manager reviews the order.
    - **[Decision] Accept or reject?**
      - Reject (e.g. store closing, cannot fulfill) → order is cancelled, customer is notified immediately, any payment is auto-refunded.
      - Accept → order moves to `Accepted by Store`; `(system: inventory reservation for each line item is confirmed/locked)`.
13. Branch manager assigns the order to a picker (or the system auto-assigns based on picker availability/load).
14. Picker opens the picking list on their device and begins fulfilling line items.
    - **[Decision] Is every item in stock as expected?**
      - Yes → picker marks each item picked; order proceeds to step 16.
      - No (item unavailable) → picker flags the item as out-of-stock and suggests a substitution (or marks "unavailable, no substitute").
15. **Substitution approval sub-flow:**
    - `(system: if the customer pre-approved substitutions for this item, auto-approve and notify customer)`
    - Otherwise, request is sent to the branch manager and/or customer for approval.
    - **[Decision] Approved?**
      - Yes → substitute item is picked instead; price difference is reconciled (refund/charge adjustment per store policy).
      - No → item is removed from the order; corresponding refund for that line item is calculated.
16. Once all line items are resolved, picker packs the order and marks it **Packing** → **Ready for Pickup**.
17. `(system: order becomes visible in the delivery-partner assignment pool; nearby available partners are notified)` — continues in Journey C.
18. Branch manager monitors the order through to `Delivered` on the dashboard; can contact the customer or delivery partner if needed.
19. Post-delivery, order and settlement details flow into the branch's sales/analytics view; commission is calculated per the platform's fee schedule.

---

## Journey C — Delivery Partner: Onboarding Through Earnings

1. Prospective delivery partner signs up on the AisleGo delivery-partner PWA: name, phone, vehicle type.
2. Partner uploads verification documents (government ID, driving license/vehicle registration where applicable, a profile photo).
3. `(system: application enters "Pending Verification"; routed to platform admin queue)`
4. Platform administrator reviews documents.
   - **[Decision] Valid?**
     - No → rejected with reason, resubmission allowed.
     - Yes → partner account is **Verified** and activated.
5. Partner opens the app and toggles status to **Online** (available for assignments).
6. `(system: location-matching service surfaces nearby ready-for-pickup or soon-to-be-ready orders within the partner's radius)`
7. Partner receives an assignment offer showing pickup branch, approximate distance, and payout estimate.
   - **[Decision] Accept or reject?**
     - Reject (or timeout) → offer is routed to the next nearest available partner.
     - Accept → assignment is locked to this partner; `(system: order status → Delivery Partner Assigned)`.
8. Partner navigates to the supermarket branch using integrated maps/routing.
9. On arrival, partner requests pickup confirmation from branch staff.
10. Branch staff or partner enters/scans the **pickup OTP/QR** shown on the branch's order-ready screen.
    - **[Decision] OTP/QR valid and not expired?**
      - No → retry allowed up to a limited number of attempts within the OTP's validity window; after repeated failure, branch staff can regenerate a new OTP.
      - Yes → `(system: order status → Picked Up)`; partner takes custody of the packed order.
11. Partner starts the delivery leg; app shares live location, visible to both the customer and the branch.
    - `(system: order status → Out for Delivery)`.
12. Partner navigates to the customer's address.
13. On arrival, partner requests the **delivery OTP** from the customer (shown on the customer's tracking screen).
    - **[Decision] OTP entered correctly within validity window?**
      - No → retry allowed up to a limited number of attempts; if it keeps failing, partner can contact the customer or escalate to support (possible non-contact/failed-delivery flow).
      - Yes → `(system: order status → Delivered)`; delivery is closed out.
14. Partner marks themselves available again (or goes offline) and returns to step 5/6 for the next opportunity.
15. Partner views the completed delivery, its payout, and any incentive bonuses in their earnings history.
16. At the end of a payout cycle, `(system: aggregates completed deliveries + incentives → settlement)`; partner can view/download statements.

---

## Journey D — Platform Administrator: Resolving a Dispute / Refund

1. A dispute case enters the admin queue via one of three triggers:
   - Customer files a "missing/damaged product" report that the store didn't resolve within its SLA window.
   - A refund request exceeds the store's/branch's approval threshold (see `02-roles-and-permissions.md` §4).
   - An automated fraud signal flags the order for review (see `08-security-and-fraud-control.md` §6).
2. Administrator opens the case, which shows: full order timeline, payment record, chat/call logs (if any), customer's uploaded evidence, and the store's response so far.
3. Administrator reviews the order's audit log for anomalies (e.g. repeated substitutions, delivery OTP entered from an unusual location, order value pattern).
4. **[Decision] Is this a legitimate customer complaint?**
   - No (evidence suggests abuse, e.g. repeated false "missing item" claims) → case is denied; flag is added to the customer's account for fraud-pattern tracking; no refund issued.
   - Yes → proceed to step 5.
5. **[Decision] Is the store or the delivery leg at fault (or neither)?**
   - Store fault (e.g. wrong/missing item picked) → refund charged against the store's settlement; store is notified with the finding.
   - Delivery fault (e.g. proof of tampering, non-delivery despite OTP claim) → refund charged against platform/delivery-partner liability pool per policy; delivery partner's record is annotated.
   - Neither at fault / genuine stock-out edge case → refund issued from platform goodwill budget, no fault annotation to either party.
6. Administrator approves the refund amount (full or partial line-item level) and selects the settlement source from step 5.
7. `(system: refund is issued through the payment gateway to the original payment method; order and ledger records are updated; audit log entry is written with administrator ID, decision, and rationale)`
8. Customer and store both receive a notification of the resolution and the refund status.
9. If the case revealed a repeat pattern (e.g. a store consistently under-fulfilling, a delivery partner with multiple non-delivery disputes, or a customer with abnormal refund velocity), administrator escalates to the fraud/quality review queue for possible account-level action (warning, suspension) — see `08-security-and-fraud-control.md` §6.
10. Case is closed and archived with full audit trail for future reference and compliance reporting.
