package jp.vemi.framework.util;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jp.vemi.framework.exeption.MirelSystemException;

import com.google.common.collect.Maps;

/**
 * モデルオブジェクトに関連するユーティリティクラスです。<br/>
 *
 * @author mirelplatform
 *
 */
public class ModelUtil {

    /**
     * construtor.
     */
    private ModelUtil() {
    }

    public static <T> Map<String, Object> createMap(final T model) {
        Map<String, Object> map = Maps.newHashMap();

        List<Field> fields = getFields(model);
        for (Field field : fields) {

            String key = field.getName();
            Object value = getFieldValue(field, model);
            map.put(key, value);
        }

        return map;
    }

    /**
     * フィールド取得.<br/>
     *
     * @param model
     * @return
     */
    public static <T> List<Field> getFields(final T model) {
        Class<?> clazz = model.getClass();
        List<Field> fileds = Arrays.asList(clazz.getFields());
        return fileds;
    }

    /**
     * フィールド値取得.<br/>
     *
     * @param field フィールド
     * @param model モデル
     * @return モデルのフィールド値
     */
    public static <T> Object getFieldValue(final Field field, final T model) {
        try {
            return field.get(model);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new MirelSystemException(e);
        }
    }
}
