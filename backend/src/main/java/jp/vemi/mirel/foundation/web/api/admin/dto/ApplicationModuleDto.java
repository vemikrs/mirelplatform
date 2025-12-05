/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * アプリケーションモジュールDTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationModuleDto {
    /** モジュールID */
    private String id;
    /** モジュール名 */
    private String name;
    /** バージョン */
    private String version;
    /** ステータス (active, inactive, error) */
    private String status;
    /** 説明 */
    private String description;
    /** 最終更新日時 */
    private String lastUpdated;
}
