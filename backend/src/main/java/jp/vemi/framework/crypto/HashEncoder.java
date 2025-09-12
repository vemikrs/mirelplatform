package jp.vemi.framework.crypto;

public interface HashEncoder {

    /**
     * エンコード
     *
     * @param rawPassword
     *            パスフレーズ
     * @param salt
     * @return エンコード済パスフレーズ
     */
    public abstract String encode(String passphrase, String salt);

    /**
     * 照会
     *
     * @param rawPassphrase
     * @param salt
     * @param encodedPassphrase
     * @return
     */
    public abstract boolean matches(String passphrase, String salt, String encodedPassphrase);
}
