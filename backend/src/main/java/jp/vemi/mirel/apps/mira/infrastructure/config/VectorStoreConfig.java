/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ベクターストア設定クラス.
 * 
 * <p>
 * Spring AI autoconfigure を使用せず、Mira独自の設定で
 * EmbeddingModel と VectorStore を手動構成します。
 * </p>
 * 
 * <h3>設計方針</h3>
 * <ul>
 * <li>Spring AI autoconfigure は全て無効化</li>
 * <li>mira.ai.* プロパティのみで設定完結</li>
 * <li>マルチプロバイダー環境でのBeanコンフリクトを回避</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {

    private final MiraAiProperties properties;

    /**
     * Primary EmbeddingModel Bean.
     * 
     * <p>
     * mira.ai.provider に応じて実装を切替:
     * </p>
     * <ul>
     * <li>"vertex-ai-gemini" かつ設定あり → VertexAiTextEmbeddingModel</li>
     * <li>それ以外 → MockEmbeddingModel（定数ベクトル）</li>
     * </ul>
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        String provider = properties.getProvider();

        if (MiraAiProperties.PROVIDER_VERTEX_AI_GEMINI.equals(provider) && isVertexAiConfigured()) {
            return createVertexAiEmbeddingModel();
        }

        log.info("[VectorStoreConfig] Using MockEmbeddingModel (provider={})", provider);
        return createMockEmbeddingModel();
    }

    /**
     * VectorStore Bean.
     * 
     * <p>
     * PgVectorStore を mira.ai.vector-store.* 設定から手動構成します。
     * </p>
     */
    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        var config = properties.getVectorStore();

        log.info("[VectorStoreConfig] Creating PgVectorStore (table={}, dimensions={}, distance={}, index={})",
                config.getTableName(), config.getDimensions(), config.getDistanceType(), config.getIndexType());

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(config.getDimensions())
                .distanceType(parseDistanceType(config.getDistanceType()))
                .indexType(parseIndexType(config.getIndexType()))
                .initializeSchema(config.isInitializeSchema())
                .vectorTableName(config.getTableName())
                .build();
    }

    private boolean isVertexAiConfigured() {
        var vertexConfig = properties.getVertexAi();
        return vertexConfig != null
                && vertexConfig.getProjectId() != null
                && !vertexConfig.getProjectId().isEmpty()
                && !"dev-dummy-project".equals(vertexConfig.getProjectId());
    }

    private EmbeddingModel createVertexAiEmbeddingModel() {
        var config = properties.getVertexAi();
        log.info("[VectorStoreConfig] Creating VertexAiTextEmbeddingModel (project={}, location={})",
                config.getProjectId(), config.getLocation());

        try {
            // Dynamic import to avoid compile-time dependency if not available
            var connectionDetailsClass = Class.forName(
                    "org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails");
            var optionsClass = Class.forName(
                    "org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingOptions");
            var modelClass = Class.forName(
                    "org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingModel");

            // Build connection details
            var builderMethod = connectionDetailsClass.getMethod("builder");
            var connBuilder = builderMethod.invoke(null);
            var projectIdMethod = connBuilder.getClass().getMethod("projectId", String.class);
            projectIdMethod.invoke(connBuilder, config.getProjectId());
            var locationMethod = connBuilder.getClass().getMethod("location", String.class);
            locationMethod.invoke(connBuilder, config.getLocation());
            var buildConnMethod = connBuilder.getClass().getMethod("build");
            var connectionDetails = buildConnMethod.invoke(connBuilder);

            // Build options
            var optBuilderMethod = optionsClass.getMethod("builder");
            var optBuilder = optBuilderMethod.invoke(null);
            var modelMethod = optBuilder.getClass().getMethod("model", String.class);
            modelMethod.invoke(optBuilder, "text-embedding-004");
            var buildOptMethod = optBuilder.getClass().getMethod("build");
            var options = buildOptMethod.invoke(optBuilder);

            // Create model
            var constructor = modelClass.getConstructor(connectionDetailsClass, optionsClass);
            return (EmbeddingModel) constructor.newInstance(connectionDetails, options);

        } catch (Exception e) {
            log.warn("[VectorStoreConfig] Failed to create VertexAiTextEmbeddingModel, falling back to mock: {}",
                    e.getMessage());
            return createMockEmbeddingModel();
        }
    }

    private EmbeddingModel createMockEmbeddingModel() {
        final int dimension = properties.getVectorStore().getDimensions();

        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                for (int i = 0; i < request.getInstructions().size(); i++) {
                    embeddings.add(new Embedding(generateVector(dimension), i));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return generateVector(dimension);
            }

            private float[] generateVector(int dim) {
                float[] vector = new float[dim];
                Arrays.fill(vector, 1.0f);
                return vector;
            }
        };
    }

    private PgVectorStore.PgDistanceType parseDistanceType(String distanceType) {
        try {
            return PgVectorStore.PgDistanceType.valueOf(distanceType);
        } catch (IllegalArgumentException e) {
            log.warn("[VectorStoreConfig] Unknown distanceType '{}', using COSINE_DISTANCE", distanceType);
            return PgVectorStore.PgDistanceType.COSINE_DISTANCE;
        }
    }

    private PgVectorStore.PgIndexType parseIndexType(String indexType) {
        try {
            return PgVectorStore.PgIndexType.valueOf(indexType);
        } catch (IllegalArgumentException e) {
            log.warn("[VectorStoreConfig] Unknown indexType '{}', using HNSW", indexType);
            return PgVectorStore.PgIndexType.HNSW;
        }
    }
}
