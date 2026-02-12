package com.clothes.controller;

import com.clothes.dao.WishlistDAO;
import com.clothes.dao.ProductDAO;
import com.clothes.model.Product;
import com.clothes.model.Wishlist;
import com.clothes.service.HybridRecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for wishlist management
 */
@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistDAO wishlistDAO;
    private final ProductDAO productDAO;
    private final HybridRecommendationService hybridRecommendationService;

    public WishlistController(WishlistDAO wishlistDAO, ProductDAO productDAO,
            HybridRecommendationService hybridRecommendationService) {
        this.wishlistDAO = wishlistDAO;
        this.productDAO = productDAO;
        this.hybridRecommendationService = hybridRecommendationService;
    }

    /**
     * Show wishlist page
     */
    @GetMapping
    public String showWishlist(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để xem danh sách yêu thích");
            return "redirect:/user/login";
        }

        List<Wishlist> wishlists = wishlistDAO.findByUserId(userId);
        model.addAttribute("wishlistItems", wishlists);
        model.addAttribute("totalItems", wishlists.size());

        // Show personalized recommendations (fallback to trending)
        List<Product> recommendedProducts;
        try {
            recommendedProducts = hybridRecommendationService.getRecommendations(userId, 4);
            if (recommendedProducts.isEmpty()) {
                recommendedProducts = productDAO.findTrending(4);
            }
        } catch (Exception e) {
            recommendedProducts = productDAO.findTrending(4);
        }
        model.addAttribute("recommendedProducts", recommendedProducts);

        return "wishlist";
    }

    /**
     * Get wishlist count (API)
     */
    @GetMapping("/count")
    @ResponseBody
    public Map<String, Object> getWishlistCount(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            int count = wishlistDAO.countByUserId(userId);
            response.put("count", count);
        } else {
            response.put("count", 0);
        }

        return response;
    }

    /**
     * Add product to wishlist
     */
    @PostMapping("/add")
    public String addToWishlist(@RequestParam Long productId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thêm vào yêu thích");
            return "redirect:/user/login";
        }

        try {
            // Check if product exists
            var productOpt = productDAO.findById(productId);
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại");
                return "redirect:/products";
            }

            // Check if already in wishlist
            if (wishlistDAO.exists(userId, productId)) {
                redirectAttributes.addFlashAttribute("warning", "Sản phẩm đã có trong danh sách yêu thích");
                return "redirect:/products/" + productId;
            }

            Wishlist wishlist = new Wishlist();
            wishlist.setUserId(userId);
            wishlist.setProductId(productId);

            wishlistDAO.save(wishlist);
            redirectAttributes.addFlashAttribute("success", "Đã thêm vào danh sách yêu thích");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/products/" + productId;
    }

    /**
     * Remove from wishlist
     */
    @PostMapping("/remove/{id}")
    public String removeFromWishlist(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            var wishlistOpt = wishlistDAO.findById(id);
            if (wishlistOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy");
                return "redirect:/wishlist";
            }

            Wishlist wishlist = wishlistOpt.get();

            // Check if user owns this wishlist item
            if (!wishlist.getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền xóa");
                return "redirect:/wishlist";
            }

            wishlistDAO.delete(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa khỏi danh sách yêu thích");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/wishlist";
    }

    /**
     * Toggle wishlist (for AJAX)
     */
    @PostMapping("/toggle")
    @ResponseBody
    public Map<String, Object> toggleWishlist(@RequestParam Long productId,
            @RequestParam(required = false, defaultValue = "toggle") String action,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            response.put("success", false);
            response.put("loggedIn", false);
            response.put("message", "Vui lòng đăng nhập để sử dụng danh sách yêu thích");
            return response;
        }

        try {
            String normalizedAction = action != null ? action.trim().toLowerCase() : "toggle";
            boolean exists = wishlistDAO.exists(userId, productId);

            switch (normalizedAction) {
                case "add":
                    if (!exists) {
                        Wishlist wishlist = new Wishlist(userId, productId);
                        wishlistDAO.save(wishlist);
                    }
                    response.put("action", "added");
                    response.put("inWishlist", true);
                    response.put("success", true);
                    break;
                case "remove":
                    if (exists) {
                        wishlistDAO.deleteByUserAndProduct(userId, productId);
                    }
                    response.put("action", "removed");
                    response.put("inWishlist", false);
                    response.put("success", true);
                    break;
                default:
                    if (exists) {
                        wishlistDAO.deleteByUserAndProduct(userId, productId);
                        response.put("action", "removed");
                        response.put("inWishlist", false);
                    } else {
                        Wishlist wishlist = new Wishlist(userId, productId);
                        wishlistDAO.save(wishlist);
                        response.put("action", "added");
                        response.put("inWishlist", true);
                    }
                    response.put("success", true);
                    break;
            }

            response.put("wishlistCount", wishlistDAO.countByUserId(userId));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra, vui lòng thử lại");
        }

        return response;
    }

    /**
     * Check if product is in wishlist (for AJAX)
     */
    @GetMapping("/check")
    @ResponseBody
    public boolean checkWishlist(@RequestParam Long productId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return false;
        }
        return wishlistDAO.exists(userId, productId);
    }
}
