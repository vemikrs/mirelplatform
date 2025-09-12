/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.vemi.framework.util.InstanceUtil;
import jp.vemi.mirel.apps.mste.domain.dto.SuggestParameter;
import jp.vemi.mirel.apps.mste.domain.dto.SuggestResult;
import jp.vemi.mirel.apps.mste.domain.service.SuggestService;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * .<br/>
 */
@Service
public class SuggestApi implements MsteApi {

    @Autowired
    protected SuggestService service;

    @Override
    public ApiResponse<SuggestResult> service(Map<String, Object> request) {
        SuggestParameter parameter = new SuggestParameter();
        Map<String, Object> content = InstanceUtil.forceCast(request.get("content"));

        parameter.stencilCategory = (String)content.get("stencilCategoy");
        parameter.stencilCd = (String)content.get("stencilCanonicalName");
        parameter.serialNo = (String)content.get("serialNo");

        ApiRequest<SuggestParameter> apiRequest = new ApiRequest<>();
        apiRequest.setModel(parameter);

        ApiResponse<SuggestResult> response = service.invoke(apiRequest);
        return response;
    }

}
