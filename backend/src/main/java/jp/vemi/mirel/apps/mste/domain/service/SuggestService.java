/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import jp.vemi.mirel.apps.mste.domain.dto.SuggestParameter;
import jp.vemi.mirel.apps.mste.domain.dto.SuggestResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * MSTEサジェスト .<br/>
 */
public interface SuggestService {

    /**
     * invoke.<br/>
     * @param parameter {@link ApiRequest<SuggestParameter>}
     * @return {@link ApiResponse<SuggestResult>}
     */
    public abstract ApiResponse<SuggestResult> invoke(ApiRequest<SuggestParameter> parameter);

}
