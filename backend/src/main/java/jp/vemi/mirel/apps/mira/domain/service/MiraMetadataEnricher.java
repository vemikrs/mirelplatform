/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mira メタデータエンリッチャー.
 * <p>
 * ドキュメントに静的および動的なメタデータ（ラベル、想定質問など）を付与します。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiraMetadataEnricher {

    private final AiProviderFactory aiProviderFactory;

    /**
     * ドキュメントリストにメタデータを付与します。
     * 
     * @param documents
     *            ドキュメントリスト
     * @param contextInfo
     *            コンテキスト情報 (fileId, fileName, tenantId, etc.)
     */
    public void enrich(List<Document> documents, Map<String, Object> contextInfo) {
        log.info("Enriching {} documents...", documents.size());

        for (Document doc : documents) {
            // 1. Static Metadata Injection
            doc.getMetadata().putAll(contextInfo);
            doc.getMetadata().put("created_at", java.time.Instant.now().toString());
            doc.getMetadata().put("document_type", determineDocumentType(doc));

            // 2. Dynamic Metadata Injection (Generated Questions)
            // TODO: Implement LLM call for generated questions.
            // Enabling this synchronously for all chunks can be very slow.
            // For Phase 1, we will skip or implement a lightweight version/placeholder.
            // Future: Implement async enrichment or batch processing.
            // generateQuestions(doc);
        }
    }

    private String determineDocumentType(Document doc) {
        String fileName = (String) doc.getMetadata().getOrDefault("fileName", "unknown");
        if (fileName.endsWith(".md"))
            return "documentation";
        if (fileName.endsWith(".java") || fileName.endsWith(".ts"))
            return "source_code";
        if (fileName.endsWith(".pdf"))
            return "manual";
        return "general";
    }

    private void generateQuestions(Document doc) {
        try {
            // Generating questions synchronously for every chunk is expensive.
            // In a real production scenario, this should be offloaded to an async job
            // queue.
            // For now, we will implement the logic but only execute if explicitly enabled
            // (e.g. via system property or if we decide to enable for small batches).
            // Currently keeping it disabled by default in this MVP to prevent timeout on
            // large file ingestion.

            // jp.vemi.mirel.apps.mira.infrastructure.ai.AiProviderClient client =
            // aiProviderFactory.getProvider().orElse(null);
            // if (client == null) return;

            // String chunkText = doc.getText();
            // // Limit context to avoid token overflow
            // if (chunkText.length() > 1000) chunkText = chunkText.substring(0, 1000);

            // String prompt = "以下のテキストの内容を問う短い質問を3つ作成してください。質問のみを箇条書きで出力してください。\n\nテキスト:\n"
            // + chunkText;

            // jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest request =
            // jp.vemi.mirel.apps.mira.infrastructure.ai.AiRequest.builder().text(prompt).build();
            // jp.vemi.mirel.apps.mira.infrastructure.ai.AiResponse response =
            // client.chat(request);

            // if (!response.hasError() && response.getResult() != null) {
            // doc.getMetadata().put("generated_questions",
            // response.getResult().getOutput().getContent());
            // }

        } catch (Exception e) {
            log.warn("Failed to generate questions for document chunk", e);
        }
    }
}
