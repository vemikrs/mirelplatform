/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;
import jp.vemi.mirel.apps.mira.domain.dto.request.ErrorReportRequest;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest;

/**
 * PromptBuilder のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class PromptBuilderTest {

    @org.mockito.Mock
    private MiraContextLayerService contextLayerService;

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder(contextLayerService);
    }

    @Nested
    @DisplayName("buildChatRequest メソッドのテスト")
    class BuildChatRequestTest {

        @Test
        @DisplayName("GENERAL_CHATモードでリクエストを構築")
        void shouldBuildRequestForGeneralChat() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("テストメッセージ")
                            .build())
                    .build();

            // Act
            AiRequest result = promptBuilder.buildChatRequest(request, MiraMode.GENERAL_CHAT, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMessages()).isNotEmpty();
            // システムプロンプトが含まれている
            assertThat(result.getMessages().get(0).getRole()).isEqualTo("system");
            // ユーザーメッセージが含まれている
            assertThat(result.getMessages())
                    .anyMatch(m -> "user".equals(m.getRole()) && "テストメッセージ".equals(m.getContent()));
            // 適切な温度設定
            assertThat(result.getTemperature()).isEqualTo(0.7);
        }

        @Test
        @DisplayName("ERROR_ANALYZEモードでリクエストを構築")
        void shouldBuildRequestForErrorAnalyze() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("エラーが発生しました")
                            .build())
                    .build();

            // Act
            AiRequest result = promptBuilder.buildChatRequest(request, MiraMode.ERROR_ANALYZE, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTemperature()).isEqualTo(0.3); // エラー分析は低温度
        }

        @Test
        @DisplayName("STUDIO_AGENTモードでリクエストを構築")
        void shouldBuildRequestForStudioAgent() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("モデルを作成して")
                            .build())
                    .build();

            // Act
            AiRequest result = promptBuilder.buildChatRequest(request, MiraMode.STUDIO_AGENT, null);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTemperature()).isEqualTo(0.4);
            assertThat(result.getMaxTokens()).isEqualTo(1500);
        }

        @Test
        @DisplayName("会話履歴が含まれる")
        void shouldIncludeConversationHistory() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("続きを教えて")
                            .build())
                    .build();

            List<AiRequest.Message> history = List.of(
                    AiRequest.Message.builder().role("user").content("こんにちは").build(),
                    AiRequest.Message.builder().role("assistant").content("こんにちは！").build());

            // Act
            AiRequest result = promptBuilder.buildChatRequest(request, MiraMode.GENERAL_CHAT, history);

            // Assert
            assertThat(result.getMessages()).hasSize(4); // system + 2 history + user
            assertThat(result.getMessages().get(1).getContent()).isEqualTo("こんにちは");
            assertThat(result.getMessages().get(2).getContent()).isEqualTo("こんにちは！");
        }

        @Test
        @DisplayName("コンテキスト情報がプロンプトに反映される")
        void shouldIncludeContextInPrompt() {
            // Arrange
            Map<String, Object> payload = new HashMap<>();
            payload.put("studioModule", "modeler");
            payload.put("targetEntity", "User");

            ChatRequest request = ChatRequest.builder()
                    .context(ChatRequest.Context.builder()
                            .appId("studio")
                            .screenId("model-editor")
                            .payload(payload)
                            .build())
                    .message(ChatRequest.Message.builder()
                            .content("テスト")
                            .build())
                    .build();

            // Act
            AiRequest result = promptBuilder.buildChatRequest(request, MiraMode.STUDIO_AGENT, null);

            // Assert
            assertThat(result).isNotNull();
            // プロンプトにコンテキスト情報が反映されていることを確認
            // (テンプレートによる置換が行われる)
        }
    }

    @Nested
    @DisplayName("buildErrorAnalyzeRequest メソッドのテスト")
    class BuildErrorAnalyzeRequestTest {

        @Test
        @DisplayName("エラーレポートからリクエストを構築")
        void shouldBuildRequestFromErrorReport() {
            // Arrange
            ErrorReportRequest request = ErrorReportRequest.builder()
                    .source("frontend")
                    .code("ERR-001")
                    .message("NullPointerException")
                    .detail("java.lang.NullPointerException at ...")
                    .build();

            // Act
            AiRequest result = promptBuilder.buildErrorAnalyzeRequest(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getMessages()).hasSize(2); // system + user
            assertThat(result.getTemperature()).isEqualTo(0.3);
            assertThat(result.getMaxTokens()).isEqualTo(1000);

            // ユーザーメッセージにエラー情報が含まれる
            String userMessage = result.getMessages().stream()
                    .filter(m -> "user".equals(m.getRole()))
                    .findFirst()
                    .map(AiRequest.Message::getContent)
                    .orElse("");
            assertThat(userMessage).contains("ERR-001");
            assertThat(userMessage).contains("NullPointerException");
        }

        @Test
        @DisplayName("コンテキスト付きエラーレポートを処理")
        void shouldIncludeContextInErrorReport() {
            // Arrange
            ErrorReportRequest request = ErrorReportRequest.builder()
                    .source("frontend")
                    .code("ERR-002")
                    .message("API Error")
                    .context(ErrorReportRequest.Context.builder()
                            .appId("promarker")
                            .screenId("document-list")
                            .build())
                    .build();

            // Act
            AiRequest result = promptBuilder.buildErrorAnalyzeRequest(request);

            // Assert
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("buildContextHelpRequest メソッドのテスト")
    class BuildContextHelpRequestTest {

        @Test
        @DisplayName("コンテキストヘルプリクエストを構築")
        void shouldBuildContextHelpRequest() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .context(ChatRequest.Context.builder()
                            .appId("promarker")
                            .screenId("dashboard")
                            .build())
                    .message(ChatRequest.Message.builder()
                            .content("この画面の使い方を教えて")
                            .build())
                    .build();

            // Act
            AiRequest result = promptBuilder.buildContextHelpRequest(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTemperature()).isEqualTo(0.5);
        }
    }
}
