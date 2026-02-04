package com.clothes.dao;

import com.clothes.model.Review;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Review entity
 */
@Repository
public class ReviewDAO {

    private final JdbcTemplate jdbcTemplate;

    public ReviewDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class ReviewRowMapper implements RowMapper<Review> {
        @Override
        public Review mapRow(ResultSet rs, int rowNum) throws SQLException {
            Review review = new Review();
            review.setReviewId(rs.getLong("review_id"));
            review.setProductId(rs.getLong("product_id"));
            review.setUserId(rs.getLong("user_id"));
            review.setRating(rs.getInt("rating"));
            review.setComment(rs.getString("comment"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                review.setCreatedAt(created.toLocalDateTime());
            }

            // Try to get user name if joined
            try {
                String userName = rs.getString("full_name");
                if (userName != null) {
                    review.setUserName(userName);
                }
            } catch (SQLException ignored) {
            }

            return review;
        }
    }

    /**
     * Save new review
     */
    public Long save(Review review) {
        String sql = "INSERT INTO product_reviews (product_id, user_id, rating, comment, created_at) " +
                "VALUES (?, ?, ?, ?, NOW())";

        jdbcTemplate.update(sql,
                review.getProductId(),
                review.getUserId(),
                review.getRating(),
                review.getComment());

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /**
     * Find review by ID
     */
    public Optional<Review> findById(Long reviewId) {
        String sql = "SELECT * FROM product_reviews WHERE review_id = ?";
        List<Review> reviews = jdbcTemplate.query(sql, new ReviewRowMapper(), reviewId);
        return reviews.isEmpty() ? Optional.empty() : Optional.of(reviews.get(0));
    }

    /**
     * Find reviews by product ID
     */
    public List<Review> findByProductId(Long productId) {
        String sql = "SELECT pr.*, u.full_name " +
                "FROM product_reviews pr " +
                "LEFT JOIN users u ON pr.user_id = u.user_id " +
                "WHERE pr.product_id = ? " +
                "ORDER BY pr.created_at DESC";
        return jdbcTemplate.query(sql, new ReviewRowMapper(), productId);
    }

    /**
     * Find reviews by user ID
     */
    public List<Review> findByUserId(Long userId) {
        String sql = "SELECT * FROM product_reviews WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new ReviewRowMapper(), userId);
    }

    /**
     * Delete review
     */
    public int delete(Long reviewId) {
        String sql = "DELETE FROM product_reviews WHERE review_id = ?";
        return jdbcTemplate.update(sql, reviewId);
    }

    /**
     * Get average rating for product
     */
    public Double getAverageRating(Long productId) {
        String sql = "SELECT AVG(rating) FROM product_reviews WHERE product_id = ?";
        return jdbcTemplate.queryForObject(sql, Double.class, productId);
    }

    /**
     * Get review count for product
     */
    public int getReviewCount(Long productId) {
        String sql = "SELECT COUNT(*) FROM product_reviews WHERE product_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, productId);
        return count != null ? count : 0;
    }

    /**
     * Check if user has reviewed product
     */
    public boolean hasUserReviewed(Long userId, Long productId) {
        String sql = "SELECT COUNT(*) FROM product_reviews WHERE user_id = ? AND product_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, productId);
        return count != null && count > 0;
    }
}
