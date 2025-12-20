/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.infrastructure.config;

import javax.sql.DataSource;

import org.springframework.ai.embedding.EmbeddingModel;
// import org.springframework.ai.vertexai.gemini.VertexAiGeminiEmbeddingModel;
// import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingModel; // New guess
// import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingOptions; // New guess

// import org.springframework.ai.vectorstore.pgvector.PgVectorStore; // Updated package
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

// import com.google.cloud.vertexai.VertexAI;

import lombok.extern.slf4j.Slf4j;

/**
 * ベクターストア設定クラス.
 * <p>
 * PGVector の設定と起動時チェックを行います。
 * </p>
 */
@Slf4j
@Configuration
public class VectorStoreConfig {

    // Manual configuration removed.
    // pgvector extension is now enabled via schema.sql and
    // spring.sql.init.mode=always

    // Manual configuration removed to rely on Spring AI Starters
    // (AutoConfiguration)
    /*
     * @Bean
     * public EmbeddingModel embeddingModel() {
     * // Vertex AI Embedding Model の構築
     * // TODO: AIProviderFactory と連携して他プロバイダ対応を行う（現在はVertex AI固定）
     * var config = miraAiProperties.getVertexAi();
     * 
     * if (config.getProjectId() == null || config.getProjectId().isEmpty()) {
     * log.warn("Vertex AI Project ID is missing. EmbeddingModel will not work.");
     * // ダミーまたは例外送出するモデルを返すのが望ましいが、一旦null取扱注意
     * return null;
     * }
     * 
     * VertexAI vertexAi = new VertexAI(config.getProjectId(),
     * config.getLocation());
     * 
     * VertexAiGeminiEmbeddingOptions options =
     * VertexAiGeminiEmbeddingOptions.builder()
     * .withModel("text-embedding-004") // or specific model from properties
     * .build();
     * 
     * return new VertexAiGeminiEmbeddingModel(vertexAi, options);
     * }
     * 
     * @Bean
     * public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel
     * embeddingModel) {
     * if (embeddingModel == null) {
     * log.warn("EmbeddingModel is null. VectorStore will not be initialized.");
     * return null;
     * }
     * return new PgVectorStore(jdbcTemplate, embeddingModel);
     * }
     */
}
