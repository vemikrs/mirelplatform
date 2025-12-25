/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.application.dto;

import jp.vemi.mirel.apps.mira.domain.dao.entity.MiraVectorStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiraKnowledgeSearchRequest {

    /** Search query */
    private String query;

    /** Search scope (SYSTEM, TENANT, USER) */
    @Builder.Default
    private MiraVectorStore.Scope scope = MiraVectorStore.Scope.USER;

    /** Target Tenant ID (for debugging as another tenant) */
    private String targetTenantId;

    /** Target User ID (for debugging as another user) */
    private String targetUserId;

    /** Similarity threshold (0.0 - 1.0) */
    @Builder.Default
    private Double threshold = 0.0;

    /** Top K results */
    @Builder.Default
    private Integer topK = 5;
}
