/*
 * Copyright(c) 2015-2025 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.evidencve;

public class EvidenceException extends RuntimeException {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * default constructor.
     *
     * @param arg
     *            message.
     */
    public EvidenceException(String message) {
        super(message);
    }

    public EvidenceException(Throwable e) {
        super(e);
    }

    public EvidenceException(String message, Throwable e) {
        super(message, e);
    }

}
