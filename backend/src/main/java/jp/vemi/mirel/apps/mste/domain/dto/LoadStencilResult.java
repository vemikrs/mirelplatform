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
 * ステンシル読込結果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadStencilResult {
    /** ステンシル設定 */
    private StencilConfigDto config;
    
    /** ファイル一覧 */
    private List<StencilFileDto> files;
    
    /** バージョン履歴 */
    private List<StencilVersionDto> versions;
}
