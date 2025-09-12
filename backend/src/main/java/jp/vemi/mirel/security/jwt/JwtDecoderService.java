/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import java.util.Optional;
import java.util.Date;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jp.vemi.mirel.foundation.abst.dao.entity.TenantSystemMaster;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantSystemMasterRepository;

@Service
public class JwtDecoderService {

    @Autowired
    private TenantSystemMasterRepository tenantSystemMasterRepository;

    @Value("${jwt.secret:defaultSecret}")
    private String jwtSecret;
    
    private static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60 * 1000;

    public String generateToken(Authentication authentication) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(authentication.getName())
            .claim("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()))
            .issuedAt(new Date(System.currentTimeMillis()).toInstant())
            .expiresAt(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY).toInstant())
            .build();

        // JWKSourceの作成
        JWKSource<SecurityContext> jwks = new ImmutableSecret<>(
            jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
        
        // エンコーダーの作成
        JwtEncoder encoder = new NimbusJwtEncoder(jwks);

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public JwtDecoder getJwtDecoder(String tenantId) {
        Optional<TenantSystemMaster> tenant = tenantSystemMasterRepository.findByTenantIdAndKey(tenantId, "jwkSetUri");
        if (tenant.isPresent()) {
            return NimbusJwtDecoder.withJwkSetUri(tenant.get().getValue()).build();
        } else {
            // デコーダーの設定も同じ秘密鍵を使用
            SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            return NimbusJwtDecoder.withSecretKey(key).build();
        }
    }
}
