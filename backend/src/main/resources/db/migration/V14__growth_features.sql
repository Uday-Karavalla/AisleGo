-- Growth foundations: controlled promotions, referrals, favourites, in-app notifications,
-- and first-party funnel analytics. All additions are backward-compatible with existing data.

ALTER TABLE coupons ADD COLUMN first_order_only BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE coupons ADD COLUMN max_redemptions INTEGER;
ALTER TABLE coupons ADD COLUMN per_user_limit INTEGER;
ALTER TABLE coupons ADD COLUMN assigned_user_id BIGINT REFERENCES users (id) ON DELETE CASCADE;

CREATE TABLE coupon_redemptions (
    id               BIGSERIAL PRIMARY KEY,
    coupon_code      VARCHAR(32) NOT NULL,
    supermarket_id   BIGINT REFERENCES supermarkets (id) ON DELETE SET NULL,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    order_id          BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    redeemed_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_coupon_redemptions_order UNIQUE (order_id)
);
CREATE INDEX idx_coupon_redemptions_code_store ON coupon_redemptions (coupon_code, supermarket_id);
CREATE INDEX idx_coupon_redemptions_user_code ON coupon_redemptions (user_id, coupon_code);

ALTER TABLE users ADD COLUMN referral_code VARCHAR(24);
ALTER TABLE users ADD COLUMN referred_by_user_id BIGINT REFERENCES users (id) ON DELETE SET NULL;
CREATE UNIQUE INDEX uq_users_referral_code ON users (referral_code) WHERE referral_code IS NOT NULL;

CREATE TABLE referral_rewards (
    id                 BIGSERIAL PRIMARY KEY,
    referrer_user_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    referred_user_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status             VARCHAR(24) NOT NULL CHECK (status IN ('PENDING', 'REWARDED')),
    rewarded_at        TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_referral_referred_user UNIQUE (referred_user_id)
);

CREATE TABLE favorite_products (
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, product_id)
);

CREATE TABLE favorite_supermarkets (
    user_id          BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    supermarket_id   BIGINT NOT NULL REFERENCES supermarkets (id) ON DELETE CASCADE,
    created_at       TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, supermarket_id)
);

CREATE TABLE user_notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(160) NOT NULL,
    message     TEXT NOT NULL,
    action_url  VARCHAR(500),
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_user_notifications_user_created ON user_notifications (user_id, created_at DESC);

CREATE TABLE growth_events (
    id          BIGSERIAL PRIMARY KEY,
    event_name  VARCHAR(48) NOT NULL,
    user_id     BIGINT REFERENCES users (id) ON DELETE SET NULL,
    session_id  VARCHAR(80),
    metadata    VARCHAR(1000),
    created_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_growth_events_name_created ON growth_events (event_name, created_at DESC);
