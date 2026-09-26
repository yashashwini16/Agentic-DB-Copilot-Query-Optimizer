-- Enable the pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Drop tables if exists for clean startup
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS semantic_query_cache CASCADE;

-- 1. Customers Table
CREATE TABLE customers (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    city VARCHAR(80),
    country VARCHAR(80) DEFAULT 'United States',
    loyalty_tier VARCHAR(20) DEFAULT 'BRONZE' CHECK (loyalty_tier IN ('BRONZE', 'SILVER', 'GOLD', 'PLATINUM')),
    lifetime_spend NUMERIC(12, 2) DEFAULT 0.00,
    signup_date DATE NOT NULL DEFAULT CURRENT_DATE
);

-- 2. Categories Table
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE,
    department VARCHAR(80) NOT NULL,
    description TEXT
);

-- 3. Products Table
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category_id INT REFERENCES categories(id) ON DELETE SET NULL,
    unit_price NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    stock_quantity INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    rating NUMERIC(3, 2) DEFAULT 4.50 CHECK (rating BETWEEN 1.0 AND 5.0),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Orders Table
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'SHIPPED', 'CANCELLED', 'REFUNDED')),
    payment_method VARCHAR(50) DEFAULT 'CREDIT_CARD'
);

-- 5. Order Items Table
CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id INT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10, 2) NOT NULL,
    discount NUMERIC(5, 2) DEFAULT 0.00
);

-- 6. Product Reviews Table
CREATE TABLE reviews (
    id SERIAL PRIMARY KEY,
    product_id INT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    customer_id INT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Semantic Query Cache Table (with pgvector embedding support: 384 dimensions for all-minilm-l6-v2)
CREATE TABLE semantic_query_cache (
    id SERIAL PRIMARY KEY,
    natural_query TEXT NOT NULL,
    query_hash VARCHAR(64) NOT NULL UNIQUE,
    embedding vector(384),
    generated_sql TEXT NOT NULL,
    explanation TEXT,
    execution_latency_ms BIGINT DEFAULT 0,
    hit_count INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create HNSW Index for rapid sub-millisecond vector similarity search
CREATE INDEX IF NOT EXISTS idx_query_cache_embedding_hnsw 
ON semantic_query_cache 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- Standard B-Tree Indexes for OLAP and Query Performance
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_order_date ON orders(order_date);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_reviews_product_id ON reviews(product_id);

-- Seed Categories
INSERT INTO categories (id, name, department, description) VALUES
(1, 'Laptops & Computers', 'Electronics', 'High-performance laptops, desktops, and computing accessories'),
(2, 'Smartphones & Audio', 'Electronics', 'Flagship mobile phones, noise-canceling headphones, and earbuds'),
(3, 'Office Furniture', 'Furniture', 'Ergonomic chairs, standing desks, and office storage'),
(4, 'Smart Home & IoT', 'Smart Devices', 'Smart lighting, security hubs, and automation devices'),
(5, 'Fitness & Wearables', 'Health & Fitness', 'Smartwatches, fitness bands, and workout monitors');

-- Seed Customers
INSERT INTO customers (id, full_name, email, city, country, loyalty_tier, lifetime_spend, signup_date) VALUES
(1, 'Alice Johnson', 'alice.j@example.com', 'San Francisco', 'United States', 'PLATINUM', 8450.00, '2023-01-15'),
(2, 'Bob Martinez', 'bob.m@example.com', 'New York', 'United States', 'GOLD', 4200.50, '2023-03-22'),
(3, 'Catherine Zhang', 'catherine.z@example.com', 'Toronto', 'Canada', 'PLATINUM', 9800.20, '2022-11-05'),
(4, 'David Kumar', 'david.k@example.com', 'London', 'United Kingdom', 'SILVER', 1850.00, '2024-02-10'),
(5, 'Elena Rostova', 'elena.r@example.com', 'Berlin', 'Germany', 'GOLD', 5120.00, '2023-08-19'),
(6, 'Faisal Al-Mansoor', 'faisal.m@example.com', 'Dubai', 'United Arab Emirates', 'PLATINUM', 12300.00, '2022-09-12'),
(7, 'Grace Hopper', 'grace.h@example.com', 'Boston', 'United States', 'GOLD', 6400.00, '2023-05-30'),
(8, 'Hiroshi Tanaka', 'hiroshi.t@example.com', 'Tokyo', 'Japan', 'SILVER', 2900.00, '2024-01-08'),
(9, 'Isabella Rossi', 'isabella.r@example.com', 'Milan', 'Italy', 'BRONZE', 650.00, '2024-06-14'),
(10, 'James Wilson', 'james.w@example.com', 'Sydney', 'Australia', 'GOLD', 4750.00, '2023-10-01');

-- Seed Products
INSERT INTO products (id, name, category_id, unit_price, stock_quantity, rating, is_active) VALUES
(1, 'QuantumBook Pro 16', 1, 2499.00, 45, 4.85, TRUE),
(2, 'UltraSlim Workstation 14', 1, 1499.00, 80, 4.70, TRUE),
(3, 'SonicNoise ANC Pro Headphones', 2, 349.00, 150, 4.90, TRUE),
(4, 'AeroBuds Wireless Earphones', 2, 179.00, 220, 4.55, TRUE),
(5, 'ErgoFlex Standing Desk Pro', 3, 799.00, 30, 4.80, TRUE),
(6, 'LumbarMaster Mesh Executive Chair', 3, 499.00, 65, 4.65, TRUE),
(7, 'SmartGlow Ambient Lighting Kit', 4, 129.00, 300, 4.40, TRUE),
(8, 'VisionSecure 4K Smart Camera Hub', 4, 289.00, 110, 4.60, TRUE),
(9, 'PulseTrack Elite Smartwatch', 5, 399.00, 95, 4.75, TRUE),
(10, 'FitBand Pro Activity Tracker', 5, 99.00, 400, 4.35, TRUE);

-- Seed Orders
INSERT INTO orders (id, customer_id, order_date, total_amount, status, payment_method) VALUES
(101, 1, '2025-01-10 10:30:00', 2848.00, 'COMPLETED', 'CREDIT_CARD'),
(102, 2, '2025-01-12 14:15:00', 499.00, 'COMPLETED', 'PAYPAL'),
(103, 3, '2025-01-15 09:00:00', 3298.00, 'COMPLETED', 'BANK_TRANSFER'),
(104, 6, '2025-02-01 16:45:00', 4426.00, 'COMPLETED', 'CREDIT_CARD'),
(105, 5, '2025-02-05 11:20:00', 1298.00, 'COMPLETED', 'CREDIT_CARD'),
(106, 7, '2025-02-18 13:10:00', 2499.00, 'COMPLETED', 'APPLE_PAY'),
(107, 4, '2025-03-01 15:50:00', 688.00, 'COMPLETED', 'DEBIT_CARD'),
(108, 1, '2025-03-08 17:05:00', 928.00, 'COMPLETED', 'CREDIT_CARD'),
(109, 8, '2025-03-12 08:30:00', 1499.00, 'COMPLETED', 'CREDIT_CARD'),
(110, 10, '2025-03-20 19:40:00', 978.00, 'COMPLETED', 'PAYPAL'),
(111, 3, '2025-04-02 12:15:00', 2628.00, 'COMPLETED', 'BANK_TRANSFER'),
(112, 6, '2025-04-10 14:00:00', 5396.00, 'COMPLETED', 'CREDIT_CARD'),
(113, 9, '2025-04-15 18:25:00', 650.00, 'PENDING', 'CREDIT_CARD');

-- Seed Order Items
INSERT INTO order_items (order_id, product_id, quantity, unit_price, discount) VALUES
(101, 1, 1, 2499.00, 0.00),
(101, 3, 1, 349.00, 0.00),
(102, 6, 1, 499.00, 0.00),
(103, 1, 1, 2499.00, 0.00),
(103, 5, 1, 799.00, 0.00),
(104, 1, 1, 2499.00, 0.00),
(104, 3, 2, 349.00, 50.00),
(104, 9, 3, 399.00, 30.00),
(105, 5, 1, 799.00, 0.00),
(105, 6, 1, 499.00, 0.00),
(106, 1, 1, 2499.00, 0.00),
(107, 8, 2, 289.00, 0.00),
(107, 10, 1, 99.00, 0.00),
(108, 9, 2, 399.00, 0.00),
(108, 7, 1, 129.00, 0.00),
(109, 2, 1, 1499.00, 0.00),
(110, 3, 2, 349.00, 0.00),
(110, 8, 1, 289.00, 0.00),
(111, 1, 1, 2499.00, 0.00),
(111, 7, 1, 129.00, 0.00),
(112, 1, 2, 2499.00, 0.00),
(112, 9, 1, 399.00, 0.00),
(113, 3, 1, 349.00, 0.00),
(113, 8, 1, 289.00, 0.00);

-- Seed Reviews
INSERT INTO reviews (product_id, customer_id, rating, comment, review_date) VALUES
(1, 1, 5, 'Unmatched build quality and ultra-fast compiling speed for heavy engineering workloads.', '2025-01-20'),
(1, 3, 5, 'Best laptop I have ever owned. Battery lasts all day.', '2025-01-25'),
(3, 2, 5, 'Active noise cancellation completely isolates airplane noise. Crisp highs and punchy bass.', '2025-01-22'),
(5, 5, 4, 'Very sturdy motorized desk. Assembly took 45 minutes.', '2025-02-10'),
(6, 2, 5, 'Saved my lower back during 10-hour remote working days.', '2025-02-01'),
(9, 6, 5, 'Heart rate and sleep tracking is astonishingly accurate compared to clinical monitors.', '2025-02-25'),
(8, 4, 4, 'Crystal clear night vision and seamless HomeKit/Google Home integration.', '2025-03-10');

-- Reset Serial Sequences
SELECT setval('customers_id_seq', (SELECT MAX(id) FROM customers));
SELECT setval('categories_id_seq', (SELECT MAX(id) FROM categories));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));
SELECT setval('order_items_id_seq', (SELECT MAX(id) FROM order_items));
SELECT setval('reviews_id_seq', (SELECT MAX(id) FROM reviews));
