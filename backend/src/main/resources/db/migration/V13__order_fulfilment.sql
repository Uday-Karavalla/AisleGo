ALTER TABLE orders
    ADD COLUMN fulfilment_type VARCHAR(16) NOT NULL DEFAULT 'IMMEDIATE',
    ADD COLUMN scheduled_for TIMESTAMPTZ;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_fulfilment_type
        CHECK (fulfilment_type IN ('IMMEDIATE', 'SCHEDULED', 'PICKUP'));
