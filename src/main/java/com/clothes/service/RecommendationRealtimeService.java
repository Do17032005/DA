package com.clothes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE subscribers and pushes recommendation refresh events to clients.
 */
@Service
public class RecommendationRealtimeService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationRealtimeService.class);
    private static final long SSE_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emittersByUser.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ex -> removeEmitter(userId, emitter));

        try {
            Map<String, Object> connectedPayload = new HashMap<>();
            connectedPayload.put("type", "CONNECTED");
            connectedPayload.put("timestamp", Instant.now().toString());
            emitter.send(SseEmitter.event().name("recommendation-refresh").data(connectedPayload));
        } catch (Exception ex) {
            removeEmitter(userId, emitter);
            logger.debug("Failed to send initial SSE event for user {}", userId, ex);
        }

        return emitter;
    }

    public void publishRecommendationRefresh(Long userId, List<Long> recentProductIds, Long orderId) {
        List<SseEmitter> emitters = emittersByUser.getOrDefault(userId, new CopyOnWriteArrayList<>());
        if (emitters.isEmpty()) {
            return;
        }

        List<Long> safeRecentProductIds = recentProductIds == null ? List.of() : recentProductIds;

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "RECOMMENDATION_REFRESH");
        payload.put("userId", userId);
        payload.put("orderId", orderId);
        payload.put("recentProductIds", safeRecentProductIds);
        payload.put("timestamp", Instant.now().toString());

        List<SseEmitter> staleEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("recommendation-refresh").data(payload));
            } catch (Exception ex) {
                staleEmitters.add(emitter);
            }
        }

        if (!staleEmitters.isEmpty()) {
            emitters.removeAll(staleEmitters);
            logger.debug("Removed {} stale recommendation SSE emitters for user {}", staleEmitters.size(), userId);
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }
}
