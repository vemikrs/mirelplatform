/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.api.plugin;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import jp.vemi.extension.function_resolver.api.AbstractInDto;
import jp.vemi.extension.function_resolver.api.ApiPluginAbstract;
import jp.vemi.extension.function_resolver.api.ApiResolverCondition;

/**
 * .<br/>
 * 
 * @author vemi/vemic.
 */
public class DummyApiPlugin extends ApiPluginAbstract {

    protected static class Constants {
        public final static String API_NAME = "dummy";
        public final static List<String> APIS =
            Lists.newArrayList("abc", "def", "ghi", "jkl", "mno", "pqr");
        public final static Integer PARAM_SIZE = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean is(ApiResolverCondition condition) {
        for (String apiName : Constants.APIS) {
            if (isHit(condition, apiName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolve(ApiResolverCondition condition) {
        return "Dummy:" + condition.getName();
    }

    /**
     * 引数Dto.<br/>
     * 
     * @author vemi/vemic.
     *
     */
    protected static class InDto extends AbstractInDto {

        /**
         * default constructor.<br/>
         * 
         * @param ctx
         */
        public InDto(ApiResolverCondition ctx) {
            @SuppressWarnings("unused")
            Map<Integer, Object> args = convertAndValidArgs(ctx);
        }

        /** arg1 */
        public String arg1;

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getApiName() {
            return Constants.API_NAME;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected int getArgsSize() {
            return Constants.PARAM_SIZE;
        }
    }
}
