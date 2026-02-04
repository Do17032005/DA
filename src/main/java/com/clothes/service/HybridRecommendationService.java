package com.clothes.service;

import com.clothes.dao.*;
import com.clothes.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid Recommendation Service
 * Combines User-Based CF, Item-Based CF, and other strategies
 * Provides the best overall recommendations
 */
@Service
public class HybridRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(HybridRecommendationService.class);

    private final UserBasedCFService userBasedCFService;
    private final ItemBasedCFService itemBasedCFService;
    private final ProductDAO productDAO;
    private final UserInteractionDAO userInteractionDAO;
    private final RecommendationDAO recommendationDAO;

    // Weights for different recommendation strategies
    private static final double WEIGHT_USER_BASED_CF = 0.3;
    private static final double WEIGHT_ITEM_BASED_CF = 0.5;
    private static final double WEIGHT_TRENDING = 0.2;

    public HybridRecommendationService(UserBasedCFService userBasedCFService,
            ItemBasedCFService itemBasedCFService,
            ProductDAO productDAO,
            UserInteractionDAO userInteractionDAO,
            RecommendationDAO recommendationDAO) {
        this.userBasedCFService = userBasedCFService;
        this.itemBasedCFService = itemBasedCFService;
        this.productDAO = productDAO;
        this.userInteractionDAO = userInteractionDAO;
        this.recommendationDAO = recommendationDAO;
    }

    /**
     * Get hybrid recommendations combining multiple strategies
     */
    public List<Product> getRecommendations(Long userId, int limit) {
        logger.info("Generating Hybrid recommendations for user: {}", userId);

        // Check cache first
        List<Recommendation> cachedRecs = recommendationDAO.findByUserIdAndType(
                userId, Recommendation.RecommendationType.HYBRID, limit);

        if (!cachedRecs.isEmpty()) {
            logger.info("Returning {} cached hybrid recommendations", cachedRecs.size());
            List<Long> productIds = cachedRecs.stream()
                    .map(Recommendation::getRecommendedProductId)
                    .collect(Collectors.toList());
            return productDAO.findByIds(productIds);
        }

        // Get user's already seen products
        Set<Long> seenProducts = new HashSet<>(
                userInteractionDAO.findProductIdsByUserId(userId));

        Map<Long, Double> hybridScores = new HashMap<>();

        // 1. User-Based CF recommendations
        try {
            List<Product> userBasedRecs = userBasedCFService.getRecommendations(userId, limit * 2);
            for (int i = 0; i < userBasedRecs.size(); i++) {
                Long productId = userBasedRecs.get(i).getProductId();
                if (!seenProducts.contains(productId)) {
                    double score = WEIGHT_USER_BASED_CF * (1.0 - (i / (double) userBasedRecs.size()));
                    hybridScores.put(productId, hybridScores.getOrDefault(productId, 0.0) + score);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting user-based recommendations", e);
        }

        // 2. Item-Based CF recommendations
        try {
            List<Product> itemBasedRecs = itemBasedCFService.getRecommendations(userId, limit * 2);
            for (int i = 0; i < itemBasedRecs.size(); i++) {
                Long productId = itemBasedRecs.get(i).getProductId();
                if (!seenProducts.contains(productId)) {
                    double score = WEIGHT_ITEM_BASED_CF * (1.0 - (i / (double) itemBasedRecs.size()));
                    hybridScores.put(productId, hybridScores.getOrDefault(productId, 0.0) + score);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting item-based recommendations", e);
        }

        // 3. Trending products (for diversity)
        try {
            List<Product> trendingProducts = productDAO.findTrending(limit);
            for (int i = 0; i < trendingProducts.size(); i++) {
                Long productId = trendingProducts.get(i).getProductId();
                if (!seenProducts.contains(productId)) {
                    double score = WEIGHT_TRENDING * (1.0 - (i / (double) trendingProducts.size()));
                    hybridScores.put(productId, hybridScores.getOrDefault(productId, 0.0) + score);
                }
            }
        } catch (Exception e) {
            logger.error("Error getting trending products", e);
        }

        // 4. Sort by combined score and get top N
        List<Map.Entry<Long, Double>> sortedProducts = new ArrayList<>(hybridScores.entrySet());
        sortedProducts.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        List<Long> topProductIds = sortedProducts.stream()
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topProductIds.isEmpty()) {
            logger.warn("No hybrid recommendations generated, falling back to trending");
            return productDAO.findTrending(limit);
        }

        // 5. Cache recommendations
        cacheRecommendations(userId, hybridScores, topProductIds);

        logger.info("Generated {} Hybrid recommendations for user: {}", topProductIds.size(), userId);
        return productDAO.findByIds(topProductIds);
    }

    /**
     * Get recommendations for homepage (personalized if logged in, trending
     * otherwise)
     */
    public List<Product> getHomepageRecommendations(Long userId, int limit) {
        if (userId != null) {
            return getRecommendations(userId, limit);
        } else {
            return productDAO.findTrending(limit);
        }
    }

    /**
     * Get "frequently bought together" recommendations
     */
    public List<Product> getFrequentlyBoughtTogether(Long productId, int limit) {
        return itemBasedCFService.getSimilarProducts(productId, limit);
    }

    /**
     * Get "customers also viewed" recommendations
     */
    public List<Product> getCustomersAlsoViewed(Long productId, int limit) {
        // Get users who viewed this product
        List<Long> userIds = userInteractionDAO.findUserIdsByProductId(productId);

        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Find what else they viewed
        Map<Long, Integer> productViewCounts = new HashMap<>();

        for (Long userId : userIds.subList(0, Math.min(50, userIds.size()))) {
            List<UserInteraction> interactions = userInteractionDAO.findByUserId(userId);
            for (UserInteraction interaction : interactions) {
                if (!interaction.getProductId().equals(productId)) {
                    productViewCounts.put(interaction.getProductId(),
                            productViewCounts.getOrDefault(interaction.getProductId(), 0) + 1);
                }
            }
        }

        // Sort by view count
        List<Long> topProducts = productViewCounts.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return productDAO.findByIds(topProducts);
    }

    /**
     * Record user interaction for future recommendations
     */
    public void recordInteraction(Long userId, Long productId,
            UserInteraction.InteractionType type,
            BigDecimal value) {
        UserInteraction interaction = new UserInteraction(userId, productId, type);
        interaction.setInteractionValue(value);
        interaction.setSessionId(UUID.randomUUID().toString());

        userInteractionDAO.save(interaction);

        // Update product statistics
        if (type == UserInteraction.InteractionType.VIEW) {
            productDAO.incrementViewCount(productId);
        } else if (type == UserInteraction.InteractionType.PURCHASE) {
            productDAO.incrementPurchaseCount(productId);
        }

        // Invalidate cache for this user
        recommendationDAO.deleteByUserId(userId);

        logger.info("Recorded {} interaction for user {} on product {}", type, userId, productId);
    }

    /**
     * Clean up expired recommendations cache
     */
    public void cleanupExpiredCache() {
        int deleted = recommendationDAO.deleteExpired();
        logger.info("Cleaned up {} expired recommendations", deleted);
    }

    /**
     * Cache hybrid recommendations
     */
    private void cacheRecommendations(Long userId, Map<Long, Double> scores, List<Long> topProductIds) {
        List<Recommendation> recommendations = new ArrayList<>();

        // Normalize scores
        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        for (Long productId : topProductIds) {
            Recommendation rec = new Recommendation(userId, productId,
                    Recommendation.RecommendationType.HYBRID);

            double normalizedScore = scores.get(productId) / maxScore;
            rec.setConfidenceScore(new BigDecimal(normalizedScore));
            rec.setExpiresAt(LocalDateTime.now().plusHours(24));
            recommendations.add(rec);
        }

        recommendationDAO.batchSave(recommendations);
    }

    /**
     * Get personalized search results
     * Re-rank search results based on user preferences
     */
    public List<Product> personalizeSearchResults(Long userId, String keyword, int limit) {
        // Get base search results
        List<Product> searchResults = productDAO.search(keyword, limit * 2);

        if (userId == null || searchResults.isEmpty()) {
            return searchResults.subList(0, Math.min(limit, searchResults.size()));
        }

        // Get user's interaction history
        List<UserInteraction> userHistory = userInteractionDAO.findByUserId(userId);
        Set<Long> userProducts = userHistory.stream()
                .map(UserInteraction::getProductId)
                .collect(Collectors.toSet());

        // Re-rank based on similarity to user's history
        Map<Long, Double> reRankedScores = new HashMap<>();

        for (Product product : searchResults) {
            double score = 1.0; // Base relevance score

            // Boost if similar to products user liked
            if (userProducts.contains(product.getProductId())) {
                score += 0.5; // User already knows this product
            }

            reRankedScores.put(product.getProductId(), score);
        }

        // Sort by re-ranked scores
        return searchResults.stream()
                .sorted((p1, p2) -> Double.compare(
                        reRankedScores.getOrDefault(p2.getProductId(), 0.0),
                        reRankedScores.getOrDefault(p1.getProductId(), 0.0)))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
