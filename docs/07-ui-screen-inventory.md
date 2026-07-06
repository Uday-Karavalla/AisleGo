# AisleGo — UI Screen Inventory & Wireframes

**Related docs:** `03-user-journeys.md`, `01-PRD.md` (accessibility/system requirements)

All customer- and delivery-partner-facing surfaces ship as a mobile-first, installable PWA. Supermarket and admin surfaces ship as responsive web dashboards, usable on tablet and desktop, since branch/back-office staff commonly work from larger or shared shop-floor devices.

---

## 1. Screen Inventory Per Role

### 1.1 Customer

| Screen | Purpose |
|---|---|
| Onboarding / Login | Register, log in, OTP verification |
| Home / Location | Detect or set delivery location; entry point to discovery |
| Store Discovery | List of nearby serviceable supermarkets with filters/sort |
| Storefront / Catalogue | Browse a specific store's products, search, filter by category |
| Product Detail | Price, stock, offers, substitution preference, add to cart |
| Cart | Review items, quantities, apply coupon |
| Checkout | Address, fulfillment type (immediate/scheduled/pickup), payment method |
| Order Confirmation | Order placed summary and order ID |
| Order Tracking | Live status timeline, delivery partner map, call store/partner |
| Order History | Past orders, repeat-order action, invoice download |
| Order Detail (post-delivery) | Ratings, report missing/damaged item, request refund |
| Favourites | Saved products and stores |
| Profile & Addresses | Manage profile, saved delivery addresses |
| Loyalty | Points balance, redemption, transaction history |

### 1.2 Supermarket Owner

| Screen | Purpose |
|---|---|
| Registration & Verification | Submit business documents, track verification status |
| Owner Dashboard | Cross-branch summary: orders, sales, alerts |
| Branch Management | Create/edit branches, hours, delivery area |
| Product Catalogue Manager | Add/edit products, barcode scan, CSV import |
| Pricing & Offers | Manage prices, coupons, bundles |
| Staff Management | Invite/manage branch managers and pickers, permissions |
| Settlements & Analytics | Payouts, commission breakdown, sales/customer analytics |
| Store Settings | Payment methods accepted, policies (returns, substitutions) |

### 1.3 Branch Manager

| Screen | Purpose |
|---|---|
| Branch Dashboard | Live queue of incoming/active orders for this branch |
| Order Detail (branch view) | Accept/reject, assign staff, monitor picking/packing |
| Inventory Manager | View/adjust branch stock levels, reorder alerts |
| Substitution Approvals | Queue of substitution requests awaiting decision |
| Staff Roster | Manage pickers assigned to this branch |
| Branch Analytics | Branch-level sales and fulfillment performance |

### 1.4 Store Employee / Picker

| Screen | Purpose |
|---|---|
| Picker Task List | Orders assigned to this picker |
| Picking List (order detail) | Line items to pick, mark picked/OOS, suggest substitute |
| Packing Confirmation | Final check, mark ready for pickup |

### 1.5 Delivery Partner

| Screen | Purpose |
|---|---|
| Registration & Verification | Submit documents, track verification status |
| Availability Toggle | Go online/offline |
| Opportunity Feed | Nearby available assignments with payout estimate |
| Active Delivery — Navigate to Store | Map/route to branch |
| Pickup Confirmation | Enter/scan pickup OTP/QR |
| Active Delivery — Navigate to Customer | Map/route to delivery address, live location sharing |
| Delivery Confirmation | Enter delivery OTP |
| Earnings & History | Completed deliveries, incentives, payout statements |

### 1.6 Platform Administrator

| Screen | Purpose |
|---|---|
| Admin Dashboard | Platform-wide KPIs, alerts, pending queues |
| Supermarket Verification Queue | Review/approve/reject pending stores |
| Delivery Partner Verification Queue | Review/approve/reject pending partners |
| Dispute & Refund Console | Investigate and resolve disputes, approve refunds |
| Category Management | Global product taxonomy |
| Commission & Subscription Config | Fee schedules, plan tiers |
| Promotions Manager | Platform-wide featured placements |
| Platform Analytics | GMV, order volume, store/delivery performance |
| Audit Log Viewer | Searchable, filterable audit trail |
| Account Management | Suspend/reinstate accounts |

---

## 2. Wireframes — Customer Flow (Mobile-First, ASCII)

### 2.1 Home / Location

```
+-------------------------------+
|  AisleGo              [ (o) ] |  <- profile icon
+-------------------------------+
|                                |
|   [ (pin) Your location ]     |
|   12 MG Road, Sector 4    [v] |
|   -------------------------   |
|   [ Use current location ]    |
|                                |
|   -------------------------   |
|   Search stores or products   |
|   [ (search) ................]|
|                                |
|   Recently ordered from:      |
|   ( logo ) Green Valley        |
|   ( logo ) FreshMart Express   |
|                                |
+-------------------------------+
| [Home] [Orders] [Fav] [Profile]|
+-------------------------------+
```

### 2.2 Store Discovery List

```
+-------------------------------+
|  < Back    Stores near you    |
+-------------------------------+
| Filter: [Open Now v][Rating v]|
+-------------------------------+
| +----------------------------+|
| | (logo) Green Valley         ||
| | Supermarket        4.5 *    ||
| | 1.8 km  |  Delivers in 35m  ||
| | [ OPEN ]                    ||
| +----------------------------+|
| +----------------------------+|
| | (logo) FreshMart Express    ||
| |                     4.2 *   ||
| | 2.4 km  |  Delivers in 45m  ||
| | [ OPEN ]                    ||
| +----------------------------+|
| +----------------------------+|
| | (logo) City Grocers         ||
| |                     3.9 *   ||
| | 3.1 km  |  Opens at 9:00 AM ||
| | [ CLOSED ]                  ||
| +----------------------------+|
+-------------------------------+
```

### 2.3 Storefront / Catalogue (Search + Filter)

```
+-------------------------------+
| < Back   Green Valley  (call) |
| 4.5 *  |  OPEN  |  ETA 35 min |
+-------------------------------+
| [ (search) Search products.. ]|
| [All][Dairy][Produce][Snacks] |
+-------------------------------+
| +--------+ +--------+         |
| | [img]  | | [img]  |         |
| | Milk   | | Bread  |         |
| | 500ml  | | 400g   |         |
| | Rs 28  | | Rs 40  |         |
| | [ Add ]| | [ Add ]|         |
| +--------+ +--------+         |
| +--------+ +--------+         |
| | [img]  | | [img]  |         |
| | Eggs   | | Butter |         |
| | 6-pack | | 100g   |         |
| | Rs 55  | | Rs 60  |         |
| | [ Add ]| | [ Add ]|         |
| +--------+ +--------+         |
+-------------------------------+
|            [ View Cart (3) ]  |
+-------------------------------+
```

### 2.4 Product Detail / Add to Cart

```
+-------------------------------+
| < Back                        |
|                                |
|         [   product image  ]  |
|                                |
|  Amul Toned Milk 500ml         |
|  Rs 28.00      [In Stock]     |
|  ------------------------      |
|  Offer: Buy 2 Get 5% Off       |
|                                |
|  Qty:   [ - ]   2   [ + ]     |
|                                |
|  [x] Allow substitution if     |
|      unavailable               |
|                                |
|  [        Add to Cart        ]|
+-------------------------------+
```

### 2.5 Cart (with cross-store guard)

```
+-------------------------------+
| < Back        Your Cart       |
|  Green Valley Supermarket     |
+-------------------------------+
| Milk 500ml       x2   Rs 56   |
| Bread 400g       x1   Rs 40   |
| Eggs 6-pack      x1   Rs 55   |
+-------------------------------+
| Coupon: [ Enter code   ][Apply]|
+-------------------------------+
| Subtotal          Rs 151.00   |
| Discount          -Rs 10.00   |
| Delivery Fee       Rs 20.00   |
| Total             Rs 161.00   |
+-------------------------------+
|      [   Proceed to Checkout ]|
+-------------------------------+

  ---- if customer tries adding from another store ----
+-------------------------------+
|  (!) Cart has items from      |
|  Green Valley Supermarket.    |
|                                |
|  Adding from FreshMart Express |
|  requires starting a new cart.|
|                                |
|  [ Clear Cart & Add ] [Cancel]|
+-------------------------------+
```

### 2.6 Checkout (Address + Fulfillment + Payment)

```
+-------------------------------+
| < Back        Checkout        |
+-------------------------------+
| Deliver to:                    |
| ( ) Home - 12 MG Road          |
| ( ) Office - Tower B           |
| [ + Add new address ]          |
+-------------------------------+
| Fulfillment:                   |
| (o) Deliver Now  (~35 min)     |
| ( ) Schedule for later         |
| ( ) Self Pickup                |
+-------------------------------+
| Payment:                       |
| (o) Card / UPI                 |
| ( ) Cash on Delivery            |
+-------------------------------+
| Order Total:       Rs 161.00  |
|      [     Place Order       ]|
+-------------------------------+
```

### 2.7 Order Tracking

```
+-------------------------------+
| < Back      Order #A1029      |
+-------------------------------+
|  Green Valley Supermarket      |
|  (call store)   (call partner) |
+-------------------------------+
|  [x] Placed                    |
|  [x] Payment Confirmed         |
|  [x] Accepted by Store         |
|  [x] Picking                   |
|  [x] Packed - Ready for Pickup |
|  [x] Partner Assigned          |
|  [ ] Out for Delivery          |
|  [ ] Delivered                 |
+-------------------------------+
|      [   live map view    ]    |
|      partner is 1.2 km away    |
+-------------------------------+
|  Delivery OTP:   4 8 2 1       |
|  (share with delivery partner) |
+-------------------------------+
```

---

## 3. Accessibility & System-Wide UI Notes

- **Large touch targets** on all primary actions (Add to Cart, Place Order, Accept/Reject) — minimum 44x44px tap area, per the PRD's accessibility requirement.
- **Iconography paired with text labels**, never icon-only, for less-technical users (e.g. "Cash on Delivery" with a cash icon, not just the icon).
- **Status is always shown as both color and text/icon** (e.g. "OPEN"/"CLOSED" badges, checklist-style order timeline) so status is never conveyed by color alone.
- **Offline/low-connectivity tolerance:** cart state and in-progress order tracking are cached client-side (PWA service worker) so a flaky connection on a shop floor or during delivery doesn't lose in-progress work.
- **Branch/picker-facing screens favor list-and-checklist patterns** over dense dashboards, since these are used quickly, standing up, often on shared devices.
