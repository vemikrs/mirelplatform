/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.api;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import jp.vemi.extension.function_resolver.exception.FunctionException;

/**
 * APIリゾルバ.<br/>
 * 
 * @author vemi/vemic.
 *
 */
public class ApiResolver {

    /**
     * plugins.
     */
    protected static Set<ApiPluginAbstract> plugins =
        new HashSet<>();
    static {
        /*
         * pluginsに手動で追加してください。
         */
        plugins.add(new jp.vemi.extension.function_resolver.api.plugin.DummyApiPlugin());
        plugins.add(new jp.vemi.extension.function_resolver.api.plugin.LocatorXpathPlugin());
    }

    /**
     * do not use default constructor.
     */
    protected ApiResolver() {
    }

    public static ApiResolver getInstance() {
        return new ApiResolver();
    }

    /**
     * {@inheritDoc}
     */
    public Object resolve(ApiResolverCondition condition) {
        return this.resolve(condition, plugins);
    }

        /**
     * 解決処理（共通）.<br/>
     * 
     * @param input
     *            引数
     * @param plugins
     *            プラグイン
     * @return 解決結果
     */
    protected Object resolve(ApiResolverCondition condition,
            Set<ApiPluginAbstract> plugins) {

        if (null == condition) {
            throw new FunctionException(
                new IllegalArgumentException("コンテキストがありません。"));
        }

        if (CollectionUtils.isEmpty(plugins)) {
            throw new FunctionException("プラグインがありません。");
        }

        Object output = null;
        boolean isHit = false;
        for (ApiPluginAbstract plugin : plugins) {
            if (false == plugin.is(condition)) {
                continue;
            }
            output = plugin.resolve(condition);
            isHit = true;
            break;
        }

        if (null != condition.getExtParam("isErrStop") && false == isHit) {
            throw new FunctionException(
                "Hitするプラグインがありません。:" + condition.getName()); // TODO:例外処理
        }

        return output;
    }

}
