/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtAuthoritiesConverter のユニットテスト。
 */
@DisplayName("JwtAuthoritiesConverter テスト")
class JwtAuthoritiesConverterTest {

    private JwtAuthoritiesConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtAuthoritiesConverter();
    }

    /**
     * テスト用の JWT を作成するヘルパーメソッド。
     */
    private Jwt createJwt(Object roles) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("user-001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        
        if (roles != null) {
            builder.claim("roles", roles);
        }
        
        return builder.build();
    }

    @Test
    @DisplayName("ROLE_ プレフィックス付きのロールが正しく変換される")
    void convert_WithRolePrefixRoles_ReturnsAuthorities() {
        // Given
        Jwt jwt = createJwt(List.of("ROLE_ADMIN", "ROLE_USER"));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("ROLE_ プレフィックスなしのロールに自動でプレフィックスが追加される")
    void convert_WithoutRolePrefixRoles_AddsPrefix() {
        // Given
        Jwt jwt = createJwt(List.of("ADMIN", "USER"));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("混在するロール形式（プレフィックスあり・なし）が正しく処理される")
    void convert_MixedRoleFormats_HandledCorrectly() {
        // Given
        Jwt jwt = createJwt(List.of("ROLE_ADMIN", "USER", "ROLE_MANAGER"));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertEquals(3, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")));
    }

    @Test
    @DisplayName("roles クレームがない場合は空のコレクションを返す")
    void convert_NoRolesClaim_ReturnsEmptyCollection() {
        // Given
        Jwt jwt = createJwt(null);

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertTrue(authorities.isEmpty());
    }

    @Test
    @DisplayName("roles クレームが空のリストの場合は空のコレクションを返す")
    void convert_EmptyRolesList_ReturnsEmptyCollection() {
        // Given
        Jwt jwt = createJwt(List.of());

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertTrue(authorities.isEmpty());
    }

    @Test
    @DisplayName("roles クレームがリスト以外の型の場合は空のコレクションを返す")
    void convert_NonListRolesClaim_ReturnsEmptyCollection() {
        // Given: roles が String 型（リストではない）
        Jwt jwt = createJwt("ADMIN");

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then
        assertTrue(authorities.isEmpty());
    }

    @Test
    @DisplayName("リスト内に非文字列要素がある場合は無視される")
    void convert_NonStringElementsInList_Ignored() {
        // Given: 一部が数値や null
        Jwt jwt = createJwt(Arrays.asList("ADMIN", 123, "USER", null));

        // When
        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        // Then: 文字列のみ変換される
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }
}
