/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira レート制限サービス.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraRateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final MiraAiProperties properties;

    private static final String KEY_PREFIX = "mira:ratelimit:";

    /**
     * レート制限チェック.
     *
     * @param tenantId
     *            テナントID
     * @param userId
     *            ユーザーID
     * @throws RuntimeException
     *             制限超過時
     */
    public void checkRateLimit(String tenantId, String userId) {
        if (!properties.getRateLimit().isEnabled()) {
            return;
        }

        // テナント単位の制限
        // String tenantKey = KEY_PREFIX + "tenant:" + tenantId;
        // checkLimit(tenantKey, properties.getRateLimit().getRequestsPerMinute());

        // 現状はグローバルRPM設定のみなので、ユーザー単位も考慮して実装（プロパティにはRPMがあるがユーザーごとはないため単純化）
        // ユーザーごとの簡易制限を入れることでDoS防止
        String userKey = KEY_PREFIX + "user:" + userId;
        int maxRpm = properties.getRateLimit().getRequestsPerMinute();

        checkLimit(userKey, maxRpm);
    }

    private void checkLimit(String key, int limit) {
        // 簡易実装: 現在の分(minute)をキーとするカウンタ
        long currentMinute = System.currentTimeMillis() / 60000;
        String timeKey = key + ":" + currentMinute;

        Long count = redisTemplate.opsForValue().increment(timeKey);
        if (count != null && count == 1) {
            redisTemplate.expire(timeKey, 2, TimeUnit.MINUTES);
        }

        if (count != null && count > limit) {
            log.warn("Rate limit exceeded for key: {}. Count: {}, Limit: {}", key, count, limit);
            throw new RuntimeException("Rate limit exceeded. Please try again later.");
        }
    }
}
