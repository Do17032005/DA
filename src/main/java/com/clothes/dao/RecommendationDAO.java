package com.clothes.dao;

import com.clothes.model.Recommendation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO for Recommendation cache
 * Stores pre-computed recommendations for faster serving
 */
@Repository
public class RecommendationDAO {

    private final JdbcTemplate jdbcTemplate;

    public RecommendationDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper for Recommendation
     */
    private static class RecommendationRowMapper implements RowMapper<Recommendation> {
        @Override
        public Recommendation mapRow(ResultSet rs, int rowNum) throws SQLException {
            Recommendation recommendation = new Recommendation();
            recommendation.setCacheId(rs.getLong("cache_id"));
            recommendation.setUserId(rs.getLong("user_id"));
            recommendation.setRecommendedProductId(rs.getLong("recommended_product_id"));
            recommendation.setRecommendationType(
                    Recommendation.RecommendationType.fromValue(rs.getString("recommendation_type")));
            recommendation.setConfidenceScore(rs.getBigDecimal("confidence_score"));

            Timestamp generated = rs.getTimestamp("generated_at");
            if (generated != null) {
                recommendation.setGeneratedAt(generated.toLocalDateTime());
            }

            Timestamp expires = rs.getTimestamp("expires_at");
            if (expires != null) {
                recommendation.setExpiresAt(expires.toLocalDateTime());
            }

            return recommendation;
        }
    }

    /**
     * Save a recommendation to cache
     */
    public Long save(Recommendation recommendation) {
        String sql = "INSERT INTO recommendations_cache (user_id, recommended_product_id, " +
                "recommendation_type, confidence_score, generated_at, expires_at) " +
                "VALUES (?, ?, ?, ?, NOW(), ?)";

        jdbcTemplate.update(sql,
                recommendation.getUserId(),
                recommendation.getRecommendedProductId(),
                recommendation.getRecommendationType().getValue(),
                recommendation.getConfidenceScore(),
                recommendation.getExpiresAt() != null ? Timestamp.valueOf(recommendation.getExpiresAt()) : null);

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    /**
     * Batch insert recommendations
     */
    public void batchSave(List<Recommendation> recommendations) {
        String sql = "INSERT INTO recommendations_cache (user_id, recommended_product_id, " +
                "recommendation_type, confidence_score, generated_at, expires_at) " +
                "VALUES (?, ?, ?, ?, NOW(), ?)";

        jdbcTemplate.batchUpdate(sql, recommendations, recommendations.size(),
                (ps, recommendation) -> {
                    ps.setLong(1, recommendation.getUserId());
                    ps.setLong(2, recommendation.getRecommendedProductId());
                    ps.setString(3, recommendation.getRecommendationType().getValue());
                    ps.setBigDecimal(4, recommendation.getConfidenceScore());
                    ps.setTimestamp(5,
                            recommendation.getExpiresAt() != null ? Timestamp.valueOf(recommendation.getExpiresAt())
                                    : null);
                });
    }

    /**
     * Find cached recommendations for a user
     */
    public List<Recommendation> findByUserId(Long userId, int limit) {
        String sql = "SELECT * FROM recommendations_cache " +
                "WHERE user_id = ? " +
                "AND (expires_at IS NULL OR expires_at > NOW()) " +
                "ORDER BY confidence_score DESC LIMIT ?";
        return jdbcTemplate.query(sql, new RecommendationRowMapper(), userId, limit);
    }

    /**
     * Find cached recommendations by type
     */
    public List<Recommendation> findByUserIdAndType(Long userId, Recommendation.RecommendationType type, int limit) {
        String sql = "SELECT * FROM recommendations_cache " +
                "WHERE user_id = ? AND recommendation_type = ? " +
                "AND (expires_at IS NULL OR expires_at > NOW()) " +
                "ORDER BY confidence_score DESC LIMIT ?";
        return jdbcTemplate.query(sql, new RecommendationRowMapper(), userId, type.getValue(), limit);
    }

    /**
     * Delete expired recommendations
     */
    public int deleteExpired() {
        String sql = "DELETE FROM recommendations_cache WHERE expires_at IS NOT NULL AND expires_at < NOW()";
        return jdbcTemplate.update(sql);
    }

    /**
     * Delete all recommendations for a user
     */
    public int deleteByUserId(Long userId) {
        String sql = "DELETE FROM recommendations_cache WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId);
    }

    /**
     * Delete recommendations by type for a user
     */
    public int deleteByUserIdAndType(Long userId, Recommendation.RecommendationType type) {
        String sql = "DELETE FROM recommendations_cache WHERE user_id = ? AND recommendation_type = ?";
        return jdbcTemplate.update(sql, userId, type.getValue());
    }

    /**
     * Check if fresh recommendations exist
     */
    public boolean hasFreshRecommendations(Long userId, Recommendation.RecommendationType type, int minCount) {
        String sql = "SELECT COUNT(*) FROM recommendations_cache " +
                "WHERE user_id = ? AND recommendation_type = ? " +
                "AND (expires_at IS NULL OR expires_at > NOW())";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, type.getValue());
        return count != null && count >= minCount;
    }
}
