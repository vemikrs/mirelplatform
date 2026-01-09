/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.vemi.mirel.apps.mira.domain.service.TokenQuotaService;
import jp.vemi.mirel.apps.mira.infrastructure.monitoring.MiraMetrics;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * MetricsWrappedAiClient のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class MetricsWrappedAiClientTest {

    @Mock
    private AiProviderClient delegateClient;

    @Mock
    private MiraMetrics metrics;

    @Mock
    private TokenQuotaService tokenQuotaService;

    @Mock
    private TokenCounter tokenCounter;

    private MetricsWrappedAiClient wrappedClient;
    private static final String TENANT_ID = "test-tenant";

    @BeforeEach
    void setUp() {
        wrappedClient = new MetricsWrappedAiClient(
                delegateClient, metrics, tokenQuotaService, tokenCounter, TENANT_ID);
        // lenient: 一部テストでは使わないが共通セットアップとして必要
        org.mockito.Mockito.lenient().when(delegateClient.getProviderName()).thenReturn("test-provider");
    }

    private AiResponse createSuccessResponse(String content, String model, Integer promptTokens,
            Integer completionTokens) {
        return AiResponse.builder()
                .content(content)
                .metadata(AiResponse.Metadata.builder()
                        .model(model)
                        .promptTokens(promptTokens)
                        .completionTokens(completionTokens)
                        .build())
                .build();
    }

    @Nested
    @DisplayName("chat() メソッドのテスト")
    class ChatTests {

        @Test
        @DisplayName("成功時にメトリクスとトークン使用量が記録される")
        void shouldRecordMetricsOnSuccess() {
            // Arrange
            AiRequest request = createTestRequest();
            AiResponse response = createSuccessResponse("Hello", "test-model", 10, 20);
            when(delegateClient.chat(request)).thenReturn(response);
            doNothing().when(tokenQuotaService).consume(
                    anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt());

            // Act
            AiResponse result = wrappedClient.chat(request);

            // Assert
            assertThat(result.getContent()).isEqualTo("Hello");
            verify(metrics).recordChatCompletion(eq("test-model"), eq(TENANT_ID), anyLong(), eq(10), eq(20));
            verify(tokenQuotaService).consume(eq(TENANT_ID), anyString(), anyString(), eq("test-model"), eq(10),
                    eq(20));
        }

        @Test
        @DisplayName("エラーレスポンス時にエラーメトリクスが記録される")
        void shouldRecordErrorMetricsOnErrorResponse() {
            // Arrange
            AiRequest request = createTestRequest();
            AiResponse errorResponse = AiResponse.error("ERROR", "Something went wrong");
            when(delegateClient.chat(request)).thenReturn(errorResponse);

            // Act
            AiResponse result = wrappedClient.chat(request);

            // Assert
            assertThat(result.hasError()).isTrue();
            verify(metrics).recordChatError(eq("ai_error"), eq(TENANT_ID));
            verify(tokenQuotaService, never()).consume(anyString(), anyString(), anyString(), anyString(), anyInt(),
                    anyInt());
        }

        @Test
        @DisplayName("トークン数がnullの場合は0として扱われる")
        void shouldTreatNullTokensAsZero() {
            // Arrange
            AiRequest request = createTestRequest();
            AiResponse response = createSuccessResponse("Hello", "test-model", null, null);
            when(delegateClient.chat(request)).thenReturn(response);
            doNothing().when(tokenQuotaService).consume(
                    anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt());

            // Act
            wrappedClient.chat(request);

            // Assert
            verify(metrics).recordChatCompletion(eq("test-model"), eq(TENANT_ID), anyLong(), eq(0), eq(0));
            verify(tokenQuotaService).consume(eq(TENANT_ID), anyString(), anyString(), eq("test-model"), eq(0), eq(0));
        }

        @Test
        @DisplayName("userId/conversationIdがnullの場合は'unknown'として記録される")
        void shouldUseUnknownForNullUserAndConversation() {
            // Arrange
            AiRequest request = AiRequest.builder()
                    .messages(List.of(AiRequest.Message.user("Hello")))
                    .tenantId(null)
                    .userId(null)
                    .conversationId(null)
                    .build();
            AiResponse response = createSuccessResponse("Hello", "test-model", 5, 10);
            when(delegateClient.chat(request)).thenReturn(response);
            doNothing().when(tokenQuotaService).consume(
                    anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt());

            // Act
            wrappedClient.chat(request);

            // Assert
            verify(tokenQuotaService).consume(eq(TENANT_ID), eq("unknown"), eq("unknown"), eq("test-model"), eq(5),
                    eq(10));
        }

        @Test
        @DisplayName("トークン使用量記録でエラーが発生しても例外がスローされない")
        void shouldNotThrowWhenTokenRecordingFails() {
            // Arrange
            AiRequest request = createTestRequest();
            AiResponse response = createSuccessResponse("Hello", "test-model", 10, 20);
            when(delegateClient.chat(request)).thenReturn(response);
            doThrow(new RuntimeException("DB Error")).when(tokenQuotaService)
                    .consume(anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt());

            // Act & Assert - should not throw
            AiResponse result = wrappedClient.chat(request);
            assertThat(result.getContent()).isEqualTo("Hello");
        }
    }

    @Nested
    @DisplayName("stream() メソッドのテスト")
    class StreamTests {

        @Test
        @DisplayName("ストリーム完了時にメトリクスが記録される")
        void shouldRecordMetricsOnStreamComplete() {
            // Arrange
            AiRequest request = createTestRequest();
            AiResponse chunk1 = AiResponse.builder()
                    .content("Hel")
                    .metadata(AiResponse.Metadata.builder().model("test-model").completionTokens(1).build())
                    .build();
            AiResponse chunk2 = AiResponse.builder()
                    .content("lo")
                    .metadata(AiResponse.Metadata.builder().model("test-model").completionTokens(1).build())
                    .build();
            when(delegateClient.stream(request)).thenReturn(Flux.just(chunk1, chunk2));
            when(tokenCounter.count(anyString(), any())).thenReturn(5);
            doNothing().when(tokenQuotaService).consume(
                    anyString(), anyString(), anyString(), anyString(), anyInt(), anyInt());

            // Act & Assert
            StepVerifier.create(wrappedClient.stream(request))
                    .expectNext(chunk1)
                    .expectNext(chunk2)
                    .verifyComplete();

            verify(metrics).recordChatCompletion(eq("test-model"), eq(TENANT_ID), anyLong(), eq(5), eq(2));
            verify(tokenQuotaService).consume(eq(TENANT_ID), anyString(), anyString(), eq("test-model"), eq(5), eq(2));
        }

        @Test
        @DisplayName("ストリームエラー時にエラーメトリクスが記録される")
        void shouldRecordErrorMetricsOnStreamError() {
            // Arrange
            AiRequest request = createTestRequest();
            when(delegateClient.stream(request)).thenReturn(Flux.error(new RuntimeException("Stream failed")));

            // Act & Assert
            StepVerifier.create(wrappedClient.stream(request))
                    .expectError(RuntimeException.class)
                    .verify();

            verify(metrics).recordChatError(eq("stream_error"), eq(TENANT_ID));
        }
    }

    @Nested
    @DisplayName("委譲メソッドのテスト")
    class DelegateTests {

        @Test
        @DisplayName("isAvailableがdelegateに委譲される")
        void shouldDelegateIsAvailable() {
            when(delegateClient.isAvailable()).thenReturn(true);
            assertThat(wrappedClient.isAvailable()).isTrue();
            verify(delegateClient).isAvailable();
        }

        @Test
        @DisplayName("getProviderNameがdelegateに委譲される")
        void shouldDelegateGetProviderName() {
            when(delegateClient.getProviderName()).thenReturn("custom-provider");
            assertThat(wrappedClient.getProviderName()).isEqualTo("custom-provider");
        }
    }

    private AiRequest createTestRequest() {
        return AiRequest.builder()
                .messages(List.of(AiRequest.Message.user("Hello")))
                .tenantId(TENANT_ID)
                .userId("test-user")
                .conversationId("test-conversation")
                .build();
    }
}
