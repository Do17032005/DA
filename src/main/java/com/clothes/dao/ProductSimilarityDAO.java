package com.clothes.dao;

import com.clothes.model.ProductSimilarity;
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
 * DAO for ProductSimilarity entity
 * Manages pre-computed product similarity scores for Item-Based CF
 */
@Repository
public class ProductSimilarityDAO {

    private final JdbcTemplate jdbcTemplate;

    public ProductSimilarityDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * RowMapper for ProductSimilarity
     */
    private static class ProductSimilarityRowMapper implements RowMapper<ProductSimilarity> {
        @Override
        public ProductSimilarity mapRow(ResultSet rs, int rowNum) throws SQLException {
            ProductSimilarity similarity = new ProductSimilarity();
            similarity.setSimilarityId(rs.getLong("similarity_id"));
            similarity.setProductId1(rs.getLong("product_id_1"));
            similarity.setProductId2(rs.getLong("product_id_2"));
            similarity.setSimilarityScore(rs.getBigDecimal("similarity_score"));
            similarity.setSimilarityType(
                    ProductSimilarity.SimilarityType.fromValue(rs.getString("similarity_type")));

            Timestamp computed = rs.getTimestamp("computed_at");
            if (computed != null) {
                similarity.setComputedAt(computed.toLocalDateTime());
            }

            return similarity;
        }
    }

    /**
     * Save or update product similarity
     */
    public Long save(ProductSimilarity similarity) {
        // Ensure productId1 < productId2 for consistency
        Long prod1 = Math.min(similarity.getProductId1(), similarity.getProductId2());
        Long prod2 = Math.max(similarity.getProductId1(), similarity.getProductId2());

        // Check if exists
        String checkSql = "SELECT similarity_id FROM product_similarity WHERE product_id_1 = ? AND product_id_2 = ?";
        List<Long> existing = jdbcTemplate.queryForList(checkSql, Long.class, prod1, prod2);

        if (!existing.isEmpty()) {
            // Update
            String updateSql = "UPDATE product_similarity SET similarity_score = ?, similarity_type = ?, " +
                    "computed_at = NOW() WHERE product_id_1 = ? AND product_id_2 = ?";
            jdbcTemplate.update(updateSql,
                    similarity.getSimilarityScore(),
                    similarity.getSimilarityType().getValue(),
                    prod1,
                    prod2);
            return existing.get(0);
        } else {
            // Insert
            String insertSql = "INSERT INTO product_similarity (product_id_1, product_id_2, similarity_score, " +
                    "similarity_type, computed_at) VALUES (?, ?, ?, ?, NOW())";
            jdbcTemplate.update(insertSql,
                    prod1,
                    prod2,
                    similarity.getSimilarityScore(),
                    similarity.getSimilarityType().getValue());
            return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
    }

    /**
     * Batch insert similarities (more efficient for bulk operations)
     */
    public void batchSave(List<ProductSimilarity> similarities) {
        String sql = "INSERT INTO product_similarity (product_id_1, product_id_2, similarity_score, " +
                "similarity_type, computed_at) VALUES (?, ?, ?, ?, NOW()) " +
                "ON DUPLICATE KEY UPDATE similarity_score = VALUES(similarity_score), " +
                "similarity_type = VALUES(similarity_type), computed_at = NOW()";

        jdbcTemplate.batchUpdate(sql, similarities, similarities.size(),
                (ps, similarity) -> {
                    Long prod1 = Math.min(similarity.getProductId1(), similarity.getProductId2());
                    Long prod2 = Math.max(similarity.getProductId1(), similarity.getProductId2());
                    ps.setLong(1, prod1);
                    ps.setLong(2, prod2);
                    ps.setBigDecimal(3, similarity.getSimilarityScore());
                    ps.setString(4, similarity.getSimilarityType().getValue());
                });
    }

    /**
     * Find similarity between two products
     */
    public Optional<ProductSimilarity> findByProductIds(Long productId1, Long productId2) {
        Long prod1 = Math.min(productId1, productId2);
        Long prod2 = Math.max(productId1, productId2);

        String sql = "SELECT * FROM product_similarity WHERE product_id_1 = ? AND product_id_2 = ?";
        List<ProductSimilarity> results = jdbcTemplate.query(sql, new ProductSimilarityRowMapper(), prod1, prod2);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Find most similar products to a given product
     */
    public List<ProductSimilarity> findMostSimilarProducts(Long productId, int limit) {
        String sql = "SELECT * FROM product_similarity " +
                "WHERE product_id_1 = ? OR product_id_2 = ? " +
                "ORDER BY similarity_score DESC LIMIT ?";
        return jdbcTemplate.query(sql, new ProductSimilarityRowMapper(), productId, productId, limit);
    }

    /**
     * Find similar products with minimum similarity threshold
     */
    public List<ProductSimilarity> findSimilarProductsAboveThreshold(Long productId, BigDecimal threshold) {
        String sql = "SELECT * FROM product_similarity " +
                "WHERE (product_id_1 = ? OR product_id_2 = ?) AND similarity_score >= ? " +
                "ORDER BY similarity_score DESC";
        return jdbcTemplate.query(sql, new ProductSimilarityRowMapper(), productId, productId, threshold);
    }

    /**
     * Get all similar products for a product (both directions)
     */
    public List<SimilarProduct> getSimilarProductsForProduct(Long productId, int limit) {
        String sql = "SELECT " +
                "CASE WHEN product_id_1 = ? THEN product_id_2 ELSE product_id_1 END as similar_product_id, " +
                "similarity_score " +
                "FROM product_similarity " +
                "WHERE product_id_1 = ? OR product_id_2 = ? " +
                "ORDER BY similarity_score DESC LIMIT ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SimilarProduct similar = new SimilarProduct();
            similar.productId = rs.getLong("similar_product_id");
            similar.similarityScore = rs.getBigDecimal("similarity_score").doubleValue();
            return similar;
        }, productId, productId, productId, limit);
    }

    /**
     * Delete old similarities (for refresh)
     */
    public int deleteOlderThan(int days) {
        String sql = "DELETE FROM product_similarity WHERE computed_at < DATE_SUB(NOW(), INTERVAL ? DAY)";
        return jdbcTemplate.update(sql, days);
    }

    /**
     * Delete all similarities for a product
     */
    public int deleteByProductId(Long productId) {
        String sql = "DELETE FROM product_similarity WHERE product_id_1 = ? OR product_id_2 = ?";
        return jdbcTemplate.update(sql, productId, productId);
    }

    /**
     * Count total similarities
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM product_similarity";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    /**
     * Helper class for similar product results
     */
    public static class SimilarProduct {
        public Long productId;
        public Double similarityScore;
    }
}
