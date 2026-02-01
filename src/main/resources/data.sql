-- Insert test users
INSERT INTO users (email, password, first_name, last_name, phone_number, address, city, country, zip_code, role, active, created_at, updated_at) 
VALUES 
('admin@example.com', '$2a$10$slYQmyNdGzin7olVVCb1Be7DlH.PKZbv5H8KfzzIgXXbVxzy2EI.e', 'Admin', 'User', '9876543214', '555 Main St', 'New York', 'USA', '10001', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user@example.com', '$2a$10$slYQmyNdGzin7olVVCb1Be7DlH.PKZbv5H8KfzzIgXXbVxzy2EI.e', 'Test', 'User', '1234567890', '123 User St', 'New York', 'USA', '10001', 'CUSTOMER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('owner@perfume.com', '$2a$10$slYQmyNdGzin7olVVCb1Be7DlH.PKZbv5H8KfzzIgXXbVxzy2EI.e', 'Owner', 'Admin', '9999999999', '1 Admin St', 'New York', 'USA', '10001', 'OWNER', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert sample products
INSERT INTO products (name, brand, description, price, discount_price, category, type, volume, image_url, stock, active, featured, rating, review_count, created_at, updated_at) VALUES
('No. 5', 'Chanel', 'Iconic floral fragrance with timeless elegance', 5000.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Chanel+No+5', 25, 1, 1, 4.8, 245, NOW(), NOW()),
('Jadore', 'Dior', 'Luxurious fruity floral with sensual notes', 4500.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Dior+Jadore', 32, 1, 0, 4.7, 189, NOW(), NOW()),
('Bloom', 'Gucci', 'Fresh floral with gardenia and tuberose', 4400.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Gucci+Bloom', 28, 1, 0, 4.6, 156, NOW(), NOW()),
('Black Orchid', 'Tom Ford', 'Exotic dark floral with spicy notes', 6500.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Tom+Ford+Black+Orchid', 18, 1, 1, 4.9, 312, NOW(), NOW()),
('La Vie est Belle', 'Lancome', 'Sweet floral fragrance with iris', 4200.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Lancome+La+Vie', 35, 1, 0, 4.7, 201, NOW(), NOW()),
('Obsession', 'Calvin Klein', 'Sensual amber fragrance', 3800.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Calvin+Klein+Obsession', 42, 1, 0, 4.5, 167, NOW(), NOW()),
('Brit', 'Burberry', 'Fresh fruity floral for women', 3900.00, NULL, 'Women', 'Eau de Toilette', 100, 'https://via.placeholder.com/300?text=Burberry+Brit', 38, 1, 0, 4.6, 178, NOW(), NOW()),
('Candy', 'Prada', 'Sweet gourmand fragrance with caramel', 4100.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Prada+Candy', 30, 1, 0, 4.7, 192, NOW(), NOW()),
('Daisy', 'Marc Jacobs', 'Fresh fruity with simple florals', 3200.00, NULL, 'Women', 'Eau de Toilette', 100, 'https://via.placeholder.com/300?text=Marc+Jacobs+Daisy', 48, 1, 0, 4.4, 145, NOW(), NOW()),
('Aventus', 'Creed', 'Prestigious fruity woody fragrance', 11000.00, NULL, 'Men', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Creed+Aventus', 12, 1, 1, 4.9, 421, NOW(), NOW()),
('Sauvage', 'Dior', 'Fresh aromatic masculine fragrance', 5200.00, NULL, 'Men', 'Eau de Toilette', 100, 'https://via.placeholder.com/300?text=Dior+Sauvage', 22, 1, 1, 4.8, 356, NOW(), NOW()),
('Bleu de Chanel', 'Chanel', 'Woody aromatic blend for men', 5500.00, NULL, 'Men', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Bleu+de+Chanel', 19, 1, 0, 4.8, 298, NOW(), NOW()),
('Eros', 'Versace', 'Fresh spicy fragrance for men', 4000.00, NULL, 'Men', 'Eau de Toilette', 100, 'https://via.placeholder.com/300?text=Versace+Eros', 31, 1, 0, 4.6, 224, NOW(), NOW()),
('Aqua', 'Calvin Klein', 'Fresh aquatic masculine scent', 3500.00, NULL, 'Men', 'Eau de Toilette', 100, 'https://via.placeholder.com/300?text=Calvin+Klein+Aqua', 41, 1, 0, 4.5, 189, NOW(), NOW()),
('Cologne Néroli', 'Jo Malone', 'Citrus and woody unisex fragrance', 2800.00, NULL, 'Unisex', 'Cologne', 100, 'https://via.placeholder.com/300?text=Neroli+Cologne', 55, 1, 0, 4.3, 98, NOW(), NOW()),
('Wood Sage & Sea Salt', 'Jo Malone', 'Fresh woody cologne', 3800.00, NULL, 'Unisex', 'Cologne', 100, 'https://via.placeholder.com/300?text=Jo+Malone', 45, 1, 0, 4.6, 167, NOW(), NOW()),
('H24', 'Hermès', 'Timeless floral fragrance', 4800.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Hermes+H24', 26, 1, 0, 4.7, 213, NOW(), NOW()),
('Mon Paris', 'Yves Saint Laurent', 'Romantic fruity floral', 4300.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=YSL+Mon+Paris', 33, 1, 0, 4.6, 178, NOW(), NOW()),
('Gentleman', 'Givenchy', 'Sophisticated woody for men', 4900.00, NULL, 'Men', 'Eau de Toilette', 100, 'https://via.placeholder.com/300?text=Givenchy+Gentleman', 24, 1, 0, 4.7, 256, NOW(), NOW()),
('Coco Mademoiselle', 'Chanel', 'Elegant modern floral', 5300.00, NULL, 'Women', 'Eau de Parfum', 50, 'https://via.placeholder.com/300?text=Coco+Mademoiselle', 20, 1, 1, 4.8, 334, NOW(), NOW());
