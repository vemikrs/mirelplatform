/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ステンシル保存パラメータ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveStencilParameter {
    /** ステンシルID */
    private String stencilId;
    
    /** シリアル番号 */
    private String serial;
    
    /** ステンシル設定 */
    private StencilConfigDto config;
    
    /** ファイル一覧 */
    private List<StencilFileDto> files;
    
    /** コミットメッセージ */
    private String message;
}
