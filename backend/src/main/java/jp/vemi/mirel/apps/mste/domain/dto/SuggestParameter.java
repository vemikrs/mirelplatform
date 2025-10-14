/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

@lombok.Data
@lombok.AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@lombok.NoArgsConstructor
@lombok.Builder
public class SuggestParameter {

    /** ステンシル種類 */
    public String stencilCategory;

    /** ステンシル */
    public String stencilCd;

    /** シリアル */
    public String serialNo;

    /** 初期ロードフラグ */
    public boolean isInitialLoad;
}
