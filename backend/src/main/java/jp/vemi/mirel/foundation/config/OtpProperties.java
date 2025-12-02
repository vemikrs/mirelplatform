/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OTP設定プロパティ.
 */
@Configuration
@ConfigurationProperties(prefix = "otp")
@Getter
@Setter
public class OtpProperties {
    
    /**
     * OTP機能有効フラグ
     */
    private boolean enabled = true;
    
    /**
     * OTPコード桁数
     */
    private int length = 6;
    
    /**
     * 有効期限 (分)
     */
    private int expirationMinutes = 5;
    
    /**
     * 最大検証試行回数
     */
    private int maxAttempts = 3;
    
    /**
     * 再送信クールダウン (秒)
     */
    private int resendCooldownSeconds = 60;
}
