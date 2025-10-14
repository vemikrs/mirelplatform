/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.api;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
        Map<String, Object> content = InstanceUtil.forceCast(request.get("content"));

        String stencilCategory = (String)content.get("stencilCategory");
        if (StringUtils.isEmpty(stencilCategory)) {
            stencilCategory = (String)content.get("stencilCategoy"); // Typo互換対応;
        }
        String stencilCd = (String)content.get("stencilCanonicalName");
        String serialNo = (String)content.get("serialNo");
        Boolean isInitialLoad = content.containsKey("isInitialLoad") ? false : (Boolean)content.get("isInitialLoad");

        ApiRequest<SuggestParameter> apiRequest = ApiRequest.<SuggestParameter>builder()
            .model(
                SuggestParameter
                .builder()
                .stencilCategory(stencilCategory)
                .stencilCd(stencilCd)
                .serialNo(serialNo)
                .isInitialLoad(isInitialLoad)
            .build())
        .build();

        ApiResponse<SuggestResult> response = service.invoke(apiRequest);
        return response;
    }

}
