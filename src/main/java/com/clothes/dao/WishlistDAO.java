package com.clothes.dao;

import com.clothes.model.Wishlist;
import com.clothes.model.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Wishlist entity
 */
@Repository
public class WishlistDAO {

    private final JdbcTemplate jdbcTemplate;

    public WishlistDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class WishlistRowMapper implements RowMapper<Wishlist> {
        @Override
        public Wishlist mapRow(ResultSet rs, int rowNum) throws SQLException {
            Wishlist wishlist = new Wishlist();
            wishlist.setWishlistId(rs.getLong("wishlist_id"));
            wishlist.setUserId(rs.getLong("user_id"));
            wishlist.setProductId(rs.getLong("product_id"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                wishlist.setCreatedAt(created.toLocalDateTime());
            }

            // Try to get product info if joined
            try {
                Product product = new Product();
                product.setProductId(rs.getLong("product_id"));
                product.setProductName(rs.getString("product_name"));
                product.setPrice(rs.getBigDecimal("price"));
                product.setImageUrl(rs.getString("image_url"));
                product.setStockQuantity(rs.getInt("stock_quantity"));
                wishlist.setProduct(product);
            } catch (SQLException ignored) {
            }

            return wishlist;
        }
    }

    /**
     * Add to wishlist
     */
    public Long save(Wishlist wishlist) {
        // Check if already exists
        if (exists(wishlist.getUserId(), wishlist.getProductId())) {
            return null; // Already in wishlist
        }

        String sql = "INSERT INTO wishlists (user_id, product_id, created_at) VALUES (?, ?, NOW())";
        jdbcTemplate.update(sql, wishlist.getUserId(), wishlist.getProductId());
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /**
     * Find wishlist by ID
     */
    public Optional<Wishlist> findById(Long wishlistId) {
        String sql = "SELECT * FROM wishlists WHERE wishlist_id = ?";
        List<Wishlist> wishlists = jdbcTemplate.query(sql, new WishlistRowMapper(), wishlistId);
        return wishlists.isEmpty() ? Optional.empty() : Optional.of(wishlists.get(0));
    }

    /**
     * Find user's wishlist with product details
     */
    public List<Wishlist> findByUserId(Long userId) {
        String sql = "SELECT w.*, p.product_name, p.price, p.image_url, p.stock_quantity " +
                "FROM wishlists w " +
                "LEFT JOIN products p ON w.product_id = p.product_id " +
                "WHERE w.user_id = ? " +
                "ORDER BY w.created_at DESC";
        return jdbcTemplate.query(sql, new WishlistRowMapper(), userId);
    }

    /**
     * Delete wishlist item
     */
    public int delete(Long wishlistId) {
        String sql = "DELETE FROM wishlists WHERE wishlist_id = ?";
        return jdbcTemplate.update(sql, wishlistId);
    }

    /**
     * Delete by user and product
     */
    public int deleteByUserAndProduct(Long userId, Long productId) {
        String sql = "DELETE FROM wishlists WHERE user_id = ? AND product_id = ?";
        return jdbcTemplate.update(sql, userId, productId);
    }

    /**
     * Check if product is in user's wishlist
     */
    public boolean exists(Long userId, Long productId) {
        String sql = "SELECT COUNT(*) FROM wishlists WHERE user_id = ? AND product_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, productId);
        return count != null && count > 0;
    }

    /**
     * Get wishlist count for user
     */
    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM wishlists WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }
}
