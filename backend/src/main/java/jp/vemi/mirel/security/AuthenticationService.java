/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.security;

import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * 認証サービス.
 * @author mirel
 */
public interface AuthenticationService {

    String authenticate(String username, String password);
    boolean isJwtSupported();
    JwtDecoder getJwtDecoder();  // JWTサポート時のみ使用
}
