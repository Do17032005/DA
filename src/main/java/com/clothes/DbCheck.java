
package com.clothes;

import java.sql.*;
import java.util.Properties;

public class DbCheck {
    public static void main(String[] args) {
        String url = "jdbc:mysql://mysql-1bf49a9c-nghiengame005.c.aivencloud.com:27021/clothesshopdb?ssl-mode=REQUIRED&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";
        Properties props = new Properties();
        props.setProperty("user", "avnadmin");
        props.setProperty("password", "AVNS_OY6UdTSUCEJY08Wic_V");

        try (Connection conn = DriverManager.getConnection(url, props)) {
            System.out.println("Connected to database!");

            checkTable(conn, "vouchers");
            checkTable(conn, "user_vouchers");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void checkTable(Connection conn, String tableName) {
        System.out.println("\n--- Checking table: " + tableName + " ---");
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, tableName, null);
            if (rs.next()) {
                System.out.println("Table " + tableName + " exists.");
                ResultSet cols = metaData.getColumns(null, null, tableName, null);
                while (cols.next()) {
                    System.out.println(
                            "Column: " + cols.getString("COLUMN_NAME") + " (" + cols.getString("TYPE_NAME") + ")");
                }
            } else {
                System.out.println("Table " + tableName + " DOES NOT exist!");
            }
        } catch (SQLException e) {
            System.err.println("Error checking table " + tableName + ": " + e.getMessage());
        }
    }
}
