package jp.vemi.framework.crypto.algos;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.lang3.StringUtils;

import jp.vemi.framework.crypto.HashEncoder;

public class DefaultJavaPassphraseEncoder implements HashEncoder {

    /** ALGORITHM */
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    /** ストレッチング回数 */
    private static final Integer STRETCH_COUNT = 100;
    /** 鍵の長さ */
    private static final Integer HASH_LEN = 1024;

    @Override
    public String encode(String passphrase, String salt) {

        char[] pwChrAry = passphrase.toCharArray();
        byte[] hashedSalt = getHashedSalt(salt);

        PBEKeySpec spec = new PBEKeySpec(pwChrAry, hashedSalt, STRETCH_COUNT, HASH_LEN);

        SecretKeyFactory skf = null;

        try {
            skf = SecretKeyFactory.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        SecretKey secretKey = null;
        try {
            secretKey = skf.generateSecret(spec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
        byte[] passByteAry = secretKey.getEncoded();

        StringBuilder sb = new StringBuilder(64);
        for (byte b : passByteAry) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Override
    public boolean matches(String passphrase, String salt, String encodedPassphrase) {
        return matches(encode(passphrase, salt), encodedPassphrase);
    }

    private boolean matches(String arg0, String arg1) {
        if (StringUtils.isEmpty(arg0))
            return false;
        return (arg0.equals(arg1)) ? true : false;
    }

    private static byte[] getHashedSalt(String salt) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        messageDigest.update(salt.getBytes());
        return messageDigest.digest();
    }

}
