package com.clothes.dao;

import com.clothes.model.Category;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DAO for Category entity
 */
@Repository
public class CategoryDAO {

    private final JdbcTemplate jdbcTemplate;

    public CategoryDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class CategoryRowMapper implements RowMapper<Category> {
        @Override
        public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
            Category category = new Category();
            category.setCategoryId(rs.getLong("category_id"));
            category.setCategoryName(rs.getString("category_name"));
            category.setDescription(rs.getString("description"));

            long parentId = rs.getLong("parent_id");
            if (!rs.wasNull()) {
                category.setParentId(parentId);
            }

            // New fields for admin
            category.setSlug(rs.getString("slug"));
            category.setDisplayOrder(rs.getInt("display_order"));
            category.setIsActive(rs.getBoolean("is_active"));
            category.setIcon(rs.getString("icon"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                category.setCreatedAt(created.toLocalDateTime());
            }

            return category;
        }
    }

    public Long save(Category category) {
        String sql = "INSERT INTO categories (category_name, description, parent_id, slug, display_order, is_active, icon, created_at) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

        jdbcTemplate.update(sql,
                category.getCategoryName(),
                category.getDescription(),
                category.getParentId(),
                category.getSlug(),
                category.getDisplayOrder() != null ? category.getDisplayOrder() : 0,
                category.getIsActive() != null ? category.getIsActive() : true,
                category.getIcon());

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public int update(Category category) {
        String sql = "UPDATE categories SET category_name = ?, description = ?, parent_id = ?, " +
                "slug = ?, display_order = ?, is_active = ?, icon = ? " +
                "WHERE category_id = ?";

        return jdbcTemplate.update(sql,
                category.getCategoryName(),
                category.getDescription(),
                category.getParentId(),
                category.getSlug(),
                category.getDisplayOrder(),
                category.getIsActive(),
                category.getIcon(),
                category.getCategoryId());
    }

    public Optional<Category> findById(Long categoryId) {
        String sql = "SELECT * FROM categories WHERE category_id = ?";
        List<Category> categories = jdbcTemplate.query(sql, new CategoryRowMapper(), categoryId);
        return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
    }

    public List<Category> findAll() {
        String sql = "SELECT * FROM categories ORDER BY category_name";
        return jdbcTemplate.query(sql, new CategoryRowMapper());
    }

    public List<Category> findByParentId(Long parentId) {
        String sql = "SELECT * FROM categories WHERE parent_id = ? ORDER BY category_name";
        return jdbcTemplate.query(sql, new CategoryRowMapper(), parentId);
    }

    public List<Category> findRootCategories() {
        String sql = "SELECT * FROM categories WHERE parent_id IS NULL ORDER BY category_name";
        return jdbcTemplate.query(sql, new CategoryRowMapper());
    }

    public int delete(Long categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        return jdbcTemplate.update(sql, categoryId);
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM categories";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Get product count for a category
     */
    public int getProductCount(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, categoryId);
        return count != null ? count : 0;
    }

    /**
     * Update display order
     */
    public int updateDisplayOrder(Long categoryId, int displayOrder) {
        String sql = "UPDATE categories SET display_order = ? WHERE category_id = ?";
        return jdbcTemplate.update(sql, displayOrder, categoryId);
    }

    /**
     * Update slug
     */
    public int updateSlug(Long categoryId, String slug) {
        String sql = "UPDATE categories SET slug = ? WHERE category_id = ?";
        return jdbcTemplate.update(sql, slug, categoryId);
    }

    /**
     * Update is_active status
     */
    public int updateIsActive(Long categoryId, Boolean isActive) {
        String sql = "UPDATE categories SET is_active = ? WHERE category_id = ?";
        return jdbcTemplate.update(sql, isActive, categoryId);
    }

    /**
     * Find all categories with product count (using view)
     */
    public List<Map<String, Object>> findAllWithProductCount() {
        String sql = "SELECT * FROM v_categories_with_count ORDER BY display_order, category_name";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * Find categories by active status
     */
    public List<Category> findByActiveStatus(Boolean isActive) {
        String sql = "SELECT * FROM categories WHERE is_active = ? ORDER BY display_order, category_name";
        return jdbcTemplate.query(sql, new CategoryRowMapper(), isActive);
    }

    /**
     * Find category by slug
     */
    public Category findBySlug(String slug) {
        String sql = "SELECT * FROM categories WHERE slug = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new CategoryRowMapper(), slug);
        } catch (Exception e) {
            return null;
        }
    }
}
