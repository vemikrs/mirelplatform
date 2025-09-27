/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import jp.vemi.mirel.apps.mste.domain.dto.ReloadStencilMasterParameter;
import jp.vemi.mirel.apps.mste.domain.dto.ReloadStencilMasterResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * MSTE 生成のサービスです。 .<br/>
 */
public interface ReloadStencilMasterService {

    /**
     * invoke.<br/>
     * @param parameter
     * @return result
     */
    public abstract ApiResponse<ReloadStencilMasterResult> invoke(ApiRequest<ReloadStencilMasterParameter> parameter);
}
