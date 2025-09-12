/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.api.plugin;

import java.util.Map;

import jp.vemi.extension.function_resolver.api.AbstractInDto;
import jp.vemi.extension.function_resolver.api.ApiPluginAbstract;
import jp.vemi.extension.function_resolver.api.ApiResolverCondition;
import jp.vemi.extension.function_resolver.dto.Api;

/**
 * .<br/>
 * 
 * @author vemi/vemic.
 */
public class LocatorXpathPlugin extends ApiPluginAbstract {

    protected static class Constants {
        static final String API_NAME = "xpath";
        static final Integer PARAM_SIZE = 1;
        private Constants() { /* nop */ }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean is(ApiResolverCondition condition) {
        return isHit(condition, Constants.API_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Api resolve(ApiResolverCondition condition) {
        Api api = new Api();
        api.setApiName(condition.getName());
        return api;
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
