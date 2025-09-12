/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.domain.service;

import javax.annotation.Generated;

import jp.vemi.mirel.apps.selenade.domain.dto.RunTestParameter;
import jp.vemi.mirel.apps.selenade.domain.dto.RunTestResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * {@link RunTestService テスト実行}.<br/>
 */
@Generated(value = {"jp.vemi.ste.domain.engine.TemplateEngineProcessor"}, comments = "Generated from /mirel/service:191207A")
public interface RunTestService {

    /**
     * invoke.<br/>
     * @param parameter
     * @return result
     */
    public abstract ApiResponse<RunTestResult> invoke(ApiRequest<RunTestParameter> parameter);
}
