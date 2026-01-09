/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import jakarta.annotation.PostConstruct;
import jp.vemi.framework.crypto.AesCryptoService;
import jp.vemi.mirel.foundation.domain.entity.SystemSecurityKey;
import jp.vemi.mirel.foundation.domain.entity.SystemSecurityKey.KeyStatus;
import jp.vemi.mirel.foundation.domain.repository.SystemSecurityKeyRepository;
import lombok.Getter;

/**
 * JWT署名鍵管理サービス.
 * <p>
 * RSA鍵ペアの生成、DB保存、キャッシュ管理を担当。
 * 起動時にDBから鍵をロードし、なければ新規生成する。
 * </p>
 */
@Service
public class JwtKeyManagerService {

    private static final Logger logger = LoggerFactory.getLogger(JwtKeyManagerService.class);
    private static final String USE_PURPOSE = "ACCESS_TOKEN_SIGN";
    private static final int RSA_KEY_SIZE = 2048;

    @Autowired
    private SystemSecurityKeyRepository keyRepository;

    @Autowired
    private AesCryptoService cryptoService;

    /** 現在の署名用鍵 */
    @Getter
    private RSAKey currentSigningKey;

    /** 検証用鍵キャッシュ (kid -> RSAKey) */
    private final Map<String, RSAKey> validationKeyCache = new ConcurrentHashMap<>();

    /** JWKSet（公開鍵セット） */
    @Getter
    private JWKSet jwkSet;

    /**
     * 初期化：DBから鍵をロードまたは新規生成.
     */
    @PostConstruct
    @Transactional
    public void init() {
        if (!cryptoService.isAvailable()) {
            logger.warn("[JwtKeyManager] CryptoService not available. " +
                    "RS256 key management disabled. Falling back to HS256.");
            return;
        }

        try {
            // DBから現在の署名鍵を取得
            Optional<SystemSecurityKey> currentKeyOpt = keyRepository.findCurrentSigningKey();

            if (currentKeyOpt.isEmpty()) {
                logger.info("[JwtKeyManager] No current signing key found. Generating new RSA key pair.");
                generateAndStoreNewKey();
            } else {
                logger.info("[JwtKeyManager] Loading existing signing key: {}",
                        currentKeyOpt.get().getKeyId());
            }

            // 検証用鍵をキャッシュにロード
            reloadKeyCache();

            logger.info("[JwtKeyManager] Initialized with {} validation keys",
                    validationKeyCache.size());
        } catch (Exception e) {
            logger.error("[JwtKeyManager] Failed to initialize: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize JWT key manager", e);
        }
    }

    /**
     * 新しいRSA鍵ペアを生成してDBに保存.
     */
    @Transactional
    public void generateAndStoreNewKey() {
        try {
            // RSA鍵ペア生成
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(RSA_KEY_SIZE);
            KeyPair keyPair = keyGen.generateKeyPair();

            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            // kid生成
            String keyId = "mirel-" + UUID.randomUUID().toString().substring(0, 8);

            // 公開鍵をPEM形式で保存
            String publicKeyPem = toPemFormat(publicKey.getEncoded(), "PUBLIC KEY");

            // 秘密鍵を暗号化して保存
            String privateKeyPem = toPemFormat(privateKey.getEncoded(), "PRIVATE KEY");
            String privateKeyEncrypted = cryptoService.encrypt(privateKeyPem);

            // エンティティ作成
            SystemSecurityKey entity = SystemSecurityKey.builder()
                    .keyId(keyId)
                    .algorithm("RS256")
                    .publicKey(publicKeyPem)
                    .privateKeyEncrypted(privateKeyEncrypted)
                    .status(KeyStatus.CURRENT)
                    .usePurpose(USE_PURPOSE)
                    .activatedAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            keyRepository.save(entity);
            logger.info("[JwtKeyManager] New RSA key pair generated and stored: {}", keyId);

            // キャッシュ更新
            reloadKeyCache();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA algorithm not available", e);
        }
    }

    /**
     * 鍵キャッシュを再ロード.
     */
    public void reloadKeyCache() {
        validationKeyCache.clear();
        currentSigningKey = null;

        List<SystemSecurityKey> validKeys = keyRepository.findValidSigningKeys();

        for (SystemSecurityKey key : validKeys) {
            try {
                RSAKey rsaKey = loadRsaKey(key);
                validationKeyCache.put(key.getKeyId(), rsaKey);

                if (key.getStatus() == KeyStatus.CURRENT) {
                    currentSigningKey = rsaKey;
                }
            } catch (Exception e) {
                logger.error("[JwtKeyManager] Failed to load key {}: {}",
                        key.getKeyId(), e.getMessage());
            }
        }

        // JWKSet（公開鍵のみ）を構築
        List<com.nimbusds.jose.jwk.JWK> publicKeys = validationKeyCache.values().stream()
                .map(k -> (com.nimbusds.jose.jwk.JWK) k.toPublicJWK())
                .toList();
        jwkSet = new JWKSet(publicKeys);
    }

    /**
     * kid でRSA公開鍵を取得.
     */
    public Optional<RSAKey> getPublicKeyByKid(String kid) {
        RSAKey key = validationKeyCache.get(kid);
        return Optional.ofNullable(key).map(RSAKey::toPublicJWK);
    }

    /**
     * RS256が有効かどうか.
     */
    public boolean isRs256Available() {
        return currentSigningKey != null;
    }

    // ===== Private Methods =====

    private RSAKey loadRsaKey(SystemSecurityKey entity) throws Exception {
        // 秘密鍵を復号
        String privateKeyPem = cryptoService.decrypt(entity.getPrivateKeyEncrypted());

        // PEMからキーペアを復元
        RSAPublicKey publicKey = fromPemPublicKey(entity.getPublicKey());
        RSAPrivateKey privateKey = fromPemPrivateKey(privateKeyPem);

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(entity.getKeyId())
                .build();
    }

    private String toPemFormat(byte[] encoded, String type) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----";
    }

    private RSAPublicKey fromPemPublicKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(base64);
        java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(encoded);
        return (RSAPublicKey) java.security.KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private RSAPrivateKey fromPemPrivateKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] encoded = Base64.getDecoder().decode(base64);
        java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) java.security.KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}
