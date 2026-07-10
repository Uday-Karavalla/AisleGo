-- Four reference supermarkets near Madanapalle (same area as V4's ValueMart, ~13.6-13.65N,
-- 78.46-78.48E) so a demo/tester sees real variety in nearby-store discovery instead of just
-- one store. Grandfathered straight in as VERIFIED (like V2/V4) since these are seed data, not
-- self-registered stores awaiting admin review. A couple of products reuse the real photos
-- already served from the frontend's public/images/ folder so these reference stores show
-- actual images instead of blank placeholders.

INSERT INTO supermarkets (name, description, phone, logo_url, active, status, created_at, updated_at, version) VALUES
    ('Sunrise Supermarket', 'Full-service neighbourhood supermarket - groceries, fresh produce and household essentials under one roof.', '+91-85-7100-1001', NULL, TRUE, 'VERIFIED', now(), now(), 0),
    ('Green Valley Organics', 'Organic and health-focused grocery store - pesticide-free produce, natural dairy and whole-grain staples.', '+91-85-7100-2002', NULL, TRUE, 'VERIFIED', now(), now(), 0),
    ('QuickBasket Express', 'Quick-commerce convenience store for everyday essentials, snacks and last-minute top-ups.', '+91-85-7100-3003', NULL, TRUE, 'VERIFIED', now(), now(), 0),
    ('Metro Grocery Hub', 'Large-format hypermarket with bulk groceries, household supplies and a wide bakery/snacks range.', '+91-85-7100-4004', NULL, TRUE, 'VERIFIED', now(), now(), 0);

INSERT INTO branches (supermarket_id, name, address_line, city, latitude, longitude, opening_time, closing_time, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'Sunrise Supermarket'), 'Sunrise Supermarket Main', 'Gandhi Road, Madanapalle', 'Madanapalle', 13.6350, 78.4700, '07:00', '22:00', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Green Valley Organics'), 'Green Valley Organics Main', 'Nehru Nagar, Madanapalle', 'Madanapalle', 13.6250, 78.4800, '08:00', '21:00', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'QuickBasket Express'), 'QuickBasket Express Main', 'Bus Stand Road, Madanapalle', 'Madanapalle', 13.6400, 78.4780, '06:00', '23:30', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Metro Grocery Hub'), 'Metro Grocery Hub Main', 'Ring Road, Madanapalle', 'Madanapalle', 13.6200, 78.4650, '09:00', '21:30', TRUE, now(), now(), 0);

-- Sunrise Supermarket - general grocery
INSERT INTO products (supermarket_id, category_id, name, description, sku, price_amount, price_currency, image_url, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'Sunrise Supermarket'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Mixed Vegetables Basket (2 kg)', 'Seasonal fresh vegetables', 'SS-001', 120.00, 'INR', '/images/vegetables.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Sunrise Supermarket'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Apples (1 kg)', 'Crisp red apples', 'SS-002', 180.00, 'INR', '/images/fruit.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Sunrise Supermarket'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Full Cream Milk (1 L)', 'Fresh full cream milk', 'SS-003', 62.00, 'INR', '/images/milk.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Sunrise Supermarket'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Eggs (tray of 12)', 'Farm-fresh eggs', 'SS-004', 84.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Sunrise Supermarket'), (SELECT id FROM categories WHERE name = 'Bakery'), 'White Bread (400 g)', 'Soft sandwich bread', 'SS-005', 45.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Sunrise Supermarket'), (SELECT id FROM categories WHERE name = 'Household & Cleaning'), 'Dish Wash Liquid (500 ml)', 'Grease-cutting dish wash', 'SS-006', 95.00, 'INR', NULL, TRUE, now(), now(), 0);

-- Green Valley Organics - organic/health focus
INSERT INTO products (supermarket_id, category_id, name, description, sku, price_amount, price_currency, image_url, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'Green Valley Organics'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Organic Spinach (250 g)', 'Pesticide-free leafy greens', 'GV-001', 40.00, 'INR', '/images/vegetables.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Green Valley Organics'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Organic Bananas (1 dozen)', 'Naturally ripened, no chemicals', 'GV-002', 70.00, 'INR', '/images/fruit.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Green Valley Organics'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'A2 Cow Milk (1 L)', 'Desi-cow A2 milk', 'GV-003', 90.00, 'INR', '/images/milk.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Green Valley Organics'), (SELECT id FROM categories WHERE name = 'Bakery'), 'Whole Wheat Bread (400 g)', 'Stone-ground whole wheat', 'GV-004', 65.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Green Valley Organics'), (SELECT id FROM categories WHERE name = 'Snacks & Beverages'), 'Roasted Makhana (100 g)', 'Air-roasted fox nuts, lightly salted', 'GV-005', 110.00, 'INR', NULL, TRUE, now(), now(), 0);

-- QuickBasket Express - convenience store
INSERT INTO products (supermarket_id, category_id, name, description, sku, price_amount, price_currency, image_url, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'QuickBasket Express'), (SELECT id FROM categories WHERE name = 'Snacks & Beverages'), 'Potato Chips (150 g)', 'Classic salted chips', 'QB-001', 40.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'QuickBasket Express'), (SELECT id FROM categories WHERE name = 'Snacks & Beverages'), 'Cola (750 ml)', 'Chilled soft drink', 'QB-002', 45.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'QuickBasket Express'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Toned Milk (500 ml)', 'Small-pack toned milk', 'QB-003', 32.00, 'INR', '/images/milk.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'QuickBasket Express'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Bananas (half dozen)', 'Grab-and-go bananas', 'QB-004', 28.00, 'INR', '/images/fruit.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'QuickBasket Express'), (SELECT id FROM categories WHERE name = 'Household & Cleaning'), 'Tissue Paper (1 roll)', 'Kitchen tissue roll', 'QB-005', 55.00, 'INR', NULL, TRUE, now(), now(), 0);

-- Metro Grocery Hub - large-format hypermarket
INSERT INTO products (supermarket_id, category_id, name, description, sku, price_amount, price_currency, image_url, active, created_at, updated_at, version) VALUES
    ((SELECT id FROM supermarkets WHERE name = 'Metro Grocery Hub'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Tomatoes (2 kg)', 'Bulk-pack fresh tomatoes', 'MG-001', 90.00, 'INR', '/images/vegetables.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Metro Grocery Hub'), (SELECT id FROM categories WHERE name = 'Fruits & Vegetables'), 'Mixed Fruit Box (3 kg)', 'Seasonal assorted fruit', 'MG-002', 320.00, 'INR', '/images/fruit.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Metro Grocery Hub'), (SELECT id FROM categories WHERE name = 'Dairy & Eggs'), 'Toned Milk (2 L)', 'Family-pack toned milk', 'MG-003', 108.00, 'INR', '/images/milk.jpg', TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Metro Grocery Hub'), (SELECT id FROM categories WHERE name = 'Bakery'), 'Assorted Cookies (300 g)', 'Butter and chocolate chip mix', 'MG-004', 85.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Metro Grocery Hub'), (SELECT id FROM categories WHERE name = 'Snacks & Beverages'), 'Instant Noodles (pack of 8)', 'Family-pack masala noodles', 'MG-005', 96.00, 'INR', NULL, TRUE, now(), now(), 0),
    ((SELECT id FROM supermarkets WHERE name = 'Metro Grocery Hub'), (SELECT id FROM categories WHERE name = 'Household & Cleaning'), 'Laundry Detergent (3 kg)', 'Bulk-pack detergent powder', 'MG-006', 420.00, 'INR', NULL, TRUE, now(), now(), 0);

INSERT INTO inventory (branch_id, product_id, quantity_on_hand, created_at, updated_at, version)
SELECT b.id, p.id, 150, now(), now(), 0
FROM products p
JOIN branches b ON b.supermarket_id = p.supermarket_id
WHERE p.supermarket_id IN (
    SELECT id FROM supermarkets WHERE name IN
        ('Sunrise Supermarket', 'Green Valley Organics', 'QuickBasket Express', 'Metro Grocery Hub')
);
