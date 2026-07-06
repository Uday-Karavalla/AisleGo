-- Saved delivery addresses for customers. The frontend's Checkout page has assumed this
-- feature exists since Phase 0 (`frontend/src/api/addresses.ts`), but no backend support
-- for it was ever built - GET/POST /api/addresses 404'd. This adds the missing piece.

CREATE TABLE addresses (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users (id),
    label        VARCHAR(100) NOT NULL,
    line1        VARCHAR(255) NOT NULL,
    line2        VARCHAR(255),
    city         VARCHAR(255) NOT NULL,
    state        VARCHAR(255) NOT NULL,
    postal_code  VARCHAR(32) NOT NULL,
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL,
    version      BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);
