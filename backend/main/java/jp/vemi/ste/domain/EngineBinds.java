/*
 * Copyright(c) 2018 mirelplatform.
 */
package jp.vemi.ste.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * TemplateEngineのバインドです.<br/>
 *
 * @author mirelplatform
 *
 */
public class EngineBinds extends HashMap<String, Object> {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3408187331049010443L;

    /**
     * default constructor.
     */
    public EngineBinds() {
    }

    /**
     * default constructor.
     */
    public EngineBinds(Map<String, Object> map) {
        super();
        this.putAll(map);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object key) {
        return super.get(key);
    }
}
