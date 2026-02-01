-- Insert 20 Products
INSERT INTO products (name, brand, description, price, stock, category, image_url, active, featured, rating, review_count, created_at, updated_at) VALUES
('Dior Jadore', 'Dior', 'Luxurious floral fragrance', 4500, 52, 'Women', 'https://images.unsplash.com/photo-1562181286-d3fee7d55364', 1, 1, 4.7, 89, NOW(), NOW()),
('Gucci Bloom', 'Gucci', 'Fresh floral composition', 4400, 38, 'Women', 'https://images.unsplash.com/photo-1595777712802-372adc3dd05e', 1, 1, 4.6, 72, NOW(), NOW()),
('Tom Ford Black Orchid', 'Tom Ford', 'Luxurious and sensual fragrance', 6500, 28, 'Women', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f', 1, 0, 4.9, 156, NOW(), NOW()),
('YSL Mon Paris', 'Yves Saint Laurent', 'Romantic floral fragrance', 4000, 55, 'Women', 'https://images.unsplash.com/photo-1588405748507-ca7b328e4e1d', 1, 0, 4.5, 65, NOW(), NOW()),
('Lancome La Vie Est Belle', 'Lancome', 'Best-selling fragrance', 3500, 95, 'Women', 'https://images.unsplash.com/photo-1569505254788-205b4f368f5f', 1, 1, 4.7, 312, NOW(), NOW()),
('Dior Sauvage', 'Dior', 'Fresh and spicy fragrance', 4200, 67, 'Men', 'https://images.unsplash.com/photo-1592078615290-033ee584e267', 1, 1, 4.8, 234, NOW(), NOW()),
('Bleu de Chanel', 'Chanel', 'Sophisticated fragrance', 4400, 58, 'Men', 'https://images.unsplash.com/photo-1613467489881-a59c0cda0b2b', 1, 1, 4.7, 178, NOW(), NOW()),
('Creed Aventus', 'Creed', 'Legendary fragrance', 11000, 15, 'Men', 'https://images.unsplash.com/photo-1561181286-d3fee7d55364', 1, 0, 5.0, 98, NOW(), NOW()),
('Versace Eros', 'Versace', 'Energetic and seductive', 3800, 42, 'Men', 'https://images.unsplash.com/photo-1577875729629-ed5fdc1d3dc3', 1, 0, 4.6, 143, NOW(), NOW()),
('Giorgio Armani Code', 'Giorgio Armani', 'Sophisticated and warm', 3900, 48, 'Men', 'https://images.unsplash.com/photo-1595836374141-a96df00e5f30', 1, 1, 4.5, 89, NOW(), NOW()),
('Marc Jacobs Daisy', 'Marc Jacobs', 'Fresh and flirty unisex', 2800, 72, 'Unisex', 'https://images.unsplash.com/photo-1548695207-9b3f53c4c82f', 1, 1, 4.4, 156, NOW(), NOW()),
('Jo Malone Cologne', 'Jo Malone', 'Light and refreshing', 3200, 88, 'Unisex', 'https://images.unsplash.com/photo-1570538108519-280658a16dda', 1, 0, 4.3, 203, NOW(), NOW()),
('Calvin Klein Obsession', 'Calvin Klein', 'Iconic fragrance', 2500, 76, 'Women', 'https://images.unsplash.com/photo-1505831477604-f2b41970a71b', 1, 0, 4.4, 189, NOW(), NOW()),
('Hugo Boss Bottled', 'Hugo Boss', 'Best-selling mens fragrance', 2800, 81, 'Men', 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45', 1, 1, 4.5, 267, NOW(), NOW()),
('Jean Paul Gaultier', 'Jean Paul Gaultier', 'Legendary masculine fragrance', 3200, 64, 'Men', 'https://images.unsplash.com/photo-1594938298603-c8148c4dae35', 1, 0, 4.6, 201, NOW(), NOW()),
('Burberry Brit Sheer', 'Burberry', 'Fruity fragrance', 3300, 54, 'Women', 'https://images.unsplash.com/photo-1591186995819-4c5150e5e44d', 1, 0, 4.4, 76, NOW(), NOW()),
('Marc Jacobs Decadence', 'Marc Jacobs', 'Rich fruity fragrance', 3150, 46, 'Women', 'https://images.unsplash.com/photo-1611591437281-460bfbe1220a', 1, 0, 4.5, 123, NOW(), NOW()),
('Prada Amber', 'Prada', 'Warm amber fragrance', 4300, 35, 'Women', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f', 1, 0, 4.4, 71, NOW(), NOW()),
('Lancome Tresor', 'Lancome', 'Luxurious oriental fragrance', 3400, 47, 'Women', 'https://images.unsplash.com/photo-1597318372441-0a2c8cf4f4bb', 1, 0, 4.5, 134, NOW(), NOW()),
('Thierry Mugler', 'Thierry Mugler', 'Sensual fragrance', 3600, 39, 'Women', 'https://images.unsplash.com/photo-1594938298603-c8148c4dae35', 1, 1, 4.6, 95, NOW(), NOW());

-- Insert Sample Users with Different Positions
-- Password for all: Test@1234 (bcrypt hash)
INSERT INTO users (email, password, first_name, last_name, phone_number, address, city, country, zip_code, role, active, created_at, updated_at) VALUES
('owner@perfume.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Priya', 'Sharma', '9876543210', '123 Business Park', 'Mumbai', 'India', '400001', 'ADMIN', 1, NOW(), NOW()),
('manager@perfume.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Rajesh', 'Kumar', '9876543211', '456 Corporate Ave', 'Bangalore', 'India', '560001', 'ADMIN', 1, NOW(), NOW()),
('warehouse1@perfume.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Amit', 'Singh', '9876543212', '789 Warehouse St', 'Delhi', 'India', '110001', 'USER', 1, NOW(), NOW()),
('warehouse2@perfume.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Vikram', 'Patel', '9876543213', '321 Warehouse Rd', 'Pune', 'India', '411001', 'USER', 1, NOW(), NOW()),
('customer1@example.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'John', 'Doe', '9876543214', '555 Main St', 'Mumbai', 'India', '400005', 'USER', 1, NOW(), NOW()),
('customer2@example.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Jane', 'Smith', '9876543215', '666 Oak Ave', 'Bangalore', 'India', '560005', 'USER', 1, NOW(), NOW()),
('customer3@example.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Mike', 'Johnson', '9876543216', '777 Pine Ln', 'Hyderabad', 'India', '500001', 'USER', 1, NOW(), NOW()),
('customer4@example.com', '$2a$10$dXJ3SW6G7P50eS3Z2grXWeVmMyHRR4h6TF5GWJX9A6I/yT7mykrBi', 'Sarah', 'Williams', '9876543217', '888 Elm Dr', 'Chennai', 'India', '600001', 'USER', 0, NOW(), NOW());
