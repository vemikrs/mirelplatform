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
     * Development/Mock EmbeddingModel to bypass Vertex AI credentials.
     */
    @Bean
    @org.springframework.context.annotation.Primary
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "mira.ai.provider", havingValue = "mock")
    public EmbeddingModel mockEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public org.springframework.ai.embedding.EmbeddingResponse call(
                    org.springframework.ai.embedding.EmbeddingRequest request) {
                java.util.List<org.springframework.ai.embedding.Embedding> embeddings = new java.util.ArrayList<>();
                for (int i = 0; i < request.getInstructions().size(); i++) {
                    embeddings.add(new org.springframework.ai.embedding.Embedding(generateVector(768), i));
                }
                return new org.springframework.ai.embedding.EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return generateVector(768);
            }

            // Implement missing default methods to avoid compilation warnings/errors if any
            public float[] embed(String document) {
                return generateVector(768);
            }

            public java.util.List<float[]> embed(java.util.List<String> texts) {
                java.util.List<float[]> result = new java.util.ArrayList<>();
                for (String text : texts) {
                    result.add(embed(text));
                }
                return result;
            }

            private float[] generateVector(int dimension) {
                float[] vector = new float[dimension];
                java.util.Arrays.fill(vector, 1.0f); // Constant vector for perfect matching
                return vector;
            }
        };
    }
}
