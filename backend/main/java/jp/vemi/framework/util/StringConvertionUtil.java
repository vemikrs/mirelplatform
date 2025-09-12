package jp.vemi.framework.util;

import com.google.common.base.CaseFormat;

public class StringConvertionUtil {

    /**
     * ucc
     * @param str
     * @return
     */
    public static String toUpperCamelCase(String str) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, str);
    }

    /**
     * lcc
     * @param str
     * @return
     */
    public static String toLowerCamelCase(String str) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, str);
    }
}
