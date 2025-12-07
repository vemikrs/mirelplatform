/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * PiiMasker のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PiiMaskerTest {

    @Mock
    private MiraAiProperties properties;

    @Mock
    private MiraAiProperties.SecurityConfig securityConfig;

    @Mock
    private MiraAiProperties.SecurityConfig.PiiMaskingConfig piiConfig;

    private PiiMasker masker;

    @BeforeEach
    void setUp() {
        when(properties.getSecurity()).thenReturn(securityConfig);
        when(securityConfig.getPiiMasking()).thenReturn(piiConfig);
        // デフォルトは有効、全パターン
        when(piiConfig.isEnabled()).thenReturn(true);
        when(piiConfig.getPatterns()).thenReturn(List.of()); // 空 = 全パターン有効
        
        masker = new PiiMasker(properties);
    }

    @Nested
    @DisplayName("メールアドレスマスキング")
    class EmailMaskingTest {

        @ParameterizedTest
        @DisplayName("メールアドレスをマスキング")
        @CsvSource({
                "test@example.com, ***",
                "user.name@domain.co.jp, ***",
                "連絡先: test@example.com です, 連絡先: *** です"
        })
        void shouldMaskEmailAddresses(String input, String expected) {
            // Act
            String result = masker.mask(input);

            // Assert
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("複数のメールアドレスをマスキング")
        void shouldMaskMultipleEmails() {
            // Arrange
            String input = "連絡先は test@example.com または admin@company.co.jp です";

            // Act
            String result = masker.mask(input);

            // Assert
            assertThat(result).doesNotContain("test@example.com");
            assertThat(result).doesNotContain("admin@company.co.jp");
            assertThat(result).contains("***");
        }
    }

    @Nested
    @DisplayName("電話番号マスキング")
    class PhoneMaskingTest {

        @ParameterizedTest
        @DisplayName("電話番号をマスキング")
        @CsvSource({
                "03-1234-5678, ***",
                "090-1234-5678, ***",
                "0312345678, ***",
                "電話番号は 03-1234-5678 です, 電話番号は *** です"
        })
        void shouldMaskPhoneNumbers(String input, String expected) {
            // Act
            String result = masker.mask(input);

            // Assert
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("クレジットカード番号マスキング")
    class CreditCardMaskingTest {

        @ParameterizedTest
        @DisplayName("クレジットカード番号をマスキング")
        @CsvSource({
                "4111-1111-1111-1111, ***",
                "4111 1111 1111 1111, ***",
                "4111111111111111, ***"
        })
        void shouldMaskCreditCardNumbers(String input, String expected) {
            // Act
            String result = masker.mask(input);

            // Assert
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("マイナンバーマスキング")
    class MyNumberMaskingTest {

        @Test
        @DisplayName("マイナンバーをマスキング")
        void shouldMaskMyNumber() {
            // Arrange
            String input = "マイナンバー: 1234-5678-9012";

            // Act
            String result = masker.mask(input);

            // Assert
            assertThat(result).doesNotContain("1234-5678-9012");
            assertThat(result).contains("***");
        }
    }

    @Nested
    @DisplayName("郵便番号マスキング")
    class PostalCodeMaskingTest {

        @ParameterizedTest
        @DisplayName("郵便番号をマスキング")
        @CsvSource({
                "〒100-0001, ***",
                "123-4567, ***"
        })
        void shouldMaskPostalCodes(String input, String expected) {
            // Act
            String result = masker.mask(input);

            // Assert
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("詳細結果テスト")
    class DetailedResultTest {

        @Test
        @DisplayName("マスキング結果の詳細を取得")
        void shouldReturnDetailedMaskingResult() {
            // Arrange
            String input = "Email: test@example.com, Phone: 03-1234-5678";

            // Act
            PiiMasker.MaskingResult result = masker.maskWithDetails(input);

            // Assert
            assertThat(result.isPiiDetected()).isTrue();
            assertThat(result.getMatches()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(result.getMaskedContent()).doesNotContain("test@example.com");
            assertThat(result.getMaskedContent()).doesNotContain("03-1234-5678");
        }

        @Test
        @DisplayName("PII がない場合の結果")
        void shouldReturnNoPiiDetected() {
            // Arrange
            String input = "こんにちは、Mira です。何かお手伝いしましょうか？";

            // Act
            PiiMasker.MaskingResult result = masker.maskWithDetails(input);

            // Assert
            assertThat(result.isPiiDetected()).isFalse();
            assertThat(result.getMatches()).isEmpty();
            assertThat(result.getMaskedContent()).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("ログ用フォーマットテスト")
    class LoggingFormatTest {

        @Test
        @DisplayName("ログ用フォーマットでマスキング")
        void shouldMaskWithLoggingFormat() {
            // Arrange
            String input = "Email: test@example.com";

            // Act
            String result = masker.mask(input, true);

            // Assert
            assertThat(result).contains("[EMAIL:MASKED]");
            assertThat(result).doesNotContain("test@example.com");
        }
    }

    @Nested
    @DisplayName("機能無効時のテスト")
    class DisabledFeatureTest {

        @Test
        @DisplayName("マスキングが無効な場合は元のテキストを返す")
        void shouldReturnOriginalWhenDisabled() {
            // Arrange
            when(piiConfig.isEnabled()).thenReturn(false);
            String input = "Email: test@example.com";

            // Act
            String result = masker.mask(input);

            // Assert
            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("空入力テスト")
    class EmptyInputTest {

        @Test
        @DisplayName("空文字列はそのまま返す")
        void shouldReturnEmptyForEmptyInput() {
            // Act
            String result = masker.mask("");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null はそのまま返す")
        void shouldReturnNullForNullInput() {
            // Act
            String result = masker.mask(null);

            // Assert
            assertThat(result).isNull();
        }
    }
}
