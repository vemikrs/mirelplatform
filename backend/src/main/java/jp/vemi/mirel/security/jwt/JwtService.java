/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.KeyOperation;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import java.util.List;

import jp.vemi.mirel.config.properties.AuthProperties;

@Service
@ConditionalOnProperty(name = "auth.method", havingValue = "jwt", matchIfMissing = true)
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Autowired
    private AuthProperties authProperties;

    private JwtEncoder encoder;
    @lombok.Getter
    private JwtDecoder decoder;

    @PostConstruct
    public void init() {
        if (!authProperties.getJwt().isEnabled()) {
            // JWT無効時はスキップ
            return;
        }

        // 秘密鍵の取得
        String secretKey = authProperties.getJwt().getSecret();
        if (secretKey == null || secretKey.length() < 32) {
             throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }
        byte[] keyBytes = secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // JWKの作成（HMAC署名用の必須属性を全て設定）
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(keyBytes)
                .keyID("mirel-jwt-key")
                .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256)
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .keyOperations(java.util.Collections.singleton(com.nimbusds.jose.jwk.KeyOperation.SIGN))
                .build();
        
        logger.info("JWK created: keyID={}, algorithm={}, keyUse={}, keyType={}", 
            jwk.getKeyID(), jwk.getAlgorithm(), jwk.getKeyUse(), jwk.getKeyType());
        
        JWKSet jwkSet = new JWKSet(jwk);
        
        logger.info("JWKSet created with {} keys", jwkSet.getKeys().size());
        
        // カスタムJWKSource: JWKSelectorを無視して常にJWKを返す
        JWKSource<SecurityContext> jwkSource = (jwkSelector, securityContext) -> {
            // logger.debug("JWKSource called. Selector: {}. Returning all {} keys unconditionally", 
            //    jwkSelector, jwkSet.getKeys().size());
            return jwkSet.getKeys(); // Selectorの条件を無視して全てのJWKを返す
        };

        // エンコーダーとデコーダーの設定
        this.encoder = new NimbusJwtEncoder(jwkSource);
        
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.decoder = NimbusJwtDecoder.withSecretKey(key).build();
    }

    public String generateToken(Authentication authentication) {
        if (!authProperties.getJwt().isEnabled() || encoder == null) {
            throw new IllegalStateException("JWT is disabled. Enable auth.jwt.enabled in application.yml");
        }

        Instant now = Instant.now();
        long expiry = authProperties.getJwt().getExpiration();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiry))
                .subject(authentication.getName())
                .claim("roles", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .build();

        // HS256アルゴリズムを明示的に指定
        JwsHeader header = JwsHeader.with(() -> "HS256").build();
        
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
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
}