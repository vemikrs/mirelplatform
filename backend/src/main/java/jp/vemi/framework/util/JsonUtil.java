package jp.vemi.framework.util;

import static org.apache.commons.lang3.StringUtils.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.vemi.framework.exeption.MirelSystemException;

/**
 * Jsonのユーティリティtを提供するクラスです。<br>
 *
 * @author nimazaka
 *
 */
public class JsonUtil {

    /** ブランクJSON **/
    private static final String JSON_BLANK = "{}";

    public static boolean isArray(String json) {

        try {
            return newObjectMapper().readTree(defaultIfEmpty(json, JSON_BLANK)).isArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        }

    }

    public static <T> T parse(String json, TypeReference<T> clazz) {
        try {
            return newObjectMapper().readValue(
                    defaultIfEmpty(json, JSON_BLANK), clazz);
        } catch (JsonParseException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        } catch (JsonMappingException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        }
    }

    /**
     * パース。<br>
     * Json文字列から指定された変換先Dtoクラスのインスタンスを生成し返します。
     *
     * @param json
     *            Json文字列
     * @param clazz
     *            変換先Dtoクラス
     * @return Dto
     */
    public static <T> T parse(String json, Class<T> clazz) {
        try {
            return newObjectMapper().readValue(
                    defaultIfEmpty(json, JSON_BLANK), clazz);
        } catch (JsonParseException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        } catch (JsonMappingException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        }
    }

    /**
     * Json生成。<br>
     * Dtoの内容をJson文字列へ変換して返します。
     *
     * @param dto
     *            Dto
     * @return Json文字列
     */
    public static String toJson(Object dto) {
        ObjectMapper mapper = newObjectMapper();
        String json = null;
        try {
            try {
                json = new String(mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsBytes(dto), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new MirelSystemException(e);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        }

        return json;
    }

    public static <T> boolean isJson(String json, Class<T> clazz) {

        try {
            parse(defaultIfEmpty(json, JSON_BLANK), clazz);
        } catch (MirelSystemException e) {
            return false;
        } catch (Throwable e) {
            return false;
        }

        return true;

    }

    private static ObjectMapper newObjectMapper() {
        return new ObjectMapper();
    }
}
