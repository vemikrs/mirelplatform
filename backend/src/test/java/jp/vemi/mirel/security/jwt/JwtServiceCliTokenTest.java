/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import jp.vemi.mirel.config.properties.AuthProperties;

/**
 * JwtService の CLI トークン生成機能のテスト。
 * SpringBoot コンテキストを使わない軽量テスト。
 */
@ExtendWith(MockitoExtension.class)
class JwtServiceCliTokenTest {
    
    @Mock
    private AuthProperties authProperties;
    
    @Mock
    private AuthProperties.Jwt jwtProperties;
    
    private JwtService jwtService;
    
    @BeforeEach
    void setUp() {
        // AuthPropertiesのモック設定
        when(authProperties.getJwt()).thenReturn(jwtProperties);
        when(jwtProperties.isEnabled()).thenReturn(true);
        when(jwtProperties.getSecret()).thenReturn("test-secret-key-that-is-at-least-32-characters-long-for-hmac256");
        
        // JwtServiceをインスタンス化してフィールドを注入
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "authProperties", authProperties);
        jwtService.init();
    }
    
    @Test
    @DisplayName("CLI向けトークンが正しく生成される")
    void shouldGenerateCliToken() {
        String token = jwtService.generateCliToken(
                "user-123",
                "api:read api:write",
                "mirel-cli",
                List.of("ROLE_USER", "ROLE_ADMIN")
        );
        
        assertThat(token).isNotBlank();
        
        // トークンをデコードして検証
        Jwt jwt = jwtService.decodeToken(token);
        assertThat(jwt.getSubject()).isEqualTo("user-123");
        assertThat(jwt.getClaimAsString("scope")).isEqualTo("api:read api:write");
        assertThat(jwt.getClaimAsString("client_id")).isEqualTo("mirel-cli");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }
    
    @Test
    @DisplayName("CLI向けトークンの有効期限が24時間に設定される")
    void shouldSetCliTokenExpiryTo24Hours() {
        String token = jwtService.generateCliToken(
                "user-123",
                "api:read",
                "mirel-cli",
                List.of("ROLE_USER")
        );
        
        Jwt jwt = jwtService.decodeToken(token);
        long expirySeconds = jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond();
        
        assertThat(expirySeconds).isEqualTo(86400); // 24時間
    }
    
    @Test
    @DisplayName("スコープがnullの場合は空文字として設定される")
    void shouldSetEmptyScopeWhenNull() {
        String token = jwtService.generateCliToken(
                "user-123",
                null,
                "mirel-cli",
                List.of("ROLE_USER")
        );
        
        Jwt jwt = jwtService.decodeToken(token);
        assertThat(jwt.getClaimAsString("scope")).isEmpty();
    }
    
    @Test
    @DisplayName("生成されたトークンが有効として検証される")
    void shouldGenerateValidToken() {
        String token = jwtService.generateCliToken(
                "user-123",
                "api:read",
                "mirel-cli",
                List.of("ROLE_USER")
        );
        
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }
    
    @Test
    @DisplayName("getUsernameFromTokenでユーザーIDが取得できる")
    void shouldGetUsernameFromToken() {
        String token = jwtService.generateCliToken(
                "user-123",
                "api:read",
                "mirel-cli",
                List.of("ROLE_USER")
        );
        
        String username = jwtService.getUsernameFromToken(token);
        assertThat(username).isEqualTo("user-123");
    }
}
