package jp.vemi.extension.function_resolver.function;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import jp.vemi.extension.function_resolver.api.ApiResolver;
import jp.vemi.extension.function_resolver.api.ApiResolverCondition;
import jp.vemi.extension.function_resolver.exception.FunctionException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Function {
        public String functionStatement;
        public String functionName;
        public Map<String, Object> args = new HashMap<>();
        public Object result;
    
        /** Double Quote. */
        protected static final String DOUBLE_QUOTE = "\"";
    
        /**
         * 実行引数の追加.<br/>
         * 
         * @param value
         */
        public synchronized void addArg(Object value) {
            addArg(Integer.toString(args.size()), value);
        }
    
        /**
         * 実行引数の追加.<br/>
         * 
         * @param key
         * @param value
         */
        public void addArg(String key, Object value) {
    
            rejectIfInvoked();
    
            if (null == value) {
                args.put(key, null);
                return;
            }
    
            if (value instanceof String || value instanceof StringBuilder) {
    
                String parsedValue = parseString(value.toString());
                args.put(key, parsedValue);
                return;
            }
    
            // others type
            if (value instanceof List
                || value instanceof Date
                || value instanceof Integer
                || value instanceof Long
                || value instanceof BigDecimal) {
                args.put(key, value);
                return;
            }
    
            throw new FunctionException("実行引数のTypeが解釈できません。" + value.getClass());
        }
    
        /**
         * 実行状態を管理する状態値.<br/>
         */
        protected boolean isInvoked;
    
        /**
         * 結果の取得.<br/>
         * <p>
         * 初回の呼び出し時に限り結果を取得します。
         * </p>
         * 
         * @return
         */
        public Object getResult() {
            if (false == isInvoked) {
                this.result = call(this);
                isInvoked = true;
            }
            return this.result;
        }
    
        /**
         * 既に実行されている場合、例外をスローします。<br/>
         */
        protected void rejectIfInvoked() {
            if (this.isInvoked) {
                throw new FunctionException("api allready invoked.");
            }
        }
    
        /**
         * parseString.<br/>
         *
         * @param value
         * @return
         */
        protected static String parseString(String value) {
    
            if (StringUtils.isEmpty(value)) {
                return StringUtils.EMPTY;
            }
    
            if (value.startsWith("\"")) {
                if (value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1);
                }
            }
    
            return value;
        }
    
        protected static Object call(Function function) {
    
            ApiResolverCondition condition = ApiResolverCondition.of(function.functionName);
            condition.putExtParam("api-args", function.args);
            return ApiResolver.getInstance().resolve(condition);
        }
}
