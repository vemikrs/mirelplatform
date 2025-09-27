/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.api;

import java.util.Map;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.apps.mste.domain.dto.ReloadStencilMasterParameter;
import jp.vemi.mirel.apps.mste.domain.service.ReloadStencilMasterService;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * ステンシルマスタの更新.<br/>
 */
@Service
public class ReloadStencilMasterApi implements MsteApi{

    @Autowired
    protected ReloadStencilMasterService service;

    public ApiResponse<?> service(Map<String, Object> request) {

        ApiRequest<ReloadStencilMasterParameter> apireq = ApiRequest.<ReloadStencilMasterParameter>builder().model(
            ReloadStencilMasterParameter.builder().params(Lists.newArrayList(request)).build()).build();

        ApiResponse<?> response = service.invoke(apireq);
        return response;
    }

}
