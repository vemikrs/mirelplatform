/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.reranker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;

import jp.vemi.mirel.apps.mira.infrastructure.config.MiraAiProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Vertex AI Reranker実装（REST API方式）.
 * <p>
 * Google Cloud Discovery Engine の RankService REST API を使用して
 * ドキュメントをクエリとの関連性で再順位付けします。
 * </p>
 * 
 * <h3>使用モデル</h3>
 * <ul>
 * <li>semantic-ranker-default@latest - 最新安定版</li>
 * <li>semantic-ranker-default-004 - 高精度（1024トークン対応）</li>
 * <li>semantic-ranker-fast-004 - 低レイテンシ</li>
 * </ul>
 * 
 * @see <a href=
 *      "https://cloud.google.com/generative-ai-app-builder/docs/ranking">Vertex
 *      AI Ranking API</a>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mira.ai.reranker.provider", havingValue = "vertex-ai", matchIfMissing = true)
public class VertexAiReranker implements Reranker {

    /**
     * Ranking API エンドポイント.
     * <p>
     * `default_ranking_config` を使用し、モデルはリクエストボディで指定する。
     * </p>
     */
    private static final String DISCOVERY_ENGINE_API_URL = "https://discoveryengine.googleapis.com/v1/projects/%s/locations/%s/rankingConfigs/default_ranking_config:rank";

    private final MiraAiProperties properties;
    private final NoOpReranker noOpReranker;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 認証トークンのキャッシュ（高負荷時のボトルネック回避）.
     */
    private volatile GoogleCredentials cachedCredentials;

    public VertexAiReranker(
            MiraAiProperties properties,
            NoOpReranker noOpReranker,
            RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.noOpReranker = noOpReranker;
        this.objectMapper = new ObjectMapper();

        // RestTemplateBuilder を使用してタイムアウト設定を適用
        int timeoutMs = properties.getReranker().getTimeoutMs();
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public RerankerResult rerank(String query, List<Document> documents, int topN) {
        if (documents == null || documents.isEmpty()) {
            return RerankerResult.fallback(List.of(), topN);
        }

        long startTime = System.currentTimeMillis();

        try {
            // Vertex AI設定を取得
            String projectId = properties.getVertexAi().getProjectId();
            String location = properties.getVertexAi().getLocation();
            String model = properties.getReranker().getModel();

            if (projectId == null || projectId.isEmpty()) {
                log.warn("VertexAiReranker: Project ID not configured, falling back to NoOp");
                return noOpReranker.rerank(query, documents, topN);
            }

            // Google認証トークンを取得（キャッシュ活用）
            String accessToken = getAccessToken();

            // リクエストボディを構築
            List<Map<String, Object>> records = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                Map<String, Object> record = new HashMap<>();
                record.put("id", doc.getId() != null ? doc.getId() : String.valueOf(i));
                record.put("content", doc.getText());

                // ドキュメントタイトルがあれば追加（精度向上）
                Object title = doc.getMetadata().get("title");
                if (title != null) {
                    record.put("title", title.toString());
                }
                records.add(record);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model); // モデルはボディで指定
            requestBody.put("query", query);
            requestBody.put("records", records);
            requestBody.put("topN", topN);

            // HTTPヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("X-Goog-User-Project", projectId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // API呼び出し（default_ranking_config を使用）
            String url = String.format(DISCOVERY_ENGINE_API_URL, projectId, location);
            String responseStr = restTemplate.postForObject(url, entity, String.class);

            // レスポンス解析
            JsonNode response = objectMapper.readTree(responseStr);
            JsonNode recordsNode = response.get("records");

            // ドキュメントをIDでマップ
            Map<String, Document> docMap = new HashMap<>();
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                String id = doc.getId() != null ? doc.getId() : String.valueOf(i);
                docMap.put(id, doc);
            }

            // リランク結果を構築
            List<Document> rerankedDocs = new ArrayList<>();
            if (recordsNode != null && recordsNode.isArray()) {
                for (JsonNode recordNode : recordsNode) {
                    String id = recordNode.get("id").asText();
                    double score = recordNode.get("score").asDouble();

                    Document originalDoc = docMap.get(id);
                    if (originalDoc != null) {
                        // メタデータにリランクスコアを追加
                        Map<String, Object> newMetadata = new HashMap<>(originalDoc.getMetadata());
                        newMetadata.put("rerank_score", score);
                        newMetadata.put("rerank_provider", "vertex-ai");

                        Document rerankedDoc = new Document(originalDoc.getId(), originalDoc.getText(), newMetadata);
                        rerankedDocs.add(rerankedDoc);
                    }
                }
            }

            long latencyMs = System.currentTimeMillis() - startTime;
            log.info("VertexAiReranker: Reranked {} -> {} documents in {}ms using model '{}'",
                    documents.size(), rerankedDocs.size(), latencyMs, model);

            return RerankerResult.builder()
                    .documents(rerankedDocs)
                    .applied(true)
                    .providerName("vertex-ai")
                    .latencyMs(latencyMs)
                    .build();

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.warn("VertexAiReranker: Failed to rerank in {}ms, falling back to original order. Error: {}",
                    latencyMs, e.getMessage());

            // フォールバック: 元の順序を維持
            return RerankerResult.fallbackWithError(documents, topN, e.getMessage());
        }
    }

    /**
     * 認証トークンを取得（キャッシュ活用）.
     * <p>
     * 高負荷時の同期ブロックを最小化するため、有効期限内のトークンをキャッシュします。
     * </p>
     */
    private String getAccessToken() throws Exception {
        if (cachedCredentials == null) {
            synchronized (this) {
                if (cachedCredentials == null) {
                    cachedCredentials = GoogleCredentials.getApplicationDefault()
                            .createScoped("https://www.googleapis.com/auth/cloud-platform");
                }
            }
        }
        cachedCredentials.refreshIfExpired();
        return cachedCredentials.getAccessToken().getTokenValue();
    }

    @Override
    public boolean isAvailable() {
        try {
            String projectId = properties.getVertexAi().getProjectId();
            return projectId != null && !projectId.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "vertex-ai";
    }
}
