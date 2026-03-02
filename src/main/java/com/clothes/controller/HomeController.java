package com.clothes.controller;

import com.clothes.dao.BannerDAO;
import com.clothes.dao.BlogPostDAO;
import com.clothes.dao.ProductDAO;
import com.clothes.dao.VoucherDAO;
import com.clothes.service.CategoryService;
import com.clothes.service.HybridRecommendationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

/**
 * Controller for home page
 */
@Controller
public class HomeController {

    private final ProductDAO productDAO;
    private final CategoryService categoryService;
    private final HybridRecommendationService recommendationService;
    private final BannerDAO bannerDAO;
    private final BlogPostDAO blogPostDAO;
    private final VoucherDAO voucherDAO;

    public HomeController(ProductDAO productDAO,
            CategoryService categoryService,
            HybridRecommendationService recommendationService,
            BannerDAO bannerDAO,
            BlogPostDAO blogPostDAO,
            VoucherDAO voucherDAO) {
        this.productDAO = productDAO;
        this.categoryService = categoryService;
        this.recommendationService = recommendationService;
        this.bannerDAO = bannerDAO;
        this.blogPostDAO = blogPostDAO;
        this.voucherDAO = voucherDAO;
    }

    /**
     * Redirect legacy checkout URL
     */
    @GetMapping("/checkout")
    public String redirectCheckout() {
        return "redirect:/orders/checkout";
    }

    /**
     * Show home page
     */
    @GetMapping("/")
    public String showHomePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");

        // Get recommended products (personalized or trending)
        var recommendedProducts = userId != null
                ? recommendationService.getHomepageRecommendations(userId, 8)
                : productDAO.findTrending(8);

        // Get new arrivals (manual flag, fallback to newest)
        var newProducts = productDAO.findNewProducts(8);
        if (newProducts.isEmpty()) {
            newProducts = productDAO.findAllActive();
            newProducts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
            if (newProducts.size() > 8) {
                newProducts = newProducts.subList(0, 8);
            }
        }

        // Get hot products (manual flag, fallback to trending)
        var hotProducts = productDAO.findHotProducts(8);
        if (hotProducts.isEmpty()) {
            hotProducts = productDAO.findTrending(8);
        }

        // Get categories
        var categories = categoryService.getRootCategories();

        // Get active homepage banners (fallback to defaults handled in template)
        var banners = bannerDAO.findByPosition("main");
        if (banners.isEmpty()) {
            banners = bannerDAO.findAllActive();
        }

        // Get featured blog posts for the news section
        var blogPosts = blogPostDAO.findFeatured(3);
        if (blogPosts.isEmpty()) {
            blogPosts = blogPostDAO.findRecent(3);
        }

        model.addAttribute("recommendedProducts", recommendedProducts);
        model.addAttribute("newProducts", newProducts);
        model.addAttribute("trendingProducts", hotProducts);
        model.addAttribute("featuredProducts", hotProducts);
        model.addAttribute("categories", categories);
        model.addAttribute("banners", banners);
        model.addAttribute("blogPosts", blogPosts);
        model.addAttribute("vouchers", voucherDAO.findValidVouchers());

        return "index";
    }

    /**
     * Show about page
     */
    @GetMapping("/about")
    public String showAboutPage() {
        return "about";
    }

    /**
     * Show stores page
     */
    @GetMapping("/stores")
    public String showStoresPage() {
        return "stores";
    }

    /**
     * Show return policy page
     */
    @GetMapping("/policy/return")
    public String showReturnPolicyPage() {
        return "policy-return";
    }
}
