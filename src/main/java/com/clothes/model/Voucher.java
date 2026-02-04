package com.clothes.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model class representing a discount voucher/coupon
 */
public class Voucher {
    private Long voucherId;
    private String voucherCode;
    private String voucherName;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private Integer quantity;
    private Integer usedCount;
    private java.time.LocalDate startDate;
    private java.time.LocalDate endDate;
    private Boolean isActive;
    private LocalDateTime createdAt;

    // Enum for discount type
    public enum DiscountType {
        PERCENTAGE("PERCENTAGE"),
        FIXED("FIXED");

        private final String value;

        DiscountType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DiscountType fromValue(String value) {
            for (DiscountType type : DiscountType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return FIXED;
        }
    }

    // Constructors
    public Voucher() {
        this.isActive = true;
        this.usedCount = 0;
    }

    public Voucher(String voucherCode, DiscountType discountType, BigDecimal discountValue) {
        this();
        this.voucherCode = voucherCode;
        this.discountType = discountType;
        this.discountValue = discountValue;
    }

    // Getters and Setters
    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    // Alias for template compatibility
    public String getCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getVoucherName() {
        return voucherName;
    }

    public void setVoucherName(String voucherName) {
        this.voucherName = voucherName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public java.time.LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(java.time.LocalDate startDate) {
        this.startDate = startDate;
    }

    public java.time.LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(java.time.LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Check if voucher is currently valid
     */
    public boolean isValid() {
        java.time.LocalDate now = java.time.LocalDate.now();
        return isActive &&
                (startDate == null || !now.isBefore(startDate)) &&
                (endDate == null || !now.isAfter(endDate)) &&
                (quantity == null || usedCount < quantity);
    }

    /**
     * Calculate discount amount for given order amount
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid()) {
            return BigDecimal.ZERO;
        }

        if (minOrderValue != null && orderAmount.compareTo(minOrderValue) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(discountValue).divide(new BigDecimal("100"));
        } else {
            discount = discountValue;
        }

        // Apply max discount cap if exists
        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount;
    }

    @Override
    public String toString() {
        return "Voucher{" +
                "voucherId=" + voucherId +
                ", voucherCode='" + voucherCode + '\'' +
                ", voucherName='" + voucherName + '\'' +
                ", discountType=" + discountType +
                ", discountValue=" + discountValue +
                ", isActive=" + isActive +
                '}';
    }
}
