package com.clothes.util;

import java.util.*;

/**
 * Utility class for Collaborative Filtering calculations
 * Implements various similarity metrics and recommendation algorithms
 */
public class CollaborativeFilteringUtil {

    /**
     * Calculate Cosine Similarity between two vectors
     * Used for both user-based and item-based CF
     * 
     * @param vector1 First vector (ratings or interactions)
     * @param vector2 Second vector
     * @return Cosine similarity score (0 to 1)
     */
    public static double cosineSimilarity(Map<Long, Double> vector1, Map<Long, Double> vector2) {
        if (vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }

        // Find common keys
        Set<Long> commonKeys = new HashSet<>(vector1.keySet());
        commonKeys.retainAll(vector2.keySet());

        if (commonKeys.isEmpty()) {
            return 0.0;
        }

        // Calculate dot product and magnitudes
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (Long key : commonKeys) {
            double value1 = vector1.get(key);
            double value2 = vector2.get(key);
            dotProduct += value1 * value2;
        }

        for (Double value : vector1.values()) {
            magnitude1 += value * value;
        }

        for (Double value : vector2.values()) {
            magnitude2 += value * value;
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * Calculate Pearson Correlation Coefficient
     * Better for user-based CF when dealing with different rating scales
     * 
     * @param ratings1 First user's ratings
     * @param ratings2 Second user's ratings
     * @return Pearson correlation (-1 to 1)
     */
    public static double pearsonCorrelation(Map<Long, Double> ratings1, Map<Long, Double> ratings2) {
        if (ratings1.isEmpty() || ratings2.isEmpty()) {
            return 0.0;
        }

        // Find common items
        Set<Long> commonItems = new HashSet<>(ratings1.keySet());
        commonItems.retainAll(ratings2.keySet());

        if (commonItems.size() < 2) {
            return 0.0; // Need at least 2 common items
        }

        // Calculate means
        double mean1 = 0.0;
        double mean2 = 0.0;
        for (Long item : commonItems) {
            mean1 += ratings1.get(item);
            mean2 += ratings2.get(item);
        }
        mean1 /= commonItems.size();
        mean2 /= commonItems.size();

        // Calculate covariance and standard deviations
        double covariance = 0.0;
        double variance1 = 0.0;
        double variance2 = 0.0;

        for (Long item : commonItems) {
            double diff1 = ratings1.get(item) - mean1;
            double diff2 = ratings2.get(item) - mean2;
            covariance += diff1 * diff2;
            variance1 += diff1 * diff1;
            variance2 += diff2 * diff2;
        }

        if (variance1 == 0.0 || variance2 == 0.0) {
            return 0.0;
        }

        return covariance / (Math.sqrt(variance1) * Math.sqrt(variance2));
    }

    /**
     * Calculate Jaccard Similarity
     * Good for binary data (purchased/not purchased)
     * 
     * @param set1 First set of items
     * @param set2 Second set of items
     * @return Jaccard similarity (0 to 1)
     */
    public static double jaccardSimilarity(Set<Long> set1, Set<Long> set2) {
        if (set1.isEmpty() && set2.isEmpty()) {
            return 1.0;
        }
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        Set<Long> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<Long> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Predict rating for a user-item pair using weighted average
     * Used in User-Based CF
     * 
     * @param userSimilarities Map of similar users and their similarity scores
     * @param userRatings      Map of similar users' ratings for the target item
     * @param topK             Number of most similar users to consider
     * @return Predicted rating
     */
    public static double predictRatingUserBased(
            Map<Long, Double> userSimilarities,
            Map<Long, Double> userRatings,
            int topK) {

        if (userSimilarities.isEmpty() || userRatings.isEmpty()) {
            return 0.0;
        }

        // Sort users by similarity and take top K
        List<Map.Entry<Long, Double>> sortedSimilarities = new ArrayList<>(userSimilarities.entrySet());
        sortedSimilarities.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        double weightedSum = 0.0;
        double similaritySum = 0.0;
        int count = 0;

        for (Map.Entry<Long, Double> entry : sortedSimilarities) {
            if (count >= topK)
                break;

            Long userId = entry.getKey();
            Double similarity = entry.getValue();

            if (userRatings.containsKey(userId) && similarity > 0) {
                weightedSum += similarity * userRatings.get(userId);
                similaritySum += Math.abs(similarity);
                count++;
            }
        }

        if (similaritySum == 0.0) {
            return 0.0;
        }

        return weightedSum / similaritySum;
    }

    /**
     * Predict rating for a user-item pair using Item-Based CF
     * 
     * @param itemSimilarities Map of similar items and their similarity scores
     * @param userItemRatings  User's ratings for similar items
     * @param topK             Number of most similar items to consider
     * @return Predicted rating
     */
    public static double predictRatingItemBased(
            Map<Long, Double> itemSimilarities,
            Map<Long, Double> userItemRatings,
            int topK) {

        if (itemSimilarities.isEmpty() || userItemRatings.isEmpty()) {
            return 0.0;
        }

        // Sort items by similarity and take top K
        List<Map.Entry<Long, Double>> sortedSimilarities = new ArrayList<>(itemSimilarities.entrySet());
        sortedSimilarities.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        double weightedSum = 0.0;
        double similaritySum = 0.0;
        int count = 0;

        for (Map.Entry<Long, Double> entry : sortedSimilarities) {
            if (count >= topK)
                break;

            Long itemId = entry.getKey();
            Double similarity = entry.getValue();

            if (userItemRatings.containsKey(itemId) && similarity > 0) {
                weightedSum += similarity * userItemRatings.get(itemId);
                similaritySum += Math.abs(similarity);
                count++;
            }
        }

        if (similaritySum == 0.0) {
            return 0.0;
        }

        return weightedSum / similaritySum;
    }

    /**
     * Normalize scores to 0-1 range
     * 
     * @param scores Map of items and their scores
     * @return Normalized scores
     */
    public static Map<Long, Double> normalizeScores(Map<Long, Double> scores) {
        if (scores.isEmpty()) {
            return new HashMap<>();
        }

        double min = Collections.min(scores.values());
        double max = Collections.max(scores.values());

        if (max == min) {
            Map<Long, Double> normalized = new HashMap<>();
            for (Long key : scores.keySet()) {
                normalized.put(key, 0.5);
            }
            return normalized;
        }

        Map<Long, Double> normalized = new HashMap<>();
        for (Map.Entry<Long, Double> entry : scores.entrySet()) {
            double normalizedScore = (entry.getValue() - min) / (max - min);
            normalized.put(entry.getKey(), normalizedScore);
        }

        return normalized;
    }

    /**
     * Get top N items from a scored map
     * 
     * @param scores Map of items and their scores
     * @param n      Number of top items to return
     * @return List of top N item IDs
     */
    public static List<Long> getTopN(Map<Long, Double> scores, int n) {
        List<Map.Entry<Long, Double>> sortedEntries = new ArrayList<>(scores.entrySet());
        sortedEntries.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        List<Long> topItems = new ArrayList<>();
        for (int i = 0; i < Math.min(n, sortedEntries.size()); i++) {
            topItems.add(sortedEntries.get(i).getKey());
        }

        return topItems;
    }

    /**
     * Calculate mean of a collection of values
     */
    public static double calculateMean(Collection<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * Calculate standard deviation
     */
    public static double calculateStdDev(Collection<Double> values) {
        if (values.size() < 2) {
            return 0.0;
        }

        double mean = calculateMean(values);
        double sumSquaredDiff = 0.0;

        for (Double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }

        return Math.sqrt(sumSquaredDiff / values.size());
    }

    /**
     * Filter out items that user has already interacted with
     * 
     * @param candidateItems All possible items to recommend
     * @param userItems      Items user has already seen/purchased
     * @return Filtered candidate items
     */
    public static Set<Long> filterSeenItems(Set<Long> candidateItems, Set<Long> userItems) {
        Set<Long> filtered = new HashSet<>(candidateItems);
        filtered.removeAll(userItems);
        return filtered;
    }
}
