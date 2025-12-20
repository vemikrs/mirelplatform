package jp.vemi.mirel.config;

import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class VertexAiFixConfig {

    @Bean
    public VertexAiEmbeddingConnectionDetails vertexAiEmbeddingConnectionDetails() {
        return new VertexAiEmbeddingConnectionDetails("dev-dummy-project", "us-central1", null, null);
    }
}
