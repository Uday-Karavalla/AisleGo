-- The V2 seed data is all Bengaluru-area (~12.9-13.0N, 77.6-77.75E), so anyone testing
-- discovery from a real-world location outside that pocket (e.g. via actual browser
-- geolocation rather than a spoofed/overridden one) sees "no supermarkets nearby" even
-- though the app is working correctly - there's just no seeded store near them. This adds
-- one more fictional supermarket near Madanapalle, Andhra Pradesh (~13.63N, 78.48E) so the
-- nearby-stores flow is demoable without needing to override browser location at all.

-- status defaults to PENDING (see V3) for self-registered stores, but this is seed data like
-- V2's - grandfather it straight in as VERIFIED so it's visible to discovery immediately.
INSERT INTO supermarkets (name, description, phone, logo_url, active, status, created_at, updated_at, version) VALUES
    ('ValueMart', 'Neighbourhood supermarket for everyday groceries and household essentials.', '+91-85-7100-0001', NULL, TRUE, 'VERIFIED', now(), now(), 0);

INSERT INTO branches (supermarket_id, name, address_line, city, latitude, longitude, opening_time, closing_time, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'ValueMart'), 'ValueMart Madanapalle', 'Chittoor Road, Madanapalle', 'Madanapalle', 13.6293, 78.4747, '07:30', '22:30', TRUE, now(), now(), 0);

INSERT INTO products (supermarket_id, category_id, name, description, sku, price_amount, price_currency, image_url, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'ValueMart'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Bananas (1 dozen)', 'Fresh ripe bananas', 'VM-001', 55.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'ValueMart'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Onions (1 kg)', 'Fresh red onions', 'VM-002', 30.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'ValueMart'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Toned Milk (1 L)', 'Pasteurised toned milk', 'VM-003', 56.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'ValueMart'), (SELECT id FROM categories WHERE name = 'Snacks & Beverages'), 'Instant Noodles (pack of 4)', 'Masala instant noodles', 'VM-004', 54.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'ValueMart'), (SELECT id FROM categories WHERE name = 'Household & Cleaning'), 'Laundry Detergent (1 kg)', 'Front and top load detergent powder', 'VM-005', 175.00, 'INR', NULL, TRUE, now(), now(), 0);

INSERT INTO inventory (branch_id, product_id, quantity_on_hand, created_at, updated_at, version)
SELECT b.id, p.id, 100, now(), now(), 0
FROM products p
JOIN branches b ON b.supermarket_id = p.supermarket_id
WHERE p.supermarket_id = (SELECT id FROM supermarkets WHERE name = 'ValueMart');
