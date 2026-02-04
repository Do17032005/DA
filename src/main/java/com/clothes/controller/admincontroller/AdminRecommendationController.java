package com.clothes.controller.admincontroller;

import com.clothes.service.UserBasedCFService;
import com.clothes.service.ItemBasedCFService;
import com.clothes.service.HybridRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Controller for managing recommendation system
 * Provides endpoints for computing similarities and managing cache
 */
@RestController
@RequestMapping("/api/admin/recommendations")
@CrossOrigin(origins = "*")
public class AdminRecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminRecommendationController.class);

    private final UserBasedCFService userBasedCFService;
    private final ItemBasedCFService itemBasedCFService;
    private final HybridRecommendationService hybridRecommendationService;

    public AdminRecommendationController(UserBasedCFService userBasedCFService,
            ItemBasedCFService itemBasedCFService,
            HybridRecommendationService hybridRecommendationService) {
        this.userBasedCFService = userBasedCFService;
        this.itemBasedCFService = itemBasedCFService;
        this.hybridRecommendationService = hybridRecommendationService;
    }

    /**
     * POST /api/admin/recommendations/compute-user-similarities
     * Trigger user similarity computation (User-Based CF)
     * This should be run periodically (e.g., daily via scheduled task)
     */
    @PostMapping("/compute-user-similarities")
    public ResponseEntity<?> computeUserSimilarities() {
        try {
            logger.info("POST /api/admin/recommendations/compute-user-similarities - Starting computation");

            long startTime = System.currentTimeMillis();
            userBasedCFService.computeUserSimilarities();
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User similarities computed successfully");
            response.put("durationMs", duration);
            response.put("durationSeconds", duration / 1000.0);

            logger.info("User similarity computation completed in {} ms", duration);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error computing user similarities", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to compute user similarities",
                    "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/recommendations/compute-product-similarities
     * Trigger product similarity computation (Item-Based CF)
     * This should be run periodically (e.g., daily via scheduled task)
     */
    @PostMapping("/compute-product-similarities")
    public ResponseEntity<?> computeProductSimilarities() {
        try {
            logger.info("POST /api/admin/recommendations/compute-product-similarities - Starting computation");

            long startTime = System.currentTimeMillis();
            itemBasedCFService.computeProductSimilarities();
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product similarities computed successfully");
            response.put("durationMs", duration);
            response.put("durationSeconds", duration / 1000.0);

            logger.info("Product similarity computation completed in {} ms", duration);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error computing product similarities", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to compute product similarities",
                    "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/recommendations/compute-product-similarities-cooccurrence
     * Compute product similarities using co-occurrence method
     * Faster alternative to standard similarity computation
     */
    @PostMapping("/compute-product-similarities-cooccurrence")
    public ResponseEntity<?> computeProductSimilaritiesCoOccurrence() {
        try {
            logger.info("POST /api/admin/recommendations/compute-product-similarities-cooccurrence - Starting");

            long startTime = System.currentTimeMillis();
            itemBasedCFService.computeProductSimilaritiesByCoOccurrence();
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Product similarities (co-occurrence) computed successfully");
            response.put("durationMs", duration);
            response.put("durationSeconds", duration / 1000.0);

            logger.info("Product similarity (co-occurrence) computation completed in {} ms", duration);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error computing product similarities (co-occurrence)", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to compute product similarities",
                    "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/recommendations/cleanup-cache
     * Clean up expired recommendations cache
     */
    @PostMapping("/cleanup-cache")
    public ResponseEntity<?> cleanupCache() {
        try {
            logger.info("POST /api/admin/recommendations/cleanup-cache - Starting cleanup");

            hybridRecommendationService.cleanupExpiredCache();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cache cleanup completed successfully");

            logger.info("Cache cleanup completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error cleaning up cache", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to cleanup cache",
                    "message", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/recommendations/compute-all
     * Trigger all similarity computations
     * WARNING: This can be resource-intensive!
     */
    @PostMapping("/compute-all")
    public ResponseEntity<?> computeAllSimilarities() {
        try {
            logger.info("POST /api/admin/recommendations/compute-all - Starting all computations");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tasks", new HashMap<String, Object>());

            // Compute user similarities
            try {
                long startTime = System.currentTimeMillis();
                userBasedCFService.computeUserSimilarities();
                long duration = System.currentTimeMillis() - startTime;
                ((Map<String, Object>) response.get("tasks")).put("userSimilarities", Map.of(
                        "success", true,
                        "durationMs", duration));
            } catch (Exception e) {
                logger.error("Error in user similarities", e);
                ((Map<String, Object>) response.get("tasks")).put("userSimilarities", Map.of(
                        "success", false,
                        "error", e.getMessage()));
            }

            // Compute product similarities
            try {
                long startTime = System.currentTimeMillis();
                itemBasedCFService.computeProductSimilarities();
                long duration = System.currentTimeMillis() - startTime;
                ((Map<String, Object>) response.get("tasks")).put("productSimilarities", Map.of(
                        "success", true,
                        "durationMs", duration));
            } catch (Exception e) {
                logger.error("Error in product similarities", e);
                ((Map<String, Object>) response.get("tasks")).put("productSimilarities", Map.of(
                        "success", false,
                        "error", e.getMessage()));
            }

            // Cleanup cache
            try {
                hybridRecommendationService.cleanupExpiredCache();
                ((Map<String, Object>) response.get("tasks")).put("cacheCleanup", Map.of(
                        "success", true));
            } catch (Exception e) {
                logger.error("Error in cache cleanup", e);
                ((Map<String, Object>) response.get("tasks")).put("cacheCleanup", Map.of(
                        "success", false,
                        "error", e.getMessage()));
            }

            logger.info("All computations completed");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in compute all", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to compute all similarities",
                    "message", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/recommendations/status
     * Get recommendation system status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("status", "operational");
            status.put("services", Map.of(
                    "userBasedCF", "enabled",
                    "itemBasedCF", "enabled",
                    "hybrid", "enabled"));
            status.put("message", "Recommendation system is running");

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("Error getting status", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to get status",
                    "message", e.getMessage()));
        }
    }
}
