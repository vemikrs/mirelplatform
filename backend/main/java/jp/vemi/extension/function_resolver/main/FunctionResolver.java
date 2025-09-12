/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.main;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import jp.vemi.extension.function_resolver.exception.FunctionException;
import jp.vemi.extension.function_resolver.function.Function;
import jp.vemi.extension.function_resolver.function.Functions;

public class FunctionResolver {

    /**
     * Constructor.
     */
    private FunctionResolver() {
    }

    /**
     * 解決.<br/>
     * @param statement ステートメント
     * @return {@link Functions} Function.
     */
    public static Function resolveSingleFunction(String statement) {
        FunctionResolverCondition condition = FunctionResolverCondition.of(statement);
        Functions functions = FunctionResolverPrototype.invoke(condition);
        Optional<Function> optional = functions.stream().findFirst();
        if (optional.isPresent()) {
            return optional.get();
        } else {
            return null;
        }
    }


    /**
     * 解決.<br/>
     * @param statement ステートメント
     * @return {@link Functions} Function.
     */
    public static Functions resolve(String statement) {
        FunctionResolverCondition condition = FunctionResolverCondition.of(statement);
        return FunctionResolverPrototype.invoke(condition);
    }

    /**
     * 解決し即パースします。.<br/>
     * 
     * @param target
     *            文字列
     * @return 解決・パースされた文字列
     * @deprecated このAPIは削除されます。
     */
    @Deprecated
    public static String resolveAndParseImmediate(String target) {
        FunctionResolverCondition condition = FunctionResolverCondition.of(target);
        return resolveAndParseImmediate(condition);
    }

    /**
     * 解決し即パースします。.<br/>
     * 
     * @param condition
     *            コンテキスト
     * @return 解決・パースされた文字列
     * @deprecated このAPIは削除されます。
     */
    @Deprecated
    public static String resolveAndParseImmediate(FunctionResolverCondition condition) {
        String resolved = resolve(condition);
        return TemplateParser.parse(resolved, condition.binds);
    }

    /**
     * 解決.<br/>
     * 
     * @param condition
     *            コンテキスト
     * @return 解決された文字列
     * @deprecated このAPIは削除されます。
     */
    @Deprecated
    public static String resolve(FunctionResolverCondition condition) {
        Functions apis = resolveFunctions(condition);

        Map<String, Object> appendBinds = parse(apis);
        condition.binds.putAll(appendBinds);

        String result = condition.writer.toString();

        if (null != condition.writer) {
            try {
                condition.writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 関数解析.<br/>
     * 
     * @param condition
     *            コンテキスト
     * @return {@link Functions 複数の関数}
     * @deprecated このAPIは削除されます。
     */
    @Deprecated
    protected static Functions resolveFunctions(FunctionResolverCondition condition) {
        return FunctionResolverPrototype.invoke(condition);
    }

    /**
     * 解決.<br/>
     * 
     * @param apis
     *            {@link Functions}
     * @return 解決されたマップ
     * 
     */
    protected static Map<String, Object> parse(Functions apis) {
        if (ObjectUtils.isEmpty(apis)) {
            return new LinkedHashMap<>();
        }

        return apis.invoke();
    }

    /**
     * FunctionResolverPrototype.<br/>
     *
     */
    protected static class FunctionResolverPrototype {

        private FunctionResolverPrototype() {
        }

        /**
         * invoke.<br/>
         * 
         * @param condition
         *            コンテキスト
         * @return {@link Functions 複数の関数}
         */
        public static Functions invoke(FunctionResolverCondition condition) {
            try(Reader reader = new StringReader(condition.masterStatement)) {
                return statementEx(condition, reader);
            } catch (IOException e) {
                throw new FunctionException(e);
            }
        }

        /**
         * ステートメント単位の処理.<br/>
         *
         * @param condition
         *            コンテキスト
         * @param reader
         *            リーダー
         * @return {@link Functions 複数の関数}
         * @throws IOException
         */
        protected static Functions statementEx(FunctionResolverCondition condition,
                Reader reader)
            throws IOException {

            Functions apis = new Functions();
            Store store = Store.create();

            int c;
            while ((c = reader.read()) != -1) {
                if (c == '$') {
                    condition.writer.write(c);
                    c = reader.read();
                    if (c == '{') {
                        condition.writer.write(c);
                        c = reader.read();
                        if (c == '_') {
                            Function api = new Function();
                            StringBuilder fnStmtSb = new StringBuilder();
                            appendAsChar(fnStmtSb, c);
                            apis.add(apiEx(condition, reader, api, fnStmtSb, store));
                            condition.writer.write(api.functionStatement);
                            continue;
                        } else {
                            condition.writer.write(c);
                        }
                    }
                } else {
                    condition.writer.write(c);
                }
            }

            return apis;
        }

        /**
         * API単位の処理.</br>
         * 
         * @param condition
         *            コンテキスト
         * @param reader
         *            リーダー
         * @param fn
         *            関数
         * @param fnStmtSb
         *            ステートメント
         * @param store
         *            ストア
         * @return {@link Function 関数}
         * @throws IOException
         */
        protected static Function apiEx(FunctionResolverCondition condition, Reader reader,
                Function fn, StringBuilder fnStmtSb, Store store)
            throws IOException {

            StringBuilder fnNameSb = new StringBuilder();
            int c;

            while ((c = reader.read()) != -1) {

                // Statementは常に追加する。
                appendAsChar(fnStmtSb, c);

                // API名のSKIPの判定
                if (c == ' ') { // whitespace
                    continue;
                }

                if (c == '(') {
                    apiArgsEx(condition, reader, fn, fnStmtSb, store);
                    // Function 情報の確定
                    fn.functionName = fnNameSb.toString();
                    fn.functionStatement = store.getValue(fnStmtSb.toString());
                    break;
                }

                appendAsChar(fnNameSb, c);

            }

            return fn;
        }

        /**
         * API実行引数単位の処理.<br/>
         *
         * @param reader
         * @param api
         * @throws IOException
         */
        protected static void apiArgsEx(FunctionResolverCondition condition, Reader reader,
                Function api, StringBuilder fnStmtSb, Store store)
            throws IOException {
            StringBuilder fnArgSb = new StringBuilder();
            int c;
            boolean isQuarting = false; // quart発動中
            parent: while ((c = reader.read()) != -1) {

                if (c == '"') {
                    // ダブルクォートの発動状態を切り替える。
                    isQuarting = !isQuarting;
                    // reader.read();
                }

                appendAsChar(fnStmtSb, c);

                // 実行引数ステートメントの終了の判定
                if (c == ')') {
                    if (0 < fnArgSb.length()) {
                        // 実行引数を持つ場合のみ、追加
                        api.addArg(fnArgSb);
                    }
                    break;
                }

                // 実行引数ステートメントのセパレート判定
                if (c == ',') {
                    if(0 < fnArgSb.length()){
                        api.addArg(fnArgSb);
                    }
                    fnArgSb = new StringBuilder(); // for next argument
                    continue;
                }

                if (c == ' ') { // whitespace
                    continue;
                }

                // 入れ子処理
                /*
                 * 関数の入れ子は先行実装です。
                 */
                if (fnArgSb.length() == 0 && c == '$') {
                    c = reader.read();
                    if (c == '{') {

                        reader.mark(0);
                        c = reader.read();

                        // Function
                        if (c == '_') {
                            Function tapi = new Function();
                            apiEx(condition, reader, tapi, fnStmtSb, store);
                            api.addArg(tapi.getResult());

                            c = skipWhitespace(reader);

                            continue;
                        }

                        reader.reset();

                        // Bind
                        StringBuilder bindSb = new StringBuilder();
                        while ((c = reader.read()) != -1) {
                            // 終了の判定
                            if (c == '}') {
                                String key = bindSb.toString();
                                Object value = condition.binds.get(key);

                                fnStmtSb.setLength(fnStmtSb.length() - 1); // 直前の'$'をクリア
                                fnStmtSb.append(value);
                                fnArgSb.append(value);
                                continue parent;
                            }
                            appendAsChar(bindSb, c);
                        }
                    }

                }

                appendAsChar(fnArgSb, c);
            }

        }

        protected static int skipWhitespace(Reader reader) throws IOException {
            int c;
            while ((c = reader.read()) != -1) {
                if (c != ' ') {
                    return c;
                }
            }
            return -1;
        }

        /**
         * StringBuilderにint型のキャラクタをAppendするAPIです.<br/>
         * 
         * @param sbr
         * @param c
         */
        protected static void appendAsChar(StringBuilder sbr, int c) {
            sbr.append((char) c);
        }
    }

    /**
     * Store.<br/>
     * 
     * @author vemi/vemic.
     *
     */
    protected static class Store {

        /** map */
        protected Map<String, String> map = Maps.newLinkedHashMap();
        /** バリュー */
        protected Set<String> values = Sets.newLinkedHashSet();

        /**
         * create.<br/>
         * 
         * @return instance of {@link Store}
         */
        public static Store create() {
            return new Store();
        }

        /**
         * getValue.<br/>
         * 
         * @param key
         * @return Store value
         */
        public String getValue(String key) {

            if (this.map.containsKey(key)) {
                return this.map.get(key);
            }

            String uniq = getUniq();
            this.map.put(key, uniq);

            return uniq;
        }

        protected String getUniq() {

            while (true) {
                // Random文字列を生成
                String randomValue =
                    RandomStringUtils.randomAlphanumeric(getKeySize());

                // prefix & lowercase
                String value =
                    StringUtils.join(getPrefix(), lowerCase(randomValue));

                if (false == map.values().contains("value")) {
                    return value;
                }
            }
        }

        /**
         * 小文字に変換します。.<br/>
         * 
         * @param str
         *            対象の文字列
         * @return 変換された文字列
         */
        protected static String lowerCase(String str) {
            return StringUtils.lowerCase(str);
        }

        /**
         * プレフィックスを返します。.<br/>
         * 
         * @return プレフィックス
         */
        protected static String getPrefix() {
            return "_";
        }

        /**
         * storeで使用する文字の長さを返します。.<br/>
         * 
         * @return 文字の長さ
         */
        protected static int getKeySize() {
            return 5;
        }

    }


}
