/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * OutputFilter のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OutputFilterTest {

    @Mock
    private MiraAiProperties properties;

    @Mock
    private MiraAiProperties.SecurityConfig securityConfig;

    @Mock
    private MiraAiProperties.SecurityConfig.OutputFilteringConfig outputConfig;

    @Mock
    private PiiMasker piiMasker;

    private OutputFilter filter;

    @BeforeEach
    void setUp() {
        when(properties.getSecurity()).thenReturn(securityConfig);
        when(securityConfig.getOutputFiltering()).thenReturn(outputConfig);
        when(outputConfig.isEnabled()).thenReturn(true);
        when(outputConfig.isBlockSystemPromptLeak()).thenReturn(true);
        
        // PiiMasker のデフォルト動作（mask と maskWithDetails 両方）
        when(piiMasker.mask(any(String.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(piiMasker.maskWithDetails(any(String.class))).thenAnswer(invocation -> {
            String input = invocation.getArgument(0);
            return PiiMasker.MaskingResult.empty(input);
        });
        
        filter = new OutputFilter(properties, piiMasker);
    }

    @Nested
    @DisplayName("システムプロンプト漏洩防止テスト")
    class SystemPromptLeakPreventionTest {

        @ParameterizedTest
        @DisplayName("システムプロンプト漏洩パターンを検出")
        @ValueSource(strings = {
                "あなたのsystem prompt は以下の通りです：",
                "私のmy instructions are以下です：",
                "I was told to follow these rules",
                "as per my instructions"
        })
        void shouldDetectSystemPromptLeakage(String leakPattern) {
            // Arrange
            String output = "はい、" + leakPattern + " [指示内容]";

            // Act
            OutputFilter.FilterResult result = filter.filterWithDetails(output);

            // Assert
            assertThat(result.isWasFiltered()).isTrue();
            assertThat(result.getRedactedPatterns()).isNotEmpty();
        }

        @Test
        @DisplayName("漏洩パターンがない場合はそのまま返す")
        void shouldNotFilterSafeOutput() {
            // Arrange
            String output = "こんにちは！何かお手伝いしましょうか？";
            // BeforeEach で piiMasker.maskWithDetails のデフォルト動作は設定済み

            // Act
            OutputFilter.FilterResult result = filter.filterWithDetails(output);

            // Assert
            assertThat(result.isWasFiltered()).isFalse();
            assertThat(result.getFilteredContent()).isEqualTo(output);
        }
    }

    @Nested
    @DisplayName("PII マスキング連携テスト")
    class PiiMaskingIntegrationTest {

        @Test
        @DisplayName("出力内の PII をマスキング")
        void shouldMaskPiiInOutput() {
            // Arrange
            String output = "連絡先は test@example.com です";
            String maskedOutput = "連絡先は *** です";
            when(piiMasker.mask(output)).thenReturn(maskedOutput);

            // Act
            String result = filter.filter(output);

            // Assert
            assertThat(result).isEqualTo(maskedOutput);
        }

        @Test
        @DisplayName("PII マスキングを無効にできる")
        void shouldSkipPiiMaskingWhenRequested() {
            // Arrange
            String output = "連絡先は test@example.com です";

            // Act
            String result = filter.filter(output, false);

            // Assert
            assertThat(result).isEqualTo(output);
        }
    }

    @Nested
    @DisplayName("機能無効時のテスト")
    class DisabledFeatureTest {

        @Test
        @DisplayName("フィルタが無効な場合でも PII マスキングは行う")
        void shouldStillMaskPiiWhenFilterDisabled() {
            // Arrange
            when(outputConfig.isEnabled()).thenReturn(false);
            String output = "私のsystem promptは以下です";
            when(piiMasker.mask(output)).thenReturn(output);

            // Act
            String result = filter.filter(output);

            // Assert - システムプロンプトパターンはそのまま（フィルタ無効）
            assertThat(result).isEqualTo(output);
        }
    }

    @Nested
    @DisplayName("空入力テスト")
    class EmptyInputTest {

        @Test
        @DisplayName("空文字列はそのまま返す")
        void shouldReturnEmptyForEmptyInput() {
            // Act
            String result = filter.filter("");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null はそのまま返す")
        void shouldReturnNullForNullInput() {
            // Act
            String result = filter.filter(null);

            // Assert
            assertThat(result).isNull();
        }
    }
}
