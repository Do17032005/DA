package com.clothes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing user ratings for products
 * Essential for Collaborative Filtering calculations
 */
public class UserRating {
    private Long ratingId;
    private Long userId;
    private Long productId;
    private BigDecimal rating; // 1.0 to 5.0
    private String reviewText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public UserRating() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UserRating(Long userId, Long productId, BigDecimal rating) {
        this();
        this.userId = userId;
        this.productId = productId;
        this.rating = rating;
    }

    // Getters and Setters
    public Long getRatingId() {
        return ratingId;
    }

    public void setRatingId(Long ratingId) {
        this.ratingId = ratingId;
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

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        // Validate rating is between 1.0 and 5.0
        if (rating != null && (rating.compareTo(BigDecimal.ONE) < 0 || rating.compareTo(new BigDecimal("5.0")) > 0)) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 5.0");
        }
        this.rating = rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserRating{" +
                "ratingId=" + ratingId +
                ", userId=" + userId +
                ", productId=" + productId +
                ", rating=" + rating +
                ", createdAt=" + createdAt +
                '}';
    }
}
