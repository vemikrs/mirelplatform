/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.vemi.mirel.apps.mste.domain.dto.UploadStencilParameter;
import jp.vemi.mirel.apps.mste.domain.dto.UploadStencilResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * {@link UploadStencilService} の具象です。
 */
@Service
@Transactional
public class UploadStencilServiceImp implements UploadStencilService {

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<UploadStencilResult> invoke(ApiRequest<UploadStencilParameter> parameter) {

        ApiResponse<UploadStencilResult> resp = ApiResponse.<UploadStencilResult>builder().build();

        // TODO concrete.
        resp.setData(UploadStencilResult.builder().build());

        return resp;

    }
}
