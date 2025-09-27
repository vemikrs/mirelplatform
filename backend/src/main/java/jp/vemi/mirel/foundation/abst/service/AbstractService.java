/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.service;

import jp.vemi.mirel.foundation.abst.dto.ServiceParameter;
import jp.vemi.mirel.foundation.abst.dto.ServiceResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * service
 */
public abstract interface AbstractService<PARAM extends ServiceParameter, RESULT extends ServiceResult> {

  /**
   * invoke.<br/>
   * 
   * @param parameter
   * @return result
   */
  public abstract ApiResponse<RESULT> invoke(ApiRequest<PARAM> parameter);

}
