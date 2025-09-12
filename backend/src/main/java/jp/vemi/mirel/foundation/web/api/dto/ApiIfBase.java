/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.foundation.web.api.dto;

import java.util.Map;

/**
 * Web APIの基底インタフェースです。 .<br/>
 */
public interface ApiIfBase {

    public ApiResponse<?> service(Map<String, Object> request);
}