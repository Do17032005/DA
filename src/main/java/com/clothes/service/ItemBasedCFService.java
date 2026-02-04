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
 * Service for Item-Based Collaborative Filtering
 * Recommends products based on item similarity (products bought together)
 * Generally more scalable and stable than User-Based CF
 */
@Service
public class ItemBasedCFService {

    private static final Logger logger = LoggerFactory.getLogger(ItemBasedCFService.class);

    private final ProductSimilarityDAO productSimilarityDAO;
    private final UserInteractionDAO userInteractionDAO;
    private final UserRatingDAO userRatingDAO;
    private final ProductDAO productDAO;
    private final RecommendationDAO recommendationDAO;

    // Configuration parameters
    private static final int TOP_K_SIMILAR_ITEMS = 20;
    private static final int RECOMMENDATION_COUNT = 10;
    private static final double MIN_SIMILARITY_THRESHOLD = 0.1;

    public ItemBasedCFService(ProductSimilarityDAO productSimilarityDAO,
            UserInteractionDAO userInteractionDAO,
            UserRatingDAO userRatingDAO,
            ProductDAO productDAO,
            RecommendationDAO recommendationDAO) {
        this.productSimilarityDAO = productSimilarityDAO;
        this.userInteractionDAO = userInteractionDAO;
        this.userRatingDAO = userRatingDAO;
        this.productDAO = productDAO;
        this.recommendationDAO = recommendationDAO;
    }

    /**
     * Get personalized recommendations for a user using Item-Based CF
     */
    public List<Product> getRecommendations(Long userId, int limit) {
        logger.info("Generating Item-Based CF recommendations for user: {}", userId);

        // 1. Check cache first
        List<Recommendation> cachedRecs = recommendationDAO.findByUserIdAndType(
                userId, Recommendation.RecommendationType.ITEM_BASED_CF, limit);

        if (!cachedRecs.isEmpty()) {
            logger.info("Returning {} cached recommendations", cachedRecs.size());
            List<Long> productIds = cachedRecs.stream()
                    .map(Recommendation::getRecommendedProductId)
                    .collect(Collectors.toList());
            return productDAO.findByIds(productIds);
        }

        // 2. Get user's interaction history
        List<UserInteraction> userInteractions = userInteractionDAO.findByUserId(userId);
        if (userInteractions.isEmpty()) {
            logger.warn("No interaction history for user: {}", userId);
            return productDAO.findTrending(limit);
        }

        // Get products user has already seen
        Set<Long> userProducts = userInteractions.stream()
                .map(UserInteraction::getProductId)
                .collect(Collectors.toSet());

        // 3. Build user's preference profile (product -> score)
        Map<Long, Double> userPreferences = new HashMap<>();
        for (UserInteraction interaction : userInteractions) {
            userPreferences.put(interaction.getProductId(), interaction.getWeightedScore());
        }

        // 4. Find similar items for each item user interacted with
        Map<Long, Double> candidateScores = new HashMap<>();

        for (Map.Entry<Long, Double> entry : userPreferences.entrySet()) {
            Long productId = entry.getKey();
            Double userScore = entry.getValue();

            // Get similar products
            List<ProductSimilarityDAO.SimilarProduct> similarProducts = productSimilarityDAO
                    .getSimilarProductsForProduct(productId, TOP_K_SIMILAR_ITEMS);

            for (ProductSimilarityDAO.SimilarProduct similar : similarProducts) {
                // Skip if user already saw this product
                if (userProducts.contains(similar.productId)) {
                    continue;
                }

                if (similar.similarityScore < MIN_SIMILARITY_THRESHOLD) {
                    continue;
                }

                // Score = user's preference * item similarity
                double score = userScore * similar.similarityScore;
                candidateScores.put(similar.productId,
                        candidateScores.getOrDefault(similar.productId, 0.0) + score);
            }
        }

        // 5. Get top N products
        List<Long> topProductIds = CollaborativeFilteringUtil.getTopN(candidateScores, limit);

        if (topProductIds.isEmpty()) {
            logger.warn("No recommendations generated for user: {}", userId);
            return productDAO.findTrending(limit);
        }

        // 6. Cache recommendations
        cacheRecommendations(userId, candidateScores, topProductIds);

        // 7. Return products
        logger.info("Generated {} Item-Based CF recommendations for user: {}",
                topProductIds.size(), userId);
        return productDAO.findByIds(topProductIds);
    }

    /**
     * Get similar products to a given product
     * Useful for "You may also like" or "Customers also bought"
     */
    public List<Product> getSimilarProducts(Long productId, int limit) {
        logger.info("Finding similar products for product: {}", productId);

        List<ProductSimilarityDAO.SimilarProduct> similarProducts = productSimilarityDAO
                .getSimilarProductsForProduct(productId, limit);

        List<Long> productIds = similarProducts.stream()
                .map(sp -> sp.productId)
                .collect(Collectors.toList());

        return productDAO.findByIds(productIds);
    }

    /**
     * Compute similarity between all products (batch process)
     * Should be run periodically (e.g., daily)
     */
    public void computeProductSimilarities() {
        logger.info("Starting Product Similarity computation");

        // Get all user-product interaction pairs
        List<UserInteractionDAO.InteractionScore> allInteractions = userInteractionDAO.getInteractionMatrix();

        // Build product vectors (product -> users who interacted)
        Map<Long, Map<Long, Double>> productVectors = new HashMap<>();
        for (UserInteractionDAO.InteractionScore interaction : allInteractions) {
            productVectors.computeIfAbsent(interaction.productId, k -> new HashMap<>())
                    .put(interaction.userId, interaction.score);
        }

        List<Long> productIds = new ArrayList<>(productVectors.keySet());
        List<ProductSimilarity> similarities = new ArrayList<>();

        // Calculate pairwise similarities
        for (int i = 0; i < productIds.size(); i++) {
            Long productId1 = productIds.get(i);
            Map<Long, Double> vector1 = productVectors.get(productId1);

            for (int j = i + 1; j < productIds.size(); j++) {
                Long productId2 = productIds.get(j);
                Map<Long, Double> vector2 = productVectors.get(productId2);

                // Calculate Cosine similarity
                double similarity = CollaborativeFilteringUtil.cosineSimilarity(vector1, vector2);

                // Only store if similarity is meaningful
                if (similarity > MIN_SIMILARITY_THRESHOLD) {
                    ProductSimilarity prodSim = new ProductSimilarity(productId1, productId2,
                            new BigDecimal(similarity));
                    prodSim.setSimilarityType(ProductSimilarity.SimilarityType.COSINE);
                    similarities.add(prodSim);
                }
            }

            // Log progress
            if ((i + 1) % 100 == 0) {
                logger.info("Processed {} products", i + 1);
            }
        }

        // Batch save
        logger.info("Saving {} product similarity records", similarities.size());
        productSimilarityDAO.batchSave(similarities);
        logger.info("Product Similarity computation completed");
    }

    /**
     * Compute product similarities based on co-occurrence
     * Faster method using "frequently bought together" data
     */
    public void computeProductSimilaritiesByCoOccurrence() {
        logger.info("Computing Product Similarities using Co-Occurrence");

        List<Product> allProducts = productDAO.findAllActive();
        List<ProductSimilarity> similarities = new ArrayList<>();

        for (Product product : allProducts) {
            // Find products frequently bought with this product
            List<UserInteractionDAO.CoOccurrence> coOccurrences = userInteractionDAO
                    .findProductCoOccurrences(product.getProductId(), 50);

            for (UserInteractionDAO.CoOccurrence coOcc : coOccurrences) {
                // Use co-occurrence count as similarity (normalized)
                double similarity = Math.min(1.0, coOcc.count / 10.0); // Normalize

                if (similarity > MIN_SIMILARITY_THRESHOLD) {
                    ProductSimilarity prodSim = new ProductSimilarity(
                            product.getProductId(), coOcc.productId, new BigDecimal(similarity));
                    prodSim.setSimilarityType(ProductSimilarity.SimilarityType.JACCARD);
                    similarities.add(prodSim);
                }
            }
        }

        logger.info("Saving {} co-occurrence based similarities", similarities.size());
        productSimilarityDAO.batchSave(similarities);
        logger.info("Co-Occurrence computation completed");
    }

    /**
     * Cache recommendations for faster serving
     */
    private void cacheRecommendations(Long userId, Map<Long, Double> productScores, List<Long> topProductIds) {
        List<Recommendation> recommendations = new ArrayList<>();
        Map<Long, Double> normalizedScores = CollaborativeFilteringUtil.normalizeScores(productScores);

        for (Long productId : topProductIds) {
            Recommendation rec = new Recommendation(userId, productId,
                    Recommendation.RecommendationType.ITEM_BASED_CF);
            rec.setConfidenceScore(new BigDecimal(normalizedScores.getOrDefault(productId, 0.0)));
            rec.setExpiresAt(java.time.LocalDateTime.now().plusHours(24));
            recommendations.add(rec);
        }

        recommendationDAO.batchSave(recommendations);
    }

    /**
     * Get explanation for why a product was recommended
     */
    public String getRecommendationExplanation(Long userId, Long recommendedProductId) {
        // Find which product in user's history is most similar to the recommended one
        List<UserInteraction> userInteractions = userInteractionDAO.findByUserId(userId);

        double maxSimilarity = 0.0;
        Long mostSimilarProductId = null;

        for (UserInteraction interaction : userInteractions) {
            Optional<ProductSimilarity> similarity = productSimilarityDAO.findByProductIds(interaction.getProductId(),
                    recommendedProductId);

            if (similarity.isPresent() &&
                    similarity.get().getSimilarityScore().doubleValue() > maxSimilarity) {
                maxSimilarity = similarity.get().getSimilarityScore().doubleValue();
                mostSimilarProductId = interaction.getProductId();
            }
        }

        if (mostSimilarProductId != null) {
            Optional<Product> product = productDAO.findById(mostSimilarProductId);
            if (product.isPresent()) {
                return "Similar to " + product.get().getProductName() + " you viewed before";
            }
        }

        return "Customers who bought similar items also bought this";
    }
}
