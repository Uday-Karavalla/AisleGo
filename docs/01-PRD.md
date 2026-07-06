# AisleGo — Product Requirements Document (PRD)

**Document owner:** Product Team
**Status:** Draft v1.0
**Last updated:** 2026-07-03

---

## 1. Problem Statement

Local and regional supermarkets — the grocery stores, provision stores, and mini-marts that neighborhoods already know and trust — are being squeezed out of the online grocery conversation by "quick-commerce" (q-commerce) platforms that operate their own dark stores. These platforms buy inventory wholesale, warehouse it themselves, and sell it under their own operational identity. The supermarket becomes irrelevant; the customer's loyalty transfers to the app, not the store.

This leaves existing supermarket owners with three bad options:

1. **Stay offline** and slowly lose footfall to 10-minute delivery apps.
2. **Become a "dark store" supplier** to a q-commerce platform, surrendering pricing control, customer data, and brand identity in exchange for order volume.
3. **Build their own app** — a cost and engineering burden most independent and regional supermarket chains cannot justify.

Customers, meanwhile, want the convenience of browsing, ordering, and tracking groceries online — but many still prefer buying from a specific supermarket they already trust for quality, price, or familiarity, rather than an anonymous fulfillment warehouse.

There is no platform today that lets an existing supermarket go online **as itself** — keeping its own inventory, pricing, staff, and customer relationships — while still offering customers the app-based discovery, ordering, and delivery experience they expect.

## 2. What AisleGo Is

AisleGo is a **multi-supermarket marketplace**: a discovery, ordering, and fulfillment-coordination layer that sits on top of existing, independently-operated supermarkets. Supermarkets register their business, create digital storefronts for each of their branches, manage their own products/prices/inventory/offers, and receive orders through AisleGo. Customers discover nearby supermarkets, browse a store's live catalogue, place an order, and get it delivered by an AisleGo delivery partner (or pick it up themselves) — while the supermarket remains the merchant of record for that transaction.

AisleGo does not buy inventory, does not set retail prices, does not operate warehouses, and does not compete with the supermarkets it lists. It is infrastructure, not a competitor.

## 3. Why AisleGo Is Different From Dark-Store (Blinkit-Style) Players

This distinction is the foundation of the product and must be visible in every customer- and partner-facing surface, not just marketing copy.

| Dimension | Dark-store q-commerce (e.g. Blinkit-style) | AisleGo |
|---|---|---|
| Who owns inventory | The platform | The supermarket |
| Who sets prices | The platform | The supermarket |
| Whose brand the customer sees | The platform's | The supermarket's own name, branches, and identity |
| Where the product ships from | A platform-run micro-warehouse (dark store) | The supermarket's own branch, picked by the supermarket's own staff |
| Who owns the customer relationship | The platform | The supermarket (AisleGo facilitates, does not intermediate the relationship away) |
| Cross-store carts | Not applicable — one warehouse fulfills everything | **Explicitly disallowed** — see Section 4 |
| Supermarket's upside | None — supermarket is disintermediated | Supermarket gains a new digital sales channel without losing identity or control |

AisleGo's pitch to a supermarket owner is simple: *"Go online as yourself. Keep your prices, your staff, your customers. We bring you discovery and delivery."*

## 4. Core Business Rule — Single-Supermarket Orders

> **One order can contain products from only one supermarket. Cross-store carts are not allowed.**

This is not a UX nicety — it is the structural expression of the "no dark stores" differentiator. If a customer could mix products from two supermarkets into one cart, AisleGo would implicitly become the fulfillment aggregator (functioning like a dark store assembling a mixed basket), which:

- Breaks the settlement model (a single order needs a single merchant-of-record for tax, invoicing, and payout).
- Breaks fulfillment (each supermarket branch picks and packs its own orders; there is no shared warehouse to consolidate a mixed basket).
- Breaks accountability (returns, refunds, and disputes must map to exactly one supermarket).

This rule is enforced at every layer of the system — UI (attempting to add a second store's product prompts the customer to clear or start a new cart), API (the checkout endpoint rejects mixed-store carts), and database (a non-null `supermarket_id` on `orders`, with referential and trigger-level checks that every `order_item`'s product belongs to that supermarket). See `04-architecture.md` and `05-database-er-diagram.md` for the enforcement design.

Customers who want to shop at two different supermarkets place two separate orders. This is treated as expected, normal behavior, not an edge case to work around.

## 5. Goals

**Business goals**

- Onboard existing local and regional supermarkets onto AisleGo as digital-first sales channels without disrupting their existing operations.
- Generate revenue through subscriptions, order commissions, delivery charges, and payment processing share (see Section 9).
- Build a defensible, differentiated position against dark-store q-commerce by being the "supermarket-friendly" alternative.

**Product goals (this build / Phase 0)**

- Ship a working, end-to-end flow: a customer can detect their location, discover nearby supermarkets, browse a store's catalogue, build a single-store cart, check out, and place an order; the supermarket can accept the order and progress it through fulfillment; a delivery partner can be assigned and complete delivery.
- Establish the modular monolith architecture and data model that the platform will scale on, without over-building for scale that doesn't yet exist.
- Prove the single-supermarket-cart rule end-to-end, including its failure-mode UX (what happens when a customer tries to break it).

## 6. Non-Goals (Explicit)

To keep AisleGo focused and to protect the core differentiator, the following are **explicitly out of scope**, now and structurally:

- **AisleGo will never operate its own dark stores or hold its own retail inventory.** This is a permanent product boundary, not a phase-gated feature.
- **AisleGo will never allow a single order to span multiple supermarkets.** No "smart basket splitting," no cross-store cart merging — ever. (A customer may place multiple separate orders to different stores in one session; that is different from a single mixed order.)
- **AisleGo will not set or influence supermarket retail prices.** Supermarkets control their own pricing and offers.
- **No dynamic/surge delivery pricing in Phase 0.** Delivery fees are flat or slab-based initially; dynamic pricing is a future consideration, not a Phase 0 concern.
- **No dark warehousing, quick-commerce SKU curation, or platform-branded private label products.**
- **No international expansion or multi-currency support in Phase 0.** Single country, single currency, single tax regime to start.
- **No native mobile apps in Phase 0.** The customer and delivery-partner experience ships as a responsive, installable Progressive Web App (PWA); native apps are a later consideration if PWA limitations (e.g. background location, push reliability) prove blocking.
- **No regional-language or voice-search support in Phase 0** — explicitly deferred to a later roadmap phase (see `09-roadmap.md`).

## 7. Target Users

AisleGo serves six distinct roles across three sides of the marketplace:

**Demand side**
- **Customer** — a shopper who wants to order groceries and household goods from a specific, familiar local supermarket for home delivery or pickup.

**Supply side (the supermarket business)**
- **Supermarket owner** — the account holder for a supermarket business (which may operate one or many branches); responsible for onboarding, verification, and top-level business configuration.
- **Branch manager** — operates a single physical branch: local inventory, staff, and order fulfillment oversight for that branch.
- **Store employee / picker** — front-line staff assigned to pick, pack, and stage individual orders at a branch.

**Fulfillment side**
- **Delivery partner** — an independent courier who accepts delivery opportunities, picks up orders from supermarket branches, and delivers them to customers.

**Platform side**
- **Platform administrator** — AisleGo's own staff, responsible for verifying supermarkets and delivery partners, resolving disputes, monitoring platform health, and configuring commercial terms.

## 8. Role Feature Sets

### 8.1 Customer

- Register and log in (email/phone + password, or OTP-based login).
- Manage profile and multiple saved delivery addresses.
- Detect current location (geolocation) or manually set a delivery location.
- Discover nearby supermarkets within deliverable range.
- View store ratings, operating hours, and estimated delivery time before opening a store.
- Open a specific supermarket's storefront.
- Search and filter that store's products (by category, price, offers, in-stock status).
- View live prices, running offers, and stock availability.
- Add products and quantities to a cart — scoped to a single supermarket (see Section 4).
- Select acceptable substitutions for out-of-stock items ahead of time.
- Apply coupons at checkout.
- Choose fulfillment type: immediate delivery, scheduled delivery, or self-pickup.
- Pay online via supported payment methods, or use permitted alternative methods (e.g. cash on delivery, where enabled by the supermarket).
- Track order status in real time, including delivery partner's live location once out for delivery.
- Call the supermarket directly from the order screen.
- Download digital invoices for completed orders.
- Request cancellation, return, or refund on an order.
- Report missing or damaged products post-delivery.
- Earn and redeem loyalty points.
- Save favourite products and favourite stores.
- Repeat a previous order in one action.
- Rate the supermarket, individual products, and the delivery experience separately.

### 8.2 Supermarket Owner

- Business registration and submission of verification documents. *(Self-service registration with admin approve/reject now exists — see `09-roadmap.md` Phase 1; document upload/annotation is not yet part of that flow.)*
- Create and manage multiple branches under one business account.
- Manage store information (name, description, contact number, operating hours) and each branch's delivery area.
- Add products via barcode scanning or manual entry.
- Bulk-import products via CSV/Excel.
- (Future) Integrate with existing POS systems for live catalogue and inventory sync.
- Manage prices and live per-branch inventory.
- Create offers, coupons, and product bundles.
- View settlements, sales reports, and customer analytics across all branches.
- Manage staff accounts and role-based permissions (branch managers, pickers) across the business.
- Configure business-level policies (accepted payment methods, return window, substitution defaults).

### 8.3 Branch Manager

- All order-management capabilities scoped to their own branch: accept or reject incoming orders, monitor picking/packing progress, mark orders ready for pickup.
- Manage the branch's own inventory levels and stock counts.
- Reserve inventory during checkout windows (system-assisted, manager can override/release).
- Assign orders to specific employees/pickers at the branch.
- Approve or reject substitution suggestions raised by pickers.
- Process branch-level cancellations and refund requests (subject to policy limits; escalates to owner/admin above threshold).
- Contact customers regarding their branch's orders.
- View branch-level sales and performance analytics.

### 8.4 Store Employee / Picker

- View assigned orders and picking lists for their branch.
- Mark items as picked, out-of-stock, or substituted (subject to manager approval where required).
- Generate/print packing lists and package orders.
- Mark an order "ready for pickup" once packed.
- Flag stock discrepancies discovered during picking.

### 8.5 Delivery Partner

- Registration and document verification (ID, vehicle, license where applicable).
- Toggle online/offline availability.
- Receive nearby delivery opportunities matched to current location and availability.
- Accept or reject individual assignments.
- Navigate to the supermarket branch (maps/routing integration).
- Confirm pickup using a time-bound OTP or QR code presented by branch staff.
- Contact the store and the customer directly from the assignment screen.
- Share live location during the delivery leg.
- Confirm delivery using a customer-presented OTP.
- View earnings, incentive payouts, and delivery history.

### 8.6 Platform Administrator

- Verify supermarket business registrations and delivery-partner documents.
- Manage customer, store, and branch records platform-wide.
- Configure commission rates and subscription plans.
- Manage the global product category taxonomy.
- Monitor orders and payments across the platform for anomalies.
- Resolve disputes and complaints escalated from stores or customers.
- Approve refunds above store-level authority thresholds.
- Detect and act on suspicious activity (fraud signals — see `08-security-and-fraud-control.md`).
- Manage platform-wide promotions and featured placements.
- View platform-wide analytics (GMV, order volume, store performance, delivery SLAs).
- Suspend fraudulent or policy-violating accounts.
- Maintain and audit the complete platform audit log.

## 9. Revenue Model

AisleGo monetizes the coordination layer, not the goods themselves:

- **Monthly supermarket subscriptions** — tiered plans for storefront hosting, number of branches, and feature access.
- **Commission on completed orders** — a percentage of order value, charged to the supermarket per fulfilled order.
- **Delivery-platform charges** — a fee for coordinating and staffing the delivery leg, paid by the customer, the supermarket, or split, depending on plan.
- **Payment-processing revenue share** — a margin on top of underlying payment gateway costs.
- **Featured supermarket and product promotions** — paid placement in discovery and search results.
- **Premium analytics** — advanced sales, customer, and demand-forecasting dashboards for supermarket owners.
- **POS integration and setup charges** — one-time or recurring fees for connecting a supermarket's existing point-of-sale system.
- **Enterprise white-label plans** — for larger regional chains wanting a more customized storefront experience (see `09-roadmap.md`, Phase 3).

## 10. Success Metrics

**Phase 0 (MVP) success criteria**

- A customer can complete the full journey — location detection → store discovery → catalogue browse → cart → checkout → order placement → status tracking — without manual intervention, in a single region/city.
- A supermarket can onboard, add products/inventory, and receive, accept, and fulfill an order end-to-end.
- A delivery partner can be assigned an order, complete pickup and delivery OTP flows, and see the completed delivery in their history.
- Zero cross-store cart incidents in testing — the single-supermarket rule holds under concurrent and adversarial test conditions (see `10-testing-strategy.md`).
- Core order-state transitions (`Placed` → ... → `Delivered`) are reliably reflected in real time to customer, store, and delivery partner via WebSocket.

**Post-launch north-star and supporting metrics**

- **North star:** Gross Merchandise Value (GMV) transacted through the platform per active supermarket branch per month.
- Supermarket activation rate (registered → verified → first order fulfilled).
- Customer order completion rate (cart created → order delivered, excluding legitimate cancellations).
- Average time from order placement to "Accepted by Store."
- Average time from "Ready for Pickup" to "Delivered."
- Delivery partner utilization and average earnings per active hour.
- Order defect rate (missing/damaged product reports, refund rate).
- Customer repeat-order rate within 30 days.
- Supermarket net promoter score (NPS) and delivery partner NPS, tracked separately from customer NPS.

## 11. Key Constraints and Principles Carried Through All Docs

- **Supermarkets keep control.** Inventory, pricing, offers, and the customer relationship belong to the supermarket. AisleGo never inserts itself as the merchant of record.
- **One order, one supermarket.** Non-negotiable, enforced at UI, API, and database layers.
- **Server-side is the source of truth for price and stock.** Client-side data is always re-validated at checkout.
- **Everything sensitive is auditable.** Order state changes, refunds, staff actions, and admin overrides are logged immutably.
- **Mobile-first, accessible, and simple.** The primary customer device is a phone; the primary picker/branch-manager device may be a low-spec tablet or phone on the shop floor. Interfaces favor large touch targets and minimal jargon over density.
