/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.reranker;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

/**
 * NoOpReranker のユニットテスト.
 */
class NoOpRerankerTest {

    private NoOpReranker noOpReranker;

    @BeforeEach
    void setUp() {
        noOpReranker = new NoOpReranker();
    }

    @Nested
    @DisplayName("rerank メソッドのテスト")
    class RerankTest {

        @Test
        @DisplayName("ドキュメントをそのまま返却（topN適用）")
        void shouldReturnDocumentsWithTopN() {
            // Arrange
            List<Document> docs = createTestDocuments(5);

            // Act
            RerankerResult result = noOpReranker.rerank("test query", docs, 3);

            // Assert
            assertThat(result.getDocuments()).hasSize(3);
            assertThat(result.isApplied()).isFalse();
            assertThat(result.getProviderName()).isEqualTo("fallback");
            assertThat(result.getLatencyMs()).isEqualTo(0);
        }

        @Test
        @DisplayName("ドキュメント数がtopN未満の場合はすべて返却")
        void shouldReturnAllDocumentsWhenLessThanTopN() {
            // Arrange
            List<Document> docs = createTestDocuments(2);

            // Act
            RerankerResult result = noOpReranker.rerank("test query", docs, 5);

            // Assert
            assertThat(result.getDocuments()).hasSize(2);
        }

        @Test
        @DisplayName("空リストの場合は空リストを返却")
        void shouldReturnEmptyListForEmptyInput() {
            // Arrange
            List<Document> docs = new ArrayList<>();

            // Act
            RerankerResult result = noOpReranker.rerank("test query", docs, 5);

            // Assert
            assertThat(result.getDocuments()).isEmpty();
        }

        @Test
        @DisplayName("元のドキュメント順序を維持")
        void shouldMaintainOriginalOrder() {
            // Arrange
            List<Document> docs = createTestDocuments(3);

            // Act
            RerankerResult result = noOpReranker.rerank("test query", docs, 3);

            // Assert
            assertThat(result.getDocuments().get(0).getId()).isEqualTo("doc_0");
            assertThat(result.getDocuments().get(1).getId()).isEqualTo("doc_1");
            assertThat(result.getDocuments().get(2).getId()).isEqualTo("doc_2");
        }
    }

    @Nested
    @DisplayName("isAvailable メソッドのテスト")
    class IsAvailableTest {

        @Test
        @DisplayName("常に利用可能")
        void shouldAlwaysBeAvailable() {
            // Act & Assert
            assertThat(noOpReranker.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("getProviderName メソッドのテスト")
    class GetProviderNameTest {

        @Test
        @DisplayName("プロバイダー名はnoop")
        void shouldReturnNoop() {
            // Act & Assert
            assertThat(noOpReranker.getProviderName()).isEqualTo("noop");
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
