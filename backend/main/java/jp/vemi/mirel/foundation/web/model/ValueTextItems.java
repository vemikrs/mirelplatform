/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * バリューテキストItems
 */
public class ValueTextItems {

    /** items */
    public List<ValueText> items = new ArrayList<>();

    /** default value */
    public String selected;

    /**
     * default constructor.
     */
    public ValueTextItems(){
    }

    /**
     * default constructor.
     */
    public ValueTextItems(List<ValueText> valueTexts) {
        items.addAll(valueTexts);
    }

    /**
     * default constructor.
     */
    public ValueTextItems(List<ValueText> valueTexts, String selected) {
        items.addAll(valueTexts);
        this.selected = selected;
    }

    /**
     * add item.<br/>
     * @param valueText
     */
    public void addItem(ValueText valueText) {
        this.items.add(valueText);
    }

    /**
     * add item.<br/>
     * @param value
     * @param text
     */
    public void addItem(String value, String text) {
        this.items.add(new ValueText(value, text));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
