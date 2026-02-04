package com.clothes.model;

import java.time.LocalDateTime;

/**
 * Model for image sliders/carousels
 */
public class Slider {
    private Long sliderId;
    private String title;
    private String subtitle;
    private String description;
    private String imageUrl;
    private String buttonText; // Call-to-action button text
    private String buttonLink; // Call-to-action button link
    private Integer displayOrder; // Order in slider
    private Boolean isActive;
    private String backgroundColor; // Hex color code
    private String textColor; // Hex color code for text overlay
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public Slider() {
        this.isActive = true;
        this.displayOrder = 0;
        this.createdAt = LocalDateTime.now();
    }

    public Slider(String title, String imageUrl, String buttonText, String buttonLink) {
        this();
        this.title = title;
        this.imageUrl = imageUrl;
        this.buttonText = buttonText;
        this.buttonLink = buttonLink;
    }

    // Getters and Setters
    public Long getSliderId() {
        return sliderId;
    }

    public void setSliderId(Long sliderId) {
        this.sliderId = sliderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
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

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public String getButtonLink() {
        return buttonLink;
    }

    public void setButtonLink(String buttonLink) {
        this.buttonLink = buttonLink;
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

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
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
        return "Slider{" +
                "sliderId=" + sliderId +
                ", title='" + title + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", displayOrder=" + displayOrder +
                ", isActive=" + isActive +
                '}';
    }
}
