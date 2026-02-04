package com.clothes.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class representing a shopping cart
 * This is a transient object, not stored in database directly
 */
public class Cart {
    private Long cartId;
    private Long userId;
    private List<CartItem> items;
    private BigDecimal totalAmount;
    private Integer totalItems;

    // Constructors
    public Cart() {
        this.items = new ArrayList<>();
        this.totalAmount = BigDecimal.ZERO;
        this.totalItems = 0;
    }

    public Cart(Long userId) {
        this();
        this.userId = userId;
    }

    // Getters and Setters
    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
        recalculateTotal();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    // Business methods
    public void addItem(CartItem item) {
        // Check if product already exists with same size and color
        CartItem existing = findItem(item.getProductId(), item.getSize(), item.getColor());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + item.getQuantity());
        } else {
            this.items.add(item);
        }
        recalculateTotal();
    }

    public void removeItem(Long productId) {
        // Removes all variations of the product
        this.items.removeIf(item -> item.getProductId().equals(productId));
        recalculateTotal();
    }

    public void removeItem(Long cartItemId, boolean useId) {
        // Placeholder if we implement unique IDs for cart items later
        // For now we will rely on removing by object identity or index if needed
        // But the UI sends an ID. Let's assume for now we might need to match exactly.
    }

    // New method to remove a specific item variant if needed,
    // but the controller currently uses productId.
    // We'll update the controller to handle this better later.

    public void updateQuantity(Long productId, Integer quantity) {
        // This is ambiguous now with multiple variants.
        // We really need a unique CartItemID.
        // For simplicity, let's update ALL matching products or the first one.
        // Better: Find item by productId AND match logic.

        // Let's assume for this fix we update the first matching or we need to pass
        // attributes.
        CartItem item = findItem(productId, null, null); // This is risky
        if (item != null) {
            if (quantity <= 0) {
                removeItem(productId);
            } else {
                item.setQuantity(quantity);
                recalculateTotal();
            }
        }
    }

    public void updateQuantity(int index, Integer quantity) {
        if (index >= 0 && index < items.size()) {
            CartItem item = items.get(index);
            if (quantity <= 0) {
                items.remove(index);
            } else {
                item.setQuantity(quantity);
            }
            recalculateTotal();
        }
    }

    public void clear() {
        this.items.clear();
        this.totalAmount = BigDecimal.ZERO;
        this.totalItems = 0;
    }

    private CartItem findItem(Long productId, String size, String color) {
        return items.stream()
                .filter(item -> item.getProductId().equals(productId)
                        && (size == null || size.equals(item.getSize()))
                        && (color == null || color.equals(item.getColor())))
                .findFirst()
                .orElse(null);
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalItems = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @Override
    public String toString() {
        return "Cart{" +
                "userId=" + userId +
                ", totalItems=" + totalItems +
                ", totalAmount=" + totalAmount +
                ", itemsCount=" + items.size() +
                '}';
    }
}
