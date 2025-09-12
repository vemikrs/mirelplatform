/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * value text
 */
public class ValueText {

    /** 値 */
    public String value;

    /** テキスト */
    public String text;

    /**
     * default constructor. <br/>
     * 
     * @param value 値
     * @param text  テキスト
     */
    public ValueText(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
