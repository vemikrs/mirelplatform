/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.function;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;


/**
 * Apis.<br/>
 *
 * @author vemi/vemic.
 *
 */
public class Functions extends ArrayList<Function> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * invoke.<br/>
     * 
     * @return {@link Map<String, Object>}
     */
    public Map<String, Object> invoke() {

        Map<String, Object> map = new LinkedHashMap<>();

        if (this.isEmpty()) {
            return map;
        }

        for (Function api : this) {
            String key = api.functionStatement;
            if (StringUtils.isEmpty(key)) {
                continue;
            }
            Object value = api.getResult();
            map.put(key, value);
        }

        return map;
    }

    /**
     * {@inheritDoc}<br/>
     * <p>
     * 整形します。
     * </p>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Function api : this) {
            sb.append("API名：");
            sb.append(api.functionName);
            sb.append("\n");
            sb.append("　パラメータ：");

            for (Object value : api.args.values()) {
                sb.append(value);
                sb.append(",");
            }

            if (false == api.args.values().isEmpty()) {
                sb.setLength(sb.length() - 1);
            }

            sb.append("\r\n");
            sb.append("　APIステートメント：");
            sb.append(api.functionStatement);
            sb.append("\r\n");
        }

        return sb.toString();
    }
}
