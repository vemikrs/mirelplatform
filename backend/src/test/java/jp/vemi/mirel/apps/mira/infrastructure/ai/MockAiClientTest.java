/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * MockAiClient のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class MockAiClientTest {

    private MockAiClient mockAiClient;

    @Mock
    private MiraAiProperties properties;

    @Mock
    private MiraAiProperties.MockConfig mockConfig;

    @BeforeEach
    void setUp() {
        // lenient を使用して不要なスタブ警告を回避
        lenient().when(properties.getMock()).thenReturn(mockConfig);
        lenient().when(mockConfig.isEnabled()).thenReturn(true);
        lenient().when(mockConfig.getDefaultResponse()).thenReturn("デフォルト応答です");
        lenient().when(mockConfig.getResponseDelayMs()).thenReturn(0); // テストでは遅延なし
        lenient().when(mockConfig.getResponses()).thenReturn(null);
        
        mockAiClient = new MockAiClient(properties);
    }

    @Nested
    @DisplayName("chat メソッドのテスト")
    class ChatTest {

        @Test
        @DisplayName("正常なリクエストで応答を返す")
        void shouldReturnResponseForValidRequest() {
            // Arrange
            AiRequest request = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.builder()
                            .role("user")
                            .content("テストメッセージ")
                            .build()))
                    .build();

            // Act
            AiResponse response = mockAiClient.chat(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getContent()).isNotBlank();
        }

        @Test
        @DisplayName("エラー関連キーワードでエラー分析応答を返す")
        void shouldReturnErrorAnalysisForErrorKeywords() {
            // Arrange
            AiRequest request = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.builder()
                            .role("user")
                            .content("エラーが発生しました")
                            .build()))
                    .build();

            // Act
            AiResponse response = mockAiClient.chat(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getContent()).contains("エラー");
        }

        @Test
        @DisplayName("画面説明キーワードで説明応答を返す")
        void shouldReturnScreenDescriptionForHelpKeywords() {
            // Arrange
            AiRequest request = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.builder()
                            .role("user")
                            .content("この画面の説明をしてください")
                            .build()))
                    .build();

            // Act
            AiResponse response = mockAiClient.chat(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getContent()).contains("画面");
        }

        @Test
        @DisplayName("設定関連キーワードで手順応答を返す")
        void shouldReturnStepsForSettingKeywords() {
            // Arrange
            AiRequest request = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.builder()
                            .role("user")
                            .content("設定手順を教えてください")
                            .build()))
                    .build();

            // Act
            AiResponse response = mockAiClient.chat(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getContent()).contains("設定");
        }

        @Test
        @DisplayName("パターンマッチ応答が設定されている場合はそれを返す")
        void shouldReturnPatternMatchedResponse() {
            // Arrange
            Map<String, String> responses = new HashMap<>();
            responses.put("hello", "こんにちは！");
            given(mockConfig.getResponses()).willReturn(responses);
            
            AiRequest request = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.builder()
                            .role("user")
                            .content("hello world")
                            .build()))
                    .build();

            // Act
            AiResponse response = mockAiClient.chat(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getContent()).isEqualTo("こんにちは！");
        }

        @Test
        @DisplayName("メタデータが正しく設定される")
        void shouldSetMetadataCorrectly() {
            // Arrange
            AiRequest request = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.builder()
                            .role("user")
                            .content("テスト")
                            .build()))
                    .build();

            // Act
            AiResponse response = mockAiClient.chat(request);

            // Assert
            assertThat(response.getModel()).isEqualTo("mock-model");
            assertThat(response.getMetadata()).isNotNull();
            assertThat(response.getMetadata().getFinishReason()).isEqualTo("stop");
        }

        @Test
        @DisplayName("空メッセージでもデフォルト応答を返す")
        void shouldReturnDefaultResponseForEmptyMessage() {
            // Arrange
            AiRequest request = AiRequest.builder()
                    .messages(List.of())
                    .build();

            // Act
            AiResponse response = mockAiClient.chat(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getContent()).isEqualTo("デフォルト応答です");
        }
    }

    @Nested
    @DisplayName("isAvailable メソッドのテスト")
    class IsAvailableTest {

        @Test
        @DisplayName("モックが有効な場合はtrueを返す")
        void shouldReturnTrueWhenEnabled() {
            // Arrange
            given(mockConfig.isEnabled()).willReturn(true);

            // Act & Assert
            assertThat(mockAiClient.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("モックが無効な場合はfalseを返す")
        void shouldReturnFalseWhenDisabled() {
            // Arrange
            given(mockConfig.isEnabled()).willReturn(false);

            // Act & Assert
            assertThat(mockAiClient.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("getProviderName メソッドのテスト")
    class GetProviderNameTest {

        @Test
        @DisplayName("プロバイダ名としてmockを返す")
        void shouldReturnMock() {
            assertThat(mockAiClient.getProviderName()).isEqualTo("mock");
        }
    }
}
