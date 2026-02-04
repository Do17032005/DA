package com.clothes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing a product recommendation for a user
 * Used for caching and serving recommendations
 */
public class Recommendation {
    private Long cacheId;
    private Long userId;
    private Long recommendedProductId;
    private RecommendationType recommendationType;
    private BigDecimal confidenceScore;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;

    // Product details (for convenience, not stored in DB)
    private Product product;

    // Enum for recommendation types
    public enum RecommendationType {
        USER_BASED_CF("user_based_cf"),
        ITEM_BASED_CF("item_based_cf"),
        HYBRID("hybrid"),
        TRENDING("trending"),
        SIMILAR_ITEMS("similar_items");

        private final String value;

        RecommendationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static RecommendationType fromValue(String value) {
            for (RecommendationType type : RecommendationType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return ITEM_BASED_CF;
        }
    }

    // Constructors
    public Recommendation() {
        this.generatedAt = LocalDateTime.now();
    }

    public Recommendation(Long userId, Long recommendedProductId, RecommendationType recommendationType) {
        this();
        this.userId = userId;
        this.recommendedProductId = recommendedProductId;
        this.recommendationType = recommendationType;
    }

    // Getters and Setters
    public Long getCacheId() {
        return cacheId;
    }

    public void setCacheId(Long cacheId) {
        this.cacheId = cacheId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRecommendedProductId() {
        return recommendedProductId;
    }

    public void setRecommendedProductId(Long recommendedProductId) {
        this.recommendedProductId = recommendedProductId;
    }

    public RecommendationType getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(RecommendationType recommendationType) {
        this.recommendationType = recommendationType;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return "Recommendation{" +
                "userId=" + userId +
                ", recommendedProductId=" + recommendedProductId +
                ", recommendationType=" + recommendationType +
                ", confidenceScore=" + confidenceScore +
                ", generatedAt=" + generatedAt +
                '}';
    }
}
