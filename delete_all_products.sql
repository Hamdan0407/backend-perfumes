-- SQL Script to Delete All Products from Database
-- Run this in pgAdmin Query Tool

-- Step 1: Delete all cart items (references products)
DELETE FROM cart_items;

-- Step 2: Delete all wishlist items (references products)
DELETE FROM wishlist_items;

-- Step 3: Delete all order items (references products)
DELETE FROM order_items;

-- Step 4: Delete all product reviews
DELETE FROM reviews;

-- Step 5: Delete all product variants
DELETE FROM product_variants;

-- Step 6: Finally delete all products
DELETE FROM products;

-- Verify deletion
SELECT COUNT(*) as remaining_products FROM products;
SELECT COUNT(*) as remaining_variants FROM product_variants;
SELECT COUNT(*) as remaining_reviews FROM reviews;

-- Show success message
SELECT 'All products have been successfully deleted. Database is ready for new products.' AS status;
