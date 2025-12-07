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

    private final Map<String, UserRateLimit> rpmLimits = new ConcurrentHashMap<>();
    private final Map<String, UserRateLimit> rphLimits = new ConcurrentHashMap<>();

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
        int rphLimit = settingService.getRateLimitRph(tenantId);

        // RPM Check (1 minute)
        UserRateLimit limitMin = rpmLimits.computeIfAbsent(userId, k -> new UserRateLimit());
        limitMin.cleanUp(60);
        if (limitMin.getCount() >= rpmLimit) {
            throw new RuntimeException("Rate limit exceeded (RPM). Please try again later.");
        }

        // RPH Check (1 hour = 3600 seconds)
        UserRateLimit limitHour = rphLimits.computeIfAbsent(userId, k -> new UserRateLimit());
        limitHour.cleanUp(3600);
        if (limitHour.getCount() >= rphLimit) {
            throw new RuntimeException("Rate limit exceeded (RPH). You have reached your hourly limit.");
        }

        // Add request to both counters
        limitMin.addRequest();
        limitHour.addRequest();
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
