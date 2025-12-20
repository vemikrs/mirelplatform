/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dao.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiraVectorStore {

    private String id;

    /** チャンク本文 */
    private String content;

    /** メタデータ (JSON) */
    private String metadata;

    /** スコープ */
    private Scope scope;

    /** テナントID */
    private String tenantId;

    /** ユーザーID */
    private String userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum Scope {
        SYSTEM, TENANT, USER
    }
}
