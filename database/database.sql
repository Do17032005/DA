-- =====================================================================
-- CLOTHES SHOP - SINGLE DATABASE SCRIPT
-- Includes: full schema rebuild + recommendation indexes + demo seed data
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `clothesshopdb`;
USE `clothesshopdb`;

SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- Drop all tables if they exist (clean rebuild)
DROP TABLE IF EXISTS `wards`;
DROP TABLE IF EXISTS `districts`;
DROP TABLE IF EXISTS `provinces`;
DROP TABLE IF EXISTS `system_settings`;
DROP TABLE IF EXISTS `recommendations_cache`;
DROP TABLE IF EXISTS `user_similarity`;
DROP TABLE IF EXISTS `product_similarity`;
DROP TABLE IF EXISTS `user_ratings`;
DROP TABLE IF EXISTS `blog_posts`;
DROP TABLE IF EXISTS `sliders`;
DROP TABLE IF EXISTS `banners`;
DROP TABLE IF EXISTS `user_interactions`;
DROP TABLE IF EXISTS `wishlists`;
DROP TABLE IF EXISTS `product_reviews`;
DROP TABLE IF EXISTS `addresses`;
DROP TABLE IF EXISTS `order_items`;
DROP TABLE IF EXISTS `cart_items`;
DROP TABLE IF EXISTS `shopping_carts`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `vouchers`;
DROP TABLE IF EXISTS `products`;
DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `users`;

-- =========================================================================
-- 0. Administrative Units (Vietnam)
-- =========================================================================
CREATE TABLE `provinces` (
    `code` VARCHAR(20) PRIMARY KEY,
    `name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `name_en` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `full_name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `name_en_full` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `code_name` VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `districts` (
    `code` VARCHAR(20) PRIMARY KEY,
    `name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `name_en` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `full_name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `name_en_full` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `code_name` VARCHAR(255),
    `province_code` VARCHAR(20),
    FOREIGN KEY (`province_code`) REFERENCES `provinces`(`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `wards` (
    `code` VARCHAR(20) PRIMARY KEY,
    `name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `name_en` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `full_name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `name_en_full` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `code_name` VARCHAR(255),
    `district_code` VARCHAR(20),
    FOREIGN KEY (`district_code`) REFERENCES `districts`(`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 1. Core Users Table
-- =========================================================================
CREATE TABLE `users` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100) NOT NULL UNIQUE,
    `full_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `phone` VARCHAR(20),
    `role` VARCHAR(20) DEFAULT 'USER',
    `is_vip` BOOLEAN DEFAULT FALSE,
    `avatar_url` VARCHAR(500),
    `gender` VARCHAR(20),
    `date_of_birth` DATE,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 2. Master Tables (Categories, Vouchers, Settings)
-- =========================================================================
CREATE TABLE `categories` (
    `category_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `category_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `slug` VARCHAR(100) UNIQUE,
    `description` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `parent_id` BIGINT DEFAULT NULL,
    `display_order` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `icon` VARCHAR(255),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`parent_id`) REFERENCES `categories`(`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `vouchers` (
    `voucher_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `voucher_code` VARCHAR(50) NOT NULL UNIQUE,
    `voucher_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `description` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `discount_type` VARCHAR(20) DEFAULT 'PERCENTAGE',
    `discount_value` DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    `min_order_value` DECIMAL(15, 2) DEFAULT 0.00,
    `max_discount` DECIMAL(15, 2) DEFAULT NULL,
    `quantity` INT DEFAULT 0,
    `used_count` INT DEFAULT 0,
    `start_date` DATE,
    `end_date` DATE,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `system_settings` (
    `setting_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `setting_key` VARCHAR(100) NOT NULL UNIQUE,
    `setting_value` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `category` VARCHAR(50) DEFAULT 'general',
    `data_type` VARCHAR(50) DEFAULT 'string',
    `description` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `is_editable` BOOLEAN DEFAULT TRUE,
    `updated_by` BIGINT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 3. Products
-- =========================================================================
CREATE TABLE `products` (
    `product_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `category_id` BIGINT,
    `product_name` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `description` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `price` DECIMAL(15, 2) NOT NULL,
    `discount_price` DECIMAL(15, 2),
    `stock_quantity` INT DEFAULT 0,
    `sku` VARCHAR(100) UNIQUE,
    `brand` VARCHAR(100),
    `color` VARCHAR(50),
    `size` VARCHAR(50),
    `material` VARCHAR(100),
    `gender` ENUM('MALE', 'FEMALE', 'UNISEX', 'KIDS') DEFAULT 'UNISEX',
    `season` ENUM('SPRING', 'SUMMER', 'FALL', 'WINTER', 'ALL_SEASON') DEFAULT 'ALL_SEASON',
    `image_url` VARCHAR(500),
    `is_active` BOOLEAN DEFAULT TRUE,
    `view_count` INT DEFAULT 0,
    `purchase_count` INT DEFAULT 0,
    `is_new` BOOLEAN DEFAULT FALSE,
    `is_hot` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `categories`(`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_products_is_new ON products(is_new);
CREATE INDEX idx_products_is_hot ON products(is_hot);

-- =========================================================================
-- 4. User Related Tables (Address, Wishlist)
-- =========================================================================
CREATE TABLE `addresses` (
    `address_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `full_name` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `phone` VARCHAR(20),
    `address_line` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `ward` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `district` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `city` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `is_default` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `wishlists` (
    `wishlist_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_user_product_wishlist` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 5. Shopping Cart
-- =========================================================================
CREATE TABLE `shopping_carts` (
    `cart_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT UNIQUE,
    `session_token` VARCHAR(255),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `cart_items` (
    `cart_item_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `cart_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `quantity` INT NOT NULL DEFAULT 1,
    `size` VARCHAR(50) DEFAULT NULL,
    `color` VARCHAR(50) DEFAULT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`cart_id`) REFERENCES `shopping_carts`(`cart_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE,
    INDEX `idx_cart_item_variant` (`cart_id`, `product_id`, `size`, `color`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);
CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);

-- =========================================================================
-- 6. Transactions (Orders)
-- =========================================================================
CREATE TABLE `orders` (
    `order_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `order_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `total_amount` DECIMAL(15, 2) NOT NULL,
    `status` VARCHAR(50) DEFAULT 'PENDING',
    `shipping_address` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `payment_method` VARCHAR(50) DEFAULT 'COD',
    `voucher_id` BIGINT,
    `discount_amount` DECIMAL(15, 2) DEFAULT 0.00,
    `note` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE SET NULL,
    FOREIGN KEY (`voucher_id`) REFERENCES `vouchers`(`voucher_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `order_items` (
    `order_item_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `order_id` BIGINT NOT NULL,
    `product_id` BIGINT,
    `quantity` INT NOT NULL,
    `price` DECIMAL(15, 2) NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `size` VARCHAR(10) DEFAULT NULL,
    `color` VARCHAR(20) DEFAULT NULL,
    FOREIGN KEY (`order_id`) REFERENCES `orders`(`order_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 7. Content & Interaction Tables
-- =========================================================================
CREATE TABLE `product_reviews` (
    `review_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `rating` INT CHECK (rating BETWEEN 1 AND 5),
    `comment` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `banners` (
    `banner_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `description` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `image_url` VARCHAR(255) NOT NULL,
    `link_url` VARCHAR(255),
    `display_order` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `start_date` DATETIME,
    `end_date` DATETIME,
    `position` VARCHAR(50) DEFAULT 'main',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sliders` (
    `slider_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `image_url` VARCHAR(255) NOT NULL,
    `link_url` VARCHAR(255),
    `display_order` INT DEFAULT 0,
    `is_active` BOOLEAN DEFAULT TRUE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `blog_posts` (
    `post_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    `slug` VARCHAR(255) UNIQUE,
    `excerpt` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `content` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
    `featured_image` VARCHAR(255),
    `author_id` BIGINT,
    `category` VARCHAR(50),
    `tags` VARCHAR(255),
    `is_published` BOOLEAN DEFAULT FALSE,
    `is_featured` BOOLEAN DEFAULT FALSE,
    `view_count` INT DEFAULT 0,
    `published_at` DATETIME,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`author_id`) REFERENCES `users`(`user_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 8. Analytics & Recommender System Tables
-- =========================================================================
CREATE TABLE `user_interactions` (
    `interaction_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `interaction_type` VARCHAR(50) NOT NULL,
    `interaction_value` DOUBLE DEFAULT 1.0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_ratings` (
    `rating_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `rating` DECIMAL(3, 2),
    `rating_count` INT DEFAULT 0,
    `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_user_product_rating` (`user_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recommendations_cache` (
    `cache_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `recommended_product_id` BIGINT NOT NULL,
    `recommendation_type` VARCHAR(50) DEFAULT 'HYBRID',
    `confidence_score` DECIMAL(10, 8) NOT NULL,
    `generated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`recommended_product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `product_similarity` (
    `similarity_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id_1` BIGINT NOT NULL,
    `product_id_2` BIGINT NOT NULL,
    `similarity_score` DECIMAL(10, 8),
    `similarity_type` VARCHAR(50) DEFAULT 'COSINE',
    `computed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`product_id_1`) REFERENCES `products`(`product_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id_2`) REFERENCES `products`(`product_id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_product_pair` (`product_id_1`, `product_id_2`, `similarity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_similarity` (
    `similarity_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id_1` BIGINT NOT NULL,
    `user_id_2` BIGINT NOT NULL,
    `similarity_score` DECIMAL(10, 8),
    `similarity_type` VARCHAR(50) DEFAULT 'COSINE',
    `computed_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id_1`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`user_id_2`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_user_pair` (`user_id_1`, `user_id_2`, `similarity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Recommendation performance indexes
CREATE INDEX idx_ui_user_created ON user_interactions(user_id, created_at);
CREATE INDEX idx_ui_product_created ON user_interactions(product_id, created_at);
CREATE INDEX idx_ui_type_created ON user_interactions(interaction_type, created_at);
CREATE INDEX idx_ui_created ON user_interactions(created_at);
CREATE INDEX idx_rc_user_type ON recommendations_cache(user_id, recommendation_type);
CREATE INDEX idx_rc_expires ON recommendations_cache(expires_at);
CREATE INDEX idx_ps_product1_score ON product_similarity(product_id_1, similarity_score);
CREATE INDEX idx_ps_product2_score ON product_similarity(product_id_2, similarity_score);
CREATE INDEX idx_us_user1_score ON user_similarity(user_id_1, similarity_score);
CREATE INDEX idx_us_user2_score ON user_similarity(user_id_2, similarity_score);

-- =====================================================================
-- Demo seed data for AI Recommendation (Time-Decay + CF)
-- =====================================================================
SET @demo_now = NOW();

SET @u1 = (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1);
SET @u2 = (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1 OFFSET 1);
SET @u3 = (SELECT user_id FROM users ORDER BY created_at ASC LIMIT 1 OFFSET 2);

SET @p1 = (SELECT product_id FROM products WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1);
SET @p2 = (SELECT product_id FROM products WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1 OFFSET 1);
SET @p3 = (SELECT product_id FROM products WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1 OFFSET 2);
SET @p4 = (SELECT product_id FROM products WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1 OFFSET 3);
SET @p5 = (SELECT product_id FROM products WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1 OFFSET 4);
SET @p6 = (SELECT product_id FROM products WHERE is_active = 1 ORDER BY created_at DESC LIMIT 1 OFFSET 5);

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT u.user_id, @p1, 'view', 1.0, DATE_SUB(@demo_now, INTERVAL 6 HOUR)
FROM users u
WHERE u.user_id IS NOT NULL
LIMIT 20;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT u.user_id, @p1, 'add_to_cart', 1.0, DATE_SUB(@demo_now, INTERVAL 4 HOUR)
FROM users u
WHERE u.user_id IS NOT NULL
LIMIT 8;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT u.user_id, @p1, 'purchase', 1.0, DATE_SUB(@demo_now, INTERVAL 2 HOUR)
FROM users u
WHERE u.user_id IS NOT NULL
LIMIT 5;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT u.user_id, @p2, 'view', 1.0, DATE_SUB(@demo_now, INTERVAL 7 DAY)
FROM users u
WHERE u.user_id IS NOT NULL
LIMIT 25;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT u.user_id, @p2, 'purchase', 1.0, DATE_SUB(@demo_now, INTERVAL 7 DAY)
FROM users u
WHERE u.user_id IS NOT NULL
LIMIT 3;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT u.user_id, @p3, 'view', 1.0, DATE_SUB(@demo_now, INTERVAL 20 DAY)
FROM users u
WHERE u.user_id IS NOT NULL
LIMIT 30;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT u.user_id, @p3, 'purchase', 1.0, DATE_SUB(@demo_now, INTERVAL 20 DAY)
FROM users u
WHERE u.user_id IS NOT NULL
LIMIT 2;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT @u1, @p4, 'view', 1.0, DATE_SUB(@demo_now, INTERVAL 1 DAY)
WHERE @u1 IS NOT NULL AND @p4 IS NOT NULL;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT @u1, @p5, 'add_to_cart', 1.0, DATE_SUB(@demo_now, INTERVAL 12 HOUR)
WHERE @u1 IS NOT NULL AND @p5 IS NOT NULL;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT @u1, @p6, 'wishlist', 1.0, DATE_SUB(@demo_now, INTERVAL 3 HOUR)
WHERE @u1 IS NOT NULL AND @p6 IS NOT NULL;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT @u2, @p4, 'view', 1.0, DATE_SUB(@demo_now, INTERVAL 18 HOUR)
WHERE @u2 IS NOT NULL AND @p4 IS NOT NULL;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT @u2, @p5, 'purchase', 1.0, DATE_SUB(@demo_now, INTERVAL 10 HOUR)
WHERE @u2 IS NOT NULL AND @p5 IS NOT NULL;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT @u3, @p4, 'view', 1.0, DATE_SUB(@demo_now, INTERVAL 2 DAY)
WHERE @u3 IS NOT NULL AND @p4 IS NOT NULL;

INSERT INTO user_interactions (user_id, product_id, interaction_type, interaction_value, created_at)
SELECT @u3, @p6, 'add_to_cart', 1.0, DATE_SUB(@demo_now, INTERVAL 30 HOUR)
WHERE @u3 IS NOT NULL AND @p6 IS NOT NULL;

UPDATE products p
LEFT JOIN (
    SELECT product_id,
           SUM(CASE WHEN interaction_type = 'view' THEN 1 ELSE 0 END) AS total_views,
           SUM(CASE WHEN interaction_type = 'purchase' THEN 1 ELSE 0 END) AS total_purchases
    FROM user_interactions
    GROUP BY product_id
) t ON p.product_id = t.product_id
SET p.view_count = COALESCE(t.total_views, 0),
    p.purchase_count = COALESCE(t.total_purchases, 0)
WHERE p.is_active = 1
  AND p.product_id IN (@p1, @p2, @p3, @p4, @p5, @p6);

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- Optional verification
-- SELECT interaction_type, COUNT(*) FROM user_interactions WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY interaction_type;
-- SELECT product_id, interaction_type, COUNT(*) FROM user_interactions WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) GROUP BY product_id, interaction_type ORDER BY product_id;

-- =====================================================================
-- CHECK SCRIPT: run these queries after import to verify schema + demo data
-- =====================================================================

-- 1) Check all required tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
    AND table_name IN (
        'users', 'categories', 'products', 'shopping_carts', 'cart_items',
        'orders', 'order_items', 'wishlists', 'user_interactions',
        'user_ratings', 'product_similarity', 'user_similarity', 'recommendations_cache'
    )
ORDER BY table_name;

-- 2) Quick row counts (core + recommendation tables)
SELECT 'users' AS table_name, COUNT(*) AS total_rows FROM users
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'products', COUNT(*) FROM products
UNION ALL SELECT 'shopping_carts', COUNT(*) FROM shopping_carts
UNION ALL SELECT 'cart_items', COUNT(*) FROM cart_items
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL SELECT 'wishlists', COUNT(*) FROM wishlists
UNION ALL SELECT 'user_interactions', COUNT(*) FROM user_interactions
UNION ALL SELECT 'user_ratings', COUNT(*) FROM user_ratings
UNION ALL SELECT 'product_similarity', COUNT(*) FROM product_similarity
UNION ALL SELECT 'user_similarity', COUNT(*) FROM user_similarity
UNION ALL SELECT 'recommendations_cache', COUNT(*) FROM recommendations_cache;

-- 3) Validate cart variant columns
SHOW COLUMNS FROM cart_items;

-- 4) Validate recommendation indexes
SHOW INDEX FROM user_interactions;
SHOW INDEX FROM recommendations_cache;
SHOW INDEX FROM product_similarity;
SHOW INDEX FROM user_similarity;

-- 5) Inspect recent interactions used by Time-Decay
SELECT interaction_id, user_id, product_id, interaction_type, interaction_value, created_at
FROM user_interactions
ORDER BY created_at DESC
LIMIT 50;

-- 6) Interaction distribution in 30-day window
SELECT interaction_type, COUNT(*) AS total
FROM user_interactions
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY interaction_type
ORDER BY total DESC;

-- 7) Product interaction matrix (for CF demo visibility)
SELECT product_id,
             SUM(CASE WHEN interaction_type = 'view' THEN 1 ELSE 0 END) AS views,
             SUM(CASE WHEN interaction_type = 'add_to_cart' THEN 1 ELSE 0 END) AS add_to_cart,
             SUM(CASE WHEN interaction_type = 'wishlist' THEN 1 ELSE 0 END) AS wishlist,
             SUM(CASE WHEN interaction_type = 'purchase' THEN 1 ELSE 0 END) AS purchases
FROM user_interactions
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY product_id
ORDER BY purchases DESC, add_to_cart DESC, views DESC;

-- 8) Legacy trending check (purchase_count/view_count)
SELECT product_id, product_name, purchase_count, view_count,
             (COALESCE(purchase_count, 0) * 10 + COALESCE(view_count, 0)) AS legacy_trending_score
FROM products
WHERE is_active = 1
ORDER BY legacy_trending_score DESC, created_at DESC
LIMIT 10;

-- 9) Time-Decay trending check (same logic family as backend)
SET @check_lambda = 0.08;
SET @check_window_days = 30;

SELECT p.product_id,
             p.product_name,
             COALESCE(SUM(
                     CASE ui.interaction_type
                             WHEN 'purchase' THEN 10.0
                             WHEN 'add_to_cart' THEN 3.0
                             WHEN 'wishlist' THEN 2.0
                             WHEN 'view' THEN 1.0
                             ELSE 0.5
                     END * EXP(-@check_lambda * (TIMESTAMPDIFF(HOUR, ui.created_at, NOW()) / 24.0))
             ), 0) AS time_decay_score
FROM products p
LEFT JOIN user_interactions ui
             ON p.product_id = ui.product_id
            AND ui.created_at >= DATE_SUB(NOW(), INTERVAL @check_window_days DAY)
WHERE p.is_active = 1
GROUP BY p.product_id, p.product_name
ORDER BY time_decay_score DESC, p.created_at DESC
LIMIT 10;

-- 10) Recommendation cache health
SELECT recommendation_type,
             COUNT(*) AS total_rows,
             SUM(CASE WHEN expires_at IS NULL OR expires_at > NOW() THEN 1 ELSE 0 END) AS active_rows,
             SUM(CASE WHEN expires_at IS NOT NULL AND expires_at <= NOW() THEN 1 ELSE 0 END) AS expired_rows
FROM recommendations_cache
GROUP BY recommendation_type;
