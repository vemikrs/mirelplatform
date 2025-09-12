/*
 * Copyright(c) 2015-2021 vemi.
 */
package jp.vemi.extension.function_resolver.api;

import org.apache.commons.lang3.StringUtils;


/**
 * API関数の抽象です。.<br/>
 * <p>
 * サブクラスはSingletonで初期化されます。必ずスレッドセーフに実装してください。
 * </p>
 * 
 * @author vemi/vemic.
 *
 */
public abstract class ApiPluginAbstract {

    /**
     * 対象を判定します。.<br/>
     * 
     * @param condition
     *            コンテキスト
     * @return 対象の場合true、それ以外の場合false
     */
    public abstract boolean is(ApiResolverCondition condition);

    /**
     * 優先度.<br/>
     * 
     * @return 優先度
     */
    /*
     * 未実装です。効果はありません。
     */
    public int priority() {
        return 0;
    }

    /**
     * 解決.<br/>
     * 
     * @param condition
     * @return 結果
     */
    public abstract Object resolve(ApiResolverCondition condition);

    /**
     * Hit判定.<br/>
     * 
     * @param condition
     *            コンテキスト
     * @param apiName
     *            比較するAPI名
     * @return ret
     */
    protected static boolean isHit(ApiResolverCondition condition, String apiName) {

        if (null == condition) {
            return false;
        }

        String content = condition.getInput();

        return StringUtils.equals(apiName, content);

    }

    /**
     * コンテナの状態から単体モードを判定します。.<br/>
     * 
     * @return 単体デバッグモード
     */
    protected static boolean is4UnitDebug() {
        return false;
    }

}
