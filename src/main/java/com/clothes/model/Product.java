package com.clothes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing a Product in the fashion store
 */
public class Product {
    private Long productId;
    private String productName;
    private String sku;
    private String description;
    private Long categoryId;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stockQuantity;
    private String imageUrl;
    private String brand;
    private String color;
    private String size;
    private String material;
    private Gender gender;
    private Season season;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private Integer viewCount;
    private Integer purchaseCount;

    // Additional fields for display
    private String categoryName;
    private Integer soldCount;
    private Integer percentage;
    private Boolean inWishlist = false;
    private Boolean isNew = false;
    private Double averageRating = 0.0;
    private Integer reviewCount = 0;
    private java.util.List<String> colorsAvailable;

    // Enums
    public enum Gender {
        MALE("male"),
        FEMALE("female"),
        UNISEX("unisex"),
        KIDS("kids");

        private final String value;

        Gender(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Gender fromValue(String value) {
            if (value == null)
                return null;
            for (Gender gender : Gender.values()) {
                if (gender.value.equalsIgnoreCase(value)) {
                    return gender;
                }
            }
            if ("nam".equalsIgnoreCase(value) || "male".equalsIgnoreCase(value))
                return MALE;
            if ("nu".equalsIgnoreCase(value) || "female".equalsIgnoreCase(value))
                return FEMALE;
            if ("tre-em".equalsIgnoreCase(value) || "kids".equalsIgnoreCase(value) || "tre_em".equalsIgnoreCase(value))
                return KIDS;
            return UNISEX;
        }
    }

    public enum Season {
        SPRING("SPRING"),
        SUMMER("SUMMER"),
        FALL("FALL"),
        WINTER("WINTER"),
        ALL_SEASON("ALL_SEASON");

        private final String value;

        Season(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Season fromValue(String value) {
            if (value == null)
                return ALL_SEASON;
            for (Season season : Season.values()) {
                if (season.value.equalsIgnoreCase(value) || season.name().equalsIgnoreCase(value)) {
                    return season;
                }
            }
            // Fallback mapping for old values
            if ("AUTUMN".equalsIgnoreCase(value) || "autumn".equalsIgnoreCase(value))
                return FALL;
            if ("ALL".equalsIgnoreCase(value) || "all".equalsIgnoreCase(value))
                return ALL_SEASON;

            return ALL_SEASON;
        }
    }

    // Constructors
    public Product() {
        this.isActive = true;
        this.viewCount = 0;
        this.purchaseCount = 0;
        this.gender = Gender.UNISEX;
        this.season = Season.ALL_SEASON;
    }

    public Product(Long productId, String productName, BigDecimal price) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.price = price;
    }

    // Getters and Setters
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Season getSeason() {
        return season;
    }

    public void setSeason(Season season) {
        this.season = season;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(Integer purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }

    public void incrementPurchaseCount() {
        this.purchaseCount = (this.purchaseCount == null ? 0 : this.purchaseCount) + 1;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(Integer soldCount) {
        this.soldCount = soldCount;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public Boolean getInWishlist() {
        return inWishlist;
    }

    public void setInWishlist(Boolean inWishlist) {
        this.inWishlist = inWishlist;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public java.util.List<String> getColorsAvailable() {
        return colorsAvailable;
    }

    public void setColorsAvailable(java.util.List<String> colorsAvailable) {
        this.colorsAvailable = colorsAvailable;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                ", brand='" + brand + '\'' +
                ", viewCount=" + viewCount +
                ", purchaseCount=" + purchaseCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Product product = (Product) o;
        return productId != null && productId.equals(product.productId);
    }

    @Override
    public int hashCode() {
        return productId != null ? productId.hashCode() : 0;
    }
}
