package jp.vemi.framework.types;

/**
 * メッセージレベルの列挙です。
 *
 * @author nimazaka
 *
 */
public enum MessageLevel {
    /** 未定義 */
    NONE(0),
    /** 情報 */
    INFO(1),
    /** 警告 */
    WARN(2),
    /** エラー */
    ERROR(3),
    /** フェイタル */
    FATAL(4), ;

    private final int level;

    MessageLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }

}
