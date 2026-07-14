ALTER TABLE delivery_partner_profiles
    ADD COLUMN last_latitude DOUBLE PRECISION,
    ADD COLUMN last_longitude DOUBLE PRECISION,
    ADD COLUMN location_updated_at TIMESTAMPTZ;
