/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ステンシル保存結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveStencilResult {
    /** 新しいシリアル番号 */
    private String newSerial;
    
    /** 成功フラグ */
    private boolean success;
}
