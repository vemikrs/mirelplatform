/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.domain.dto.request.ChatRequest;

/**
 * ModeResolver のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class ModeResolverTest {

    private ModeResolver modeResolver;

    @BeforeEach
    void setUp() {
        modeResolver = new ModeResolver();
    }

    @Nested
    @DisplayName("resolve メソッドのテスト")
    class ResolveTest {

        @Test
        @DisplayName("明示的なモード指定があればそれを返す")
        void shouldReturnExplicitModeWhenSpecified() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .mode("ERROR_ANALYZE")
                    .message(ChatRequest.Message.builder()
                            .content("テスト")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.ERROR_ANALYZE);
        }

        @Test
        @DisplayName("モード未指定時はデフォルトを返す")
        void shouldReturnDefaultModeWhenNotSpecified() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("普通のメッセージ")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.GENERAL_CHAT);
        }

        @Test
        @DisplayName("コンテキストにStudio指定がある場合はSTUDIO_AGENTを返す")
        void shouldReturnStudioAgentForStudioContext() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .context(ChatRequest.Context.builder()
                            .appId("studio")
                            .build())
                    .message(ChatRequest.Message.builder()
                            .content("テスト")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.STUDIO_AGENT);
        }

        @Test
        @DisplayName("コンテキストにWorkflow指定がある場合はWORKFLOW_AGENTを返す")
        void shouldReturnWorkflowAgentForWorkflowContext() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .context(ChatRequest.Context.builder()
                            .appId("workflow")
                            .build())
                    .message(ChatRequest.Message.builder()
                            .content("テスト")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.WORKFLOW_AGENT);
        }
    }

    @Nested
    @DisplayName("メッセージ内容からの意図推定テスト")
    class IntentResolutionTest {

        @Test
        @DisplayName("この画面という言葉でCONTEXT_HELPを推定")
        void shouldResolveCotextHelpForScreenQuestion() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("この画面の説明をしてください")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.CONTEXT_HELP);
        }

        @Test
        @DisplayName("エラーという言葉でERROR_ANALYZEを推定")
        void shouldResolveErrorAnalyzeForErrorMessage() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("エラーが発生しました")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.ERROR_ANALYZE);
        }

        @Test
        @DisplayName("使い方という言葉でCONTEXT_HELPを推定")
        void shouldResolveContextHelpForUsageQuestion() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("使い方を教えてください")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.CONTEXT_HELP);
        }

        @Test
        @DisplayName("ワークフローという言葉でWORKFLOW_AGENTを推定")
        void shouldResolveWorkflowAgentForWorkflowMessage() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("ワークフローを作成したい")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.WORKFLOW_AGENT);
        }

        @Test
        @DisplayName("モデル作成でSTUDIO_AGENTを推定")
        void shouldResolveStudioAgentForModelCreation() {
            // Arrange
            ChatRequest request = ChatRequest.builder()
                    .message(ChatRequest.Message.builder()
                            .content("モデルを作成したい")
                            .build())
                    .build();

            // Act
            MiraMode result = modeResolver.resolve(request);

            // Assert
            assertThat(result).isEqualTo(MiraMode.STUDIO_AGENT);
        }
    }
}
