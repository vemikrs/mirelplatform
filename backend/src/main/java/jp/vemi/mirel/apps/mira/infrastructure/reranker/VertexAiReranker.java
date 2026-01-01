/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.reranker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <li>semantic-ranker-default-004 - 高精度（デフォルト）</li>
 * <li>semantic-ranker-fast-004 - 低レイテンシ</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mira.ai.reranker.provider", havingValue = "vertex-ai", matchIfMissing = true)
public class VertexAiReranker implements Reranker {

    private static final String DISCOVERY_ENGINE_API_URL = "https://discoveryengine.googleapis.com/v1/projects/%s/locations/%s/rankingConfigs/%s:rank";

    private final MiraAiProperties properties;
    private final NoOpReranker noOpReranker;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public VertexAiReranker(MiraAiProperties properties, NoOpReranker noOpReranker) {
        this.properties = properties;
        this.noOpReranker = noOpReranker;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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

            // Google認証トークンを取得
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            credentials.refreshIfExpired();
            String accessToken = credentials.getAccessToken().getTokenValue();

            // リクエストボディを構築
            List<Map<String, Object>> records = new ArrayList<>();
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                Map<String, Object> record = new HashMap<>();
                record.put("id", doc.getId() != null ? doc.getId() : String.valueOf(i));
                record.put("content", doc.getText());
                records.add(record);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("records", records);
            requestBody.put("topN", topN);

            // HTTPヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // API呼び出し
            String url = String.format(DISCOVERY_ENGINE_API_URL, projectId, location, model);
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
            log.info("VertexAiReranker: Reranked {} documents in {}ms", rerankedDocs.size(), latencyMs);

            return RerankerResult.builder()
                    .documents(rerankedDocs)
                    .applied(true)
                    .providerName("vertex-ai")
                    .latencyMs(latencyMs)
                    .build();

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startTime;
            log.warn("VertexAiReranker: Failed to rerank, falling back to original order. Error: {}",
                    e.getMessage());

            // フォールバック: 元の順序を維持
            return RerankerResult.fallbackWithError(documents, topN, e.getMessage());
        }
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
