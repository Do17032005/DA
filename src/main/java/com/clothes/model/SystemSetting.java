package com.clothes.model;

import java.time.LocalDateTime;

/**
 * Model for system configuration settings
 */
public class SystemSetting {
    private Long settingId;
    private String settingKey; // Unique key (e.g., 'site_name', 'smtp_host')
    private String settingValue; // Value (can be JSON for complex data)
    private String category; // 'general', 'payment', 'shipping', 'email', 'seo'
    private String dataType; // 'string', 'number', 'boolean', 'json', 'text'
    private String description; // Description for admin UI
    private Boolean isEditable; // Can admin edit this setting?
    private Long updatedBy; // Admin user ID who last updated
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;

    // Constructors
    public SystemSetting() {
        this.isEditable = true;
        this.createdAt = LocalDateTime.now();
        this.dataType = "string";
    }

    public SystemSetting(String settingKey, String settingValue, String category) {
        this();
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.category = category;
    }

    // Getters and Setters
    public Long getSettingId() {
        return settingId;
    }

    public void setSettingId(Long settingId) {
        this.settingId = settingId;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsEditable() {
        return isEditable;
    }

    public void setIsEditable(Boolean isEditable) {
        this.isEditable = isEditable;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get value as boolean
     */
    public boolean getAsBoolean() {
        if (settingValue == null)
            return false;
        return "true".equalsIgnoreCase(settingValue) || "1".equals(settingValue);
    }

    /**
     * Get value as integer
     */
    public int getAsInt() {
        if (settingValue == null)
            return 0;
        try {
            return Integer.parseInt(settingValue);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get value as double
     */
    public double getAsDouble() {
        if (settingValue == null)
            return 0.0;
        try {
            return Double.parseDouble(settingValue);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public String toString() {
        return "SystemSetting{" +
                "settingKey='" + settingKey + '\'' +
                ", settingValue='" + settingValue + '\'' +
                ", category='" + category + '\'' +
                ", dataType='" + dataType + '\'' +
                '}';
    }
}
