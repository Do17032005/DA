package com.clothes.controller;

import com.clothes.dao.ProductDAO;
import com.clothes.dao.ReviewDAO;
import com.clothes.dao.WishlistDAO;
import com.clothes.model.Category;
import com.clothes.model.Wishlist;
import com.clothes.model.Product;
import com.clothes.model.Review;
import com.clothes.service.CategoryService;
import com.clothes.service.ItemBasedCFService;
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
    private final ItemBasedCFService itemBasedCFService;

    public ProductController(ProductDAO productDAO, CategoryService categoryService,
            ReviewDAO reviewDAO, WishlistDAO wishlistDAO, ItemBasedCFService itemBasedCFService) {
        this.productDAO = productDAO;
        this.categoryService = categoryService;
        this.reviewDAO = reviewDAO;
        this.wishlistDAO = wishlistDAO;
        this.itemBasedCFService = itemBasedCFService;
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
            @RequestParam(required = false) Boolean isNew,
            @RequestParam(required = false) Boolean isHot,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {
        List<Product> products;

        Product.Gender genderEnum = gender != null ? Product.Gender.fromValue(gender) : null;
        boolean hasTagFilter = Boolean.TRUE.equals(isNew) || Boolean.TRUE.equals(isHot);

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Search by keyword
            products = productDAO.search(keyword, 100);
            products = applyTagFilters(products, isNew, isHot);
        } else if (categoryId != null || gender != null || priceMin != null || priceMax != null || hasTagFilter) {
            // Apply filters
            java.math.BigDecimal min = priceMin != null && !priceMin.isEmpty() ? new java.math.BigDecimal(priceMin)
                    : null;
            java.math.BigDecimal max = priceMax != null && !priceMax.isEmpty() ? new java.math.BigDecimal(priceMax)
                    : null;
            products = productDAO.findWithFilters(categoryId, genderEnum, min, max, sortBy, isNew, isHot);
        } else if (brand != null && !brand.trim().isEmpty()) {
            // Filter by brand
            products = productDAO.findByBrand(brand);
            products = applyTagFilters(products, isNew, isHot);
        } else {
            // Show all products
            products = productDAO.findAllActive();
            products = applyTagFilters(products, isNew, isHot);

            // Apply sorting if specified
            if (sortBy != null) {
                products = productDAO.findWithFilters(null, null, null, null, sortBy, isNew, isHot);
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

    private List<Product> applyTagFilters(List<Product> products, Boolean isNew, Boolean isHot) {
        boolean filterNew = Boolean.TRUE.equals(isNew);
        boolean filterHot = Boolean.TRUE.equals(isHot);

        if (!filterNew && !filterHot) {
            return products;
        }

        return products.stream()
                .filter(product -> (filterNew && Boolean.TRUE.equals(product.getIsNew()))
                        || (filterHot && Boolean.TRUE.equals(product.getIsHot())))
                .toList();
    }

    /**
     * Show product detail page
     */
    @GetMapping("/{id}")
    public String showProductDetail(@PathVariable Long id,
            @RequestParam(name = "tab", required = false) String tab,
            HttpSession session,
            Model model) {
        Optional<Product> productOpt = productDAO.findById(id);

        if (productOpt.isEmpty()) {
            return "redirect:/products";
        }

        Product product = productOpt.get();

        // Increment view count
        productDAO.incrementViewCount(id);

        // Get category info
        Category category = null;
        if (product.getCategoryId() != null) {
            category = categoryService.getCategoryById(product.getCategoryId()).orElse(null);
        }

        // Get reviews
        List<Review> reviews = reviewDAO.findByProductId(id);
        Double avgRating = reviewDAO.getAverageRating(id);
        int reviewCount = reviewDAO.getReviewCount(id);

        // Check if user is logged in and can review
        Long userId = (Long) session.getAttribute("userId");
        boolean canReview = userId != null && !reviewDAO.hasUserReviewed(userId, id);
        Long userReviewId = null;
        if (userId != null) {
            for (Review review : reviews) {
                if (review.getUserId() != null && review.getUserId().equals(userId)) {
                    userReviewId = review.getReviewId();
                    break;
                }
            }
        }
        boolean isInWishlist = userId != null && wishlistDAO.exists(userId, id);

        // Get similar products (item-based CF) with category-based fallback
        List<Product> similarProducts;
        try {
            similarProducts = itemBasedCFService.getSimilarProducts(id, 4);
            similarProducts.removeIf(p -> p.getProductId().equals(id));
            if (similarProducts.isEmpty()) {
                similarProducts = product.getCategoryId() != null
                        ? productDAO.findByCategoryId(product.getCategoryId())
                        : productDAO.findAllActive();
                similarProducts.removeIf(p -> p.getProductId().equals(id));
                if (similarProducts.size() > 4) {
                    similarProducts = similarProducts.subList(0, 4);
                }
            }
        } catch (Exception e) {
            similarProducts = product.getCategoryId() != null
                    ? productDAO.findByCategoryId(product.getCategoryId())
                    : productDAO.findAllActive();
            similarProducts.removeIf(p -> p.getProductId().equals(id));
            if (similarProducts.size() > 4) {
                similarProducts = similarProducts.subList(0, 4);
            }
        }

        String activeTab = (tab != null && !tab.isBlank()) ? tab : "description";

        model.addAttribute("product", product);
        model.addAttribute("category", category);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating != null ? avgRating : 0.0);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("canReview", canReview);
        model.addAttribute("isInWishlist", isInWishlist);
        model.addAttribute("similarProducts", similarProducts);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("loggedInUserId", userId);
        model.addAttribute("userReviewId", userReviewId);

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
        List<Product> products = productDAO.findHotProducts(12);
        if (products.isEmpty()) {
            products = productDAO.findTrending(12);
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Sản phẩm nổi bật");

        return "products";
    }

    /**
     * Show new arrivals
     */
    @GetMapping("/new")
    public String showNewArrivals(Model model) {
        List<Product> products = productDAO.findNewProducts(12);

        if (products.isEmpty()) {
            // Fallback to newest if no manual tags
            products = productDAO.findAllActive();
            products.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));
            if (products.size() > 12) {
                products = products.subList(0, 12);
            }
        }

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("pageTitle", "Sản phẩm mới");

        return "products";
    }
}
