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

    /** ワイルドカード(*)指定時に最初の要素を自動選択するか */
    public boolean selectFirstIfWildcard;
}
