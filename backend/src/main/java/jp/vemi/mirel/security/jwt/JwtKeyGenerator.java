/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtKeyGenerator {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    public String generateSecretKey() {
        // 環境変数 or 設定ファイルのキーを優先
        if (jwtSecret != null && !jwtSecret.isBlank()) {
            return jwtSecret;
        }

        if ("prod".equals(activeProfile)) {
            String envKey = System.getenv("JWT_SECRET");
            if (envKey == null) {
                throw new IllegalStateException("Production environment requires JWT_SECRET environment variable");
            }
            return envKey;
        }

        // 開発環境用の自動生成
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[32];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
