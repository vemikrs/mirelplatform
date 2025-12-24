/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

/**
 * Mira RAG コンテキストビルダー.
 * <p>
 * 検索されたドキュメントリストから、LLMに渡すためのコンテキスト文字列を構築します。
 * 出典の明記に関する指示やフォーマットの統一を担います。
 * </p>
 */
@Component
public class MiraRagContextBuilder {

    /**
     * ドキュメントリストからコンテキスト文字列を構築します。
     *
     * @param documents
     *            検索されたドキュメントリスト
     * @return フォーマットされたコンテキスト文字列
     */
    public String buildContextString(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        String ragContext = documents.stream()
                .map(doc -> {
                    String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown Source");
                    return "Source: " + fileName + "\nContent:\n" + doc.getText();
                })
                .collect(Collectors.joining("\n\n---\n\n"));

        return "\n\n[Reference Knowledge]\n" +
                "Use the following information to answer the user's question. If you use information from the context, please cite the Source filename at the end of your answer.\n\n"
                + ragContext;
    }
}
