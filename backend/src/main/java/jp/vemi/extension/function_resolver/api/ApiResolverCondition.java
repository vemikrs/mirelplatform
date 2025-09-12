/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.api;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * 文字列をパースするコンテキスト.<br/>
 * 
 * @author vemi/vemic.
 *
 */
public class ApiResolverCondition {

    /**
     * default constructor.
     */
    private ApiResolverCondition() {
    }

    
    /**
     * input.
     */
    private String input;

    /**
     * 拡張パラメータ.
     */
    private Map<String, Object> extParams = Maps.newLinkedHashMap();

    /**
     * default setter.
     */
    public void setInput(String input) {
        this.input = input;
    }

    /**
     * default getter.
     */
    public String getInput() {
        return this.input;
    }

    /**
     * putExtParams.
     * 
     * @param entries
     */
    public void putExtParam(Map<String, Object> entries) {
        this.extParams.putAll(entries);
    }

    /**
     * putExtParam.
     * 
     * @param entry
     */
    public void putExtParam(Map.Entry<String, Object> entry) {
        this.putExtParam(entry.getKey(), entry.getValue());
    }

    /**
     * putExtParam.
     * 
     * @param key
     * @param value
     */
    public void putExtParam(String key, Object value) {
        this.extParams.put(key, value);
    }

    /**
     * getExtParam.
     * 
     * @param key
     * @return value.
     */
    public Object getExtParam(String key) {
        return extParams.get(key);
    }

    /**
     * remove.
     * 
     * @param key
     */
    public void removeExtParam(String key) {
        this.extParams.remove(key);
    }

    /**
     * of.
     * 
     * @param content
     * @return instance of Condition.
     */
    public static ApiResolverCondition of(String content) {

        ApiResolverCondition condition = new ApiResolverCondition();
        condition.setInput(content);
        return condition;
    }

    public ApiResolverCondition putExtParams(Map<String, Object> params) {
        putExtParam(params);
        return this;
    }

    public String getName() {
        return this.getInput();
    }
}
