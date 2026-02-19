-- Fix the product ID sequence after demo data insertion
-- This resets the sequence to start from ID 100 to avoid conflicts with pre-seeded data
ALTER TABLE products ALTER COLUMN id RESTART WITH 100;
