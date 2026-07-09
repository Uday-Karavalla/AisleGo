-- Store reviews: a customer can rate/review a supermarket once they have a delivered order
-- from it (enforced in ReviewService, not here - the DB has no view of order history at
-- constraint-check time). One review per (user, supermarket), editable in place - upserted
-- rather than accumulating a new row per edit.

CREATE TABLE reviews (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users (id),
    supermarket_id BIGINT NOT NULL REFERENCES supermarkets (id),
    rating         INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment        TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_reviews_user_supermarket UNIQUE (user_id, supermarket_id)
);

CREATE INDEX idx_reviews_supermarket_id ON reviews (supermarket_id);
