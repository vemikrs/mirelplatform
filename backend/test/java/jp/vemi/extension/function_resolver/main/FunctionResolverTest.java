package jp.vemi.extension.function_resolver.main;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Maps;

import jp.vemi.extension.function_resolver.function.Functions;

/**
 * テストクラス.<br/>
 *
 * @author vemi/vemic.
 *
 */
public class FunctionResolverTest {

    // private static String sentence1 =
    //     "${_dummyGreeting()}、 ${_dummyUserName('U100')} さん。${purposeName} を行うには、{_dummyPurposeProcess(purposeId, 'ボタン押下')} を行ってください。";

    @Test
    public final void aaa1() {
        // String statement = sentence1;

        Map<String, Object > binds = defaultBinds();

        // String result =
        //         FunctionResolver.resolveAndParseImmediate(
        //             FunctionResolverCondition.of(statement, binds));

        // log(result);
        log(binds);
    }

    protected Map<String, Object> execute(String arg) {
        Map<String, Object> bind = Maps.newHashMap();
        return execute(arg, bind);
    }

    protected Map<String, Object> execute(String arg,
            Map<String, Object> binds) {
        // FunctionResolverCondition condition = FunctionResolverCondition.of(arg, binds);
        // Functions apis = FunctionResolver.resolveFunctions(condition);
        // log(apis);
        // Map<String, Object> result = apis.invoke();
        // return result;
        return null;
    }

    protected static Map<String, Object> defaultBinds() {
        Map<String, Object> binds = Maps.newHashMap();
        binds.put("tranBind1", "A123");
        binds.put("alreadyObject", "★already-object!★");
        binds.put("tranBind2", 3);
        binds.put("registerDeptCd", "10000");
        binds.put("targetDate", parseDate("20190430", "yyyyMMdd"));
        return binds;
    }

    protected static void log(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.print("key:");
            System.out.print(entry.getKey());
            System.out.print("/value:");
            System.out.println(entry.getValue());
        }
    }

    protected static void log(Functions apis) {
        log(apis.toString());
    }

    protected static void log(String value) {
        System.out.println(value);
    }

    private static Date parseDate(String date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
