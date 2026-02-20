-- Sample products only - Users are created by initializers


INSERT INTO products (name, brand, description, price, discount_price, category, type, volume, image_url, stock, active, featured, rating, review_count, created_at, updated_at) VALUES
('No. 5', 'Chanel', 'Iconic floral fragrance with timeless elegance', 5000.00, NULL, 'Women', 'Perfume', 50, 'https://placehold.co/600x600?text=Chanel+No+5', 100, TRUE, TRUE, 4.8, 245, NOW(), NOW()),
('Jadore', 'Dior', 'Luxurious fruity floral with sensual notes', 4500.00, NULL, 'Women', 'Perfume', 50, 'https://placehold.co/600x600?text=Dior+Jadore', 100, TRUE, FALSE, 4.7, 189, NOW(), NOW()),
('Bloom', 'Gucci', 'Fresh floral with gardenia and tuberose', 4400.00, NULL, 'Women', 'Perfume', 50, 'https://placehold.co/600x600?text=Gucci+Bloom', 100, TRUE, FALSE, 4.6, 156, NOW(), NOW()),
('Aventus', 'Creed', 'Prestigious fruity woody fragrance', 11000.00, NULL, 'Men', 'Perfume', 50, 'https://placehold.co/600x600?text=Creed+Aventus', 100, TRUE, TRUE, 4.9, 421, NOW(), NOW()),
('Sauvage', 'Dior', 'Fresh aromatic masculine fragrance', 5200.00, NULL, 'Men', 'Perfume', 100, 'https://placehold.co/600x600?text=Dior+Sauvage', 100, TRUE, TRUE, 4.8, 356, NOW(), NOW()),
('Musk Al Kaaba', 'Al Haramain', 'Premium oriental attar with deep musk notes', 1200.00, NULL, 'Unisex', 'Attar', 12, 'https://placehold.co/600x600?text=Musk+Al+Kaaba', 100, TRUE, TRUE, 4.9, 87, NOW(), NOW()),
('Oudh 360', 'Al Rehab', 'Classic concentrated perfume oil', 800.00, NULL, 'Unisex', 'Attar', 6, 'https://placehold.co/600x600?text=Oudh+360', 100, TRUE, FALSE, 4.7, 124, NOW(), NOW()),
('White Musk', 'Ajmal', 'Fresh and clean white musk oil', 1500.00, NULL, 'Unisex', 'Attar', 10, 'https://placehold.co/600x600?text=White+Musk', 100, TRUE, TRUE, 4.8, 56, NOW(), NOW());

-- Variants with sizes based on category (Indices follow TRUNCATE sequence)
INSERT INTO product_variants (product_id, size, price, discount_price, stock, active, sku, created_at, updated_at) VALUES
(1, 30, 3200.00, NULL, 50, TRUE, 'CH-NO5-30', NOW(), NOW()),
(1, 50, 5000.00, NULL, 50, TRUE, 'CH-NO5-50', NOW(), NOW()),
(1, 100, 8500.00, 8000.00, 30, TRUE, 'CH-NO5-100', NOW(), NOW()),
(2, 30, 2800.00, NULL, 40, TRUE, 'DI-JAD-30', NOW(), NOW()),
(2, 50, 4500.00, NULL, 40, TRUE, 'DI-JAD-50', NOW(), NOW()),
(2, 100, 7800.00, NULL, 25, TRUE, 'DI-JAD-100', NOW(), NOW()),
(6, 6, 800.00, NULL, 45, TRUE, 'AL-MAK-6', NOW(), NOW()),
(6, 10, 1200.00, NULL, 35, TRUE, 'AL-MAK-10', NOW(), NOW()),
(7, 6, 800.00, NULL, 60, TRUE, 'AL-REH-6', NOW(), NOW()),
(7, 10, 1200.00, NULL, 40, TRUE, 'AL-REH-10', NOW(), NOW()),
(8, 6, 1000.00, NULL, 30, TRUE, 'AJ-WMU-6', NOW(), NOW()),
(8, 10, 1500.00, NULL, 25, TRUE, 'AJ-WMU-10', NOW(), NOW());
