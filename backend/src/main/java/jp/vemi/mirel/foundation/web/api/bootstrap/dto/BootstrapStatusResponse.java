/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.bootstrap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bootstrapステータスレスポンス.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapStatusResponse {

    /**
     * Bootstrap可能かどうか
     * true: まだ初期管理者が作成されていない
     * false: 既に初期管理者が作成済み
     */
    private boolean available;

    /**
     * メッセージ
     */
    private String message;
}
