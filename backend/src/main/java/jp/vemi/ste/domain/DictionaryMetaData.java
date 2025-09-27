package jp.vemi.ste.domain;

import jp.vemi.framework.util.StringConvertionUtil;
import jp.vemi.ste.domain.context.SteContext;

/**
 * 辞書情報のタデータ.<br/>
 * @author h_kurosawa
 *
 */
public class DictionaryMetaData {

    /** STEコンテキスト */
    protected SteContext ctx;

    public DictionaryMetaData(SteContext ctx) {
        this.ctx = ctx;
    }

    /**
     * アッパーキャメルケース
     * @param string 文字レス
     * @return
     */
    public String toUcc(String string) {
        return StringConvertionUtil.toUpperCamelCase(string);
    }

    /**
     * ローワーキャメルケース
     * @param string 文字レス
     * @return
     */
    public String toLcc(String string) {
        return StringConvertionUtil.toLowerCamelCase(string);
    }

    /**
     * 機能名.<br/>
     * @return 機能名
     */
    public String getFeatureName() {
        return ctx.getFeatureName();
    }
}
