/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.reranker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

/**
 * RerankerResult のユニットテスト.
 */
class RerankerResultTest {

    @Nested
    @DisplayName("fallback メソッドのテスト")
    class FallbackTest {

        @Test
        @DisplayName("フォールバック結果を正しく生成")
        void shouldCreateFallbackResult() {
            // Arrange
            List<Document> docs = createTestDocuments(5);

            // Act
            RerankerResult result = RerankerResult.fallback(docs, 3);

            // Assert
            assertThat(result.getDocuments()).hasSize(3);
            assertThat(result.isApplied()).isFalse();
            assertThat(result.getProviderName()).isEqualTo("fallback");
            assertThat(result.getLatencyMs()).isEqualTo(0);
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("ドキュメント数がtopN未満の場合はすべて返却")
        void shouldReturnAllDocumentsWhenLessThanTopN() {
            // Arrange
            List<Document> docs = createTestDocuments(2);

            // Act
            RerankerResult result = RerankerResult.fallback(docs, 5);

            // Assert
            assertThat(result.getDocuments()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("fallbackWithError メソッドのテスト")
    class FallbackWithErrorTest {

        @Test
        @DisplayName("エラー付きフォールバック結果を正しく生成")
        void shouldCreateFallbackResultWithError() {
            // Arrange
            List<Document> docs = createTestDocuments(5);
            String errorMessage = "API connection failed";

            // Act
            RerankerResult result = RerankerResult.fallbackWithError(docs, 3, errorMessage);

            // Assert
            assertThat(result.getDocuments()).hasSize(3);
            assertThat(result.isApplied()).isFalse();
            assertThat(result.getProviderName()).isEqualTo("fallback");
            assertThat(result.getLatencyMs()).isEqualTo(0);
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("Builder パターンのテスト")
    class BuilderTest {

        @Test
        @DisplayName("ビルダーで正常な結果を構築")
        void shouldBuildSuccessfulResult() {
            // Arrange
            List<Document> docs = createTestDocuments(3);

            // Act
            RerankerResult result = RerankerResult.builder()
                    .documents(docs)
                    .applied(true)
                    .providerName("vertex-ai")
                    .latencyMs(150)
                    .build();

            // Assert
            assertThat(result.getDocuments()).hasSize(3);
            assertThat(result.isApplied()).isTrue();
            assertThat(result.getProviderName()).isEqualTo("vertex-ai");
            assertThat(result.getLatencyMs()).isEqualTo(150);
            assertThat(result.getErrorMessage()).isNull();
        }
    }

    private List<Document> createTestDocuments(int count) {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("index", i);
            docs.add(new Document("doc_" + i, "Content of document " + i, metadata));
        }
        return docs;
    }
}
