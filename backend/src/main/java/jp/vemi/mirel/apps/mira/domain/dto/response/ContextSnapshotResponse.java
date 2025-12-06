/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * コンテキストスナップショットレスポンス DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextSnapshotResponse {

    /** スナップショットID */
    private String snapshotId;

    /** 作成日時 */
    private LocalDateTime createdAt;
}
