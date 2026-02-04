-- Fix for cart_items table
-- 1. Add missing columns size and color
-- 2. Remove strict unique constraint on product_id to allow multiple variants (size/color) of same product

-- This block attempts to add columns. If they exist, it might fail depending on DB, 
-- giving a "Duplicate column name" error. If that happens, user can ignore.
-- But the user's error "Bad SQL Grammar" on INSERT suggests they are definitely missing.

ALTER TABLE `cart_items` ADD COLUMN `size` VARCHAR(50) DEFAULT NULL;
ALTER TABLE `cart_items` ADD COLUMN `color` VARCHAR(50) DEFAULT NULL;

-- Drop the old unique constraint that prevents adding same product with different sizes
ALTER TABLE `cart_items` DROP INDEX `unique_cart_product`;

-- Add composit index for performance (optional, but good)
CREATE INDEX `idx_cart_item_variant` ON `cart_items` (`cart_id`, `product_id`);
