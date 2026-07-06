-- Demo seed data so the first working flow (discovery -> browse -> cart -> checkout ->
-- order placed) is testable end to end without building any onboarding UI.
-- Three fictional supermarkets, one branch each with distinct coordinates (Bengaluru
-- area, a few km apart) so the Haversine "nearby stores" query returns different
-- distances depending on the caller's lat/lng.

INSERT INTO supermarkets (name, description, phone, logo_url, active, created_at, updated_at, version) VALUES
    ('FreshMart', 'Neighbourhood supermarket for fruits, vegetables and everyday essentials.', '+91-80-1000-0001', NULL, TRUE, now(), now(), 0),
    ('GreenBasket', 'Organic-friendly grocery store with a focus on dairy and bakery.', '+91-80-1000-0002', NULL, TRUE, now(), now(), 0),
    ('DailyNeeds Superstore', 'Large-format supermarket for household and packaged goods.', '+91-80-1000-0003', NULL, TRUE, now(), now(), 0);

INSERT INTO branches (supermarket_id, name, address_line, city, latitude, longitude, opening_time, closing_time, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'FreshMart'), 'FreshMart Indiranagar', '100 Ft Road, Indiranagar', 'Bengaluru', 12.9716, 77.6412, '07:00', '23:00', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'GreenBasket'), 'GreenBasket Koramangala', '5th Block, Koramangala', 'Bengaluru', 12.9352, 77.6146, '08:00', '22:00', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'DailyNeeds Superstore'), 'DailyNeeds Whitefield', 'ITPL Main Road, Whitefield', 'Bengaluru', 12.9698, 77.7500, '06:00', '23:30', TRUE, now(), now(), 0);

INSERT INTO categories (name, created_at, updated_at, version) VALUES
    ('Fruits & Vegetables', now(), now(), 0),
    ('Dairy & Eggs', now(), now(), 0),
    ('Bakery', now(), now(), 0),
    ('Snacks & Beverages', now(), now(), 0),
    ('Household & Cleaning', now(), now(), 0);

-- FreshMart products
INSERT INTO products (supermarket_id, category_id, name, description, sku, price_amount, price_currency, image_url, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'FreshMart'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Bananas (1 dozen)', 'Fresh ripe bananas', 'FM-001', 60.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'FreshMart'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Tomatoes (1 kg)', 'Farm-fresh tomatoes', 'FM-002', 40.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'FreshMart'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Toned Milk (1 L)', 'Pasteurised toned milk', 'FM-003', 58.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'FreshMart'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Eggs (tray of 12)', 'Farm eggs', 'FM-004', 84.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'FreshMart'), (SELECT id FROM categories WHERE name = 'Snacks & Beverages'), 'Potato Chips (150 g)', 'Salted potato chips', 'FM-005', 45.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'FreshMart'), (SELECT id FROM categories WHERE name = 'Household & Cleaning'), 'Dish Wash Liquid (500 ml)', 'Lemon dish wash liquid', 'FM-006', 99.00, 'INR', NULL, TRUE, now(), now(), 0);

-- GreenBasket products
INSERT INTO products (supermarket_id, category_id, name, description, sku, price_amount, price_currency, image_url, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'GreenBasket'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Organic Whole Milk (1 L)', 'Certified organic whole milk', 'GB-001', 72.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'GreenBasket'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Greek Yogurt (400 g)', 'Thick set greek yogurt', 'GB-002', 110.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'GreenBasket'), (SELECT id FROM categories WHERE name = 'Bakery'), 'Multigrain Bread', 'Freshly baked multigrain loaf', 'GB-003', 65.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'GreenBasket'), (SELECT id FROM categories WHERE name = 'Bakery'), 'Butter Croissant (pack of 2)', 'Baked fresh daily', 'GB-004', 90.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'GreenBasket'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Organic Spinach (250 g)', 'Pesticide-free spinach', 'GB-005', 35.00, 'INR', NULL, TRUE, now(), now(), 0);

-- DailyNeeds Superstore products
INSERT INTO products (supermarket_id, category_id, name, description, sku, price_amount, price_currency, image_url, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'DailyNeeds Superstore'), (SELECT id FROM categories WHERE name = 'Snacks & Beverages'), 'Cola (2 L bottle)', 'Carbonated soft drink', 'DN-001', 95.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'DailyNeeds Superstore'), (SELECT id FROM categories WHERE name = 'Snacks & Beverages'), 'Instant Noodles (pack of 4)', 'Masala instant noodles', 'DN-002', 56.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'DailyNeeds Superstore'), (SELECT id FROM categories WHERE name = 'Household & Cleaning'), 'Laundry Detergent (1 kg)', 'Front and top load detergent powder', 'DN-003', 180.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'DailyNeeds Superstore'), (SELECT id FROM categories WHERE name = 'Household & Cleaning'), 'Toilet Paper (pack of 4)', '2-ply soft toilet rolls', 'DN-004', 150.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'DailyNeeds Superstore'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Onions (1 kg)', 'Fresh red onions', 'DN-005', 32.00, 'INR', NULL, TRUE, now(), now(), 0);

-- Inventory: stock each product at its own supermarket's single branch.
INSERT INTO inventory (branch_id, product_id, quantity_on_hand, created_at, updated_at, version)
SELECT b.id, p.id, 100, now(), now(), 0
FROM products p
JOIN branches b ON b.supermarket_id = p.supermarket_id;

-- Give one FreshMart product a low stock count so the insufficient-stock path is easy to
-- exercise manually against the seed data.
UPDATE inventory SET quantity_on_hand = 2
WHERE product_id = (SELECT id FROM products WHERE sku = 'FM-004');
