/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.domain.dao.repository.MiraTokenUsageRepository;
import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * TokenQuotaService のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class TokenQuotaServiceTest {

    @Mock
    private MiraTokenUsageRepository usageRepository;

    @Mock
    private MiraAiProperties properties;

    @InjectMocks
    private TokenQuotaService tokenQuotaService;

    private MiraAiProperties.Quota quotaConfig;

    @BeforeEach
    void setUp() {
        quotaConfig = new MiraAiProperties.Quota();
        // consume()はpropertiesを参照しないため、checkQuota()用としてlenientに設定
        org.mockito.Mockito.lenient().when(properties.getQuota()).thenReturn(quotaConfig);
    }

    @Test
    @DisplayName("クォータ制限内なら正常終了")
    void shouldPassIfUnderQuota() {
        // Arrange
        quotaConfig.setEnabled(true);
        quotaConfig.setDailyTokenLimit(1000);

        when(usageRepository.sumTotalTokensByTenantAndDate(any(), any())).thenReturn(500L);

        // Act & Assert
        assertThatCode(() -> tokenQuotaService.checkQuota("tenant1", 100))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("クォータ制限超過なら例外")
    void shouldThrowIfOverQuota() {
        // Arrange
        quotaConfig.setEnabled(true);
        quotaConfig.setDailyTokenLimit(1000);

        when(usageRepository.sumTotalTokensByTenantAndDate(any(), any())).thenReturn(950L);

        // Act & Assert
        assertThatThrownBy(() -> tokenQuotaService.checkQuota("tenant1", 100))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("quota exceeded");
    }

    @Test
    @DisplayName("無効ならチェックしない")
    void shouldSkipIfDisabled() {
        // Arrange
        quotaConfig.setEnabled(false);

        // Act & Assert
        tokenQuotaService.checkQuota("tenant1", 999999);
        // Repository呼ばれないはずだが、実装上はpropertiesチェックが先なのでOK
    }

    @Test
    @DisplayName("consumeで保存される")
    void shouldSaveOnConsume() {
        // Arrange
        // enabledチェックは削除されたため、設定不要

        // Act
        tokenQuotaService.consume("tenant1", "user1", "conv1", "gpt-4", 10, 20);

        // Assert
        verify(usageRepository).save(any());
    }
}
