/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * MiraRateLimitService のユニットテスト.
 * 
 * インメモリ実装に対応したテスト。
 */
@ExtendWith(MockitoExtension.class)
class MiraRateLimitServiceTest {

    @Mock
    private MiraAiProperties properties;

    @Mock
    private MiraSettingService settingService;

    @Mock
    private MiraAiProperties.RateLimit rateLimitConfig;

    @InjectMocks
    private MiraRateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getRateLimit()).thenReturn(rateLimitConfig);
    }

    @Test
    @DisplayName("無効時はスキップ")
    void shouldSkipWhenDisabled() {
        // Arrange
        when(rateLimitConfig.isEnabled()).thenReturn(false);

        // Act & Assert
        assertThatCode(() -> rateLimitService.checkRateLimit("user1", "tenant1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("制限内ならOK")
    void shouldPassIfUnderLimit() {
        // Arrange
        when(rateLimitConfig.isEnabled()).thenReturn(true);
        when(settingService.getRateLimitRpm("tenant1")).thenReturn(10);
        when(settingService.getRateLimitRph("tenant1")).thenReturn(100);

        // Act & Assert
        assertThatCode(() -> rateLimitService.checkRateLimit("user1", "tenant1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("制限超過なら例外")
    void shouldThrowIfOverLimit() {
        // Arrange
        when(rateLimitConfig.isEnabled()).thenReturn(true);
        when(settingService.getRateLimitRpm("tenant1")).thenReturn(2);
        when(settingService.getRateLimitRph("tenant1")).thenReturn(100);

        // 制限回数呼び出す
        rateLimitService.checkRateLimit("user2", "tenant1");
        rateLimitService.checkRateLimit("user2", "tenant1");

        // Act & Assert
        assertThatThrownBy(() -> rateLimitService.checkRateLimit("user2", "tenant1"))
                .isInstanceOf(jp.vemi.framework.exeption.MirelQuotaExceededException.class)
                .hasMessageContaining("Rate limit exceeded");
    }
}
