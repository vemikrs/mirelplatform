package jp.vemi.framework.exeption;

import java.util.List;

import jp.vemi.framework.types.MessageLevel;

/**
 * システム固有の汎用的な例外クラス。<br>
 *
 * @author nimazaka
 * @since 1.0
 */
public class MirelSystemException extends MessagingException {

    /**
     *
     */
    private static final long serialVersionUID = 6213061652415873160L;

    /**
     * {@link MessageLevel}
     */
    private MessageLevel level;

    /**
     * default constructor
     */
    public MirelSystemException() {
        super();
    }

    /**
     * 他の例外インスタンスから生成します。
     *
     * @param e
     *            例外
     */
    public MirelSystemException(Throwable e) {
        super(e);
        level = MessageLevel.NONE;
    }

    /**
     * 他の例外インスタンスから生成します。
     *
     * @param e
     *            例外
     */
    public MirelSystemException(String message, Throwable e) {
        super(message, e);
        level = MessageLevel.NONE;
    }

    /**
     * {@inheritDoc}
     */
    public MirelSystemException(List<String> msgs) {
        super(msgs);
    }
    /**
     * levelの取得
     *
     * @return メッセージレベル
     */
    public MessageLevel getLevel() {
        return this.level;
    }

}
