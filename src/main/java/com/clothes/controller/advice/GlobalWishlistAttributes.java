package com.clothes.controller.advice;

import com.clothes.dao.WishlistDAO;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Collections;
import java.util.Set;

/**
 * Provides commonly used wishlist-related model attributes to all views.
 */
@ControllerAdvice
public class GlobalWishlistAttributes {

    private final WishlistDAO wishlistDAO;

    public GlobalWishlistAttributes(WishlistDAO wishlistDAO) {
        this.wishlistDAO = wishlistDAO;
    }

    @ModelAttribute("wishlistProductIds")
    public Set<Long> populateWishlistProductIds(HttpSession session) {
        if (session == null) {
            return Collections.emptySet();
        }

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Collections.emptySet();
        }

        return wishlistDAO.findProductIdsByUserId(userId);
    }
}
