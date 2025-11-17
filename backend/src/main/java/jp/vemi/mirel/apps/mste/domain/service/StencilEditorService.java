/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import jp.vemi.mirel.apps.mste.domain.dto.LoadStencilParameter;
import jp.vemi.mirel.apps.mste.domain.dto.LoadStencilResult;
import jp.vemi.mirel.apps.mste.domain.dto.SaveStencilParameter;
import jp.vemi.mirel.apps.mste.domain.dto.SaveStencilResult;
import jp.vemi.mirel.apps.mste.domain.dto.StencilVersionDto;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

import java.util.List;

/**
 * ステンシルエディタサービス
 */
public interface StencilEditorService {
    
    /**
     * ステンシル情報を読み込む
     * @param parameter リクエストパラメータ
     * @return ステンシル情報
     */
    ApiResponse<LoadStencilResult> loadStencil(ApiRequest<LoadStencilParameter> parameter);
    
    /**
     * ステンシルを保存する
     * @param parameter リクエストパラメータ
     * @return 保存結果
     */
    ApiResponse<SaveStencilResult> saveStencil(ApiRequest<SaveStencilParameter> parameter);
    
    /**
     * バージョン履歴を取得する
     * @param stencilId ステンシルID
     * @return バージョン履歴
     */
    ApiResponse<List<StencilVersionDto>> getVersionHistory(String stencilId);
}
