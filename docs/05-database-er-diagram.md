# AisleGo — Database ER Diagram

**Related docs:** `04-architecture.md` (module/schema ownership), `08-security-and-fraud-control.md` (tenant isolation)

---

## 1. Design Principles

- **PostgreSQL is the single system of record for Phase 0.** Tables are grouped into logical schemas that mirror module ownership (`identity`, `catalogue`, `inventory`, `orders`, `payments`, `delivery`, `loyalty`, `admin`), even though they live in one physical database/instance for now — this makes a future extraction (see `04-architecture.md` §5) a schema-boundary move rather than a redesign.
- **Every row that belongs to a supermarket carries a `supermarket_id`** (directly or transitively through `branch_id`), enabling row-level tenant scoping everywhere (see `08-security-and-fraud-control.md` §8).
- **The single-supermarket-per-order rule is modeled structurally**, not left to application discipline alone (see §3 below).
- **Money is stored as integer minor units** (e.g. paise/cents) to avoid floating-point rounding issues, with a `currency` column reserved for future multi-currency support even though Phase 0 is single-currency.
- **All primary keys are UUIDs** generated application-side or via `gen_random_uuid()`, to avoid exposing sequential IDs across tenant boundaries and to simplify future horizontal partitioning.

## 2. Entity-Relationship Diagram

```mermaid
erDiagram
    SUPERMARKET ||--o{ BRANCH : operates
    SUPERMARKET ||--o{ STAFF_ACCOUNT : employs
    SUPERMARKET ||--o{ COUPON : issues
    BRANCH ||--o{ INVENTORY : stocks
    BRANCH ||--o{ STAFF_ACCOUNT : "assigned to"
    BRANCH ||--o{ ORDER : fulfills

    CATEGORY ||--o{ CATEGORY : "parent of"
    CATEGORY ||--o{ PRODUCT : classifies
    SUPERMARKET ||--o{ PRODUCT : lists
    PRODUCT ||--o{ INVENTORY : "stocked as"
    PRODUCT ||--o{ CART_ITEM : "added as"
    PRODUCT ||--o{ ORDER_ITEM : "ordered as"
    PRODUCT ||--o{ REVIEW : receives

    CUSTOMER ||--o{ ADDRESS : owns
    CUSTOMER ||--o| CART : has
    CUSTOMER ||--o{ ORDER : places
    CUSTOMER ||--o{ REVIEW : writes
    CUSTOMER ||--|| LOYALTY_ACCOUNT : has

    CART ||--o{ CART_ITEM : contains
    CART }o--o| SUPERMARKET : "scoped to (nullable until first item added)"

    ORDER ||--|{ ORDER_ITEM : contains
    ORDER }o--|| SUPERMARKET : "belongs to exactly one"
    ORDER }o--|| BRANCH : "fulfilled by"
    ORDER ||--o| PAYMENT : "paid via"
    ORDER ||--o| DELIVERY : "delivered via"
    ORDER ||--o{ REVIEW : "reviewed after"
    ORDER }o--o| COUPON : "may apply"
    ORDER ||--o{ LOYALTY_TRANSACTION : "generates"

    ORDER_ITEM }o--|| PRODUCT : references

    DELIVERY_PARTNER ||--o{ DELIVERY : performs
    DELIVERY ||--o| ADDRESS : "delivers to"

    STAFF_ACCOUNT }o--|| BRANCH : "scoped to"
    STAFF_ACCOUNT }o--|| SUPERMARKET : "employed by"

    LOYALTY_ACCOUNT ||--o{ LOYALTY_TRANSACTION : records

    SUPERMARKET {
        uuid id PK
        string legal_name
        string display_name
        string contact_phone
        string tax_id
        enum verification_status "PENDING, VERIFIED, REJECTED, SUSPENDED"
        enum subscription_tier
        timestamptz created_at
    }

    BRANCH {
        uuid id PK
        uuid supermarket_id FK
        string name
        string address_line
        geography location "lat/long point"
        jsonb delivery_area "polygon or radius definition"
        jsonb operating_hours
        boolean is_active
        timestamptz created_at
    }

    CATEGORY {
        uuid id PK
        uuid parent_category_id FK "nullable, self-referencing"
        string name
        string slug
        int sort_order
    }

    PRODUCT {
        uuid id PK
        uuid supermarket_id FK
        uuid category_id FK
        string sku
        string barcode
        string name
        string unit "e.g. 500g, 1L, each"
        int base_price_minor
        string currency
        boolean is_active
        string image_url
        timestamptz created_at
    }

    INVENTORY {
        uuid id PK
        uuid branch_id FK
        uuid product_id FK
        int stock_quantity
        int reserved_quantity
        int reorder_threshold
        int version "optimistic lock"
        timestamptz updated_at
    }

    CUSTOMER {
        uuid id PK
        string full_name
        string phone
        string email
        timestamptz created_at
        boolean is_suspended
    }

    ADDRESS {
        uuid id PK
        uuid customer_id FK
        string label
        string address_line
        geography location
        boolean is_default
    }

    CART {
        uuid id PK
        uuid customer_id FK
        uuid supermarket_id FK "NULLABLE — set on first item added, enforces single-store cart"
        timestamptz updated_at
    }

    CART_ITEM {
        uuid id PK
        uuid cart_id FK
        uuid product_id FK
        int quantity
        boolean allow_substitution
    }

    ORDER {
        uuid id PK
        uuid customer_id FK
        uuid supermarket_id FK "NOT NULL — single-supermarket rule anchor"
        uuid branch_id FK "NOT NULL"
        uuid coupon_id FK "nullable"
        enum status "PLACED, PAYMENT_CONFIRMED, ACCEPTED, PICKING, SUBSTITUTION_PENDING, PACKING, READY_FOR_PICKUP, PARTNER_ASSIGNED, PICKED_UP, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, REFUNDED"
        enum fulfillment_type "IMMEDIATE, SCHEDULED, PICKUP"
        timestamptz scheduled_for "nullable"
        int subtotal_minor
        int discount_minor
        int delivery_fee_minor
        int total_minor
        string idempotency_key UK
        timestamptz created_at
        timestamptz updated_at
    }

    ORDER_ITEM {
        uuid id PK
        uuid order_id FK
        uuid product_id FK
        string product_name_snapshot
        int unit_price_minor_snapshot
        int quantity
        boolean substituted
        uuid substituted_product_id "nullable"
        enum line_status "PENDING, PICKED, SUBSTITUTED, UNAVAILABLE, REFUNDED"
    }

    PAYMENT {
        uuid id PK
        uuid order_id FK
        string gateway_payment_intent_id
        string gateway_token_reference "no PAN/CVV ever stored"
        enum method "CARD, UPI, WALLET, COD"
        enum status "INITIATED, AUTHORIZED, CAPTURED, FAILED, REFUNDED, PARTIALLY_REFUNDED"
        int amount_minor
        string idempotency_key UK
        timestamptz created_at
    }

    DELIVERY_PARTNER {
        uuid id PK
        string full_name
        string phone
        enum verification_status
        enum availability_status "ONLINE, OFFLINE, ON_DELIVERY"
        geography last_known_location
        timestamptz created_at
    }

    DELIVERY {
        uuid id PK
        uuid order_id FK UK
        uuid delivery_partner_id FK "nullable until assigned"
        string pickup_otp_hash
        timestamptz pickup_otp_expires_at
        string delivery_otp_hash
        timestamptz delivery_otp_expires_at
        enum status "AWAITING_ASSIGNMENT, ASSIGNED, PICKED_UP, EN_ROUTE, DELIVERED, FAILED"
        timestamptz picked_up_at
        timestamptz delivered_at
        int payout_minor
    }

    COUPON {
        uuid id PK
        uuid supermarket_id FK "nullable — null means platform-wide promotion"
        string code UK
        enum discount_type "PERCENTAGE, FLAT"
        int discount_value
        int min_order_value_minor
        timestamptz valid_from
        timestamptz valid_until
        int usage_limit
        int usage_count
    }

    LOYALTY_ACCOUNT {
        uuid id PK
        uuid customer_id FK UK
        int points_balance
        timestamptz updated_at
    }

    LOYALTY_TRANSACTION {
        uuid id PK
        uuid loyalty_account_id FK
        uuid order_id FK "nullable"
        enum type "EARN, REDEEM, EXPIRE, ADJUSTMENT"
        int points
        timestamptz created_at
    }

    REVIEW {
        uuid id PK
        uuid customer_id FK
        uuid order_id FK
        uuid supermarket_id FK
        uuid product_id FK "nullable — product-level review"
        enum review_type "STORE, PRODUCT, DELIVERY"
        int rating "1-5"
        string comment
        timestamptz created_at
    }

    STAFF_ACCOUNT {
        uuid id PK
        uuid supermarket_id FK
        uuid branch_id FK "nullable for owner-level staff"
        string full_name
        string email
        enum role "OWNER, BRANCH_MANAGER, PICKER"
        boolean is_active
        timestamptz created_at
    }

    AUDIT_LOG {
        uuid id PK
        uuid actor_id "nullable for system-initiated"
        string actor_role
        string action
        string entity_type
        uuid entity_id
        jsonb before_state
        jsonb after_state
        string ip_address
        timestamptz created_at
    }
```

## 3. Enforcing the Single-Supermarket-Per-Order Rule

This is the most structurally important constraint in the schema and is enforced at three layers, deliberately redundant:

1. **Column-level:** `orders.supermarket_id` and `orders.branch_id` are `NOT NULL`. An order without a resolved single supermarket cannot be persisted at all.
2. **Application-level (primary enforcement, source of truth for user-facing behavior):** the Orders module's `AddToCart` use case checks, before inserting a `cart_item`, whether the target `cart.supermarket_id` is `NULL` (empty cart — set it to the new product's `supermarket_id`) or already equal to the new product's `supermarket_id` (allowed) or different (rejected with a `409 Conflict`, see `06-api-specification.md`). The same check is re-run at checkout as a defense-in-depth guard immediately before order creation, inside the same DB transaction as inventory reservation.
3. **Database-level (defense in depth against bugs/bypass, not the primary UX mechanism):** a `BEFORE INSERT/UPDATE` trigger on `order_items` re-derives the owning supermarket of `NEW.product_id` (via a join to `products`) and raises an exception if it does not match `orders.supermarket_id` for `NEW.order_id`. Illustrative definition:

```sql
CREATE OR REPLACE FUNCTION enforce_single_supermarket_order()
RETURNS TRIGGER AS $$
DECLARE
  order_supermarket_id UUID;
  product_supermarket_id UUID;
BEGIN
  SELECT supermarket_id INTO order_supermarket_id FROM orders WHERE id = NEW.order_id;
  SELECT supermarket_id INTO product_supermarket_id FROM products WHERE id = NEW.product_id;

  IF product_supermarket_id IS DISTINCT FROM order_supermarket_id THEN
    RAISE EXCEPTION
      'order_item product % belongs to supermarket % but order % belongs to supermarket %',
      NEW.product_id, product_supermarket_id, NEW.order_id, order_supermarket_id
      USING ERRCODE = 'check_violation';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enforce_single_supermarket_order
BEFORE INSERT OR UPDATE ON order_items
FOR EACH ROW EXECUTE FUNCTION enforce_single_supermarket_order();
```

The same pattern (nullable `supermarket_id` on `cart`, populated on first item, checked on every subsequent add) applies to `cart`/`cart_items` so that the rule is felt as early as possible in the customer's flow rather than only surfacing as a checkout-time failure.

## 4. Notes on Key Entities

**`SUPERMARKET` / `BRANCH`** — a supermarket is the business entity (one legal/tax identity, one owner account); a branch is a physical location with its own address, delivery area (`delivery_area` as a polygon or radius, used by the catalogue/discovery query to determine serviceability), operating hours, and independently-managed inventory. `verification_status` gates whether a supermarket appears in customer discovery at all.

**`PRODUCT` / `CATEGORY` / `INVENTORY`** — product metadata (name, price, images, category) is owned by the supermarket and shared across its branches; `INVENTORY` is the per-branch join that tracks actual stock, with `reserved_quantity` tracking units held against in-flight, unconfirmed orders (see `08-security-and-fraud-control.md` §4) and a `version` column supporting optimistic-locking updates under concurrent checkout load. `CATEGORY` is self-referencing to support a shallow parent/child taxonomy (e.g. "Dairy" → "Milk").

**`CART` / `CART_ITEM`** — a customer has at most one active cart at a time in Phase 0 (simplifies the single-store rule enforcement); `CART.supermarket_id` is nullable specifically to represent the "empty cart, not yet scoped to a store" state, and becomes immutable once the first item is added until the cart is cleared.

**`ORDER` / `ORDER_ITEM`** — `ORDER` carries denormalized snapshots on `ORDER_ITEM` (`product_name_snapshot`, `unit_price_minor_snapshot`) so that historical orders and invoices remain accurate even if the product's live name or price later changes. `idempotency_key` on `ORDER` (and separately on `PAYMENT`) ensures that a retried checkout request (e.g. due to client-side network timeout) cannot create duplicate orders/charges — see `08-security-and-fraud-control.md` §5.

**`PAYMENT`** — deliberately minimal: only gateway references and status, never card/PAN/CVV data. `gateway_token_reference` points to a tokenized payment method held by the gateway, not by AisleGo.

**`DELIVERY`** — `pickup_otp_hash` and `delivery_otp_hash` store only a hash of the OTP (never plaintext) alongside an expiry timestamp, mirroring how passwords are stored; the plaintext OTP exists only transiently at generation/display time. `delivery_partner_id` is nullable to represent orders still `AWAITING_ASSIGNMENT`.

**`COUPON`** — `supermarket_id` is nullable to distinguish a store-specific offer from a platform-wide promotion (managed by platform administrators); coupon validity is always re-checked server-side against the specific order's supermarket, value, and category eligibility at apply-time and again at checkout-time.

**`LOYALTY_ACCOUNT` / `LOYALTY_TRANSACTION`** — one loyalty account per customer, platform-wide (not per-supermarket), with an append-only transaction ledger (`EARN`/`REDEEM`/`EXPIRE`/`ADJUSTMENT`) so the running `points_balance` is always reconstructable and auditable.

**`REVIEW`** — a single table with a `review_type` discriminator (`STORE`/`PRODUCT`/`DELIVERY`) rather than three separate tables, since all three share the same shape (rating + comment tied to a customer and an order) and are naturally queried together per order.

**`STAFF_ACCOUNT`** — models supermarket owner, branch manager, and picker roles in one table with a `role` enum and a nullable `branch_id` (owner-level staff are supermarket-scoped, not branch-scoped; branch managers and pickers require a `branch_id`). This mirrors the scope model in `02-roles-and-permissions.md`.

**`AUDIT_LOG`** — an append-only, platform-wide table capturing `before_state`/`after_state` as JSONB snapshots for any sensitive mutation (order status changes, refunds, staff permission changes, admin overrides). Never updated or deleted, only inserted — see `08-security-and-fraud-control.md` §5 for retention policy and what specifically gets logged.
