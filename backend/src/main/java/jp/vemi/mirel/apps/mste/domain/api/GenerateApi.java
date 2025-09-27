/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.api;

import java.util.Map;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mste.domain.dto.GenerateParameter;
import jp.vemi.mirel.apps.mste.domain.service.GenerateService;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 *  .<br/>
 */
@Service
public class GenerateApi implements MsteApi {

    @Autowired
    protected GenerateService service;

    @Override
    public ApiResponse<?> service(Map<String, Object> request) {

        ApiRequest<GenerateParameter> apireq = ApiRequest.<GenerateParameter>builder().model(
            GenerateParameter.builder().params(Lists.newArrayList(request)).build()).build();

        ApiResponse<?> response = service.invoke(apireq);
        return response;
    }

}
