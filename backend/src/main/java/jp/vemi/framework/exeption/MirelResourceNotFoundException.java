package jp.vemi.framework.exeption;

import jp.vemi.framework.types.MessageLevel;

/**
 * リソースが見つからない場合にスローされる例外 (404 Not Found 相当).
 */
public class MirelResourceNotFoundException extends MirelApplicationException {

    private static final long serialVersionUID = 1L;

    public MirelResourceNotFoundException(String message) {
        super(message, null);
    }

    public MirelResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
