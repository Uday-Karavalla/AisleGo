ALTER TABLE orders
    ADD COLUMN pickup_otp_expires_at TIMESTAMPTZ,
    ADD COLUMN delivery_otp_expires_at TIMESTAMPTZ,
    ADD COLUMN pickup_otp_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN delivery_otp_attempts INTEGER NOT NULL DEFAULT 0;
