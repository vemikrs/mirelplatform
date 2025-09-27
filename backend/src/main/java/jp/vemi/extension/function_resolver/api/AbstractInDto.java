/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.api;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;

import jp.vemi.extension.function_resolver.exception.FunctionException;

/**
 * 抽象のInDto.<br/>
 * 
 * @author vemi/vemic.
 *
 */
public abstract class AbstractInDto {

    /**
     * argsを取得するAPI
     * 
     * @param condition
     * @return Map
     */
    protected static Map<Integer, Object> convertArgs(ApiResolverCondition condition) {

        Object apiArgs = condition.getExtParam("api-args");

        if (null == apiArgs) {
            return Maps.newHashMap();
        }

        @SuppressWarnings("unchecked")
        Map<Integer, Object> parsedApiArgs = (Map<Integer, Object>) apiArgs;
        return parsedApiArgs;
    }

    /**
     * isErrStopの場合、Validが作動します。.<br/>
     * 
     * @param condition
     * @return args.
     */
    public Map<Integer, Object> convertAndValidArgs(ApiResolverCondition condition) {
        Map<Integer, Object> args = convertArgs(condition);

        if (null != condition.getExtParam("isErrStop")) {
            int argsSize = getArgsSize();
            if (argsSize != args.size()) {
                throw new FunctionException(
                    new IllegalArgumentException(
                        "API:"
                            + getApiName()
                            + "のパラメータ数は"
                            + argsSize
                            + "件のところ、"
                            + args.size()
                            + "件でした。"));
            }
        }
        return args;
    }

    /**
     * Set変換.<br/>
     * 
     * @param object
     *            対象オブジェクト
     * @return 変換されたリスト
     */
    protected Set<String> convertToSet(Object object) {

        Set<String> row = Sets.newLinkedHashSet();

        if (null == object) {
            return row;
        }

        if (object instanceof String) {
            String objectStr = String.class.cast(object);
            if (false == StringUtils.isEmpty(objectStr)) {
                row.add(objectStr);
            }
            return row;
        }

        if (object instanceof Set) {
            return forceCast(object);
        }

        return null;

    }

    /**
     * API名.<br/>
     * 
     * @return API名
     */
    protected abstract String getApiName();

    /**
     * 引数サイズ.<br/>
     * 
     * @return
     */
    protected abstract int getArgsSize();


    /**
     * forceCast.<br/>
     * @param <T> Dest class.
     * @param object Src object.
     * @return Casted instance.
     */
    @SuppressWarnings("unchecked")
    private static <T> T forceCast(Object object) {
        return (T) object;
    }
}
