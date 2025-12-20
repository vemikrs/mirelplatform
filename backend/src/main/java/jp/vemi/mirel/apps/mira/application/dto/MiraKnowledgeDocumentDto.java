/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.dto;

import java.time.LocalDateTime;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiraKnowledgeDocumentDto {
    private String id;
    private String fileId;
    private String fileName;
    private MiraVectorStore.Scope scope;
    private String tenantId;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
