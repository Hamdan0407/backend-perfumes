-- Production Database Indexes
-- Run this script after migrating to PostgreSQL

-- Product indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_product_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_product_brand ON products(brand);
CREATE INDEX IF NOT EXISTS idx_product_featured ON products(featured, active);
CREATE INDEX IF NOT EXISTS idx_product_active ON products(active);
CREATE INDEX IF NOT EXISTS idx_product_name ON products(name);

-- Order indexes for user queries
CREATE INDEX IF NOT EXISTS idx_order_user ON orders(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_created ON orders(created_at DESC);

-- Cart indexes
CREATE INDEX IF NOT EXISTS idx_cart_user ON cart_items(user_id);

-- Review indexes
CREATE INDEX IF NOT EXISTS idx_review_product ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_review_user ON reviews(user_id);

-- User indexes
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_role ON users(role);

-- Wishlist indexes
CREATE INDEX IF NOT EXISTS idx_wishlist_user ON wishlist(user_id);

-- Product view tracking indexes
CREATE INDEX IF NOT EXISTS idx_product_view_user ON product_views(user_id, viewed_at DESC);
CREATE INDEX IF NOT EXISTS idx_product_view_session ON product_views(session_id, viewed_at DESC);
