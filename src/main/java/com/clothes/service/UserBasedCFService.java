package com.clothes.service;

import com.clothes.dao.*;
import com.clothes.model.*;
import com.clothes.util.CollaborativeFilteringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for User-Based Collaborative Filtering
 * Recommends products based on similar users' preferences
 */
@Service
public class UserBasedCFService {

    private static final Logger logger = LoggerFactory.getLogger(UserBasedCFService.class);

    private final UserRatingDAO userRatingDAO;
    private final UserSimilarityDAO userSimilarityDAO;
    private final UserInteractionDAO userInteractionDAO;
    private final ProductDAO productDAO;
    private final RecommendationDAO recommendationDAO;

    // Configuration parameters
    private static final int TOP_K_NEIGHBORS = 20; // Number of similar users to consider
    private static final int RECOMMENDATION_COUNT = 10;
    private static final double MIN_SIMILARITY_THRESHOLD = 0.1;

    public UserBasedCFService(UserRatingDAO userRatingDAO,
            UserSimilarityDAO userSimilarityDAO,
            UserInteractionDAO userInteractionDAO,
            ProductDAO productDAO,
            RecommendationDAO recommendationDAO) {
        this.userRatingDAO = userRatingDAO;
        this.userSimilarityDAO = userSimilarityDAO;
        this.userInteractionDAO = userInteractionDAO;
        this.productDAO = productDAO;
        this.recommendationDAO = recommendationDAO;
    }

    /**
     * Get personalized recommendations for a user using User-Based CF
     */
    public List<Product> getRecommendations(Long userId, int limit) {
        logger.info("Generating User-Based CF recommendations for user: {}", userId);

        // 1. Check cache first
        List<Recommendation> cachedRecs = recommendationDAO.findByUserIdAndType(
                userId, Recommendation.RecommendationType.USER_BASED_CF, limit);

        if (!cachedRecs.isEmpty()) {
            logger.info("Returning {} cached recommendations", cachedRecs.size());
            List<Long> productIds = cachedRecs.stream()
                    .map(Recommendation::getRecommendedProductId)
                    .collect(Collectors.toList());
            return productDAO.findByIds(productIds);
        }

        // 2. Find similar users
        List<UserSimilarityDAO.SimilarUser> similarUsers = userSimilarityDAO.getSimilarUsersForUser(userId,
                TOP_K_NEIGHBORS);

        if (similarUsers.isEmpty()) {
            logger.warn("No similar users found for user: {}", userId);
            // Fallback to trending products
            return productDAO.findTrending(limit);
        }

        // 3. Get products the user has already interacted with (to filter out)
        Set<Long> userProducts = new HashSet<>(userInteractionDAO.findProductIdsByUserId(userId));

        // 4. Calculate recommendation scores
        Map<Long, Double> productScores = new HashMap<>();

        for (UserSimilarityDAO.SimilarUser similarUser : similarUsers) {
            if (similarUser.similarityScore < MIN_SIMILARITY_THRESHOLD) {
                continue;
            }

            // Get products this similar user liked
            List<UserInteraction> similarUserInteractions = userInteractionDAO.findByUserId(similarUser.userId);

            for (UserInteraction interaction : similarUserInteractions) {
                Long productId = interaction.getProductId();

                // Skip if user already saw this product
                if (userProducts.contains(productId)) {
                    continue;
                }

                // Weight by similarity and interaction type
                double score = similarUser.similarityScore * interaction.getWeightedScore();
                productScores.put(productId,
                        productScores.getOrDefault(productId, 0.0) + score);
            }
        }

        // 5. Get top N products
        List<Long> topProductIds = CollaborativeFilteringUtil.getTopN(productScores, limit);

        if (topProductIds.isEmpty()) {
            logger.warn("No recommendations generated for user: {}", userId);
            return productDAO.findTrending(limit);
        }

        // 6. Cache recommendations
        cacheRecommendations(userId, productScores, topProductIds);

        // 7. Return products
        logger.info("Generated {} User-Based CF recommendations for user: {}",
                topProductIds.size(), userId);
        return productDAO.findByIds(topProductIds);
    }

    /**
     * Compute similarity between all users (batch process)
     * Should be run periodically (e.g., daily)
     */
    public void computeUserSimilarities() {
        logger.info("Starting User Similarity computation");

        // Get all user-product rating pairs
        List<UserRatingDAO.RatingPair> allRatings = userRatingDAO.getAllRatingPairs();

        // Build user rating vectors
        Map<Long, Map<Long, Double>> userVectors = new HashMap<>();
        for (UserRatingDAO.RatingPair pair : allRatings) {
            userVectors.computeIfAbsent(pair.userId, k -> new HashMap<>())
                    .put(pair.productId, pair.rating);
        }

        List<Long> userIds = new ArrayList<>(userVectors.keySet());
        List<UserSimilarity> similarities = new ArrayList<>();

        // Calculate pairwise similarities
        for (int i = 0; i < userIds.size(); i++) {
            Long userId1 = userIds.get(i);
            Map<Long, Double> vector1 = userVectors.get(userId1);

            for (int j = i + 1; j < userIds.size(); j++) {
                Long userId2 = userIds.get(j);
                Map<Long, Double> vector2 = userVectors.get(userId2);

                // Calculate Pearson correlation
                double similarity = CollaborativeFilteringUtil.pearsonCorrelation(vector1, vector2);

                // Only store if similarity is meaningful
                if (Math.abs(similarity) > MIN_SIMILARITY_THRESHOLD) {
                    UserSimilarity userSim = new UserSimilarity(userId1, userId2,
                            new BigDecimal(similarity));
                    userSim.setSimilarityType(UserSimilarity.SimilarityType.PEARSON);
                    similarities.add(userSim);
                }
            }

            // Log progress
            if ((i + 1) % 100 == 0) {
                logger.info("Processed {} users", i + 1);
            }
        }

        // Batch save
        logger.info("Saving {} user similarity records", similarities.size());
        userSimilarityDAO.batchSave(similarities);
        logger.info("User Similarity computation completed");
    }

    /**
     * Cache recommendations for faster serving
     */
    private void cacheRecommendations(Long userId, Map<Long, Double> productScores, List<Long> topProductIds) {
        List<Recommendation> recommendations = new ArrayList<>();
        Map<Long, Double> normalizedScores = CollaborativeFilteringUtil.normalizeScores(productScores);

        for (Long productId : topProductIds) {
            Recommendation rec = new Recommendation(userId, productId,
                    Recommendation.RecommendationType.USER_BASED_CF);
            rec.setConfidenceScore(new BigDecimal(normalizedScores.getOrDefault(productId, 0.0)));
            rec.setExpiresAt(java.time.LocalDateTime.now().plusHours(24)); // Cache for 24 hours
            recommendations.add(rec);
        }

        recommendationDAO.batchSave(recommendations);
    }

    /**
     * Get explanation for why a product was recommended
     */
    public String getRecommendationExplanation(Long userId, Long productId) {
        List<UserSimilarityDAO.SimilarUser> similarUsers = userSimilarityDAO.getSimilarUsersForUser(userId, 5);

        int count = 0;
        for (UserSimilarityDAO.SimilarUser similarUser : similarUsers) {
            List<UserInteraction> interactions = userInteractionDAO.findByUserIdAndProductId(similarUser.userId,
                    productId);
            if (!interactions.isEmpty()) {
                count++;
            }
        }

        if (count > 0) {
            return String.format("%d similar users liked this product", count);
        }
        return "Recommended based on your preferences";
    }
}
