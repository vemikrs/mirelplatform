package jp.vemi.framework.exeption;

/**
 * 割り当て制限やレートリミットを超過した場合の例外 (429 Too Many Requests 相当).
 */
public class MirelQuotaExceededException extends MirelApplicationException {

    private static final long serialVersionUID = 1L;

    public MirelQuotaExceededException(String message) {
        super(message, null);
    }

    public MirelQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
