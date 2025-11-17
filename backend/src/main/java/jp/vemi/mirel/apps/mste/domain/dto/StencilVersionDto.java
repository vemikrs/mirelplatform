/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ステンシルバージョンDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StencilVersionDto {
    /** シリアル番号 */
    private String serial;
    
    /** 作成日時 */
    private String createdAt;
    
    /** 作成者 */
    private String createdBy;
    
    /** アクティブフラグ */
    private boolean isActive;
    
    /** 変更内容 */
    private String changes;
}
