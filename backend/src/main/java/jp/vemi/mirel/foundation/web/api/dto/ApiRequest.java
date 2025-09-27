/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.foundation.web.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * APIリクエストの共通クラスです .<br/>
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiRequest<T> {

    public T model;
}