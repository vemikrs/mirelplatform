/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.main;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

public class FunctionResolverCondition {

        /**
     * default constructor.
     */
    private FunctionResolverCondition() {
    }

    public String masterStatement;

    /** 一次パースステートメント */
    public StringWriter writer;

    /** バインド */
    public Map<String, Object> binds;

    /**
     * of.
     * 
     * @param masterStatement
     * @param binds
     * @return instance of condition.
     */
    public static FunctionResolverCondition of(String masterStatement,
            Map<String, Object> binds) {

        FunctionResolverCondition inDto = new FunctionResolverCondition();
        inDto.masterStatement =
            StringUtils.defaultIfEmpty(masterStatement, StringUtils.EMPTY);
        inDto.writer = new StringWriter();
        inDto.binds = binds;

        
        return inDto;
    }

    /**
     * of.
     * 
     * @param masterStatement
     * @return instance of condition.
     */
    public static FunctionResolverCondition of(String masterStatement) {
        Map<String, Object> defaultMap = new LinkedHashMap<>();
        return of(masterStatement, defaultMap);
    }

    /**
     * closeWriter.<br/>
     */
    public void closeWriter() {
        if (null != this.writer) {
            try {
                this.writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}