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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Service
public class JwtService {

    @Autowired
    private JwtKeyGenerator keyGenerator;

    private JwtEncoder encoder;
    @lombok.Getter
    private JwtDecoder decoder;

    @PostConstruct
    public void init() {
        // 秘密鍵の生成
        String secretKey = keyGenerator.generateSecretKey();
        SecretKey key = new SecretKeySpec(
            Base64.getDecoder().decode(secretKey),
            "HmacSHA256"
        );

        // JWKSourceの作成（デコーダー用の鍵と同じものを使用）
        JWKSource<SecurityContext> jwks = new ImmutableSecret<>(key);

        // エンコーダーとデコーダーの設定
        this.encoder = new NimbusJwtEncoder(jwks);
        this.decoder = NimbusJwtDecoder.withSecretKey(key).build();
    }

    public String generateToken(Authentication authentication) {
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