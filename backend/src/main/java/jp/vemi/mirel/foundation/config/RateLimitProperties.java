/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * レート制限設定プロパティ.
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {
    
    private OtpRateLimit otp = new OtpRateLimit();
    private RedisSettings redis = new RedisSettings();
    
    @Getter
    @Setter
    public static class OtpRateLimit {
        /**
         * OTP要求: N回/分
         */
        private int requestPerMinute = 3;
        
        /**
         * OTP検証: N回/分
         */
        private int verifyPerMinute = 5;
    }
    
    @Getter
    @Setter
    public static class RedisSettings {
        /**
         * Redis障害時にインメモリフォールバック使用
         */
        private boolean fallbackToMemory = true;
    }
}
