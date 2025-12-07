/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira レート制限サービス.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MiraRateLimitService {

    private final MiraAiProperties properties;
    private final MiraSettingService settingService;

    // Simple in-memory rate limiter for demonstration.
    // In production, use Redis or similar distributed cache.
    private final Map<String, UserRateLimit> userLimits = new ConcurrentHashMap<>();

    /**
     * レート制限チェック.
     *
     * @param userId
     *            ユーザーID
     * @param tenantId
     *            テナントID
     * @throws RuntimeException
     *             制限超過時
     */
    public void checkRateLimit(String userId, String tenantId) {
        if (!properties.getRateLimit().isEnabled()) {
            return;
        }

        int rpmLimit = settingService.getRateLimitRpm(tenantId);
        // int rphLimit = settingService.getRateLimitRph(tenantId); // TODO: Implement
        // RPH logic

        UserRateLimit limit = userLimits.computeIfAbsent(userId, k -> new UserRateLimit());

        // RPM Check
        limit.cleanUp(60); // 1 minute window
        if (limit.getCount() >= rpmLimit) {
            throw new RuntimeException("Rate limit exceeded (RPM). Please try again later.");
        }

        // RPH (Requests Per Hour) check could be implemented here similarly
        // using a separate list or extending UserRateLimit to handle multiple windows.
        // For now, we will stick to RPM as the primary short-term protection.
        // TODO: Implement RPH check

        limit.addRequest();
    }

    // Helper class for in-memory rate limiting
    private static class UserRateLimit {
        private final List<Long> timestamps = new CopyOnWriteArrayList<>();

        public void addRequest() {
            timestamps.add(System.currentTimeMillis());
        }

        public int getCount() {
            return timestamps.size();
        }

        public void cleanUp(int seconds) {
            long windowStart = System.currentTimeMillis() - (seconds * 1000L);
            timestamps.removeIf(ts -> ts < windowStart);
        }
    }
}
