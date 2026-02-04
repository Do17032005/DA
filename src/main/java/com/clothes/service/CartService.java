package com.clothes.service;

import com.clothes.dao.CartDAO;
import com.clothes.model.Cart;
import com.clothes.model.CartItem;
import com.clothes.model.Product;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for Shopping Cart management
 * Database-backed persistent cart
 */
@Service
public class CartService {

    private final CartDAO cartDAO;

    public CartService(CartDAO cartDAO) {
        this.cartDAO = cartDAO;
    }

    private HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attr.getRequest().getSession(true);
    }

    /**
     * Get current cart (or create if not exists)
     */
    public Cart getCart() {
        HttpSession session = getSession();
        Long userId = (Long) session.getAttribute("userId");
        String sessionToken = (String) session.getAttribute("cartToken");

        // If no token and not logged in, generate token
        if (userId == null && sessionToken == null) {
            sessionToken = UUID.randomUUID().toString();
            session.setAttribute("cartToken", sessionToken);
        }

        Optional<Cart> cartOpt;
        if (userId != null) {
            cartOpt = cartDAO.findByUserId(userId);
            // If user logged in but has no cart, check if they have a session cart to
            // merge?
            // Usually merge happens at login. If we are here, and no cart, create one.
            if (cartOpt.isEmpty() && sessionToken != null) {
                // Double check if session cart exists (edge case where sessionToken remains but
                // userId is set)
                cartOpt = cartDAO.findBySessionToken(sessionToken);
            }
        } else {
            cartOpt = cartDAO.findBySessionToken(sessionToken);
        }

        if (cartOpt.isPresent()) {
            return cartOpt.get();
        }

        // Create new cart
        return cartDAO.createCart(userId, sessionToken);
    }

    /**
     * Add item to cart
     */
    public void addItem(Long productId, String productName, BigDecimal price,
            Integer quantity, String imageUrl, String size, String color) {
        Cart cart = getCart();

        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setSize(size);
        item.setColor(color);

        cartDAO.addItem(cart.getCartId(), item);
    }

    /**
     * Remove item from cart
     */
    public void removeItem(Long productId) {
        Cart cart = getCart();
        cartDAO.removeItemByProduct(cart.getCartId(), productId);
    }

    /**
     * Update item quantity
     */
    public void updateQuantity(Long productId, Integer quantity) {
        if (quantity <= 0) {
            removeItem(productId);
            return;
        }

        Cart cart = getCart();
        Optional<CartItem> itemOpt = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (itemOpt.isPresent()) {
            cartDAO.updateItemQuantity(itemOpt.get().getCartItemId(), quantity);
        }
    }

    /**
     * Get cart item by product ID
     */
    public Optional<CartItem> getCartItem(Long productId) {
        return getCart().getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
    }

    /**
     * Clear cart
     */
    public void clearCart() {
        Cart cart = getCart();
        cartDAO.clearCart(cart.getCartId());
    }

    /**
     * Get total items count
     */
    public int getTotalItems() {
        return getCart().getTotalItems();
    }

    /**
     * Get total amount
     */
    public BigDecimal getTotalAmount() {
        return getCart().getTotalAmount();
    }

    /**
     * Check if cart is empty
     */
    public boolean isEmpty() {
        return getCart().getItems().isEmpty();
    }

    /**
     * Merge session cart to user cart on login
     */
    public void mergeCart(Long userId) {
        HttpSession session = getSession();
        String sessionToken = (String) session.getAttribute("cartToken");

        if (sessionToken != null) {
            Optional<Cart> sessionCartOpt = cartDAO.findBySessionToken(sessionToken);
            if (sessionCartOpt.isPresent()) {
                Cart sessionCart = sessionCartOpt.get();

                Optional<Cart> userCartOpt = cartDAO.findByUserId(userId);
                if (userCartOpt.isPresent()) {
                    // Merge items
                    for (CartItem item : sessionCart.getItems()) {
                        cartDAO.addItem(userCartOpt.get().getCartId(), item);
                    }
                    cartDAO.clearCart(sessionCart.getCartId());
                    // Delete the old shell if possible, or just leave empty
                } else {
                    cartDAO.updateCartUserId(sessionCart.getCartId(), userId);
                }
            }
        }
        session.removeAttribute("cartToken");
    }
}
