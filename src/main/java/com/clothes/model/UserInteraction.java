package com.clothes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing user interactions with products
 * Critical for Collaborative Filtering algorithms
 */
public class UserInteraction {
    private Long interactionId;
    private Long userId;
    private Long productId;
    private InteractionType interactionType;
    private BigDecimal interactionValue; // For ratings or weighted values
    private LocalDateTime createdAt;
    private String sessionId;

    // Enum for interaction types
    public enum InteractionType {
        VIEW("view", 1.0),
        ADD_TO_CART("add_to_cart", 3.0),
        PURCHASE("purchase", 10.0),
        RATING("rating", 5.0),
        WISHLIST("wishlist", 2.0);

        private final String value;
        private final Double weight; // Weight for recommendation scoring

        InteractionType(String value, Double weight) {
            this.value = value;
            this.weight = weight;
        }

        public String getValue() {
            return value;
        }

        public Double getWeight() {
            return weight;
        }

        public static InteractionType fromValue(String value) {
            for (InteractionType type : InteractionType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return VIEW;
        }
    }

    // Constructors
    public UserInteraction() {
        this.createdAt = LocalDateTime.now();
    }

    public UserInteraction(Long userId, Long productId, InteractionType interactionType) {
        this();
        this.userId = userId;
        this.productId = productId;
        this.interactionType = interactionType;
    }

    // Getters and Setters
    public Long getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(Long interactionId) {
        this.interactionId = interactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public BigDecimal getInteractionValue() {
        return interactionValue;
    }

    public void setInteractionValue(BigDecimal interactionValue) {
        this.interactionValue = interactionValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Get weighted score for this interaction
     * Used in recommendation calculations
     */
    public Double getWeightedScore() {
        if (interactionType == null) {
            return 0.0;
        }

        Double baseWeight = interactionType.getWeight();

        // If there's a rating value, use it to adjust the weight
        if (interactionValue != null && interactionType == InteractionType.RATING) {
            return interactionValue.doubleValue();
        }

        return baseWeight;
    }

    @Override
    public String toString() {
        return "UserInteraction{" +
                "interactionId=" + interactionId +
                ", userId=" + userId +
                ", productId=" + productId +
                ", interactionType=" + interactionType +
                ", interactionValue=" + interactionValue +
                ", createdAt=" + createdAt +
                '}';
    }
}
