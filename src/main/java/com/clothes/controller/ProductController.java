package com.clothes.controller;

import com.clothes.dao.ProductDAO;
import com.clothes.dao.ReviewDAO;
import com.clothes.dao.WishlistDAO;
import com.clothes.model.Category;
import com.clothes.model.Wishlist;
import com.clothes.model.Product;
import com.clothes.model.Review;
import com.clothes.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

/**
 * Controller for product browsing and details
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductDAO productDAO;
    private final CategoryService categoryService;
    private final ReviewDAO reviewDAO;
    private final WishlistDAO wishlistDAO;

    public ProductController(ProductDAO productDAO, CategoryService categoryService,
            ReviewDAO reviewDAO, WishlistDAO wishlistDAO) {
        this.productDAO = productDAO;
        this.categoryService = categoryService;
        this.reviewDAO = reviewDAO;
        this.wishlistDAO = wishlistDAO;
    }

    /**
     * Show products page with filters
     */
    @GetMapping
    public String showProductsPage(@RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String priceMin,
            @RequestParam(required = false) String priceMax,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {
        List<Product> products;

        Product.Gender genderEnum = gender != null ? Product.Gender.fromValue(gender) : null;

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Search by keyword
            products = productDAO.search(keyword, 100);
        } else if (categoryId != null || gender != null || priceMin != null || priceMax != null) {
            // Apply filters
            java.math.BigDecimal min = priceMin != null && !priceMin.isEmpty() ? new java.math.BigDecimal(priceMin)
                    : null;
            java.math.BigDecimal max = priceMax != null && !priceMax.isEmpty() ? new java.math.BigDecimal(priceMax)
                    : null;
            products = productDAO.findWithFilters(categoryId, genderEnum, min, max, sortBy);
        } else if (brand != null && !brand.trim().isEmpty()) {
            // Filter by brand
            products = productDAO.findByBrand(brand);
        } else {
            // Show all products
            products = productDAO.findAllActive();

            // Apply sorting if specified
            if (sortBy != null) {
                products = productDAO.findWithFilters(null, null, null, null, sortBy);
            }
        }

        model.addAttribute("products", products);
        model.addAttribute("totalProducts", products.size());
        model.addAttribute("totalPages", 1);
        model.addAttribute("currentPage", page);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("brands", productDAO.getAllBrands());
        model.addAttribute("colors", productDAO.getAllColors());
        model.addAttribute("sizes", productDAO.getAllSizes());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedGender", gender);
        model.addAttribute("selectedBrand", brand);
        model.addAttribute("priceMin", priceMin);
        model.addAttribute("priceMax", priceMax);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("keyword", keyword);

        return "products";
    }

    /**
     * Show product detail page
     */
    @GetMapping("/{id}")
    public String showProductDetail(@PathVariable Long id, HttpSession session, Model model) {
        Optional<Product> productOpt = productDAO.findById(id);

        if (productOpt.isEmpty()) {
            return "redirect:/products";
        }

        Product product = productOpt.get();

        // Increment view count
        productDAO.incrementViewCount(id);

        // Get reviews
        List<Review> reviews = reviewDAO.findByProductId(id);
        Double avgRating = reviewDAO.getAverageRating(id);
        int reviewCount = reviewDAO.getReviewCount(id);

        // Check if user is logged in and can review
        Long userId = (Long) session.getAttribute("userId");
        boolean canReview = userId != null && !reviewDAO.hasUserReviewed(userId, id);
        boolean isInWishlist = userId != null && wishlistDAO.exists(userId, id);

        // Get similar products by category
        List<Product> similarProducts = productDAO.findByCategoryId(product.getCategoryId());
        similarProducts.removeIf(p -> p.getProductId().equals(id));
        if (similarProducts.size() > 4) {
            similarProducts = similarProducts.subList(0, 4);
        }

        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating != null ? avgRating : 0.0);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("canReview", canReview);
        model.addAttribute("isInWishlist", isInWishlist);
        model.addAttribute("similarProducts", similarProducts);

        return "product-detail";
    }

    /**
     * Search products
     */
    @GetMapping("/search")
    public String searchProducts(@RequestParam String q, Model model) {
        List<Product> products = productDAO.search(q, 100);

        model.addAttribute("products", products);
        model.addAttribute("keyword", q);
        model.addAttribute("categories", categoryService.getAllCategories());

        return "products";
    }

    /**
     * Show trending products
     */
    @GetMapping("/trending")
    public String showTrendingProducts(Model model) {
        List<Product> products = productDAO.findTrending(12);

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Sản phẩm thịnh hành");

        return "products";
    }

    /**
     * Show new arrivals
     */
    @GetMapping("/new")
    public String showNewArrivals(Model model) {
        List<Product> products = productDAO.findAllActive();

        // Sort by created date (newest first)
        products.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

        if (products.size() > 12) {
            products = products.subList(0, 12);
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Sản phẩm mới");

        return "products";
    }
}
