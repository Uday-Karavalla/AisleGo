-- Real coupon codes: the cart page has had a coupon input since early on, but it was always
-- UI decoration - any non-empty code applied a hardcoded flat discount client-side, nothing
-- was validated or persisted server-side. This adds the real thing: a coupon is either
-- platform-wide (supermarket_id NULL, created by an admin) or scoped to one store (created by
-- that store's owner), a percentage or flat amount off, with an optional expiry.

CREATE TABLE coupons (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(32) NOT NULL,
    supermarket_id       BIGINT REFERENCES supermarkets (id),
    discount_type        VARCHAR(16) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FLAT')),
    percent_off          INTEGER CHECK (percent_off IS NULL OR (percent_off BETWEEN 1 AND 100)),
    amount_off           NUMERIC(19, 2),
    amount_off_currency  VARCHAR(3),
    expires_at           TIMESTAMPTZ,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL,
    updated_at           TIMESTAMPTZ NOT NULL,
    version              BIGINT NOT NULL DEFAULT 0
);

-- A code is unique within its own scope: two different stores (or one store and the platform)
-- can each have their own "SAVE10" without colliding. COALESCE'd to 0 rather than using
-- Postgres 15+'s `NULLS NOT DISTINCT` syntax, to stay compatible with older Postgres too.
CREATE UNIQUE INDEX uq_coupons_code_scope ON coupons (UPPER(code), COALESCE(supermarket_id, 0));

-- A cart/order stores the coupon *code*, not a foreign key to the coupon row - same
-- "snapshot, don't hard-reference" pattern already used for orders.delivery_address and
-- order_items.product_name. This means deleting a coupon later is always safe (no FK to
-- violate), and an expired/deleted coupon still shown as "applied" on an old cart just quietly
-- stops discounting rather than blocking anything - see CartService#toResponse.
ALTER TABLE carts ADD COLUMN coupon_code VARCHAR(32);

ALTER TABLE orders ADD COLUMN coupon_code VARCHAR(32);
ALTER TABLE orders ADD COLUMN discount_amount NUMERIC(19, 2) NOT NULL DEFAULT 0;
