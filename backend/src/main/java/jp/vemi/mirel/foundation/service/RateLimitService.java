/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service;

import jp.vemi.mirel.foundation.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * レート制限サービス.
 * Redis分散レート制限、障害時はインメモリフォールバック
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final RedisTemplate<String, Long> longRedisTemplate;
    private final RateLimitProperties rateLimitProperties;
    
    // インメモリフォールバック用
    private final ConcurrentHashMap<String, RateLimitCounter> memoryRateLimits = new ConcurrentHashMap<>();
    
    /**
     * レート制限チェック
     * 
     * @param key レート制限キー (例: "otp:request:user@example.com")
     * @param maxRequests 最大リクエスト数
     * @param windowSeconds ウィンドウ時間（秒）
     * @return 許可された場合true、制限超過の場合false
     */
    public boolean checkRateLimit(String key, int maxRequests, int windowSeconds) {
        try {
            return checkRateLimitRedis(key, maxRequests, windowSeconds);
        } catch (RedisConnectionFailureException e) {
            if (rateLimitProperties.getRedis().isFallbackToMemory()) {
                log.warn("Redis接続失敗、インメモリにフォールバック: {}", e.getMessage());
                return checkRateLimitMemory(key, maxRequests, windowSeconds);
            }
            log.error("Redis接続失敗、レート制限を許可: {}", e.getMessage());
            return true; // 障害時は許可（可用性重視）
        }
    }
    
    /**
     * Redis分散レート制限
     */
    private boolean checkRateLimitRedis(String key, int maxRequests, int windowSeconds) {
        Long count = longRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            count = 0L;
        }
        
        if (count == 1) {
            // 初回リクエスト時に有効期限を設定
            longRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        
        return count <= maxRequests;
    }
    
    /**
     * インメモリレート制限
     */
    private boolean checkRateLimitMemory(String key, int maxRequests, int windowSeconds) {
        long now = System.currentTimeMillis();
        RateLimitCounter counter = memoryRateLimits.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowSeconds * 1000L) {
                return new RateLimitCounter(now, 1);
            }
            v.count++;
            return v;
        });
        
        // 古いエントリを削除（メモリリーク防止）
        memoryRateLimits.entrySet().removeIf(entry -> 
            now - entry.getValue().windowStart > windowSeconds * 2000L
        );
        
        return counter.count <= maxRequests;
    }
    
    /**
     * クールダウン中かチェック
     * 
     * @param key クールダウンキー (例: "otp:cooldown:user@example.com")
     * @return クールダウン中の場合true
     */
    public boolean isInCooldown(String key) {
        try {
            Boolean exists = longRedisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis接続失敗、クールダウンチェック不可: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * クールダウンを設定
     * 
     * @param key クールダウンキー
     * @param seconds クールダウン時間（秒）
     */
    public void setCooldown(String key, int seconds) {
        try {
            longRedisTemplate.opsForValue().set(key, 1L, seconds, TimeUnit.SECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis接続失敗、クールダウン設定不可: {}", e.getMessage());
        }
    }
    
    /**
     * レート制限カウンターをクリア
     * 
     * @param key レート制限キー
     */
    public void clearRateLimit(String key) {
        try {
            longRedisTemplate.delete(key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis接続失敗、レート制限クリア不可: {}", e.getMessage());
        }
        memoryRateLimits.remove(key);
    }
    
    /**
     * インメモリレート制限カウンター
     */
    private static class RateLimitCounter {
        private final long windowStart;
        private int count;
        
        RateLimitCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
