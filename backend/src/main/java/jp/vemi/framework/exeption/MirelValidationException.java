package jp.vemi.framework.exeption;

/**
 * ユーザー入力や状態のバリデーションエラー (400 Bad Request 相当).
 * 重複チェック、形式チェック違反など。
 */
public class MirelValidationException extends MirelApplicationException {

    private static final long serialVersionUID = 1L;

    public MirelValidationException(String message) {
        super(message, null);
    }

    public MirelValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
