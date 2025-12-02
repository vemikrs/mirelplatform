/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ステンシル読込パラメータ
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoadStencilParameter {
    /** ステンシルID */
    private String stencilId;
    
    /** シリアル番号 */
    private String serial;
}
