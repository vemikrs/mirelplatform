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
 * @see <a href=
 *      "https://cloud.google.com/generative-ai-app-builder/docs/ranking">Vertex
 *      AI Ranking API</a>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mira.ai.reranker.provider", havingValue = "vertex-ai", matchIfMissing = true)
public class VertexAiReranker implements Reranker {

    private static final String DISCOVERY_ENGINE_API_URL = "https://discoveryengine.googleapis.com/v1/projects/%s/locations/%s/rankingConfigs/default_ranking_config:rank";

    private final MiraAiProperties properties;
    private final NoOpReranker noOpReranker;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** 認証トークンのキャッシュ. */
    private volatile GoogleCredentials cachedCredentials;

    // ===== Request/Response DTOs (型安全、可読性向上) =====

    /** Ranking APIリクエストDTO. */
    private record RankingRequest(
            String model,
            String query,
            List<RankingRecord> records,
            Integer topN) {
    }

    /** Ranking APIリクエストのレコードDTO. */
    private record RankingRecord(
            String id,
            String title,
            String content) {
    }

    /** Ranking APIレスポンスDTO. */
    private record RankingResponse(
            List<RankedRecord> records) {
    }

    /** Ranking APIレスポンスのレコードDTO. */
    private record RankedRecord(
            String id,
            String title,
            String content,
            double score) {
    }

    public VertexAiReranker(
            MiraAiProperties properties,
            NoOpReranker noOpReranker,
            RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.noOpReranker = noOpReranker;
        this.objectMapper = new ObjectMapper();

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
            String projectId = properties.getVertexAi().getProjectId();
            String location = properties.getVertexAi().getLocation();
            String model = properties.getReranker().getModel();

            if (projectId == null || projectId.isEmpty()) {
                log.warn("VertexAiReranker: Project ID not configured, falling back to NoOp");
                return noOpReranker.rerank(query, documents, topN);
            }

            String accessToken = getAccessToken();

            // DTOを使用してリクエスト構築（型安全）
            List<RankingRecord> records = buildRankingRecords(documents);
            RankingRequest request = new RankingRequest(model, query, records, topN);

            // HTTPヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
            headers.set("X-Goog-User-Project", projectId);

            HttpEntity<RankingRequest> entity = new HttpEntity<>(request, headers);

            // API呼び出し（文字列で受け取りJacksonでパース - テスト容易性のため）
            String url = String.format(DISCOVERY_ENGINE_API_URL, projectId, location);
            String responseStr = restTemplate.postForObject(url, entity, String.class);
            RankingResponse response = objectMapper.readValue(responseStr, RankingResponse.class);

            // レスポンスをドキュメントリストに変換
            List<Document> rerankedDocs = buildRerankedDocuments(documents, response);

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
            log.warn("VertexAiReranker: Failed to rerank in {}ms, falling back. Error: {}",
                    latencyMs, e.getMessage());

            return RerankerResult.fallbackWithError(documents, topN, e.getMessage());
        }
    }

    /**
     * ドキュメントリストからRankingRecordリストを構築.
     */
    private List<RankingRecord> buildRankingRecords(List<Document> documents) {
        List<RankingRecord> records = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String id = doc.getId() != null ? doc.getId() : String.valueOf(i);
            String title = extractTitle(doc);
            String content = doc.getText();
            records.add(new RankingRecord(id, title, content));
        }
        return records;
    }

    /**
     * ドキュメントからタイトルを抽出.
     */
    private String extractTitle(Document doc) {
        Object title = doc.getMetadata().get("title");
        return title != null ? title.toString() : null;
    }

    /**
     * APIレスポンスから再ランク済みドキュメントリストを構築.
     */
    private List<Document> buildRerankedDocuments(List<Document> originals, RankingResponse response) {
        if (response == null || response.records() == null) {
            return List.of();
        }

        // 元ドキュメントをIDでマップ化
        Map<String, Document> docMap = new HashMap<>();
        for (int i = 0; i < originals.size(); i++) {
            Document doc = originals.get(i);
            String id = doc.getId() != null ? doc.getId() : String.valueOf(i);
            docMap.put(id, doc);
        }

        // スコア順にドキュメントを構築
        List<Document> rerankedDocs = new ArrayList<>();
        for (RankedRecord ranked : response.records()) {
            Document original = docMap.get(ranked.id());
            if (original != null) {
                Map<String, Object> newMetadata = new HashMap<>(original.getMetadata());
                newMetadata.put("rerank_score", ranked.score());
                newMetadata.put("rerank_provider", "vertex-ai");
                rerankedDocs.add(new Document(original.getId(), original.getText(), newMetadata));
            }
        }
        return rerankedDocs;
    }

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
