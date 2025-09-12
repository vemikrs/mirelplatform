package jp.vemi.framework.exeption;

import java.util.List;

import jp.vemi.framework.types.MessageLevel;

/**
 * アプリケーションレベル固有の汎用的な例外クラス。<br>
 *
 * @author nimazaka
 * @since 1.0
 */
public class MirelApplicationException extends MessagingException {

    /**
     *
     */
    private static final long serialVersionUID = 395314433169351776L;

    /**
     * {@link MessageLevel}
     */
    private MessageLevel level;

    /**
     * default constructor
     */
    public MirelApplicationException() {
        super();
    }

    /**
     * 他の例外インスタンスから生成します。
     *
     * @param e
     *            例外
     */
    public MirelApplicationException(Throwable e) {
        super(e);
        e.printStackTrace();
        level = MessageLevel.NONE;
    }

    /**
     * 他の例外インスタンスから生成します。
     *
     * @param e
     *            例外
     */
    public MirelApplicationException(String message, Throwable e) {
        super(message, e);
        level = MessageLevel.NONE;
    }

    /**
     * {@inheritDoc}
     */
    public MirelApplicationException(List<String> msgs) {
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
