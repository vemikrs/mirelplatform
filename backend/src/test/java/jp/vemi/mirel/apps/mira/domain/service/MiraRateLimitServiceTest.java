/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong; // Import anyLong
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * MiraRateLimitService のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class MiraRateLimitServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MiraAiProperties properties;

    @InjectMocks
    private MiraRateLimitService rateLimitService;

    private MiraAiProperties.RateLimit rateLimitConfig;

    @BeforeEach
    void setUp() {
        rateLimitConfig = new MiraAiProperties.RateLimit();
        // lenient() to avoid strict stubbing errors if not called in disabled test
        org.mockito.Mockito.lenient().when(properties.getRateLimit()).thenReturn(rateLimitConfig);

        // Redis Mock setup for checkLimit
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("制限内ならOK")
    void shouldPassIfUnderLimit() {
        // Arrange
        rateLimitConfig.setEnabled(true);
        rateLimitConfig.setRequestsPerMinute(10);

        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act & Assert
        assertThatCode(() -> rateLimitService.checkRateLimit("tenant1", "user1"))
                .doesNotThrowAnyException();

        verify(redisTemplate).expire(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("制限超過なら例外")
    void shouldThrowIfOverLimit() {
        // Arrange
        rateLimitConfig.setEnabled(true);
        rateLimitConfig.setRequestsPerMinute(10);

        when(valueOperations.increment(anyString())).thenReturn(11L);

        // Act & Assert
        assertThatThrownBy(() -> rateLimitService.checkRateLimit("tenant1", "user1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rate limit exceeded");
    }
}
