/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Service
@ConditionalOnProperty(name = "auth.jwt.enabled", havingValue = "true", matchIfMissing = false)
public class JwtService {

    @Value("${auth.jwt.enabled:false}")
    private boolean jwtEnabled;

    @Autowired
    private JwtKeyGenerator keyGenerator;

    private JwtEncoder encoder;
    @lombok.Getter
    private JwtDecoder decoder;

    @PostConstruct
    public void init() {
        if (!jwtEnabled) {
            // JWT無効時はスキップ
            return;
        }

        // 秘密鍵の生成
        String secretKey = keyGenerator.generateSecretKey();
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);

        // JWKの作成（use=sig を明示）
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(keyBytes)
                .keyID("mirel-jwt-key")
                .algorithm(com.nimbusds.jose.JWSAlgorithm.HS256)
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .build();
        
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));

        // エンコーダーとデコーダーの設定
        this.encoder = new NimbusJwtEncoder(jwks);
        
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.decoder = NimbusJwtDecoder.withSecretKey(key).build();
    }

    public String generateToken(Authentication authentication) {
        if (!jwtEnabled || encoder == null) {
            throw new IllegalStateException("JWT is disabled. Enable auth.jwt.enabled in application.yml");
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(authentication.getName())
                .claim("roles", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
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