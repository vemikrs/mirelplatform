/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.ai.document.Document;

import jp.vemi.mirel.apps.mira.infrastructure.reranker.NoOpReranker;
import jp.vemi.mirel.apps.mira.infrastructure.reranker.Reranker;
import jp.vemi.mirel.apps.mira.infrastructure.reranker.RerankerResult;

/**
 * RerankerService のユニットテスト.
 */
@ExtendWith(MockitoExtension.class)
class RerankerServiceTest {

    @Mock
    private MiraSettingService settingService;

    @Mock
    private Reranker vertexAiReranker;

    private NoOpReranker noOpReranker;

    private RerankerService rerankerService;

    @BeforeEach
    void setUp() {
        noOpReranker = new NoOpReranker();

        // モックのプロバイダー名設定
        lenient().when(vertexAiReranker.getProviderName()).thenReturn("vertex-ai");
        lenient().when(vertexAiReranker.isAvailable()).thenReturn(true);

        // RerankerServiceの生成（リランカーリストからマップを構築）
        List<Reranker> rerankers = List.of(vertexAiReranker, noOpReranker);
        rerankerService = new RerankerService(settingService, rerankers, noOpReranker);
    }

    @Nested
    @DisplayName("shouldRerank メソッドのテスト")
    class ShouldRerankTest {

        @Test
        @DisplayName("リランカー無効時はfalseを返す")
        void shouldReturnFalseWhenDisabled() {
            // Arrange
            when(settingService.isRerankerEnabled("tenant1")).thenReturn(false);

            // Act
            boolean result = rerankerService.shouldRerank("tenant1", 15);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("候補数が閾値未満の場合はfalseを返す")
        void shouldReturnFalseWhenBelowMinCandidates() {
            // Arrange
            when(settingService.isRerankerEnabled("tenant1")).thenReturn(true);
            when(settingService.getRerankerMinCandidates("tenant1")).thenReturn(10);

            // Act
            boolean result = rerankerService.shouldRerank("tenant1", 5);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("有効かつ候補数が閾値以上の場合はtrueを返す")
        void shouldReturnTrueWhenEnabledAndAboveThreshold() {
            // Arrange
            when(settingService.isRerankerEnabled("tenant1")).thenReturn(true);
            when(settingService.getRerankerMinCandidates("tenant1")).thenReturn(10);

            // Act
            boolean result = rerankerService.shouldRerank("tenant1", 15);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("rerank メソッドのテスト")
    class RerankTest {

        @Test
        @DisplayName("指定プロバイダーでリランキングを実行")
        void shouldRerankWithSpecifiedProvider() {
            // Arrange
            List<Document> docs = createTestDocuments(5);
            List<Document> rerankedDocs = createTestDocuments(3);
            RerankerResult expectedResult = RerankerResult.builder()
                    .documents(rerankedDocs)
                    .applied(true)
                    .providerName("vertex-ai")
                    .latencyMs(100)
                    .build();

            when(settingService.getRerankerProvider("tenant1")).thenReturn("vertex-ai");
            when(settingService.getRerankerTopN("tenant1")).thenReturn(3);
            when(vertexAiReranker.rerank(anyString(), any(), anyInt())).thenReturn(expectedResult);

            // Act
            List<Document> result = rerankerService.rerank("query", docs, "tenant1");

            // Assert
            assertThat(result).hasSize(3);
            verify(vertexAiReranker).rerank("query", docs, 3);
        }

        @Test
        @DisplayName("プロバイダーが利用不可の場合はNoOpにフォールバック")
        void shouldFallbackToNoOpWhenProviderUnavailable() {
            // Arrange
            List<Document> docs = createTestDocuments(5);

            when(settingService.getRerankerProvider("tenant1")).thenReturn("vertex-ai");
            when(settingService.getRerankerTopN("tenant1")).thenReturn(3);
            when(vertexAiReranker.isAvailable()).thenReturn(false);

            // Act
            List<Document> result = rerankerService.rerank("query", docs, "tenant1");

            // Assert
            assertThat(result).hasSize(3);
            verify(vertexAiReranker, never()).rerank(anyString(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("rerankWithOverride メソッドのテスト")
    class RerankWithOverrideTest {

        @Test
        @DisplayName("enabledオーバーライドがfalseの場合はフォールバック")
        void shouldFallbackWhenEnabledOverrideIsFalse() {
            // Arrange
            List<Document> docs = createTestDocuments(5);
            when(settingService.getRerankerTopN("tenant1")).thenReturn(3);

            // Act
            RerankerResult result = rerankerService.rerankWithOverride(
                    "query", docs, "tenant1", false, null);

            // Assert
            assertThat(result.isApplied()).isFalse();
            assertThat(result.getProviderName()).isEqualTo("fallback");
        }

        @Test
        @DisplayName("topNオーバーライドが適用される")
        void shouldApplyTopNOverride() {
            // Arrange
            List<Document> docs = createTestDocuments(10);
            List<Document> rerankedDocs = createTestDocuments(7);
            RerankerResult expectedResult = RerankerResult.builder()
                    .documents(rerankedDocs)
                    .applied(true)
                    .providerName("vertex-ai")
                    .latencyMs(100)
                    .build();

            when(settingService.getRerankerProvider("tenant1")).thenReturn("vertex-ai");
            when(vertexAiReranker.rerank(anyString(), any(), anyInt())).thenReturn(expectedResult);

            // Act
            RerankerResult result = rerankerService.rerankWithOverride(
                    "query", docs, "tenant1", true, 7);

            // Assert
            verify(vertexAiReranker).rerank("query", docs, 7);
        }

        @Test
        @DisplayName("オーバーライドがnullの場合はテナント設定を使用")
        void shouldUseSettingsWhenOverrideIsNull() {
            // Arrange
            List<Document> docs = createTestDocuments(5);
            RerankerResult expectedResult = RerankerResult.builder()
                    .documents(createTestDocuments(3))
                    .applied(true)
                    .providerName("vertex-ai")
                    .latencyMs(100)
                    .build();

            when(settingService.isRerankerEnabled("tenant1")).thenReturn(true);
            when(settingService.getRerankerProvider("tenant1")).thenReturn("vertex-ai");
            when(settingService.getRerankerTopN("tenant1")).thenReturn(3);
            when(vertexAiReranker.rerank(anyString(), any(), anyInt())).thenReturn(expectedResult);

            // Act
            RerankerResult result = rerankerService.rerankWithOverride(
                    "query", docs, "tenant1", null, null);

            // Assert
            assertThat(result.isApplied()).isTrue();
            verify(vertexAiReranker).rerank("query", docs, 3);
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
