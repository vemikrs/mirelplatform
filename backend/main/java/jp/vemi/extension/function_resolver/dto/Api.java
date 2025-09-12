/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.dto;

import java.util.Map;

import com.google.common.collect.Maps;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Api.<br/>
 */
@Getter
@Setter
@NoArgsConstructor
public class Api {
    /** 分類 */
    String classification;
    /** API名 */
    String apiName;
    /** パラメータ */
    Map<String, Object> parameter = Maps.newLinkedHashMap();

}
