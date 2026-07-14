ALTER TABLE delivery_partner_profiles
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    ADD COLUMN rejection_reason TEXT,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN reviewed_by BIGINT REFERENCES users (id);

UPDATE delivery_partner_profiles SET available = FALSE;
CREATE INDEX idx_delivery_partner_profiles_status ON delivery_partner_profiles (status);
