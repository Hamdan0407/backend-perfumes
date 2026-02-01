-- Insert realistic perfume products
INSERT INTO products (name, description, price, stock, category, image_url, created_at) VALUES
-- Luxury Women's Fragrances
('Chanel No. 5', 'The iconic timeless classic. A floral bouquet with top notes of neroli and ylang-ylang. Sophisticated and elegant for the modern woman.', 165.00, 45, 'Women', 'https://images.unsplash.com/photo-1596178065887-cf38d2e3e02f?w=500', NOW()),
('Dior J''adore', 'Luxurious floral fragrance with notes of jasmine, rose, and orange blossom. A symbol of absolute femininity and elegance.', 150.00, 52, 'Women', 'https://images.unsplash.com/photo-1562181286-d3fee7d55364?w=500', NOW()),
('Gucci Bloom', 'A fresh floral composition with gardenia, tuberose, and jasmine. Elegant and refined, perfect for daytime wear.', 148.00, 38, 'Women', 'https://images.unsplash.com/photo-1595777712802-372adc3dd05e?w=500', NOW()),
('Tom Ford Black Orchid', 'A luxurious and sensual fragrance blending black orchid with dark chocolate and rum. Deep and mysterious.', 210.00, 28, 'Women', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=500', NOW()),
('Yves Saint Laurent Mon Paris', 'A romantic floral fragrance with notes of rose, peony, and blackcurrant. Perfect for special occasions.', 135.00, 55, 'Women', 'https://images.unsplash.com/photo-1588405748507-ca7b328e4e1d?w=500', NOW()),

-- Premium Men's Fragrances
('Dior Sauvage', 'Fresh and spicy fragrance with ambroxan, pepper, and ambrette seed. The most versatile mens fragrance ever.', 140.00, 67, 'Men', 'https://images.unsplash.com/photo-1592078615290-033ee584e267?w=500', NOW()),
('Bleu de Chanel', 'A sophisticated fragrance with notes of citrus, ginger, and cedar. Modern, fresh, and masculine.', 145.00, 58, 'Men', 'https://images.unsplash.com/photo-1613467489881-a59c0cda0b2b?w=500', NOW()),
('Creed Aventus', 'Legendary fragrance with pineapple, blackcurrant, and oakmoss. Bold, commanding, and unforgettable.', 380.00, 15, 'Men', 'https://images.unsplash.com/photo-1561181286-d3fee7d55364?w=500', NOW()),
('Versace Eros', 'Energetic and seductive fragrance with mint, green apple, and tonka bean. For the confident man.', 125.00, 42, 'Men', 'https://images.unsplash.com/photo-1577875729629-ed5fdc1d3dc3?w=500', NOW()),
('Giorgio Armani Code', 'Sophisticated and warm fragrance with iris, ambroxan, and leather. The ultimate masculine scent.', 130.00, 48, 'Men', 'https://images.unsplash.com/photo-1595836374141-a96df00e5f30?w=500', NOW()),

-- Unisex Fragrances
('Marc Jacobs Daisy Love', 'Fresh and flirty unisex fragrance with wildflower, white raspberry, and vanilla.', 95.00, 72, 'Unisex', 'https://images.unsplash.com/photo-1548695207-9b3f53c4c82f?w=500', NOW()),
('Jo Malone Cologne', 'Light and refreshing unisex fragrance. Versatile and perfect for layering.', 110.00, 88, 'Unisex', 'https://images.unsplash.com/photo-1570538108519-280658a16dda?w=500', NOW()),
('Maison Martin Margiela Beach Walk', 'Fresh, salty ocean breeze fragrance perfect for any time of year.', 185.00, 33, 'Unisex', 'https://images.unsplash.com/photo-1506755855726-5ff32e8c59d1?w=500', NOW()),
('Hermès Eau de Gentilhomme', 'Refined unisex fragrance with lavender, tobacco leaf, and cedar.', 155.00, 41, 'Unisex', 'https://images.unsplash.com/photo-1565958011504-98d342d9ffbd?w=500', NOW()),

-- Premium Limited Edition
('Roja Parfums Elixir', 'Exquisite luxury fragrance with precious ingredients. Ultra-rare and collectible.', 450.00, 8, 'Limited Edition', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=500', NOW()),
('Creed Royal Oud', 'Precious oud-based fragrance with rose and jasmine. Extremely limited production.', 420.00, 12, 'Limited Edition', 'https://images.unsplash.com/photo-1550355291-bbee04a92027?w=500', NOW()),
('Tom Ford Ombre Leather', 'Rich leather fragrance with tonka bean and amber. Premium and sophisticated.', 280.00, 22, 'Limited Edition', 'https://images.unsplash.com/photo-1588405748507-ca7b328e4e1d?w=500', NOW()),

-- Best Sellers
('Lancôme La Vie Est Belle', 'Best-selling fragrance with praline, iris, and patchouli. Sweet and irresistible.', 98.00, 95, 'Women', 'https://images.unsplash.com/photo-1569505254788-205b4f368f5f?w=500', NOW()),
('Calvin Klein Obsession', 'Iconic fragrance with mandarin, amber, and vanilla. Timeless and seductive.', 85.00, 76, 'Women', 'https://images.unsplash.com/photo-1505831477604-f2b41970a71b?w=500', NOW()),
('Hugo Boss Bottled', 'Best-selling mens fragrance with apple, cinnamon, and leather. Perfect daily scent.', 92.00, 81, 'Men', 'https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=500', NOW()),
('Jean Paul Gaultier Le Male', 'Legendary masculine fragrance with mint, jasmine, and amber. Iconic bottle design.', 105.00, 64, 'Men', 'https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=500', NOW()),

-- Fresh & Citrus
('Acqua di Parma Blu Mediterraneo', 'Fresh Mediterranean fragrance with lemon and Sicilian citrus. Breezy and uplifting.', 155.00, 39, 'Unisex', 'https://images.unsplash.com/photo-1588405748507-ca7b328e4e1d?w=500', NOW()),
('Citrus Garden Fresh', 'Bright citrus composition with bergamot, lemon, and grapefruit. Energy in a bottle.', 78.00, 50, 'Unisex', 'https://images.unsplash.com/photo-1596178065887-cf38d2e3e02f?w=500', NOW()),

-- Oriental & Amber
('Prada Amber', 'Warm amber fragrance with patchouli and cedar. Cozy and inviting.', 145.00, 35, 'Women', 'https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=500', NOW()),
('Lancôme Trésor', 'Luxurious oriental fragrance with vanilla, amber, and flowers. Sensual and warm.', 115.00, 47, 'Women', 'https://images.unsplash.com/photo-1597318372441-0a2c8cf4f4bb?w=500', NOW()),

-- Woody & Musky
('Prada L''Homme', 'Refined woody fragrance with iris, ambroxan, and incense. Modern and elegant.', 138.00, 44, 'Men', 'https://images.unsplash.com/photo-1559056199-641a0ac8b8d5?w=500', NOW()),
('Spicebomb Extreme', 'Spicy woody fragrance with cardamom, tobacco, and leather. Intense and masculine.', 128.00, 36, 'Men', 'https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=500', NOW()),

-- Fruity Fragrances
('Burberry Brit Sheer', 'Fruity fragrance with yuzu, peony, and subtle musk. Fresh and summery.', 110.00, 54, 'Women', 'https://images.unsplash.com/photo-1591186995819-4c5150e5e44d?w=500', NOW()),
('Marc Jacobs Decadence', 'Rich fruity fragrance with peach, wildflower, and almond. Luxurious and sensual.', 105.00, 46, 'Women', 'https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=500', NOW()),

-- Floral Bouquet
('Flowerbomb Viktor & Rolf', 'Explosive floral fragrance with sambac jasmine and centifolia rose. Intoxicating.', 165.00, 29, 'Women', 'https://images.unsplash.com/photo-1577875729629-ed5fdc1d3dc3?w=500', NOW()),
('Angel Thierry Mugler', 'Iconic fragrance with chocolate, vanilla, and praline. Sweet and addictive.', 118.00, 51, 'Women', 'https://images.unsplash.com/photo-1588405748507-ca7b328e4e1d?w=500', NOW());

-- Update product counts
UPDATE products SET stock = stock WHERE id > 0;
