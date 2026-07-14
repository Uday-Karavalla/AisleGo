# AisleGo — Phased Development Roadmap

**Related docs:** `01-PRD.md` (goals/non-goals), `04-architecture.md` (extraction path)

This roadmap is deliberately concrete: each phase lists shippable deliverables, not aspirational goals. A capability only moves to "done" when it's usable end-to-end, not merely started.

---

## Phase 0 — Modular Monolith MVP (this build)

**Scope:** a single city/region, one currency, mock payment gateway, and the store-discovery-to-order-placement flow proven end-to-end. The goal of Phase 0 is to validate the core product loop and the single-supermarket-cart architecture before spending on real external integrations.

**Deliverables:**

- Modular monolith Spring Boot application with enforced module boundaries: Identity, Catalogue, Inventory, Orders, Payments, Delivery, Loyalty, Administration.
- PostgreSQL schema live for all core entities (see `05-database-er-diagram.md`), including the three-layer single-supermarket-order enforcement (app check, `NOT NULL` FK, DB trigger).
- Customer PWA: registration/login (email/phone + OTP), location detection, store discovery list, storefront browse/search/filter, cart with cross-store guard, checkout (address, fulfillment type, mock payment), real-time order tracking via WebSocket, invoice download, ratings.
- Supermarket dashboard: registration + document submission, branch creation, manual + CSV product entry, inventory management, order accept/reject, picker assignment, substitution approval, mark ready-for-pickup.
- Picker view: task list, picking checklist, mark picked/OOS, packing confirmation.
- Delivery-partner PWA: registration, availability toggle, opportunity feed (single-region), accept/reject assignment, pickup OTP verification, delivery OTP verification, basic earnings view.
- Admin console (minimal): supermarket verification queue, delivery-partner verification queue, basic dispute/refund resolution screen, audit log viewer.
- **Mock payment gateway** — a stubbed internal service simulating authorize/capture/refund/webhook behavior with configurable success/failure scenarios, so checkout and refund flows can be fully built and tested without a live payment provider contract.
- Atomic inventory reservation (optimistic locking) with release-on-timeout job.
- Idempotency keys enforced on checkout and payment-intent creation.
- Core audit logging (order transitions, refunds, verification decisions).
- Docker Compose local dev environment; GitHub Actions CI (build, unit tests, lint).
- All 11 documentation deliverables (this doc set).

**Explicitly not in Phase 0:** real payment gateway, real maps/routing provider (stubbed/static ETA calculation instead), push/SMS notifications (in-app/WebSocket only), native delivery-partner GPS background tracking, Kafka-backed eventing, OpenSearch, multi-region support.

---

## Phase 1 — Real-World Integration

**Scope:** replace Phase 0's stand-ins with production-grade external integrations and extend delivery/admin functionality needed to run a real pilot in the field.

**Deliverables:**

- **Real payment gateway integration** — **Done.** Razorpay is wired in behind the existing `PaymentService` interface (two-phase create-intent/verify flow; Checkout.js talks directly to Razorpay so card/UPI details never reach the backend — see `08-security-and-fraud-control.md` §2). Defaults to the `mock` provider so `docker compose up` still needs zero setup; opt in with `PAYMENTS_PROVIDER=razorpay` + key env vars (see root `README.md` → Payments). The `POST /api/payments/webhook/razorpay` path is implemented but not live-tested end-to-end, since receiving real calls from Razorpay's servers needs a public URL this dev sandbox doesn't have.
- **Real maps/routing API integration** — **Partially done.** Real ETA calculation for store discovery and reverse-geocoding for manual address entry are wired in via OpenRouteService (OSM-based) behind a `RoutingService` interface, mirroring the Razorpay pattern above: defaults to the existing Haversine great-circle estimate with zero setup, opt in with `ROUTING_PROVIDER=openrouteservice` + `OPENROUTESERVICE_API_KEY=...` (see root `README.md` → Maps & routing). On any ORS API failure it falls back to the Haversine calculation automatically. **Not yet started**: turn-by-turn navigation for delivery partners and live location sharing on the customer tracking map — both depend on the delivery-partner app/module, which doesn't exist yet.
- **Push and SMS notifications**: order status changes, OTP delivery, assignment offers to delivery partners, verification status updates — replacing in-app-only notification during Phase 0.
- **Delivery-partner workflow — Done for foreground PWA pilot.** Self-registration, admin verification, availability, row-locked offer acceptance, pickup/delivery OTP verification, customer live-location polling, earnings, and history are implemented. Remaining hardening: native/background location, incentives/bonuses, support/help, reassignment automation, and configurable payouts.
- **Admin verification workflows — Partially done.** Supermarket and delivery-partner approve/reject queues are live with immutable decisions and rejection reasons. Remaining: document viewer/annotation, rejection templates, re-submission, and verification SLA tracking.
- **Substitution & refund UX polish**: customer-side pre-approval preference sets, partial-refund line-item calculation shown transparently to the customer.
- **Store operating-hours automation**: automatic open/closed state driven by configured hours rather than manual toggle only.
- Expanded automated test suite: contract tests for the now-live payment webhook, E2E coverage of the substitution and refund flows.

---

## Phase 2 — Scale-Readiness & Merchant Tooling

**Scope:** introduce the event backbone and search infrastructure the architecture was designed for, and give supermarkets the tooling to onboard and operate at real catalogue scale.

**Deliverables:**

- **Kafka event backbone live**: order lifecycle events (`OrderPlaced`, `OrderStatusChanged`, `OrderDelivered`, `PaymentConfirmed`, `DeliveryCompleted`) published from Orders/Payments/Delivery; consumed by Loyalty (points accrual decoupled from the order transaction), Administration (audit/analytics aggregation), and notification dispatch.
- **OpenSearch-powered product and store search**: typo-tolerant, relevance-ranked search replacing Postgres `ILIKE`/trigram search, fed by a Kafka consumer indexing catalogue change events; faceted filtering (price range, category, offers, in-stock) at search-engine speed.
- **POS integration**: connector framework for common point-of-sale systems to sync live inventory and pricing automatically, reducing manual re-entry for supermarkets that already run a POS.
- **CSV/barcode product import, hardened**: bulk barcode lookup against a shared product database to auto-populate name/image/category, richer CSV validation and error reporting for large catalogues.
- **Loyalty engine, full version**: configurable earn/redeem rules per supermarket tier, tiered loyalty status, points expiry policy, redemption at checkout.
- **Analytics dashboards**: supermarket-facing sales/customer/demand analytics (premium tier), platform-facing GMV/cohort/retention dashboards for admin.
- **Fraud detection, statistical layer**: velocity- and anomaly-based scoring augmenting the Phase 0 rule-based signals (`08-security-and-fraud-control.md` §6), feeding a risk score into the admin dispute queue rather than purely manual triage.
- Load-testing program established for the checkout/inventory-reservation hot path (see `10-testing-strategy.md`) ahead of anticipated volume growth.

---

## Phase 3 — Multi-Region Scale & Platform Maturity

**Scope:** scale beyond a single region, broaden accessibility, open enterprise commercial tiers, and begin selective microservice extraction as real load data justifies it.

**Deliverables:**

- **Multi-region scaling**: region-aware discovery and routing, regional configuration (tax rules, currency if applicable, region-specific delivery partner pools), regional read-replica/data locality strategy for Postgres.
- **Regional language support**: localized UI strings and product content across the customer and delivery-partner apps.
- **Voice search**: voice-to-text product search integrated into the catalogue search experience, building on the OpenSearch layer from Phase 2.
- **White-label enterprise plans**: customizable storefront theming and domain options for large regional supermarket chains, as a distinct commercial tier from standard subscriptions.
- **Selective microservice extraction**: Delivery and Payments extracted from the monolith first per the triggers and process defined in `04-architecture.md` §5, based on actual observed scaling/isolation needs rather than a fixed calendar date.
- **Enterprise analytics & SLAs**: dedicated account management tooling, custom reporting exports, contractual uptime/SLA monitoring for white-label partners.
- **Continued fraud/ML maturation**: model-based fraud scoring trained on accumulated platform transaction history, replacing/augmenting Phase 2's statistical layer.
