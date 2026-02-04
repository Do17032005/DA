package com.clothes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing similarity between two products
 * Pre-computed for Item-Based Collaborative Filtering
 */
public class ProductSimilarity {
    private Long similarityId;
    private Long productId1;
    private Long productId2;
    private BigDecimal similarityScore;
    private SimilarityType similarityType;
    private LocalDateTime computedAt;

    // Enum for similarity calculation methods
    public enum SimilarityType {
        COSINE("cosine"),
        JACCARD("jaccard"),
        CONTENT("content"); // Content-based similarity

        private final String value;

        SimilarityType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SimilarityType fromValue(String value) {
            for (SimilarityType type : SimilarityType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return COSINE;
        }
    }

    // Constructors
    public ProductSimilarity() {
        this.computedAt = LocalDateTime.now();
        this.similarityType = SimilarityType.COSINE;
    }

    public ProductSimilarity(Long productId1, Long productId2, BigDecimal similarityScore) {
        this();
        this.productId1 = productId1;
        this.productId2 = productId2;
        this.similarityScore = similarityScore;
    }

    // Getters and Setters
    public Long getSimilarityId() {
        return similarityId;
    }

    public void setSimilarityId(Long similarityId) {
        this.similarityId = similarityId;
    }

    public Long getProductId1() {
        return productId1;
    }

    public void setProductId1(Long productId1) {
        this.productId1 = productId1;
    }

    public Long getProductId2() {
        return productId2;
    }

    public void setProductId2(Long productId2) {
        this.productId2 = productId2;
    }

    public BigDecimal getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(BigDecimal similarityScore) {
        this.similarityScore = similarityScore;
    }

    public SimilarityType getSimilarityType() {
        return similarityType;
    }

    public void setSimilarityType(SimilarityType similarityType) {
        this.similarityType = similarityType;
    }

    public LocalDateTime getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }

    @Override
    public String toString() {
        return "ProductSimilarity{" +
                "productId1=" + productId1 +
                ", productId2=" + productId2 +
                ", similarityScore=" + similarityScore +
                ", similarityType=" + similarityType +
                '}';
    }
}
