/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import jakarta.annotation.PostConstruct;
import jp.vemi.mirel.config.properties.AuthProperties;

/**
 * JWT署名・検証サービス.
 * <p>
 * RS256（推奨）またはHS256でJWTを生成・検証する。
 * RS256が利用可能な場合（JwtKeyManagerServiceが設定済み）はRS256を使用し、
 * そうでなければ従来のHS256にフォールバックする。
 * </p>
 */
@Service
@ConditionalOnProperty(name = "auth.method", havingValue = "jwt", matchIfMissing = true)
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private AuthProperties authProperties;

    @Autowired(required = false)
    private JwtKeyManagerService keyManagerService;

    private JwtEncoder encoder;
    @lombok.Getter
    private JwtDecoder decoder;

    private boolean useRs256 = false;
    private String currentKeyId;

    @PostConstruct
    public void init() {
        if (!authProperties.getJwt().isEnabled()) {
            logger.info("[JwtService] JWT disabled");
            return;
        }

        // RS256が利用可能か確認
        if (keyManagerService != null && keyManagerService.isRs256Available()) {
            initRs256();
        } else {
            initHs256();
        }
    }

    /**
     * RS256モードで初期化.
     */
    private void initRs256() {
        RSAKey signingKey = keyManagerService.getCurrentSigningKey();
        currentKeyId = signingKey.getKeyID();

        logger.info("[JwtService] Initializing RS256 mode with key: {}", currentKeyId);

        // JWKSourceを設定（署名時に現在の鍵を返す）
        JWKSource<SecurityContext> jwkSource = (jwkSelector, securityContext) -> {
            return List.of(keyManagerService.getCurrentSigningKey());
        };

        this.encoder = new NimbusJwtEncoder(jwkSource);

        // カスタムデコーダーを使用（kidで公開鍵をルックアップ）
        this.decoder = buildRs256Decoder();

        this.useRs256 = true;
        logger.info("[JwtService] RS256 mode initialized successfully");
    }

    /**
     * RS256用デコーダーを構築.
     */
    private JwtDecoder buildRs256Decoder() {
        return token -> {
            try {
                // トークンからkidを抽出してデコード
                com.nimbusds.jwt.SignedJWT signedJWT = com.nimbusds.jwt.SignedJWT.parse(token);
                String kid = signedJWT.getHeader().getKeyID();

                RSAKey rsaKey = keyManagerService.getPublicKeyByKid(kid)
                        .orElseThrow(() -> new JwtException("Unknown key ID: " + kid));

                NimbusJwtDecoder kidDecoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
                return kidDecoder.decode(token);
            } catch (Exception e) {
                throw new JwtException("Failed to decode JWT: " + e.getMessage(), e);
            }
        };
    }

    /**
     * HS256モードで初期化（フォールバック）.
     */
    private void initHs256() {
        String secretKey = authProperties.getJwt().getSecret();
        if (secretKey == null || secretKey.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }

        logger.info("[JwtService] Initializing HS256 mode (fallback)");

        byte[] keyBytes = secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        OctetSequenceKey jwk = new OctetSequenceKey.Builder(keyBytes)
                .keyID("mirel-jwt-key")
                .algorithm(JWSAlgorithm.HS256)
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .keyOperations(java.util.Collections.singleton(com.nimbusds.jose.jwk.KeyOperation.SIGN))
                .build();

        JWKSet jwkSet = new JWKSet(jwk);
        JWKSource<SecurityContext> jwkSource = (jwkSelector, securityContext) -> jwkSet.getKeys();

        this.encoder = new NimbusJwtEncoder(jwkSource);

        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.decoder = NimbusJwtDecoder.withSecretKey(key).build();

        this.useRs256 = false;
        this.currentKeyId = "mirel-jwt-key";
        logger.info("[JwtService] HS256 mode initialized");
    }

    /**
     * 認証情報からJWTトークンを生成.
     */
    public String generateToken(Authentication authentication) {
        if (!authProperties.getJwt().isEnabled() || encoder == null) {
            throw new IllegalStateException("JWT is disabled");
        }

        Instant now = Instant.now();
        long expiry = authProperties.getJwt().getExpiration();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mirel")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(authentication.getName())
                .claim("roles", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .build();

        return encodeWithHeader(claims);
    }

    /**
     * CLI向けJWTトークンを生成.
     */
    public String generateCliToken(String userId, String scope, String clientId, List<String> roles) {
        if (!authProperties.getJwt().isEnabled() || encoder == null) {
            throw new IllegalStateException("JWT is disabled");
        }

        Instant now = Instant.now();
        long cliExpiry = 86400; // 24時間

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mirel")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(cliExpiry))
                .subject(userId)
                .claim("roles", roles)
                .claim("scope", scope != null ? scope : "")
                .claim("client_id", clientId)
                .build();

        return encodeWithHeader(claims);
    }

    /**
     * クレームをエンコードしてJWTを生成.
     */
    private String encodeWithHeader(JwtClaimsSet claims) {
        JwsHeader.Builder headerBuilder;

        if (useRs256) {
            headerBuilder = JwsHeader.with(() -> "RS256")
                    .keyId(currentKeyId);
        } else {
            headerBuilder = JwsHeader.with(() -> "HS256");
        }

        return encoder.encode(JwtEncoderParameters.from(headerBuilder.build(), claims)).getTokenValue();
    }

    public Jwt decodeToken(String token) {
        return this.decoder.decode(token);
    }

    public String getUsernameFromToken(String token) {
        return this.decoder.decode(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwt jwt = this.decoder.decode(token);
            return jwt.getExpiresAt().isAfter(Instant.now());
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * RS256モードかどうか.
     */
    public boolean isRs256Mode() {
        return useRs256;
    }
}