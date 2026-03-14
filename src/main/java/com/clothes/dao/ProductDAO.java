package com.clothes.dao;

import com.clothes.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Product entity
 * Manages product data operations
 */
@Repository
public class ProductDAO {

    public static class TimeDecayTrendingScore {
        public Long productId;
        public String productName;
        public Double score;

        public TimeDecayTrendingScore(Long productId, String productName, Double score) {
            this.productId = productId;
            this.productName = productName;
            this.score = score;
        }
    }

    private final JdbcTemplate jdbcTemplate;

    public ProductDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper for Product
     */
    private static class ProductRowMapper implements RowMapper<Product> {
        @Override
        public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
            Product product = new Product();
            product.setProductId(rs.getLong("product_id"));
            product.setProductName(rs.getString("product_name"));
            product.setDescription(rs.getString("description"));

            long categoryId = rs.getLong("category_id");
            if (!rs.wasNull()) {
                product.setCategoryId(categoryId);
            }

            product.setPrice(rs.getBigDecimal("price"));

            BigDecimal discountPrice = rs.getBigDecimal("discount_price");
            if (!rs.wasNull()) {
                product.setDiscountPrice(discountPrice);
            }

            product.setStockQuantity(rs.getInt("stock_quantity"));
            product.setImageUrl(rs.getString("image_url"));
            product.setBrand(rs.getString("brand"));
            product.setColor(rs.getString("color"));
            product.setSize(rs.getString("size"));
            product.setMaterial(rs.getString("material"));
            product.setGender(Product.Gender.fromValue(rs.getString("gender")));
            product.setSeason(Product.Season.fromValue(rs.getString("season")));

            // New field for admin
            product.setSku(rs.getString("sku"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                product.setCreatedAt(created.toLocalDateTime());
            }

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) {
                product.setUpdatedAt(updated.toLocalDateTime());
            }

            product.setIsActive(rs.getBoolean("is_active"));

            // Handle optional columns
            try {
                product.setViewCount(rs.getInt("view_count"));
            } catch (Exception e) {
                product.setViewCount(0);
            }

            try {
                product.setPurchaseCount(rs.getInt("purchase_count"));
            } catch (Exception e) {
                product.setPurchaseCount(0);
            }

            // Map sold_count if present (for best selling query)
            try {
                int soldCount = rs.getInt("sold_count");
                if (!rs.wasNull()) {
                    product.setSoldCount(soldCount);
                }
            } catch (SQLException e) {
                // Ignore if column not present
            }

            try {
                product.setIsNew(rs.getBoolean("is_new"));
            } catch (Exception e) {
                product.setIsNew(false);
            }

            try {
                product.setIsHot(rs.getBoolean("is_hot"));
            } catch (Exception e) {
                product.setIsHot(false);
            }

            return product;
        }
    }

    /**
     * Find product by ID
     */
    public Optional<Product> findById(Long productId) {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        List<Product> products = jdbcTemplate.query(sql, new ProductRowMapper(), productId);
        return products.isEmpty() ? Optional.empty() : Optional.of(products.get(0));
    }

    /**
     * Find all active products
     */
    public List<Product> findAllActive() {
        String sql = "SELECT * FROM products WHERE is_active = 1 ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new ProductRowMapper());
    }

    /**
     * Find products by IDs
     */
    public List<Product> findByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        String placeholders = String.join(",", productIds.stream().map(id -> "?").toArray(String[]::new));
        String sql = "SELECT * FROM products WHERE product_id IN (" + placeholders + ") " +
                "AND is_active = TRUE ORDER BY FIELD(product_id, " + placeholders + ")";

        Object[] params = new Object[productIds.size() * 2];
        for (int i = 0; i < productIds.size(); i++) {
            params[i] = productIds.get(i);
            params[i + productIds.size()] = productIds.get(i);
        }

        return jdbcTemplate.query(sql, new ProductRowMapper(), params);
    }

    /**
     * Find trending products (most views/purchases recently)
     */
    public List<Product> findTrending(int limit) {
        String sql = "SELECT * FROM products " +
                "WHERE is_active = TRUE " +
                "ORDER BY COALESCE(purchase_count, 0) * 10 + COALESCE(view_count, 0) DESC, created_at DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, new ProductRowMapper(), limit);
    }

    /**
     * Find trending products using time-decay interaction scoring
     */
    public List<Product> findTrendingTimeDecay(int limit, int windowDays, double lambda) {
        String sql = "SELECT p.* FROM products p " +
                "LEFT JOIN user_interactions ui ON p.product_id = ui.product_id " +
                "AND ui.created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "WHERE p.is_active = TRUE " +
                "GROUP BY p.product_id " +
                "ORDER BY COALESCE(SUM( " +
                "CASE ui.interaction_type " +
                "WHEN 'purchase' THEN 10.0 " +
                "WHEN 'add_to_cart' THEN 3.0 " +
                "WHEN 'wishlist' THEN 2.0 " +
                "WHEN 'view' THEN 1.0 " +
                "ELSE 0.5 END " +
                "* EXP(-? * (TIMESTAMPDIFF(HOUR, ui.created_at, NOW()) / 24.0))" +
                "), 0) DESC, p.created_at DESC " +
                "LIMIT ?";

        return jdbcTemplate.query(sql, new ProductRowMapper(), windowDays, lambda, limit);
    }

    /**
     * Get raw time-decay scores for debugging/demo
     */
    public List<TimeDecayTrendingScore> getTimeDecayTrendingScores(int limit, int windowDays, double lambda) {
        String sql = "SELECT p.product_id, p.product_name, COALESCE(SUM( " +
                "CASE ui.interaction_type " +
                "WHEN 'purchase' THEN 10.0 " +
                "WHEN 'add_to_cart' THEN 3.0 " +
                "WHEN 'wishlist' THEN 2.0 " +
                "WHEN 'view' THEN 1.0 " +
                "ELSE 0.5 END " +
                "* EXP(-? * (TIMESTAMPDIFF(HOUR, ui.created_at, NOW()) / 24.0))" +
                "), 0) AS time_decay_score " +
                "FROM products p " +
                "LEFT JOIN user_interactions ui ON p.product_id = ui.product_id " +
                "AND ui.created_at >= DATE_SUB(NOW(), INTERVAL ? DAY) " +
                "WHERE p.is_active = TRUE " +
                "GROUP BY p.product_id, p.product_name " +
                "ORDER BY time_decay_score DESC, p.created_at DESC " +
                "LIMIT ?";

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new TimeDecayTrendingScore(
                        rs.getLong("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("time_decay_score")),
                lambda, windowDays, limit);
    }

    /**
     * Find products marked as new
     */
    public List<Product> findNewProducts(int limit) {
        String sql = "SELECT * FROM products WHERE is_active = TRUE AND is_new = TRUE " +
                "ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, new ProductRowMapper(), limit);
    }

    /**
     * Find products marked as hot
     */
    public List<Product> findHotProducts(int limit) {
        String sql = "SELECT * FROM products WHERE is_active = TRUE AND is_hot = TRUE " +
                "ORDER BY updated_at DESC, created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, new ProductRowMapper(), limit);
    }

    /**
     * Update view count
     */
    public int incrementViewCount(Long productId) {
        String sql = "UPDATE products SET view_count = view_count + 1 WHERE product_id = ?";
        return jdbcTemplate.update(sql, productId);
    }

    /**
     * Update purchase count
     */
    public int incrementPurchaseCount(Long productId) {
        String sql = "UPDATE products SET purchase_count = purchase_count + 1 WHERE product_id = ?";
        return jdbcTemplate.update(sql, productId);
    }

    /**
     * Find products by category
     */
    public List<Product> findByCategoryId(Long categoryId) {
        String sql = "SELECT * FROM products WHERE category_id = ? AND is_active = TRUE";
        return jdbcTemplate.query(sql, new ProductRowMapper(), categoryId);
    }

    /**
     * Search products by name or description
     */
    public List<Product> search(String keyword, int limit) {
        String sql = "SELECT * FROM products " +
                "WHERE is_active = TRUE " +
                "AND (product_name LIKE ? OR description LIKE ?) " +
                "ORDER BY (purchase_count * 5 + view_count) DESC " +
                "LIMIT ?";
        String searchPattern = "%" + keyword + "%";
        return jdbcTemplate.query(sql, new ProductRowMapper(), searchPattern, searchPattern, limit);
    }

    /**
     * Get total product count
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM products WHERE is_active = TRUE";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Find products by gender
     */
    public List<Product> findByGender(Product.Gender gender) {
        String sql = "SELECT * FROM products WHERE gender = ? AND is_active = TRUE ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new ProductRowMapper(), gender.name());
    }

    /**
     * Find products by price range
     */
    public List<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        String sql = "SELECT * FROM products WHERE price BETWEEN ? AND ? AND is_active = TRUE ORDER BY price ASC";
        return jdbcTemplate.query(sql, new ProductRowMapper(), minPrice, maxPrice);
    }

    /**
     * Find products with filters
     */
    public List<Product> findWithFilters(Long categoryId, Product.Gender gender,
            BigDecimal minPrice, BigDecimal maxPrice,
            String sortBy, Boolean isNew, Boolean isHot) {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE is_active = 1");

        List<Object> params = new java.util.ArrayList<>();

        // Build WHERE clause
        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }

        if (gender != null) {
            sql.append(" AND gender = ?");
            params.add(gender.name()); // Use enum name (KIDS) instead of value (kids)
        }

        if (minPrice != null && maxPrice != null) {
            sql.append(" AND price BETWEEN ? AND ?");
            params.add(minPrice);
            params.add(maxPrice);
        } else if (minPrice != null) {
            sql.append(" AND price >= ?");
            params.add(minPrice);
        } else if (maxPrice != null) {
            sql.append(" AND price <= ?");
            params.add(maxPrice);
        }

        boolean filterNew = Boolean.TRUE.equals(isNew);
        boolean filterHot = Boolean.TRUE.equals(isHot);
        if (filterNew && filterHot) {
            sql.append(" AND (is_new = TRUE OR is_hot = TRUE)");
        } else if (filterNew) {
            sql.append(" AND is_new = TRUE");
        } else if (filterHot) {
            sql.append(" AND is_hot = TRUE");
        }

        // Build ORDER BY clause
        if (sortBy != null) {
            switch (sortBy) {
                case "price_asc":
                    sql.append(" ORDER BY price ASC");
                    break;
                case "price_desc":
                    sql.append(" ORDER BY price DESC");
                    break;
                case "name_asc":
                    sql.append(" ORDER BY product_name ASC");
                    break;
                case "name_desc":
                    sql.append(" ORDER BY product_name DESC");
                    break;
                case "newest":
                    sql.append(" ORDER BY created_at DESC");
                    break;
                case "popular":
                    sql.append(" ORDER BY (purchase_count * 10 + view_count) DESC");
                    break;
                default:
                    sql.append(" ORDER BY created_at DESC");
            }
        } else {
            sql.append(" ORDER BY created_at DESC");
        }
        System.out.println("SQL: " + sql.toString());
        System.out.println("Params: " + params);
        return jdbcTemplate.query(sql.toString(), new ProductRowMapper(), params.toArray());
    }

    /**
     * Find products for admin with filters (include inactive)
     */
    public List<Product> findAdminWithFilters(String keyword, Long categoryId, String status,
            String sortBy, Boolean isNew, Boolean isHot) {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (product_name LIKE ? OR sku LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }

        if (status != null && !status.isBlank()) {
            switch (status) {
                case "active":
                    sql.append(" AND is_active = TRUE");
                    break;
                case "inactive":
                    sql.append(" AND is_active = FALSE");
                    break;
                case "out_of_stock":
                    sql.append(" AND stock_quantity <= 0");
                    break;
                default:
                    break;
            }
        }

        boolean filterNew = Boolean.TRUE.equals(isNew);
        boolean filterHot = Boolean.TRUE.equals(isHot);
        if (filterNew && filterHot) {
            sql.append(" AND (is_new = TRUE OR is_hot = TRUE)");
        } else if (filterNew) {
            sql.append(" AND is_new = TRUE");
        } else if (filterHot) {
            sql.append(" AND is_hot = TRUE");
        }

        if (sortBy != null && !sortBy.isBlank()) {
            switch (sortBy) {
                case "price_asc":
                    sql.append(" ORDER BY price ASC");
                    break;
                case "price_desc":
                    sql.append(" ORDER BY price DESC");
                    break;
                case "name":
                    sql.append(" ORDER BY product_name ASC");
                    break;
                case "newest":
                default:
                    sql.append(" ORDER BY created_at DESC");
                    break;
            }
        } else {
            sql.append(" ORDER BY created_at DESC");
        }

        return jdbcTemplate.query(sql.toString(), new ProductRowMapper(), params.toArray());
    }

    /**
     * Find products for admin with filters and pagination
     */
    public List<Product> findAdminWithFiltersPaginated(String keyword, Long categoryId, String status,
            String sortBy, Boolean isNew, Boolean isHot, int page, int size) {
        StringBuilder sql = new StringBuilder("SELECT p.*, COALESCE(SUM(oi.quantity), 0) as sold_count ");
        sql.append("FROM products p ");
        sql.append("LEFT JOIN order_items oi ON p.product_id = oi.product_id ");
        sql.append("LEFT JOIN orders o ON oi.order_id = o.order_id ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new java.util.ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (p.product_name LIKE ? OR p.sku LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (categoryId != null) {
            sql.append(" AND p.category_id = ?");
            params.add(categoryId);
        }

        if (status != null && !status.isBlank()) {
            switch (status) {
                case "active":
                    sql.append(" AND p.is_active = TRUE");
                    break;
                case "inactive":
                    sql.append(" AND p.is_active = FALSE");
                    break;
                case "out_of_stock":
                    sql.append(" AND p.stock_quantity <= 0");
                    break;
                default:
                    break;
            }
        }

        boolean filterNew = Boolean.TRUE.equals(isNew);
        boolean filterHot = Boolean.TRUE.equals(isHot);
        if (filterNew && filterHot) {
            sql.append(" AND (p.is_new = TRUE OR p.is_hot = TRUE)");
        } else if (filterNew) {
            sql.append(" AND p.is_new = TRUE");
        } else if (filterHot) {
            sql.append(" AND p.is_hot = TRUE");
        }

        sql.append(" GROUP BY p.product_id ");

        if (sortBy != null && !sortBy.isBlank()) {
            switch (sortBy) {
                case "price_asc":
                    sql.append(" ORDER BY p.price ASC");
                    break;
                case "price_desc":
                    sql.append(" ORDER BY p.price DESC");
                    break;
                case "name":
                    sql.append(" ORDER BY p.product_name ASC");
                    break;
                case "top_selling":
                    sql.append(" ORDER BY sold_count DESC, p.view_count DESC");
                    break;
                default:
                    sql.append(" ORDER BY p.created_at DESC");
                    break;
            }
        } else {
            sql.append(" ORDER BY p.created_at DESC");
        }

        sql.append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), new ProductRowMapper(), params.toArray());
    }

    /**
     * Count products for admin with filters
     */
    public int countAdminWithFilters(String keyword, Long categoryId, String status,
            Boolean isNew, Boolean isHot) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM products WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (product_name LIKE ? OR sku LIKE ?)");
            String pattern = "%" + keyword.trim() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        if (categoryId != null) {
            sql.append(" AND category_id = ?");
            params.add(categoryId);
        }

        if (status != null && !status.isBlank()) {
            switch (status) {
                case "active":
                    sql.append(" AND is_active = TRUE");
                    break;
                case "inactive":
                    sql.append(" AND is_active = FALSE");
                    break;
                case "out_of_stock":
                    sql.append(" AND stock_quantity <= 0");
                    break;
                default:
                    break;
            }
        }

        boolean filterNew = Boolean.TRUE.equals(isNew);
        boolean filterHot = Boolean.TRUE.equals(isHot);
        if (filterNew && filterHot) {
            sql.append(" AND (is_new = TRUE OR is_hot = TRUE)");
        } else if (filterNew) {
            sql.append(" AND is_new = TRUE");
        } else if (filterHot) {
            sql.append(" AND is_hot = TRUE");
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    /**
     * Find products by brand
     */
    public List<Product> findByBrand(String brand) {
        String sql = "SELECT * FROM products WHERE brand = ? AND is_active = TRUE ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new ProductRowMapper(), brand);
    }

    /**
     * Get all distinct brands
     */
    public List<String> getAllBrands() {
        String sql = "SELECT DISTINCT brand FROM products WHERE brand IS NOT NULL AND is_active = TRUE ORDER BY brand";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Get all distinct colors
     */
    public List<String> getAllColors() {
        String sql = "SELECT DISTINCT color FROM products WHERE color IS NOT NULL AND is_active = TRUE ORDER BY color";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Get all distinct sizes
     */
    public List<String> getAllSizes() {
        String sql = "SELECT DISTINCT size FROM products WHERE size IS NOT NULL AND is_active = TRUE ORDER BY size";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    /**
     * Find all products (including inactive)
     */
    public List<Product> findAll() {
        String sql = "SELECT * FROM products ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new ProductRowMapper());
    }

    /**
     * Find all inactive products
     */
    public List<Product> findAllInactive() {
        String sql = "SELECT * FROM products WHERE is_active = FALSE ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new ProductRowMapper());
    }

    /**
     * Find products with low stock
     */
    public List<Product> findLowStock(int threshold) {
        String sql = "SELECT * FROM products WHERE stock_quantity <= ? AND is_active = TRUE " +
                "ORDER BY stock_quantity ASC";
        return jdbcTemplate.query(sql, new ProductRowMapper(), threshold);
    }

    /**
     * Find best selling products based on completed orders
     */
    public List<Product> findBestSelling(int limit) {
        String sql = "SELECT p.*, COALESCE(SUM(oi.quantity), 0) as sold_count " +
                "FROM products p " +
                "LEFT JOIN order_items oi ON p.product_id = oi.product_id " +
                "LEFT JOIN orders o ON oi.order_id = o.order_id " +
                "WHERE p.is_active = 1 AND (o.status = 'COMPLETED' OR o.status IS NULL) " +
                "GROUP BY p.product_id " +
                "ORDER BY sold_count DESC, p.view_count DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, new ProductRowMapper(), limit);
    }

    /**
     * Update product status
     */
    public int updateStatus(Long productId, boolean isActive) {
        String sql = "UPDATE products SET is_active = ?, updated_at = NOW() WHERE product_id = ?";
        return jdbcTemplate.update(sql, isActive, productId);
    }

    /**
     * Update new flag
     */
    public int updateNewStatus(Long productId, boolean isNew) {
        String sql = "UPDATE products SET is_new = ?, updated_at = NOW() WHERE product_id = ?";
        return jdbcTemplate.update(sql, isNew, productId);
    }

    /**
     * Update hot flag
     */
    public int updateHotStatus(Long productId, boolean isHot) {
        String sql = "UPDATE products SET is_hot = ?, updated_at = NOW() WHERE product_id = ?";
        return jdbcTemplate.update(sql, isHot, productId);
    }

    /**
     * Update stock quantity
     */
    public int updateStock(Long productId, int stockQuantity) {
        String sql = "UPDATE products SET stock_quantity = ?, updated_at = NOW() WHERE product_id = ?";
        return jdbcTemplate.update(sql, stockQuantity, productId);
    }

    /**
     * Update product
     */
    public int update(Product product) {
        String sql = "UPDATE products SET " +
                "product_name = ?, description = ?, category_id = ?, price = ?, " +
                "discount_price = ?, stock_quantity = ?, image_url = ?, brand = ?, " +
                "color = ?, size = ?, material = ?, gender = ?, season = ?, " +
                "sku = ?, is_active = ?, is_new = ?, is_hot = ?, updated_at = NOW() " +
                "WHERE product_id = ?";

        return jdbcTemplate.update(sql,
                product.getProductName(),
                product.getDescription(),
                product.getCategoryId(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getBrand(),
                product.getColor(),
                product.getSize(),
                product.getMaterial(),
                product.getGender() != null ? product.getGender().name() : null,
                product.getSeason() != null ? product.getSeason().name() : null,
                product.getSku(),
                product.getIsActive(),
                Boolean.TRUE.equals(product.getIsNew()),
                Boolean.TRUE.equals(product.getIsHot()),
                product.getProductId());
    }

    /**
     * Save new product
     */
    public Long save(Product product) {
        String sql = "INSERT INTO products (product_name, description, category_id, price, " +
                "discount_price, stock_quantity, image_url, brand, color, size, material, " +
                "gender, season, sku, is_active, is_new, is_hot, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        jdbcTemplate.update(sql,
                product.getProductName(),
                product.getDescription(),
                product.getCategoryId(),
                product.getPrice(),
                product.getDiscountPrice(),
                product.getStockQuantity(),
                product.getImageUrl(),
                product.getBrand(),
                product.getColor(),
                product.getSize(),
                product.getMaterial(),
                product.getGender() != null ? product.getGender().name() : null,
                product.getSeason() != null ? product.getSeason().name() : null,
                product.getSku(),
                product.getIsActive(),
                Boolean.TRUE.equals(product.getIsNew()),
                Boolean.TRUE.equals(product.getIsHot()));

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /**
     * Delete product
     */
    public int delete(Long productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        return jdbcTemplate.update(sql, productId);
    }

    /**
     * Find product by SKU
     */
    public Product findBySku(String sku) {
        String sql = "SELECT * FROM products WHERE sku = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new ProductRowMapper(), sku);
        } catch (Exception e) {
            return null;
        }
    }
}
