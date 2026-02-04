-- Create database if not exists and select it
CREATE DATABASE IF NOT EXISTS `clothesshopdb`;
USE `clothesshopdb`;

-- Disable foreign key checks and safe update mode to allow table drops and updates
SET SQL_SAFE_UPDATES = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- Drop all tables if they exist to ensure a clean schema rebuild
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
    `discount_type` VARCHAR(20) DEFAULT 'PERCENTAGE', -- PERCENTAGE, FIXED_AMOUNT
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
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `categories`(`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
-- New: Persistent Shopping Cart
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
    `size` VARCHAR(50),
    `color` VARCHAR(50),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (`cart_id`) REFERENCES `shopping_carts`(`cart_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE,
    -- Unique constraint should ideally include size and color, but handling NULLs in unique index varies.
    -- For simplicity, we remove strict uniqueness on DB or just unique on cart_id + product_id if variants are Products themselves.
    -- But since Products table has size/color columns, maybe product_id IS the variant?
    -- Looking at Products table: `sku`, `color`, `size`. So `products` table stores Variants?
    -- If `products` table stores variants, then `product_id` IS unique for that size/color.
    -- User's Product table: has `color` and `size`. 
    -- So `product_id` uniquely identifies a specific variant (e.g. Shirt Red M).
    -- In that case, `unique_cart_product` (cart_id, product_id) IS CORRECT.
    -- And `size`/`color` columns in `cart_items` are redundant but harmless (denormalization).
    -- WAIT. If Product table has size/color, then do we need to store it in cart_items?
    -- `CartItem` model has `size` and `color`.
    -- `CartDAO` inserts them.
    -- If I assume `product_id` is just the "Parent Style", then we need size/color in cart.
    -- If `product_id` is the "SKU/Variant", then we don't need size/color in cart (it's properties of product).
    -- The `CartService.addItem` takes `productId`, `size`, `color`.
    -- If `products` table has `size`/`color`, then `productId` implies them.
    -- However, often ecommerce has `product` as "Style" and `product_variant` table.
    -- Here `products` table has `size`, `color`.
    -- `sku` is unique.
    -- If I have Shirt X in Red/S and Red/M, are they 2 rows in `products` table?
    -- If yes, then `product_id` is unique variant.
    -- If no (e.g. product is "Shirt X" and has lists of sizes?), but column `size` is `VARCHAR(50)`, not `TEXT` or `JSON`.
    -- This implies 1 row per variant OR 1 row per style and size is just text description?
    -- Let's assume 1 row per variant since `stock_quantity` is there.
    -- IF 1 row per variant: `unique_cart_product` is correct. `size`/`color` in `cart_items` is redundant but useful for snapshot.
    -- So I will add `size` and `color` columns to `cart_items` to match `CartDAO` code, but keep Unique key as is.
    UNIQUE KEY `unique_cart_product` (`cart_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 5. Transactions (Orders)
-- =========================================================================
CREATE TABLE `orders` (
    `order_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `order_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `total_amount` DECIMAL(15, 2) NOT NULL,
    `status` VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
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
    -- Future proofing if size/color stored in order items
    `size` VARCHAR(10) DEFAULT NULL,
    `color` VARCHAR(20) DEFAULT NULL,
    FOREIGN KEY (`order_id`) REFERENCES `orders`(`order_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================================================
-- 6. Content & Interaction Tables
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
-- 7. Analytics & Recommender System Tables
-- =========================================================================
CREATE TABLE `user_interactions` (
    `interaction_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `interaction_type` VARCHAR(50) NOT NULL, -- VIEW, CLICK, PURCHASE
    `interaction_value` DOUBLE DEFAULT 1.0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`product_id`) REFERENCES `products`(`product_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_ratings` (
    `rating_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `product_id` BIGINT NOT NULL,
    `rating` DECIMAL(3, 2), -- 3.50, 4.00
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

-- Re-enable checks
SET FOREIGN_KEY_CHECKS = 1;
