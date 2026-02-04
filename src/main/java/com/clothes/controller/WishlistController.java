package com.clothes.controller;

import com.clothes.dao.WishlistDAO;
import com.clothes.dao.ProductDAO;
import com.clothes.model.Wishlist;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * Controller for wishlist management
 */
@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistDAO wishlistDAO;
    private final ProductDAO productDAO;

    public WishlistController(WishlistDAO wishlistDAO, ProductDAO productDAO) {
        this.wishlistDAO = wishlistDAO;
        this.productDAO = productDAO;
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
        model.addAttribute("wishlists", wishlists);
        model.addAttribute("totalItems", wishlists.size());

        return "wishlist";
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
    public String toggleWishlist(@RequestParam Long productId,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "login_required";
        }

        try {
            if (wishlistDAO.exists(userId, productId)) {
                wishlistDAO.deleteByUserAndProduct(userId, productId);
                return "removed";
            } else {
                Wishlist wishlist = new Wishlist(userId, productId);
                wishlistDAO.save(wishlist);
                return "added";
            }
        } catch (Exception e) {
            return "error";
        }
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
