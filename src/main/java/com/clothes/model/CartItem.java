package com.clothes.model;

import java.math.BigDecimal;

/**
 * Model class representing an item in a shopping cart
 */
public class CartItem {
    private Long cartItemId;
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private String size;
    private String color;

    // Additional fields
    private Product product;

    // Constructors
    public CartItem() {
    }

    public CartItem(Long productId, Integer quantity, BigDecimal price, String size, String color) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.size = size;
        this.color = color;
        this.subtotal = price.multiply(new BigDecimal(quantity));
    }

    // Getters and Setters
    public Long getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(Long cartItemId) {
        this.cartItemId = cartItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        if (this.price != null) {
            this.subtotal = this.price.multiply(new BigDecimal(quantity));
        }
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        if (this.quantity != null) {
            this.subtotal = price.multiply(new BigDecimal(this.quantity));
        }
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            this.productId = product.getProductId();
            // Use discount price if available, otherwise regular price
            this.price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            if (this.quantity != null) {
                this.subtotal = this.price.multiply(new BigDecimal(this.quantity));
            }
        }
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                ", price=" + price +
                ", subtotal=" + subtotal +
                '}';
    }
}
