/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ステンシル設定DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StencilConfigDto {
    /** ID */
    private String id;
    
    /** 名前 */
    private String name;
    
    /** カテゴリID */
    private String categoryId;
    
    /** カテゴリ名 */
    private String categoryName;
    
    /** シリアル番号 */
    private String serial;
    
    /** 最終更新日 */
    private String lastUpdate;
    
    /** 最終更新者 */
    private String lastUpdateUser;
    
    /** 説明 */
    private String description;
}
