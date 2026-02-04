package com.clothes.dao;

import com.clothes.model.UserRating;
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
 * DAO for UserRating entity
 * Manages user ratings for products - critical for CF algorithms
 */
@Repository
public class UserRatingDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserRatingDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper for UserRating
     */
    private static class UserRatingRowMapper implements RowMapper<UserRating> {
        @Override
        public UserRating mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserRating rating = new UserRating();
            rating.setRatingId(rs.getLong("rating_id"));
            rating.setUserId(rs.getLong("user_id"));
            rating.setProductId(rs.getLong("product_id"));
            rating.setRating(rs.getBigDecimal("rating"));
            rating.setReviewText(rs.getString("review_text"));

            Timestamp created = rs.getTimestamp("created_at");
            if (created != null) {
                rating.setCreatedAt(created.toLocalDateTime());
            }

            Timestamp updated = rs.getTimestamp("updated_at");
            if (updated != null) {
                rating.setUpdatedAt(updated.toLocalDateTime());
            }

            return rating;
        }
    }

    /**
     * Save a new rating or update existing one
     */
    public Long save(UserRating rating) {
        // Check if rating already exists
        String checkSql = "SELECT rating_id FROM user_ratings WHERE user_id = ? AND product_id = ?";
        List<Long> existing = jdbcTemplate.queryForList(checkSql, Long.class,
                rating.getUserId(), rating.getProductId());

        if (!existing.isEmpty()) {
            // Update existing rating
            String updateSql = "UPDATE user_ratings SET rating = ?, review_text = ?, " +
                    "updated_at = NOW() WHERE user_id = ? AND product_id = ?";
            jdbcTemplate.update(updateSql,
                    rating.getRating(),
                    rating.getReviewText(),
                    rating.getUserId(),
                    rating.getProductId());
            return existing.get(0);
        } else {
            // Insert new rating
            String insertSql = "INSERT INTO user_ratings (user_id, product_id, rating, review_text, " +
                    "created_at, updated_at) VALUES (?, ?, ?, ?, NOW(), NOW())";
            jdbcTemplate.update(insertSql,
                    rating.getUserId(),
                    rating.getProductId(),
                    rating.getRating(),
                    rating.getReviewText());
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
    }

    /**
     * Find rating by user and product
     */
    public Optional<UserRating> findByUserIdAndProductId(Long userId, Long productId) {
        String sql = "SELECT * FROM user_ratings WHERE user_id = ? AND product_id = ?";
        List<UserRating> ratings = jdbcTemplate.query(sql, new UserRatingRowMapper(), userId, productId);
        return ratings.isEmpty() ? Optional.empty() : Optional.of(ratings.get(0));
    }

    /**
     * Find all ratings by user
     */
    public List<UserRating> findByUserId(Long userId) {
        String sql = "SELECT * FROM user_ratings WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserRatingRowMapper(), userId);
    }

    /**
     * Find all ratings for a product
     */
    public List<UserRating> findByProductId(Long productId) {
        String sql = "SELECT * FROM user_ratings WHERE product_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserRatingRowMapper(), productId);
    }

    /**
     * Get average rating for a product
     */
    public BigDecimal getAverageRatingByProductId(Long productId) {
        String sql = "SELECT AVG(rating) FROM user_ratings WHERE product_id = ?";
        BigDecimal avg = jdbcTemplate.queryForObject(sql, BigDecimal.class, productId);
        return avg != null ? avg : BigDecimal.ZERO;
    }

    /**
     * Count ratings for a product
     */
    public int countByProductId(Long productId) {
        String sql = "SELECT COUNT(*) FROM user_ratings WHERE product_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, productId);
        return count != null ? count : 0;
    }

    /**
     * Get all user-product rating pairs (for CF calculations)
     */
    public List<RatingPair> getAllRatingPairs() {
        String sql = "SELECT user_id, product_id, rating FROM user_ratings ORDER BY user_id, product_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            RatingPair pair = new RatingPair();
            pair.userId = rs.getLong("user_id");
            pair.productId = rs.getLong("product_id");
            pair.rating = rs.getBigDecimal("rating").doubleValue();
            return pair;
        });
    }

    /**
     * Get users who rated both products (for similarity calculation)
     */
    public List<CommonRating> findCommonRatings(Long productId1, Long productId2) {
        String sql = "SELECT r1.user_id, r1.rating as rating1, r2.rating as rating2 " +
                "FROM user_ratings r1 " +
                "JOIN user_ratings r2 ON r1.user_id = r2.user_id " +
                "WHERE r1.product_id = ? AND r2.product_id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CommonRating common = new CommonRating();
            common.userId = rs.getLong("user_id");
            common.rating1 = rs.getBigDecimal("rating1").doubleValue();
            common.rating2 = rs.getBigDecimal("rating2").doubleValue();
            return common;
        }, productId1, productId2);
    }

    /**
     * Get products rated by both users (for user similarity calculation)
     */
    public List<CommonRating> findCommonRatingsByUsers(Long userId1, Long userId2) {
        String sql = "SELECT r1.product_id, r1.rating as rating1, r2.rating as rating2 " +
                "FROM user_ratings r1 " +
                "JOIN user_ratings r2 ON r1.product_id = r2.product_id " +
                "WHERE r1.user_id = ? AND r2.user_id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CommonRating common = new CommonRating();
            common.productId = rs.getLong("product_id");
            common.rating1 = rs.getBigDecimal("rating1").doubleValue();
            common.rating2 = rs.getBigDecimal("rating2").doubleValue();
            return common;
        }, userId1, userId2);
    }

    /**
     * Delete rating
     */
    public int delete(Long userId, Long productId) {
        String sql = "DELETE FROM user_ratings WHERE user_id = ? AND product_id = ?";
        return jdbcTemplate.update(sql, userId, productId);
    }

    /**
     * Helper class for rating pairs
     */
    public static class RatingPair {
        public Long userId;
        public Long productId;
        public Double rating;
    }

    /**
     * Helper class for common ratings
     */
    public static class CommonRating {
        public Long userId;
        public Long productId;
        public Double rating1;
        public Double rating2;
    }
}
