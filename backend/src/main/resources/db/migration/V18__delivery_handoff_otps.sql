ALTER TABLE orders
    ADD COLUMN pickup_otp_hash VARCHAR(100),
    ADD COLUMN delivery_otp_hash VARCHAR(100);
