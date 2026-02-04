package com.clothes.controller;

import com.clothes.dao.ProductDAO;
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

    public HomeController(ProductDAO productDAO, CategoryService categoryService,
            HybridRecommendationService recommendationService) {
        this.productDAO = productDAO;
        this.categoryService = categoryService;
        this.recommendationService = recommendationService;
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

        // Get new arrivals
        var newProducts = productDAO.findAllActive();
        newProducts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
        if (newProducts.size() > 8) {
            newProducts = newProducts.subList(0, 8);
        }

        // Get trending products
        var trendingProducts = productDAO.findTrending(8);

        // Get categories
        var categories = categoryService.getRootCategories();

        model.addAttribute("recommendedProducts", recommendedProducts);
        model.addAttribute("newProducts", newProducts);
        model.addAttribute("trendingProducts", trendingProducts);
        model.addAttribute("categories", categories);

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
