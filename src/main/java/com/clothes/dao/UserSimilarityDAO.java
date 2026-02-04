package com.clothes.dao;

import com.clothes.model.UserSimilarity;
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
 * DAO for UserSimilarity entity
 * Manages pre-computed user similarity scores for User-Based CF
 */
@Repository
public class UserSimilarityDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserSimilarityDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper for UserSimilarity
     */
    private static class UserSimilarityRowMapper implements RowMapper<UserSimilarity> {
        @Override
        public UserSimilarity mapRow(ResultSet rs, int rowNum) throws SQLException {
            UserSimilarity similarity = new UserSimilarity();
            similarity.setSimilarityId(rs.getLong("similarity_id"));
            similarity.setUserId1(rs.getLong("user_id_1"));
            similarity.setUserId2(rs.getLong("user_id_2"));
            similarity.setSimilarityScore(rs.getBigDecimal("similarity_score"));
            similarity.setSimilarityType(
                    UserSimilarity.SimilarityType.fromValue(rs.getString("similarity_type")));

            Timestamp computed = rs.getTimestamp("computed_at");
            if (computed != null) {
                similarity.setComputedAt(computed.toLocalDateTime());
            }

            return similarity;
        }
    }

    /**
     * Save or update user similarity
     */
    public Long save(UserSimilarity similarity) {
        // Ensure userId1 < userId2 for consistency
        Long user1 = Math.min(similarity.getUserId1(), similarity.getUserId2());
        Long user2 = Math.max(similarity.getUserId1(), similarity.getUserId2());

        // Check if exists
        String checkSql = "SELECT similarity_id FROM user_similarity WHERE user_id_1 = ? AND user_id_2 = ?";
        List<Long> existing = jdbcTemplate.queryForList(checkSql, Long.class, user1, user2);

        if (!existing.isEmpty()) {
            // Update
            String updateSql = "UPDATE user_similarity SET similarity_score = ?, similarity_type = ?, " +
                    "computed_at = NOW() WHERE user_id_1 = ? AND user_id_2 = ?";
            jdbcTemplate.update(updateSql,
                    similarity.getSimilarityScore(),
                    similarity.getSimilarityType().getValue(),
                    user1,
                    user2);
            return existing.get(0);
        } else {
            // Insert
            String insertSql = "INSERT INTO user_similarity (user_id_1, user_id_2, similarity_score, " +
                    "similarity_type, computed_at) VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(insertSql,
                    user1,
                    user2,
                    similarity.getSimilarityScore(),
                    similarity.getSimilarityType().getValue());
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
    }

    /**
     * Batch insert similarities (more efficient for bulk operations)
     */
    public void batchSave(List<UserSimilarity> similarities) {
        String sql = "INSERT INTO user_similarity (user_id_1, user_id_2, similarity_score, " +
                "similarity_type, computed_at) VALUES (?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE similarity_score = VALUES(similarity_score), " +
                "similarity_type = VALUES(similarity_type), computed_at = NOW()";

        jdbcTemplate.batchUpdate(sql, similarities, similarities.size(),
                (ps, similarity) -> {
                    Long user1 = Math.min(similarity.getUserId1(), similarity.getUserId2());
                    Long user2 = Math.max(similarity.getUserId1(), similarity.getUserId2());
                    ps.setLong(1, user1);
                    ps.setLong(2, user2);
                    ps.setBigDecimal(3, similarity.getSimilarityScore());
                    ps.setString(4, similarity.getSimilarityType().getValue());
                });
    }

    /**
     * Find similarity between two users
     */
    public Optional<UserSimilarity> findByUserIds(Long userId1, Long userId2) {
        Long user1 = Math.min(userId1, userId2);
        Long user2 = Math.max(userId1, userId2);

        String sql = "SELECT * FROM user_similarity WHERE user_id_1 = ? AND user_id_2 = ?";
        List<UserSimilarity> results = jdbcTemplate.query(sql, new UserSimilarityRowMapper(), user1, user2);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find most similar users to a given user
     */
    public List<UserSimilarity> findMostSimilarUsers(Long userId, int limit) {
        String sql = "SELECT * FROM user_similarity " +
                "WHERE user_id_1 = ? OR user_id_2 = ? " +
                "ORDER BY similarity_score DESC LIMIT ?";
        return jdbcTemplate.query(sql, new UserSimilarityRowMapper(), userId, userId, limit);
    }

    /**
     * Find similar users with minimum similarity threshold
     */
    public List<UserSimilarity> findSimilarUsersAboveThreshold(Long userId, BigDecimal threshold) {
        String sql = "SELECT * FROM user_similarity " +
                "WHERE (user_id_1 = ? OR user_id_2 = ?) AND similarity_score >= ? " +
                "ORDER BY similarity_score DESC";
        return jdbcTemplate.query(sql, new UserSimilarityRowMapper(), userId, userId, threshold);
    }

    /**
     * Get all similarities for a user (both directions)
     */
    public List<SimilarUser> getSimilarUsersForUser(Long userId, int limit) {
        String sql = "SELECT " +
                "CASE WHEN user_id_1 = ? THEN user_id_2 ELSE user_id_1 END as similar_user_id, " +
                "similarity_score " +
                "FROM user_similarity " +
                "WHERE user_id_1 = ? OR user_id_2 = ? " +
                "ORDER BY similarity_score DESC LIMIT ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SimilarUser similar = new SimilarUser();
            similar.userId = rs.getLong("similar_user_id");
            similar.similarityScore = rs.getBigDecimal("similarity_score").doubleValue();
            return similar;
        }, userId, userId, userId, limit);
    }

    /**
     * Delete old similarities (for refresh)
     */
    public int deleteOlderThan(int days) {
        String sql = "DELETE FROM user_similarity WHERE computed_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
        return jdbcTemplate.update(sql, days);
    }

    /**
     * Delete all similarities for a user
     */
    public int deleteByUserId(Long userId) {
        String sql = "DELETE FROM user_similarity WHERE user_id_1 = ? OR user_id_2 = ?";
        return jdbcTemplate.update(sql, userId, userId);
    }

    /**
     * Count total similarities
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM user_similarity";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Helper class for similar user results
     */
    public static class SimilarUser {
        public Long userId;
        public Double similarityScore;
    }
}
