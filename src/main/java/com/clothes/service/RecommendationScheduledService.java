package com.clothes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service for periodic recommendation system maintenance
 * Runs similarity computations and cache cleanup automatically
 */
@Service
public class RecommendationScheduledService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationScheduledService.class);

    private final UserBasedCFService userBasedCFService;
    private final ItemBasedCFService itemBasedCFService;
    private final HybridRecommendationService hybridRecommendationService;

    public RecommendationScheduledService(UserBasedCFService userBasedCFService,
            ItemBasedCFService itemBasedCFService,
            HybridRecommendationService hybridRecommendationService) {
        this.userBasedCFService = userBasedCFService;
        this.itemBasedCFService = itemBasedCFService;
        this.hybridRecommendationService = hybridRecommendationService;
    }

    /**
     * Compute product similarities daily at 2 AM
     * Cron: second, minute, hour, day, month, weekday
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void computeProductSimilaritiesDaily() {
        logger.info("Starting scheduled product similarity computation");
        try {
            long startTime = System.currentTimeMillis();
            itemBasedCFService.computeProductSimilarities();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Product similarity computation completed in {} ms", duration);
        } catch (Exception e) {
            logger.error("Error in scheduled product similarity computation", e);
        }
    }

    /**
     * Compute user similarities daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void computeUserSimilaritiesDaily() {
        logger.info("Starting scheduled user similarity computation");
        try {
            long startTime = System.currentTimeMillis();
            userBasedCFService.computeUserSimilarities();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("User similarity computation completed in {} ms", duration);
        } catch (Exception e) {
            logger.error("Error in scheduled user similarity computation", e);
        }
    }

    /**
     * Cleanup expired cache every 6 hours
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 hours in milliseconds
    public void cleanupExpiredCache() {
        logger.info("Starting scheduled cache cleanup");
        try {
            hybridRecommendationService.cleanupExpiredCache();
            logger.info("Cache cleanup completed");
        } catch (Exception e) {
            logger.error("Error in scheduled cache cleanup", e);
        }
    }

    /**
     * Optional: Compute product similarities by co-occurrence weekly
     * Runs every Sunday at 4 AM
     */
    @Scheduled(cron = "0 0 4 * * SUN")
    public void computeCoOccurrenceSimilaritiesWeekly() {
        logger.info("Starting scheduled co-occurrence similarity computation");
        try {
            long startTime = System.currentTimeMillis();
            itemBasedCFService.computeProductSimilaritiesByCoOccurrence();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Co-occurrence similarity computation completed in {} ms", duration);
        } catch (Exception e) {
            logger.error("Error in scheduled co-occurrence similarity computation", e);
        }
    }
}
