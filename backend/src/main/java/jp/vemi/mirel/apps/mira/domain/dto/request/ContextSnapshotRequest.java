/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.apps.mira.domain.dto.request;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * コンテキストスナップショットリクエスト DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextSnapshotRequest {

    /** アプリケーションID（studio / workflow / admin 等） */
    private String appId;

    /** 画面ID */
    private String screenId;

    /** システムロール（ROLE_ADMIN / ROLE_USER） */
    private String systemRole;

    /** アプリケーションロール（SystemAdmin / Builder / Operator / Viewer） */
    private String appRole;

    /** 画面固有コンテキスト */
    private Map<String, Object> payload;
}
