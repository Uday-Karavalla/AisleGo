-- AisleGo V1: schema for the first working flow
-- (store discovery -> browse catalogue -> add to cart -> checkout -> order placed).
-- Only the tables needed for that flow are created here.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------------
-- catalogue
-- ---------------------------------------------------------------------------

CREATE TABLE supermarkets (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    phone       VARCHAR(32),
    logo_url    VARCHAR(512),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    version     BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE branches (
    id             BIGSERIAL PRIMARY KEY,
    supermarket_id BIGINT NOT NULL REFERENCES supermarkets (id),
    name           VARCHAR(255) NOT NULL,
    address_line   VARCHAR(512),
    city           VARCHAR(255),
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    opening_time   VARCHAR(16),
    closing_time   VARCHAR(16),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0
);

-- Supports the Haversine "nearby stores" query: narrows candidate branches by a
-- coordinate bounding box before the trig distance calculation runs.
CREATE INDEX idx_branches_lat_lng ON branches (latitude, longitude);
CREATE INDEX idx_branches_supermarket ON branches (supermarket_id);

CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id             BIGSERIAL PRIMARY KEY,
    supermarket_id BIGINT NOT NULL REFERENCES supermarkets (id),
    category_id    BIGINT REFERENCES categories (id),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    sku            VARCHAR(64) NOT NULL,
    price_amount   NUMERIC(19, 2) NOT NULL,
    price_currency VARCHAR(3) NOT NULL,
    image_url      VARCHAR(512),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_products_supermarket_sku UNIQUE (supermarket_id, sku)
);

CREATE INDEX idx_products_supermarket ON products (supermarket_id);
-- Postgres-native stand-in for a real search engine (OpenSearch is a later phase):
-- trigram GIN index lets ILIKE '%term%' searches use an index instead of a full scan.
CREATE INDEX idx_products_name_trgm ON products USING gin (name gin_trgm_ops);

-- ---------------------------------------------------------------------------
-- inventory
-- ---------------------------------------------------------------------------

CREATE TABLE inventory (
    id                BIGSERIAL PRIMARY KEY,
    branch_id         BIGINT NOT NULL REFERENCES branches (id),
    product_id        BIGINT NOT NULL REFERENCES products (id),
    quantity_on_hand  INTEGER NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    created_at        TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL,
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_branch_product UNIQUE (branch_id, product_id)
);

CREATE INDEX idx_inventory_branch ON inventory (branch_id);

-- ---------------------------------------------------------------------------
-- identity
-- ---------------------------------------------------------------------------

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(32),
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0
);

-- Role values mirror com.aislego.identity.domain.Role. Only CUSTOMER is exercised by the
-- first working flow; the others exist so the schema doesn't change shape when
-- store-ops/delivery/admin phases are implemented.
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(32) NOT NULL CHECK (role IN (
        'CUSTOMER', 'SUPERMARKET_OWNER', 'BRANCH_MANAGER', 'PICKER', 'DELIVERY_PARTNER', 'ADMIN'
    )),
    PRIMARY KEY (user_id, role)
);

-- ---------------------------------------------------------------------------
-- orders (carts + orders)
-- ---------------------------------------------------------------------------

CREATE TABLE carts (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL UNIQUE REFERENCES users (id),
    supermarket_id BIGINT REFERENCES supermarkets (id),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE cart_items (
    id                   BIGSERIAL PRIMARY KEY,
    cart_id              BIGINT NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id           BIGINT NOT NULL REFERENCES products (id),
    quantity             INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_amount    NUMERIC(19, 2) NOT NULL,
    unit_price_currency  VARCHAR(3) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    version              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id)
);

CREATE INDEX idx_cart_items_cart ON cart_items (cart_id);

-- Defense in depth for the platform's one hard business rule (single supermarket per
-- cart), enforced first in application code (CartService.addItem). If a cart already has
-- a supermarket pinned, no item from a different supermarket's product may be inserted.
CREATE OR REPLACE FUNCTION check_cart_item_single_supermarket() RETURNS TRIGGER AS $$
DECLARE
    v_cart_supermarket_id BIGINT;
    v_product_supermarket_id BIGINT;
BEGIN
    SELECT supermarket_id INTO v_cart_supermarket_id FROM carts WHERE id = NEW.cart_id;
    SELECT supermarket_id INTO v_product_supermarket_id FROM products WHERE id = NEW.product_id;

    IF v_cart_supermarket_id IS NOT NULL AND v_cart_supermarket_id <> v_product_supermarket_id THEN
        RAISE EXCEPTION 'Cart % already contains items from supermarket %, cannot add product % from supermarket %',
            NEW.cart_id, v_cart_supermarket_id, NEW.product_id, v_product_supermarket_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_cart_item_single_supermarket
    BEFORE INSERT OR UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION check_cart_item_single_supermarket();

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users (id),
    supermarket_id   BIGINT NOT NULL REFERENCES supermarkets (id),
    branch_id        BIGINT NOT NULL REFERENCES branches (id),
    status           VARCHAR(32) NOT NULL CHECK (status IN (
        'PLACED', 'PAYMENT_CONFIRMED', 'ACCEPTED_BY_STORE', 'PICKING', 'SUBSTITUTION_APPROVAL',
        'PACKING', 'READY_FOR_PICKUP', 'DELIVERY_PARTNER_ASSIGNED', 'PICKED_UP',
        'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'
    )),
    total_amount     NUMERIC(19, 2) NOT NULL,
    total_currency   VARCHAR(3) NOT NULL,
    idempotency_key  VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    -- Makes checkout retries with the same Idempotency-Key header safe: a repeat insert
    -- for the same user+key collides here instead of creating a duplicate order.
    CONSTRAINT uq_orders_user_idempotency_key UNIQUE (user_id, idempotency_key)
);

CREATE INDEX idx_orders_user ON orders (user_id);
CREATE INDEX idx_orders_supermarket ON orders (supermarket_id);

CREATE TABLE order_items (
    id                    BIGSERIAL PRIMARY KEY,
    order_id              BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id            BIGINT NOT NULL REFERENCES products (id),
    product_name          VARCHAR(255) NOT NULL,
    quantity              INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_amount     NUMERIC(19, 2) NOT NULL,
    unit_price_currency   VARCHAR(3) NOT NULL,
    line_total_amount     NUMERIC(19, 2) NOT NULL,
    line_total_currency   VARCHAR(3) NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    version               BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

-- Same single-supermarket safeguard, mirrored for orders: every order_item's product must
-- belong to the order's one supermarket.
CREATE OR REPLACE FUNCTION check_order_item_single_supermarket() RETURNS TRIGGER AS $$
DECLARE
    v_order_supermarket_id BIGINT;
    v_product_supermarket_id BIGINT;
BEGIN
    SELECT supermarket_id INTO v_order_supermarket_id FROM orders WHERE id = NEW.order_id;
    SELECT supermarket_id INTO v_product_supermarket_id FROM products WHERE id = NEW.product_id;

    IF v_order_supermarket_id <> v_product_supermarket_id THEN
        RAISE EXCEPTION 'Order % belongs to supermarket %, cannot add product % from supermarket %',
            NEW.order_id, v_order_supermarket_id, NEW.product_id, v_product_supermarket_id
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_order_item_single_supermarket
    BEFORE INSERT OR UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION check_order_item_single_supermarket();

-- ---------------------------------------------------------------------------
-- payments
-- ---------------------------------------------------------------------------

-- Gateway reference/status only - never card, UPI or wallet details.
CREATE TABLE payments (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT NOT NULL UNIQUE REFERENCES orders (id),
    gateway_reference  VARCHAR(255) NOT NULL,
    provider           VARCHAR(32) NOT NULL,
    status             VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED')),
    amount_amount      NUMERIC(19, 2) NOT NULL,
    amount_currency    VARCHAR(3) NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    version            BIGINT NOT NULL DEFAULT 0
);
