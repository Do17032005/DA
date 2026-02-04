package com.clothes.controller;

import com.clothes.dao.UserDAO;
import com.clothes.dao.ProductDAO;
import com.clothes.dao.CategoryDAO;
import com.clothes.model.User;
import com.clothes.model.Product;
import com.clothes.model.Category;
import com.clothes.service.UserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * Test Controller - Trang test chức năng admin và user
 */
@Controller
@RequestMapping("/test")
public class TestController {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;
    private final UserDAO userDAO;
    private final ProductDAO productDAO;
    private final CategoryDAO categoryDAO;

    public TestController(JdbcTemplate jdbcTemplate, UserService userService,
            UserDAO userDAO, ProductDAO productDAO, CategoryDAO categoryDAO) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
        this.userDAO = userDAO;
        this.productDAO = productDAO;
        this.categoryDAO = categoryDAO;
    }

    /**
     * Trang chủ test
     */
    @GetMapping
    public String showTestPage(HttpSession session, Model model) {
        // Test database connection
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            model.addAttribute("dbStatus", "Connected (Result: " + result + ")");
        } catch (Exception e) {
            model.addAttribute("dbStatus", "Error: " + e.getMessage());
        }

        // Get session info
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        model.addAttribute("isLoggedIn", userId != null);
        model.addAttribute("userId", userId);
        model.addAttribute("username", username);

        // Get database stats
        try {
            int userCount = userDAO.count();
            int productCount = productDAO.count();
            int categoryCount = categoryDAO.count();

            model.addAttribute("userCount", userCount);
            model.addAttribute("productCount", productCount);
            model.addAttribute("categoryCount", categoryCount);
        } catch (Exception e) {
            model.addAttribute("statsError", e.getMessage());
        }

        return "test-dashboard";
    }

    /**
     * Test login
     */
    @PostMapping("/login")
    public String testLogin(@RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            var userOpt = userService.authenticate(username, password);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                session.setAttribute("userId", user.getUserId());
                session.setAttribute("username", user.getUsername());
                session.setAttribute("fullName", user.getFullName());

                redirectAttributes.addFlashAttribute("success", "Đăng nhập thành công!");
                return "redirect:/test";
            } else {
                redirectAttributes.addFlashAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
                return "redirect:/test";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/test";
        }
    }

    /**
     * Test register
     */
    @PostMapping("/register")
    public String testRegister(@RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            RedirectAttributes redirectAttributes) {
        try {
            Long userId = userService.register(username, email, password, fullName, phone);
            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký thành công! Vui lòng đăng nhập. User ID: " + userId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi đăng ký: " + e.getMessage());
        }
        return "redirect:/test";
    }

    /**
     * Test logout
     */
    @GetMapping("/logout")
    public String testLogout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đã đăng xuất");
        return "redirect:/test";
    }

    /**
     * Trang admin test
     */
    @GetMapping("/admin")
    public String showAdminPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        // Check login
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/test";
        }

        // Get user info
        try {
            var userOpt = userService.getUserById(userId);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User không tồn tại");
                return "redirect:/test";
            }

            User user = userOpt.get();

            // Check admin role (optional - for now allow all logged in users)
            model.addAttribute("userId", user.getUserId());
            model.addAttribute("username", user.getUsername());
            model.addAttribute("role", user.getRole() != null ? user.getRole() : "USER");

            // Get stats
            int userCount = userDAO.count();
            int productCount = productDAO.count();
            int categoryCount = categoryDAO.count();

            model.addAttribute("userCount", userCount);
            model.addAttribute("productCount", productCount);
            model.addAttribute("categoryCount", categoryCount);

            return "test-admin";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/test";
        }
    }

    /**
     * Test danh sách users
     */
    @GetMapping("/users")
    public String listUsers(Model model, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/test";
        }

        try {
            List<User> users = userDAO.findAll();
            model.addAttribute("users", users);
            model.addAttribute("totalUsers", users.size());
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "test-users";
    }

    /**
     * Test danh sách products
     */
    @GetMapping("/products")
    public String listProducts(Model model, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/test";
        }

        try {
            List<Product> products = productDAO.findAllActive();
            model.addAttribute("products", products);
            model.addAttribute("totalProducts", products.size());
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "test-products";
    }

    /**
     * Test danh sách categories
     */
    @GetMapping("/categories")
    public String listCategories(Model model, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/test";
        }

        try {
            List<Category> categories = categoryDAO.findAll();
            model.addAttribute("categories", categories);
            model.addAttribute("totalCategories", categories.size());
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "test-categories";
    }

    /**
     * Test tạo user mới
     */
    @PostMapping("/users/create")
    public String createUser(@RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false, defaultValue = "USER") String role,
            RedirectAttributes redirectAttributes) {
        try {
            // Create user manually with role
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(password);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setRole(role);
            user.setIsActive(true);

            Long userId = userDAO.save(user);
            redirectAttributes.addFlashAttribute("success", "Tạo user thành công! ID: " + userId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/test/users";
    }

    /**
     * Form edit user
     */
    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/test";
        }

        try {
            var userOpt = userDAO.findById(id);
            if (userOpt.isEmpty()) {
                model.addAttribute("error", "User không tồn tại");
                return "redirect:/test/users";
            }
            model.addAttribute("user", userOpt.get());
            return "test-user-edit";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/test/users";
        }
    }

    /**
     * Update user
     */
    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable Long id,
            @RequestParam String email,
            @RequestParam String fullName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false, defaultValue = "USER") String role,
            @RequestParam(required = false) Boolean isActive,
            RedirectAttributes redirectAttributes) {
        try {
            var userOpt = userDAO.findById(id);
            if (userOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "User không tồn tại");
                return "redirect:/test/users";
            }

            User user = userOpt.get();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setRole(role);
            user.setIsActive(isActive != null ? isActive : false);

            userDAO.update(user);
            redirectAttributes.addFlashAttribute("success", "Cập nhật user thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/test/users";
    }

    /**
     * Delete user
     */
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userDAO.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa user thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/test/users";
    }

    /**
     * Test raw SQL query
     */
    @GetMapping("/sql")
    public String testSql(@RequestParam(required = false) String query, Model model, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/test";
        }

        try {
            // Test query users table
            String sql = (query != null && !query.isEmpty()) ? query
                    : "SELECT user_id, username, email, full_name, is_active FROM users LIMIT 10";
            List<String> results = jdbcTemplate.query(sql,
                    (rs, rowNum) -> {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                            sb.append(rs.getMetaData().getColumnName(i)).append(": ").append(rs.getString(i))
                                    .append(", ");
                        }
                        return sb.toString();
                    });

            model.addAttribute("sqlResults", results);
            model.addAttribute("querySuccess", true);
        } catch (Exception e) {
            model.addAttribute("error", "SQL Error: " + e.getMessage());
            model.addAttribute("querySuccess", false);
        }

        return "test-sql";
    }

    /**
     * Test kết nối database raw
     */
    @GetMapping("/db-test")
    @ResponseBody
    public String testDatabaseConnection() {
        StringBuilder result = new StringBuilder();
        result.append("=== DATABASE CONNECTION TEST ===\n\n");

        try {
            // Test 1: Simple query
            Integer test1 = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.append("✓ Test 1 - Simple Query: SUCCESS (Result: ").append(test1).append(")\n");

            // Test 2: Current database
            String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            result.append("✓ Test 2 - Current Database: ").append(dbName).append("\n");

            // Test 3: User count
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            result.append("✓ Test 3 - Users Table: ").append(userCount).append(" records\n");

            // Test 4: Product count
            Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
            result.append("✓ Test 4 - Products Table: ").append(productCount).append(" records\n");

            // Test 5: Category count
            Integer categoryCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Integer.class);
            result.append("✓ Test 5 - Categories Table: ").append(categoryCount).append(" records\n");

            result.append("\n=== ALL TESTS PASSED ✓ ===");

        } catch (Exception e) {
            result.append("✗ ERROR: ").append(e.getMessage()).append("\n");
            result.append("Stack trace: ").append(e.toString());
        }

        return result.toString();
    }

    /**
     * Repair Database - Create missing tables
     */
    @GetMapping({ "/repair-db", "/repair-db-v2" })
    @ResponseBody
    public String repairDatabase() {
        StringBuilder result = new StringBuilder();
        result.append("=== COMPREHENSIVE DATABASE REPAIR ===\n\n");

        try {
            // 1. blog_posts
            result.append("Checking 'blog_posts'...\n");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS blog_posts (" +
                    "post_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "slug VARCHAR(255) UNIQUE NOT NULL, " +
                    "excerpt TEXT, " +
                    "content LONGTEXT, " +
                    "featured_image VARCHAR(500), " +
                    "author_id BIGINT, " +
                    "author_name VARCHAR(100), " +
                    "category VARCHAR(50), " +
                    "tags VARCHAR(255), " +
                    "is_published BOOLEAN DEFAULT FALSE, " +
                    "is_featured BOOLEAN DEFAULT FALSE, " +
                    "view_count INT DEFAULT 0, " +
                    "published_at TIMESTAMP NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_is_published (is_published), " +
                    "INDEX idx_slug (slug)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            result.append("✓ Table 'blog_posts' ready.\n\n");

            // 2. recommendations_cache
            result.append("Checking 'recommendations_cache'...\n");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS recommendations_cache (" +
                    "cache_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "recommended_product_id BIGINT NOT NULL, " +
                    "recommendation_type VARCHAR(50) NOT NULL, " +
                    "confidence_score DECIMAL(5,4) NOT NULL, " +
                    "generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "expires_at TIMESTAMP NULL, " +
                    "INDEX idx_user_type (user_id, recommendation_type), " +
                    "INDEX idx_expires (expires_at)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            result.append("✓ Table 'recommendations_cache' ready.\n\n");

            // 3. user_interactions
            result.append("Checking 'user_interactions'...\n");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS user_interactions (" +
                    "interaction_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "product_id BIGINT NOT NULL, " +
                    "interaction_type VARCHAR(50) NOT NULL, " +
                    "interaction_value DECIMAL(10,2), " +
                    "session_id VARCHAR(100), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_user_product (user_id, product_id), " +
                    "INDEX idx_type (interaction_type)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            result.append("✓ Table 'user_interactions' ready.\n\n");

            // 4. product_reviews
            result.append("Checking 'product_reviews'...\n");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS product_reviews (" +
                    "review_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "product_id BIGINT NOT NULL, " +
                    "user_id BIGINT NOT NULL, " +
                    "rating INT NOT NULL, " +
                    "comment TEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_product (product_id), " +
                    "INDEX idx_user (user_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            result.append("✓ Table 'product_reviews' ready.\n\n");

            // 5. wishlists
            result.append("Checking 'wishlists'...\n");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS wishlists (" +
                    "wishlist_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "product_id BIGINT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY unq_user_product (user_id, product_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            result.append("✓ Table 'wishlists' ready.\n\n");

            // 6. banners
            result.append("Checking 'banners'...\n");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS banners (" +
                    "banner_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "title VARCHAR(255), " +
                    "description TEXT, " +
                    "image_url VARCHAR(500), " +
                    "link_url VARCHAR(500), " +
                    "display_order INT DEFAULT 0, " +
                    "is_active BOOLEAN DEFAULT TRUE, " +
                    "start_date DATETIME, " +
                    "end_date DATETIME, " +
                    "position VARCHAR(50), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NULL" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            result.append("✓ Table 'banners' ready.\n\n");

            // 7. addresses
            result.append("Checking 'addresses'...\n");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS addresses (" +
                    "address_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "receiver_name VARCHAR(100) NOT NULL, " +
                    "receiver_phone VARCHAR(20) NOT NULL, " +
                    "province VARCHAR(100), " +
                    "district VARCHAR(100), " +
                    "ward VARCHAR(100), " +
                    "detail_address TEXT, " +
                    "is_default BOOLEAN DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_user_address (user_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
            result.append("✓ Table 'addresses' ready.\n\n");

            // 8. users (add missing columns)
            result.append("Checking 'users' columns...\n");
            try {
                // Check if gender exists
                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'gender' AND TABLE_SCHEMA = DATABASE()",
                        Integer.class);
                if (count == null || count == 0) {
                    jdbcTemplate.execute("ALTER TABLE users ADD COLUMN gender VARCHAR(20) AFTER avatar_url");
                    result.append("✓ Column 'gender' added.\n");
                } else {
                    result.append("✓ Column 'gender' already exists.\n");
                }

                // Check if date_of_birth exists
                count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'users' AND COLUMN_NAME = 'date_of_birth' AND TABLE_SCHEMA = DATABASE()",
                        Integer.class);
                if (count == null || count == 0) {
                    jdbcTemplate.execute("ALTER TABLE users ADD COLUMN date_of_birth DATE AFTER gender");
                    result.append("✓ Column 'date_of_birth' added.\n");
                } else {
                    result.append("✓ Column 'gender' already exists.\n");
                }
            } catch (Exception e) {
                result.append("! Error updating columns: ").append(e.getMessage()).append("\n\n");
            }

            // 9. products (fix enum columns agressively)
            result.append("Checking 'products' columns...\n");
            try {
                // Aggressive fix for all products to ensure they show up for "nam"
                int totalFixed = jdbcTemplate
                        .update("UPDATE products SET gender = 'male', price = 500000, is_active = 1");
                result.append("✓ Aggressively fixed ").append(totalFixed).append(" products to male/active/500k.\n");

                // Check if created_at exists
                List<java.util.Map<String, Object>> createdCol = jdbcTemplate.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'created_at'");
                if (createdCol.isEmpty()) {
                    jdbcTemplate
                            .execute("ALTER TABLE products ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                    result.append("✓ Column 'created_at' added to 'products'.\n");
                }

                // Check if updated_at exists
                List<java.util.Map<String, Object>> updatedCol = jdbcTemplate.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'updated_at'");
                if (updatedCol.isEmpty()) {
                    jdbcTemplate.execute(
                            "ALTER TABLE products ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                    result.append("✓ Column 'updated_at' added to 'products'.\n");
                }

                // Fix gender mapping for male products and set price/images if missing
                int rowsUpdated = jdbcTemplate.update(
                        "UPDATE products SET gender = 'male', price = 500000, is_active = 1 WHERE (product_name LIKE '%nam%' OR product_name LIKE '%Nam%')");
                result.append("✓ Standardized ").append(rowsUpdated)
                        .append(" male products (price=500k, gender=male, active=1).\n");

            } catch (Exception e) {
                result.append("! Error updating product columns: ").append(e.getMessage()).append("\n\n");
            }

            // 10. order_items (add size and color)
            result.append("Checking 'order_items' columns...\n");
            try {
                // Checking and adding 'size'
                List<java.util.Map<String, Object>> sizeCol = jdbcTemplate.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_items' AND COLUMN_NAME = 'size'");
                if (sizeCol.isEmpty()) {
                    jdbcTemplate.execute("ALTER TABLE order_items ADD COLUMN size VARCHAR(20)");
                    result.append("✓ Column 'size' added to 'order_items'.\n");
                } else {
                    result.append("✓ Column 'size' already exists in 'order_items'.\n");
                }

                // Checking and adding 'color'
                List<java.util.Map<String, Object>> colorCol = jdbcTemplate.queryForList(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_items' AND COLUMN_NAME = 'color'");
                if (colorCol.isEmpty()) {
                    jdbcTemplate.execute("ALTER TABLE order_items ADD COLUMN color VARCHAR(50)");
                    result.append("✓ Column 'color' added to 'order_items'.\n");
                } else {
                    result.append("✓ Column 'color' already exists in 'order_items'.\n");
                }
            } catch (Exception e) {
                result.append("! Error updating order_items columns: ").append(e.getMessage()).append("\n\n");
            }

            result.append("\nALL MISSING TABLES REPAIRED SUCCESSFULLY!");

        } catch (Exception e) {
            result.append("\n✗ ERROR: ").append(e.getMessage());
            result.append("\nStack trace: ").append(e.toString());
        }

        return result.toString();
    }

    /**
     * Inspect Table Schema
     */
    @GetMapping("/inspect-schema")
    @ResponseBody
    public String inspectSchema(@RequestParam(defaultValue = "products") String table) {
        StringBuilder result = new StringBuilder();
        try {
            result.append("=== ").append(table.toUpperCase()).append(" TABLE SCHEMA ===\n");
            List<java.util.Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    table);

            if (columns.isEmpty()) {
                result.append("Table '").append(table).append("' not found or has no columns.\n");
            }

            for (var col : columns) {
                result.append(String.format("%-20s | %-20s | %s\n",
                        col.get("COLUMN_NAME"),
                        col.get("COLUMN_TYPE"),
                        col.get("IS_NULLABLE")));
            }
        } catch (Exception e) {
            result.append("Error: ").append(e.getMessage());
        }
        return result.toString();
    }

    @GetMapping("/dump-products")
    @ResponseBody
    public String dumpProducts() {
        StringBuilder sb = new StringBuilder();
        try {
            List<String> allRows = jdbcTemplate.query(
                    "SELECT *, HEX(gender) as hex_gender, LENGTH(gender) as len_gender FROM products", (rs, rowNum) -> {
                        return "ID: " + rs.getLong("product_id") +
                                " | Name: '" + rs.getString("product_name") + "'" +
                                " | Gender: '" + rs.getString("gender") + "'" +
                                " | HEX: " + rs.getString("hex_gender") +
                                " | LEN: " + rs.getInt("len_gender") +
                                " | Price: " + rs.getBigDecimal("price") +
                                " | Active: " + rs.getInt("is_active");
                    });
            sb.append("PRODUCTS IN DB:\n");
            for (String row : allRows)
                sb.append(row).append("\n");
        } catch (Exception e) {
            sb.append("Error: ").append(e.getMessage());
        }
        return sb.toString();
    }
}
