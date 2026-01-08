/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.framework.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * AES-256-GCM暗号化サービス.
 * <p>
 * マスターキー(KEK)を使用してデータ(DEK)を暗号化/復号する。
 * JWT署名鍵のDB内暗号化などに使用。
 * </p>
 * 
 * <h2>セキュリティ仕様</h2>
 * <ul>
 * <li>アルゴリズム: AES-256-GCM</li>
 * <li>認証タグ長: 128bit</li>
 * <li>IV: 12bytes（ランダム生成、暗号文に付加）</li>
 * <li>出力形式: Base64(IV + CipherText + AuthTag)</li>
 * </ul>
 */
@Slf4j
@Service
public class AesCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int MIN_KEY_LENGTH = 32;

    @Value("${mirel.security.master-key:}")
    private String masterKeyBase64;

    private SecretKey masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 初期化時にマスターキーを検証.
     */
    @PostConstruct
    public void init() {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank()) {
            log.warn("[AesCryptoService] MIREL_MASTER_KEY not configured. " +
                    "Encryption features will not be available.");
            return;
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
            if (keyBytes.length < MIN_KEY_LENGTH) {
                throw new IllegalArgumentException(
                        "Master key must be at least " + MIN_KEY_LENGTH + " bytes (256 bits)");
            }
            // 先頭32バイトを使用（256bit）
            byte[] aesKeyBytes = new byte[32];
            System.arraycopy(keyBytes, 0, aesKeyBytes, 0, 32);
            this.masterKey = new SecretKeySpec(aesKeyBytes, "AES");
            log.info("[AesCryptoService] Master key initialized successfully");
        } catch (IllegalArgumentException e) {
            log.error("[AesCryptoService] Invalid master key format: {}", e.getMessage());
            throw new IllegalStateException("Failed to initialize master key", e);
        }
    }

    /**
     * マスターキーが設定されているか確認.
     *
     * @return マスターキーが利用可能な場合true
     */
    public boolean isAvailable() {
        return masterKey != null;
    }

    /**
     * 平文を暗号化してBase64エンコードした文字列を返す.
     *
     * @param plainText
     *            平文
     * @return Base64エンコードされた暗号文（IV + CipherText + AuthTag）
     * @throws IllegalStateException
     *             マスターキー未設定の場合
     */
    public String encrypt(String plainText) {
        if (!isAvailable()) {
            throw new IllegalStateException("Master key is not configured");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + CipherText を連結
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Base64エンコードされた暗号文を復号して平文を返す.
     *
     * @param cipherTextBase64
     *            Base64エンコードされた暗号文
     * @return 復号された平文
     * @throws IllegalStateException
     *             マスターキー未設定の場合
     */
    public String decrypt(String cipherTextBase64) {
        if (!isAvailable()) {
            throw new IllegalStateException("Master key is not configured");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
