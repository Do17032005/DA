package com.clothes.controller;

import com.clothes.model.Product;
import com.clothes.model.UserInteraction;
import com.clothes.service.HybridRecommendationService;
import com.clothes.service.ItemBasedCFService;
import com.clothes.service.UserBasedCFService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for AI Recommendation System
 * Provides endpoints for getting personalized recommendations
 */
@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final HybridRecommendationService hybridRecommendationService;
    private final UserBasedCFService userBasedCFService;
    private final ItemBasedCFService itemBasedCFService;

    public RecommendationController(HybridRecommendationService hybridRecommendationService,
            UserBasedCFService userBasedCFService,
            ItemBasedCFService itemBasedCFService) {
        this.hybridRecommendationService = hybridRecommendationService;
        this.userBasedCFService = userBasedCFService;
        this.itemBasedCFService = itemBasedCFService;
    }

    /**
     * GET /api/recommendations/user/{userId}
     * Get personalized hybrid recommendations for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            logger.info("GET /api/recommendations/user/{} with limit={}", userId, limit);

            List<Product> recommendations = hybridRecommendationService.getRecommendations(userId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);
            response.put("type", "hybrid");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting recommendations for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get recommendations", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/recommendations/user/{userId}/user-based
     * Get User-Based CF recommendations
     */
    @GetMapping("/user/{userId}/user-based")
    public ResponseEntity<?> getUserBasedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            logger.info("GET /api/recommendations/user/{}/user-based with limit={}", userId, limit);

            List<Product> recommendations = userBasedCFService.getRecommendations(userId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);
            response.put("type", "user_based_cf");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting user-based recommendations for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get recommendations", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/recommendations/user/{userId}/item-based
     * Get Item-Based CF recommendations
     */
    @GetMapping("/user/{userId}/item-based")
    public ResponseEntity<?> getItemBasedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            logger.info("GET /api/recommendations/user/{}/item-based with limit={}", userId, limit);

            List<Product> recommendations = itemBasedCFService.getRecommendations(userId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);
            response.put("type", "item_based_cf");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting item-based recommendations for user: " + userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get recommendations", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/recommendations/homepage
     * Get homepage recommendations (personalized or trending)
     */
    @GetMapping("/homepage")
    public ResponseEntity<?> getHomepageRecommendations(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            logger.info("GET /api/recommendations/homepage with userId={}, limit={}", userId, limit);

            List<Product> recommendations = hybridRecommendationService.getHomepageRecommendations(userId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);
            response.put("type", userId != null ? "personalized" : "trending");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting homepage recommendations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get recommendations", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/recommendations/product/{productId}/similar
     * Get similar products (for product detail page)
     */
    @GetMapping("/product/{productId}/similar")
    public ResponseEntity<?> getSimilarProducts(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit) {

        try {
            logger.info("GET /api/recommendations/product/{}/similar with limit={}", productId, limit);

            List<Product> recommendations = itemBasedCFService.getSimilarProducts(productId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);
            response.put("type", "similar_products");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting similar products for product: " + productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get similar products", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/recommendations/product/{productId}/bought-together
     * Get frequently bought together products
     */
    @GetMapping("/product/{productId}/bought-together")
    public ResponseEntity<?> getFrequentlyBoughtTogether(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "4") int limit) {

        try {
            logger.info("GET /api/recommendations/product/{}/bought-together with limit={}", productId, limit);

            List<Product> recommendations = hybridRecommendationService.getFrequentlyBoughtTogether(productId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);
            response.put("type", "bought_together");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting bought together products for product: " + productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get bought together products", "message", e.getMessage()));
        }
    }

    /**
     * GET /api/recommendations/product/{productId}/also-viewed
     * Get customers also viewed products
     */
    @GetMapping("/product/{productId}/also-viewed")
    public ResponseEntity<?> getCustomersAlsoViewed(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "6") int limit) {

        try {
            logger.info("GET /api/recommendations/product/{}/also-viewed with limit={}", productId, limit);

            List<Product> recommendations = hybridRecommendationService.getCustomersAlsoViewed(productId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);
            response.put("type", "also_viewed");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting also viewed products for product: " + productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get also viewed products", "message", e.getMessage()));
        }
    }

    /**
     * POST /api/recommendations/interaction
     * Record user interaction for future recommendations
     */
    @PostMapping("/interaction")
    public ResponseEntity<?> recordInteraction(@RequestBody InteractionRequest request) {
        try {
            logger.info("POST /api/recommendations/interaction: {}", request);

            UserInteraction.InteractionType type = UserInteraction.InteractionType
                    .fromValue(request.getInteractionType());

            hybridRecommendationService.recordInteraction(
                    request.getUserId(),
                    request.getProductId(),
                    type,
                    request.getValue());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Interaction recorded successfully"));
        } catch (Exception e) {
            logger.error("Error recording interaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to record interaction", "message", e.getMessage()));
        }
    }

    /**
     * Request DTO for interaction recording
     */
    public static class InteractionRequest {
        private Long userId;
        private Long productId;
        private String interactionType; // view, add_to_cart, purchase, rating, wishlist
        private BigDecimal value; // Optional, for ratings

        // Getters and Setters
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getInteractionType() {
            return interactionType;
        }

        public void setInteractionType(String interactionType) {
            this.interactionType = interactionType;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "InteractionRequest{" +
                    "userId=" + userId +
                    ", productId=" + productId +
                    ", interactionType='" + interactionType + '\'' +
                    ", value=" + value +
                    '}';
        }
    }
}
