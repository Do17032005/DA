package com.clothes.model;

import java.time.LocalDateTime;

/**
 * Model for homepage banners and promotional images
 */
public class Banner {
    private Long bannerId;
    private String title;
    private String description;
    private String imageUrl;
    private String linkUrl; // URL to redirect when clicked
    private Integer displayOrder; // Order of display (lower number = higher priority)
    private Boolean isActive;
    private LocalDateTime startDate; // Banner start date
    private LocalDateTime endDate; // Banner end date
    private String position; // Position: 'main', 'sidebar', 'footer'
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Banner() {
        this.isActive = true;
        this.displayOrder = 0;
        this.createdAt = LocalDateTime.now();
    }

    public Banner(String title, String imageUrl, String linkUrl, String position) {
        this();
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.position = position;
    }

    // Getters and Setters
    public Long getBannerId() {
        return bannerId;
    }

    public void setBannerId(Long bannerId) {
        this.bannerId = bannerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
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
     * Check if banner is currently active (within date range and enabled)
     */
    public boolean isCurrentlyActive() {
        if (!isActive) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }

        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Banner{" +
                "bannerId=" + bannerId +
                ", title='" + title + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", position='" + position + '\'' +
                ", displayOrder=" + displayOrder +
                ", isActive=" + isActive +
                '}';
    }
}
