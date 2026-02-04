package com.clothes.model;

import java.time.LocalDateTime;

/**
 * Model class representing a user's shipping address
 */
public class Address {
    private Long addressId;
    private Long userId;
    private String recipientName;
    private String phoneNumber;
    private String addressLine;
    private String ward;
    private String district;
    private String city;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Address() {
        this.isDefault = false;
    }

    public Address(Long userId, String recipientName, String phoneNumber) {
        this();
        this.userId = userId;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
    }

    // Getters and Setters
    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    // Aliases for Thymeleaf template compatibility
    public String getFullName() {
        return recipientName;
    }

    public String getPhone() {
        return phoneNumber;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
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

    /**
     * Get full address as a string
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null)
            sb.append(addressLine);
        if (ward != null)
            sb.append(", ").append(ward);
        if (district != null)
            sb.append(", ").append(district);
        if (city != null)
            sb.append(", ").append(city);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Address{" +
                "addressId=" + addressId +
                ", userId=" + userId +
                ", recipientName='" + recipientName + '\'' +
                ", city='" + city + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}
