-- Script to create user_vouchers table
USE `clothesshopdb`;

CREATE TABLE IF NOT EXISTS `user_vouchers` (
    `user_voucher_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `voucher_id` BIGINT NOT NULL,
    `collected_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `is_used` BOOLEAN DEFAULT FALSE,
    `used_at` TIMESTAMP NULL,
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`voucher_id`) REFERENCES `vouchers`(`voucher_id`) ON DELETE CASCADE,
    UNIQUE KEY `unique_user_voucher` (`user_id`, `voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
