/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * VertexAiGeminiClient のリトライロジックテスト.
 * 
 * <p>
 * VertexAiGeminiClient.shouldRetryException() を直接呼び出してテスト。
 * </p>
 */
class VertexAiGeminiClientTest {

    @Nested
    @DisplayName("リトライフィルタリングテスト")
    class RetryFilterTest {

        @Test
        @DisplayName("PERMISSION_DENIED はリトライ対象外")
        void shouldNotRetryPermissionDenied() {
            // Arrange
            StatusRuntimeException exception = new StatusRuntimeException(Status.PERMISSION_DENIED);

            // Act
            boolean shouldRetry = VertexAiGeminiClient.shouldRetryException(exception);

            // Assert
            assertThat(shouldRetry).isFalse();
        }

        @Test
        @DisplayName("UNAUTHENTICATED はリトライ対象外")
        void shouldNotRetryUnauthenticated() {
            // Arrange
            StatusRuntimeException exception = new StatusRuntimeException(Status.UNAUTHENTICATED);

            // Act
            boolean shouldRetry = VertexAiGeminiClient.shouldRetryException(exception);

            // Assert
            assertThat(shouldRetry).isFalse();
        }

        @Test
        @DisplayName("INVALID_ARGUMENT はリトライ対象外")
        void shouldNotRetryInvalidArgument() {
            // Arrange
            StatusRuntimeException exception = new StatusRuntimeException(Status.INVALID_ARGUMENT);

            // Act
            boolean shouldRetry = VertexAiGeminiClient.shouldRetryException(exception);

            // Assert
            assertThat(shouldRetry).isFalse();
        }

        @Test
        @DisplayName("UNAVAILABLE はリトライ対象")
        void shouldRetryUnavailable() {
            // Arrange
            StatusRuntimeException exception = new StatusRuntimeException(Status.UNAVAILABLE);

            // Act
            boolean shouldRetry = VertexAiGeminiClient.shouldRetryException(exception);

            // Assert
            assertThat(shouldRetry).isTrue();
        }

        @Test
        @DisplayName("DEADLINE_EXCEEDED はリトライ対象")
        void shouldRetryDeadlineExceeded() {
            // Arrange
            StatusRuntimeException exception = new StatusRuntimeException(Status.DEADLINE_EXCEEDED);

            // Act
            boolean shouldRetry = VertexAiGeminiClient.shouldRetryException(exception);

            // Assert
            assertThat(shouldRetry).isTrue();
        }

        @Test
        @DisplayName("RESOURCE_EXHAUSTED はリトライ対象")
        void shouldRetryResourceExhausted() {
            // Arrange
            StatusRuntimeException exception = new StatusRuntimeException(Status.RESOURCE_EXHAUSTED);

            // Act
            boolean shouldRetry = VertexAiGeminiClient.shouldRetryException(exception);

            // Assert
            assertThat(shouldRetry).isTrue();
        }

        @Test
        @DisplayName("他の例外はリトライ対象")
        void shouldRetryOtherExceptions() {
            // Arrange
            RuntimeException exception = new RuntimeException("Network error");

            // Act
            boolean shouldRetry = VertexAiGeminiClient.shouldRetryException(exception);

            // Assert
            assertThat(shouldRetry).isTrue();
        }
    }
}
