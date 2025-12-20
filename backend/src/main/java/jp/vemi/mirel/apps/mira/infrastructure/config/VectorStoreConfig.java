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

    /**
     * Vector Store Configuration.
     * <p>
     * This configuration class relies on Spring AI AutoConfiguration for setting up
     * EmbeddingModel and VectorStore beans. Manual bean definitions have been removed
     * in favor of Spring Boot's automatic configuration based on properties.
     * </p>
     * <p>
     * Migration note: Previously, beans were manually defined here. Now they are
     * automatically configured via Spring AI Starter dependencies and application properties.
     * The pgvector extension is enabled via schema.sql with spring.sql.init.mode=always.
     * </p>
     */
}
