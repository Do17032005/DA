package com.clothes.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting Database Migration Check...");

        try {
            // Check if 'size' column exists in 'cart_items'
            try {
                jdbcTemplate.execute("SELECT size FROM cart_items LIMIT 1");
                System.out.println("Column 'size' already exists.");
            } catch (Exception e) {
                System.out.println("Column 'size' missing. Adding...");
                jdbcTemplate.execute("ALTER TABLE cart_items ADD COLUMN size VARCHAR(50) DEFAULT NULL");
            }

            // Check if 'color' column exists
            try {
                jdbcTemplate.execute("SELECT color FROM cart_items LIMIT 1");
                System.out.println("Column 'color' already exists.");
            } catch (Exception e) {
                System.out.println("Column 'color' missing. Adding...");
                jdbcTemplate.execute("ALTER TABLE cart_items ADD COLUMN color VARCHAR(50) DEFAULT NULL");
            }

            // Drop old unique index if exists
            try {
                // Try to see if existing items violate the new unique logic (though we just
                // added cols)
                // Or just drop the index blindly.
                // In MySQL, to check index: SHOW INDEX FROM cart_items WHERE Key_name =
                // 'unique_cart_product'
                // But simpler to just try drop and ignore error.
                jdbcTemplate.execute("ALTER TABLE cart_items DROP INDEX unique_cart_product");
                System.out.println("Dropped index 'unique_cart_product'.");
            } catch (Exception e) {
                System.out.println(
                        "Index 'unique_cart_product' might not exist or already dropped. details: " + e.getMessage());
            }

            // Optional: Add proper index for searching
            try {
                jdbcTemplate.execute("CREATE INDEX idx_cart_item_variant ON cart_items (cart_id, product_id)");
                System.out.println("Created index 'idx_cart_item_variant'.");
            } catch (Exception e) {
                // Ignore if exists
            }

            System.out.println("Database Migration Completed Successfully.");

        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
