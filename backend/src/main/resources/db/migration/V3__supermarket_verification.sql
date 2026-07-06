-- Supermarket self-registration + admin verification: a supermarket can now be created by
-- its own owner (identity/service/AuthService#registerSupermarketOwner) rather than only via
-- the V2 seed script, so it needs a status an admin can move through PENDING -> VERIFIED/REJECTED
-- before it becomes visible to customer-facing discovery (see StoreDiscoveryService).

ALTER TABLE supermarkets
    ADD COLUMN owner_id BIGINT REFERENCES users(id),
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN reviewed_by BIGINT REFERENCES users(id);

-- Seed data (V2) predates the owner/verification concept - grandfather it in as verified.
UPDATE supermarkets SET status = 'VERIFIED';

-- Dev-only seeded admin account so the workflow is demoable with zero setup, following
-- the same convention as the rest of this demo (mirrors V2's seeding style). Real password
-- is documented in README; hash generated via the same BCryptPasswordEncoder the app uses.
INSERT INTO users (email, password_hash, full_name, enabled, created_at, updated_at, version)
VALUES ('admin@aislego.com', '$2b$10$J1x27APf4lv8ZqkIRK9NkuMB6eoqWBkEObzeK54mhpw0WZYjkv.DW', 'AisleGo Admin', true, now(), now(), 0);
INSERT INTO user_roles (user_id, role) SELECT id, 'ADMIN' FROM users WHERE email = 'admin@aislego.com';
