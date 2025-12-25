/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * グローバルコンテキスト.
 * <p>
 * チャンク分割時に失われる文書全体の文脈（ファイル名、カテゴリ、要約等）を保持し、
 * 各チャンクの先頭に注入するために使用します。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalContext {

    /** ファイル名 */
    private String fileName;

    /** ドキュメントカテゴリ (documentation, source_code, manual, etc.) */
    private String category;

    /** LLMまたは手動で設定された要約 */
    private String summary;

    /** 手動注釈（解説文）- 施策3で追加 */
    private String description;

    /**
     * チャンク先頭に注入するグローバルプレフィックスを構築します。
     *
     * @return 構築されたプレフィックス文字列
     */
    public String buildPrefix() {
        StringBuilder sb = new StringBuilder();

        if (fileName != null && !fileName.isBlank()) {
            sb.append("[Document: ").append(fileName).append("]\n");
        }

        if (category != null && !category.isBlank()) {
            sb.append("[Category: ").append(category).append("]\n");
        }

        if (description != null && !description.isBlank()) {
            sb.append("[Description: ").append(description).append("]\n");
        }

        if (summary != null && !summary.isBlank()) {
            sb.append("[Summary: ").append(summary).append("]\n");
        }

        return sb.toString();
    }
}
