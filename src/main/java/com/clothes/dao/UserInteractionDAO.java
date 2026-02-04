package com.clothes.dao;

import com.clothes.model.UserInteraction;
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
 * DAO for UserInteraction entity
 * Handles all database operations for user-product interactions
 */
@Repository
public class UserInteractionDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserInteractionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper for UserInteraction
     */
    private static class UserInteractionRowMapper implements RowMapper<UserInteraction> {
        @Override
        public UserInteraction mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserInteraction interaction = new UserInteraction();
            interaction.setInteractionId(rs.getLong("interaction_id"));
            interaction.setUserId(rs.getLong("user_id"));
            interaction.setProductId(rs.getLong("product_id"));
            interaction.setInteractionType(
                    UserInteraction.InteractionType.fromValue(rs.getString("interaction_type")));

            BigDecimal value = rs.getBigDecimal("interaction_value");
            if (!rs.wasNull()) {
                interaction.setInteractionValue(value);
            }

            Timestamp timestamp = rs.getTimestamp("created_at");
            if (timestamp != null) {
                interaction.setCreatedAt(timestamp.toLocalDateTime());
            }

            interaction.setSessionId(rs.getString("session_id"));

            return interaction;
        }
    }

    /**
     * Save a new user interaction
     */
    public Long save(UserInteraction interaction) {
        String sql = "INSERT INTO user_interactions (user_id, product_id, interaction_type, " +
                "interaction_value, session_id, created_at) VALUES (?, ?, ?, ?, ?, NOW())";

        jdbcTemplate.update(sql,
                interaction.getUserId(),
                interaction.getProductId(),
                interaction.getInteractionType().getValue(),
                interaction.getInteractionValue(),
                interaction.getSessionId());

        // Get the last inserted ID
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /**
     * Find all interactions by user ID
     */
    public List<UserInteraction> findByUserId(Long userId) {
        String sql = "SELECT * FROM user_interactions WHERE user_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserInteractionRowMapper(), userId);
    }

    /**
     * Find all interactions by product ID
     */
    public List<UserInteraction> findByProductId(Long productId) {
        String sql = "SELECT * FROM user_interactions WHERE product_id = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserInteractionRowMapper(), productId);
    }

    /**
     * Find interactions by user and product
     */
    public List<UserInteraction> findByUserIdAndProductId(Long userId, Long productId) {
        String sql = "SELECT * FROM user_interactions WHERE user_id = ? AND product_id = ? " +
                "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserInteractionRowMapper(), userId, productId);
    }

    /**
     * Find interactions by type
     */
    public List<UserInteraction> findByInteractionType(UserInteraction.InteractionType type) {
        String sql = "SELECT * FROM user_interactions WHERE interaction_type = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserInteractionRowMapper(), type.getValue());
    }

    /**
     * Find recent interactions by user (limit by count)
     */
    public List<UserInteraction> findRecentByUserId(Long userId, int limit) {
        String sql = "SELECT * FROM user_interactions WHERE user_id = ? " +
                "ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, new UserInteractionRowMapper(), userId, limit);
    }

    /**
     * Get user's purchase history
     */
    public List<UserInteraction> findPurchasesByUserId(Long userId) {
        String sql = "SELECT * FROM user_interactions WHERE user_id = ? AND interaction_type = 'purchase' " +
                "ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, new UserInteractionRowMapper(), userId);
    }

    /**
     * Get product IDs that user has interacted with
     */
    public List<Long> findProductIdsByUserId(Long userId) {
        String sql = "SELECT DISTINCT product_id FROM user_interactions WHERE user_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, userId);
    }

    /**
     * Get user IDs who interacted with a specific product
     */
    public List<Long> findUserIdsByProductId(Long productId) {
        String sql = "SELECT DISTINCT user_id FROM user_interactions WHERE product_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, productId);
    }

    /**
     * Count interactions by user
     */
    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_interactions WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }

    /**
     * Count interactions by product
     */
    public int countByProductId(Long productId) {
        String sql = "SELECT COUNT(*) FROM user_interactions WHERE product_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, productId);
        return count != null ? count : 0;
    }

    /**
     * Get co-occurrence matrix for items (products bought together)
     * Used for Item-Based CF
     */
    public List<CoOccurrence> findProductCoOccurrences(Long productId, int limit) {
        String sql = "SELECT ui2.product_id as product_id, COUNT(*) as co_occurrence_count " +
                "FROM user_interactions ui1 " +
                "JOIN user_interactions ui2 ON ui1.user_id = ui2.user_id " +
                "WHERE ui1.product_id = ? AND ui2.product_id != ? " +
                "AND ui1.interaction_type IN ('purchase', 'add_to_cart') " +
                "AND ui2.interaction_type IN ('purchase', 'add_to_cart') " +
                "GROUP BY ui2.product_id " +
                "ORDER BY co_occurrence_count DESC " +
                "LIMIT ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CoOccurrence coOcc = new CoOccurrence();
            coOcc.productId = rs.getLong("product_id");
            coOcc.count = rs.getInt("co_occurrence_count");
            return coOcc;
        }, productId, productId, limit);
    }

    /**
     * Delete old interactions (for data cleanup)
     */
    public int deleteOlderThan(int days) {
        String sql = "DELETE FROM user_interactions WHERE created_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
        return jdbcTemplate.update(sql, days);
    }

    /**
     * Helper class for co-occurrence results
     */
    public static class CoOccurrence {
        public Long productId;
        public Integer count;
    }

    /**
     * Get interaction matrix for Collaborative Filtering
     * Returns user-product pairs with weighted scores
     */
    public List<InteractionScore> getInteractionMatrix() {
        String sql = "SELECT user_id, product_id, interaction_type, interaction_value " +
                "FROM user_interactions " +
                "ORDER BY user_id, product_id";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            InteractionScore score = new InteractionScore();
            score.userId = rs.getLong("user_id");
            score.productId = rs.getLong("product_id");

            UserInteraction.InteractionType type = UserInteraction.InteractionType
                    .fromValue(rs.getString("interaction_type"));

            BigDecimal value = rs.getBigDecimal("interaction_value");
            if (value != null) {
                score.score = value.doubleValue();
            } else {
                score.score = type.getWeight();
            }

            return score;
        });
    }

    /**
     * Helper class for interaction scores
     */
    public static class InteractionScore {
        public Long userId;
        public Long productId;
        public Double score;
    }
}
