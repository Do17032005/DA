package com.clothes.controller;

import com.clothes.dao.ReviewDAO;
import com.clothes.dao.ProductDAO;
import com.clothes.model.Review;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

/**
 * Controller for product reviews
 */
@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewDAO reviewDAO;
    private final ProductDAO productDAO;

    public ReviewController(ReviewDAO reviewDAO, ProductDAO productDAO) {
        this.reviewDAO = reviewDAO;
        this.productDAO = productDAO;
    }

    /**
     * Add review to product
     */
    @PostMapping("/add")
    public String addReview(@RequestParam Long productId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để đánh giá");
            return "redirect:/user/login";
        }

        try {
            // Validate rating
            if (rating < 1 || rating > 5) {
                redirectAttributes.addFlashAttribute("error", "Đánh giá phải từ 1 đến 5 sao");
                return "redirect:/products/" + productId;
            }

            // Check if user already reviewed
            if (reviewDAO.hasUserReviewed(userId, productId)) {
                redirectAttributes.addFlashAttribute("error", "Bạn đã đánh giá sản phẩm này rồi");
                return "redirect:/products/" + productId;
            }

            // Check if product exists
            var productOpt = productDAO.findById(productId);
            if (productOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại");
                return "redirect:/products";
            }

            Review review = new Review();
            review.setProductId(productId);
            review.setUserId(userId);
            review.setRating(rating);
            review.setComment(comment);

            reviewDAO.save(review);
            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/products/" + productId;
    }

    /**
     * Delete review
     */
    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/user/login";
        }

        try {
            var reviewOpt = reviewDAO.findById(id);
            if (reviewOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Đánh giá không tồn tại");
                return "redirect:/user/profile";
            }

            Review review = reviewOpt.get();

            // Check if user owns this review
            if (!review.getUserId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Không có quyền xóa đánh giá này");
                return "redirect:/user/profile";
            }

            reviewDAO.delete(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa đánh giá");

            return "redirect:/products/" + review.getProductId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/user/profile";
        }
    }

    /**
     * Get user's reviews
     */
    @GetMapping("/my-reviews")
    public String showMyReviews(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/user/login";
        }

        var reviews = reviewDAO.findByUserId(userId);
        model.addAttribute("reviews", reviews);

        return "my-reviews";
    }
}
