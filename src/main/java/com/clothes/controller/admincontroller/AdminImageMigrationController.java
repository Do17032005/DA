package com.clothes.controller.admincontroller;

import com.clothes.service.CloudinaryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One-time migration: upload local images to Cloudinary and update DB URLs.
 * Access: /admin/migrate-images (admin login required)
 */
@RestController
@RequestMapping("/admin")
public class AdminImageMigrationController {

    private final CloudinaryService cloudinaryService;
    private final JdbcTemplate jdbcTemplate;

    public AdminImageMigrationController(CloudinaryService cloudinaryService, JdbcTemplate jdbcTemplate) {
        this.cloudinaryService = cloudinaryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/migrate-images")
    public ResponseEntity<Map<String, Object>> migrateImages(HttpSession session) {
        if (session.getAttribute("adminId") == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> result = new HashMap<>();
        List<String> migrated = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Migrate product images
        List<Map<String, Object>> products = jdbcTemplate.queryForList(
                "SELECT product_id, image_url FROM products WHERE image_url IS NOT NULL AND image_url LIKE '/uploads/%'");

        for (Map<String, Object> product : products) {
            Long productId = ((Number) product.get("product_id")).longValue();
            String oldUrl = (String) product.get("image_url");
            // oldUrl = "/uploads/products/xxx.png"
            String localPath = oldUrl.startsWith("/") ? oldUrl.substring(1) : oldUrl;
            File file = new File(localPath);

            if (file.exists()) {
                try {
                    String newUrl = cloudinaryService.uploadFile(file, "products");
                    jdbcTemplate.update("UPDATE products SET image_url = ? WHERE product_id = ?", newUrl, productId);
                    migrated.add("Product #" + productId + ": " + oldUrl + " -> " + newUrl);
                } catch (IOException e) {
                    errors.add("Product #" + productId + ": " + e.getMessage());
                }
            } else {
                errors.add("Product #" + productId + ": File not found - " + localPath);
            }
        }

        // Migrate avatar images
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT user_id, avatar_url FROM users WHERE avatar_url IS NOT NULL AND avatar_url LIKE '/uploads/%'");

        for (Map<String, Object> user : users) {
            Long userId = ((Number) user.get("user_id")).longValue();
            String oldUrl = (String) user.get("avatar_url");
            String localPath = oldUrl.startsWith("/") ? oldUrl.substring(1) : oldUrl;
            File file = new File(localPath);

            if (file.exists()) {
                try {
                    String newUrl = cloudinaryService.uploadFile(file, "avatars");
                    jdbcTemplate.update("UPDATE users SET avatar_url = ? WHERE user_id = ?", newUrl, userId);
                    migrated.add("User #" + userId + ": " + oldUrl + " -> " + newUrl);
                } catch (IOException e) {
                    errors.add("User #" + userId + ": " + e.getMessage());
                }
            } else {
                errors.add("User #" + userId + ": File not found - " + localPath);
            }
        }

        result.put("migrated", migrated);
        result.put("errors", errors);
        result.put("totalMigrated", migrated.size());
        result.put("totalErrors", errors.size());

        return ResponseEntity.ok(result);
    }
}
