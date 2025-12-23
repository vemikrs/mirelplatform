/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderFactory;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderClient;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest;
import jp.vemi.mirel.apps.mira.infrastructure.ai.AiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira Query Transform Service.
 * <p>
 * Provides query transformation capabilities like HyDE (Hypothetical Document
 * Embeddings).
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraQueryTransformService {

    private final AiProviderFactory aiProviderFactory;

    /**
     * Generates a hypothetical document based on the user query (HyDE).
     *
     * @param query
     *            User query
     * @return Hypothetical document text
     */
    public String transformToHypotheticalDocument(String query) {
        log.info("Generating HyDE for query: {}", query);

        try {
            // Use default provider (or specific tenant context if available in future)
            // TODO: Pass tenantId dynamically if possible. For now, use default/system
            // context.
            AiProviderClient client = aiProviderFactory.getProvider();

            if (client == null) {
                log.warn("No AI provider available for HyDE. Returning original query.");
                return query;
            }

            // Prompt engineering for Japanese HyDE
            String prompt = "次の質問に対して、事実に基づいた的確な回答のドラフトを作成してください。回答は簡潔に、検索クエリとして機能するようにキーワードを含めて記述してください。\n\n質問: "
                    + query;

            java.util.List<AiRequest.Message> messages = java.util.List.of(AiRequest.Message.user(prompt));
            AiRequest request = AiRequest.builder()
                    .messages(messages)
                    .build();

            AiResponse response = client.chat(request);

            if (response.hasError() || !response.isSuccess()) {
                log.warn("HyDE generation error: {}", response.getErrorMessage());
                return query;
            }

            String hypotheticalDoc = response.getContent();
            log.debug("HyDE generated: {}", hypotheticalDoc);
            return hypotheticalDoc;

        } catch (Exception e) {
            log.warn("HyDE generation failed, falling back to original query", e);
            return query;
        }
    }
}
