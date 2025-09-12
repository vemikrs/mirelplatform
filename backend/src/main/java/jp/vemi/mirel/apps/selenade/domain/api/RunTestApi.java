/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.domain.api;

import java.util.Map;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.selenade.domain.dto.RunTestParameter;
import jp.vemi.mirel.apps.selenade.domain.service.RunTestService;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * テスト実行.<br/>
 */
@Service
public class RunTestApi implements SelenadeApi {

    @Autowired
    protected RunTestService service;

    public ApiResponse<?> service(Map<String, Object> request) {
        Map<String, Object> parameterContent = forceCast(request.get("content"));

        ApiRequest<RunTestParameter> apireq = ApiRequest.<RunTestParameter>builder().model(
            RunTestParameter.builder().params(Lists.newArrayList(parameterContent)).build()).build();

        ApiResponse<?> response = service.invoke(apireq);
        return response;
    }

    /**
     * forceCast.<br/>
     * @param <T> Dest class.
     * @param object Src object.
     * @return Casted instance.
     */
    @SuppressWarnings("unchecked")
    private static <T> T forceCast(Object object) {
        return (T) object;
    }

}
