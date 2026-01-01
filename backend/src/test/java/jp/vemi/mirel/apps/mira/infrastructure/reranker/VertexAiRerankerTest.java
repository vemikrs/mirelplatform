/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.reranker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;

/**
 * VertexAiReranker のユニットテスト.
 * <p>
 * APIコールはモックし、スコアによる並び替え、フォールバック等をテスト。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class VertexAiRerankerTest {

    @Mock
    private MiraAiProperties properties;

    @Mock
    private MiraAiProperties.VertexAiConfig vertexAiConfig;

    @Mock
    private MiraAiProperties.RerankerConfig rerankerConfig;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private NoOpReranker noOpReranker;
    private VertexAiReranker vertexAiReranker;

    @BeforeEach
    void setUp() {
        noOpReranker = new NoOpReranker();

        // 基本設定
        lenient().when(properties.getVertexAi()).thenReturn(vertexAiConfig);
        lenient().when(properties.getReranker()).thenReturn(rerankerConfig);
        lenient().when(vertexAiConfig.getProjectId()).thenReturn("test-project");
        lenient().when(vertexAiConfig.getLocation()).thenReturn("global");
        lenient().when(rerankerConfig.getModel()).thenReturn("semantic-ranker-default@latest");
        lenient().when(rerankerConfig.getTimeoutMs()).thenReturn(5000);

        // RestTemplateBuilder モック
        lenient().when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        lenient().when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        lenient().when(restTemplateBuilder.build()).thenReturn(restTemplate);

        vertexAiReranker = new VertexAiReranker(properties, noOpReranker, restTemplateBuilder);
    }

    @Nested
    @DisplayName("正常系: スコアによる並び替え")
    class ScoreBasedReorderingTest {

        @Test
        @DisplayName("A: スコアに基づいてドキュメントが並び替えられる")
        void shouldReorderDocumentsByScore() {
            // Arrange: DocA: 0.1, DocB: 0.9, DocC: 0.5
            List<Document> docs = List.of(
                    createDocument("docA", "Content A"),
                    createDocument("docB", "Content B"),
                    createDocument("docC", "Content C"));

            // APIレスポンス: DocB(0.9) > DocC(0.5) > DocA(0.1)
            String apiResponse = """
                    {
                        "records": [
                            {"id": "docB", "content": "Content B", "score": 0.9},
                            {"id": "docC", "content": "Content C", "score": 0.5},
                            {"id": "docA", "content": "Content A", "score": 0.1}
                        ]
                    }
                    """;

            when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                    .thenReturn(apiResponse);

            // Act
            RerankerResult result = vertexAiReranker.rerank("test query", docs, 3);

            // Assert: スコア順に並び替えられていること
            assertThat(result.isApplied()).isTrue();
            assertThat(result.getDocuments()).hasSize(3);
            assertThat(result.getDocuments().get(0).getId()).isEqualTo("docB");
            assertThat(result.getDocuments().get(1).getId()).isEqualTo("docC");
            assertThat(result.getDocuments().get(2).getId()).isEqualTo("docA");
        }

        @Test
        @DisplayName("B: TopNによるカットオフが適用される")
        void shouldApplyTopNCutoff() {
            // Arrange: 10件のドキュメント
            List<Document> docs = createTestDocuments(10);

            // APIレスポンス: 上位3件のみ返却
            String apiResponse = """
                    {
                        "records": [
                            {"id": "doc_5", "content": "Content 5", "score": 0.95},
                            {"id": "doc_2", "content": "Content 2", "score": 0.85},
                            {"id": "doc_8", "content": "Content 8", "score": 0.75}
                        ]
                    }
                    """;

            when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                    .thenReturn(apiResponse);

            // Act
            RerankerResult result = vertexAiReranker.rerank("test query", docs, 3);

            // Assert
            assertThat(result.getDocuments()).hasSize(3);
            assertThat(result.getDocuments().get(0).getId()).isEqualTo("doc_5");
        }

        @Test
        @DisplayName("C: メタデータにrerank_scoreが付与される")
        void shouldAddRerankScoreToMetadata() {
            // Arrange
            List<Document> docs = List.of(createDocument("doc1", "Content 1"));

            String apiResponse = """
                    {
                        "records": [
                            {"id": "doc1", "content": "Content 1", "score": 0.87}
                        ]
                    }
                    """;

            when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                    .thenReturn(apiResponse);

            // Act
            RerankerResult result = vertexAiReranker.rerank("test query", docs, 1);

            // Assert
            assertThat(result.getDocuments().get(0).getMetadata())
                    .containsEntry("rerank_score", 0.87)
                    .containsEntry("rerank_provider", "vertex-ai");
        }
    }

    @Nested
    @DisplayName("異常系: エラーハンドリングとフォールバック")
    class ErrorHandlingTest {

        @Test
        @DisplayName("D: APIエラー時はフォールバックして元の順序を維持")
        void shouldFallbackOnApiError() {
            // Arrange
            List<Document> docs = List.of(
                    createDocument("docA", "Content A"),
                    createDocument("docB", "Content B"),
                    createDocument("docC", "Content C"));

            when(restTemplate.postForObject(anyString(), any(), eq(String.class)))
                    .thenThrow(new RestClientException("Connection timeout"));

            // Act
            RerankerResult result = vertexAiReranker.rerank("test query", docs, 3);

            // Assert: フォールバックで元の順序が維持される
            assertThat(result.isApplied()).isFalse();
            assertThat(result.getProviderName()).isEqualTo("fallback");
            assertThat(result.getErrorMessage()).contains("Connection timeout");
            assertThat(result.getDocuments()).hasSize(3);
            assertThat(result.getDocuments().get(0).getId()).isEqualTo("docA");
            assertThat(result.getDocuments().get(1).getId()).isEqualTo("docB");
            assertThat(result.getDocuments().get(2).getId()).isEqualTo("docC");
        }

        @Test
        @DisplayName("E: 空リストの場合はエラーにならず空リストを返す")
        void shouldHandleEmptyList() {
            // Arrange
            List<Document> docs = new ArrayList<>();

            // Act
            RerankerResult result = vertexAiReranker.rerank("test query", docs, 5);

            // Assert
            assertThat(result.getDocuments()).isEmpty();
            assertThat(result.isApplied()).isFalse();
        }

        @Test
        @DisplayName("nullリストの場合はエラーにならず空リストを返す")
        void shouldHandleNullList() {
            // Act
            RerankerResult result = vertexAiReranker.rerank("test query", null, 5);

            // Assert
            assertThat(result.getDocuments()).isEmpty();
            assertThat(result.isApplied()).isFalse();
        }

        @Test
        @DisplayName("プロジェクトID未設定時はNoOpにフォールバック")
        void shouldFallbackWhenProjectIdNotConfigured() {
            // Arrange
            when(vertexAiConfig.getProjectId()).thenReturn(null);
            List<Document> docs = createTestDocuments(3);

            // Act
            RerankerResult result = vertexAiReranker.rerank("test query", docs, 3);

            // Assert
            assertThat(result.isApplied()).isFalse();
            assertThat(result.getProviderName()).isEqualTo("fallback");
        }
    }

    @Nested
    @DisplayName("isAvailable メソッドのテスト")
    class IsAvailableTest {

        @Test
        @DisplayName("プロジェクトIDが設定されていれば利用可能")
        void shouldBeAvailableWithProjectId() {
            // Arrange
            when(vertexAiConfig.getProjectId()).thenReturn("my-project");

            // Act & Assert
            assertThat(vertexAiReranker.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("プロジェクトIDが未設定なら利用不可")
        void shouldNotBeAvailableWithoutProjectId() {
            // Arrange
            when(vertexAiConfig.getProjectId()).thenReturn(null);

            // Act & Assert
            assertThat(vertexAiReranker.isAvailable()).isFalse();
        }
    }

    // ヘルパーメソッド
    private Document createDocument(String id, String content) {
        Map<String, Object> metadata = new HashMap<>();
        return new Document(id, content, metadata);
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
