package com.clothes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing similarity between two users
 * Pre-computed for User-Based Collaborative Filtering
 */
public class UserSimilarity {
    private Long similarityId;
    private Long userId1;
    private Long userId2;
    private BigDecimal similarityScore; // -1.0 to 1.0 or 0.0 to 1.0 depending on algorithm
    private SimilarityType similarityType;
    private LocalDateTime computedAt;

    // Enum for similarity calculation methods
    public enum SimilarityType {
        COSINE("cosine"),
        PEARSON("pearson"),
        JACCARD("jaccard");

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
    public UserSimilarity() {
        this.computedAt = LocalDateTime.now();
        this.similarityType = SimilarityType.COSINE;
    }

    public UserSimilarity(Long userId1, Long userId2, BigDecimal similarityScore) {
        this();
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.similarityScore = similarityScore;
    }

    // Getters and Setters
    public Long getSimilarityId() {
        return similarityId;
    }

    public void setSimilarityId(Long similarityId) {
        this.similarityId = similarityId;
    }

    public Long getUserId1() {
        return userId1;
    }

    public void setUserId1(Long userId1) {
        this.userId1 = userId1;
    }

    public Long getUserId2() {
        return userId2;
    }

    public void setUserId2(Long userId2) {
        this.userId2 = userId2;
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
        return "UserSimilarity{" +
                "userId1=" + userId1 +
                ", userId2=" + userId2 +
                ", similarityScore=" + similarityScore +
                ", similarityType=" + similarityType +
                '}';
    }
}
