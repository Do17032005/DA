
package com.clothes;

import java.sql.*;
import java.util.Properties;

public class DbFix {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-1bf49a9c-nghiengame005.c.aivencloud.com:27021/clothesshopdb?ssl-mode=REQUIRED&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
        Properties props = new Properties();
        props.setProperty("user", "avnadmin");
        props.setProperty("password", "AVNS_OY6UdTSUCEJY08Wic_V");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("Connected to database!");

            try (Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE IF NOT EXISTS user_vouchers (" +
                        "  user_voucher_id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "  user_id BIGINT NOT NULL," +
                        "  voucher_id BIGINT NOT NULL," +
                        "  collected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "  is_used BOOLEAN DEFAULT FALSE," +
                        "  used_at TIMESTAMP NULL," +
                        "  UNIQUE KEY unique_user_voucher (user_id, voucher_id)," +
                        "  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                        "  FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id) ON DELETE CASCADE" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

                stmt.execute(sql);
                System.out.println("Table 'user_vouchers' created or already exists!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
