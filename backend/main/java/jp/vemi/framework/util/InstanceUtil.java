package jp.vemi.framework.util;

import java.lang.reflect.InvocationTargetException;

public class InstanceUtil {

    @SuppressWarnings("unchecked")
    public static <T> T forceCast(Object object) {
        return (T) object;
    }

    public static <T> T newInstance(Class<T> clazz) {
        T result = null;
        try {
            result = clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return result;
    }
}
