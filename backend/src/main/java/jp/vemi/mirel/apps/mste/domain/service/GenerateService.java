/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import jp.vemi.mirel.apps.mste.domain.dto.GenerateParameter;
import jp.vemi.mirel.apps.mste.domain.dto.GenerateResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * MSTE 生成のサービスです。 .<br/>
 */
public interface GenerateService {

    /**
     * invoke.<br/>
     * @param parameter
     * @return result
     */
    public abstract ApiResponse<GenerateResult> invoke(ApiRequest<GenerateParameter> parameter);
}
