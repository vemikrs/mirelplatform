/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ステンシルファイルDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StencilFileDto {
    /** ファイルパス */
    private String path;
    
    /** ファイル名 */
    private String name;
    
    /** ファイル内容 */
    private String content;
    
    /** ファイル種別 */
    private String type;
    
    /** 言語モード */
    private String language;
    
    /** 編集可能フラグ */
    private boolean isEditable;
}
