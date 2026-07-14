ALTER TABLE orders
    ADD COLUMN delivery_partner_id BIGINT REFERENCES delivery_partner_profiles (id);

CREATE INDEX idx_orders_delivery_partner ON orders (delivery_partner_id);
