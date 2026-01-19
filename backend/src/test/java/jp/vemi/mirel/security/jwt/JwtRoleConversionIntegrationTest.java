/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtService と JwtAuthoritiesConverter の統合テスト。
 * JWT トークンに roles クレームが含まれ、正しく GrantedAuthority に変換されることを検証。
 */
@SpringBootTest(properties = {
        "auth.method=jwt",
        "auth.jwt.enabled=true",
        "auth.jwt.secret=test-secret-key-that-is-at-least-32-characters-long",
        "spring.main.allow-bean-definition-overriding=true"
})
@org.springframework.test.context.ActiveProfiles("e2e")
@DisplayName("JWT ロール変換 統合テスト")
class JwtRoleConversionIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.mail.javamail.JavaMailSender javaMailSender;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtAuthoritiesConverter jwtAuthoritiesConverter;

    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @BeforeEach
    void setUp() {
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtAuthoritiesConverter);
    }

    @Test
    @DisplayName("ADMIN ロールを持つ JWT トークンが正しく認証される")
    void jwtWithAdminRole_ShouldHaveAdminAuthority() {
        // Given: ADMIN ロールを持つ認証情報
        Authentication mockAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user-admin-001",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")));

        // When: JWT トークンを生成
        String token = jwtService.generateToken(mockAuth);

        // Then: トークンをデコードしてロールを確認
        Jwt jwt = jwtService.decodeToken(token);

        List<String> roles = jwt.getClaim("roles");
        assertThat(roles).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");

        // And: JwtAuthenticationConverter で正しく変換される
        Authentication convertedAuth = jwtAuthenticationConverter.convert(jwt);
        assertThat(convertedAuth.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    @DisplayName("USER ロールのみを持つ JWT トークンが正しく認証される")
    void jwtWithUserRole_ShouldHaveUserAuthority() {
        // Given: USER ロールのみを持つ認証情報
        Authentication mockAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user-regular-001",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // When: JWT トークンを生成
        String token = jwtService.generateToken(mockAuth);

        // Then: トークンをデコードしてロールを確認
        Jwt jwt = jwtService.decodeToken(token);

        List<String> roles = jwt.getClaim("roles");
        assertThat(roles).containsExactly("ROLE_USER");

        // And: JwtAuthenticationConverter で正しく変換される
        Authentication convertedAuth = jwtAuthenticationConverter.convert(jwt);
        assertThat(convertedAuth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("ロールなしの JWT トークンは空の authorities を持つ")
    void jwtWithNoRoles_ShouldHaveEmptyAuthorities() {
        // Given: ロールを持たない認証情報
        Authentication mockAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user-no-role",
                null,
                List.of());

        // When: JWT トークンを生成
        String token = jwtService.generateToken(mockAuth);

        // Then: トークンをデコードしてロールを確認
        Jwt jwt = jwtService.decodeToken(token);

        List<String> roles = jwt.getClaim("roles");
        assertThat(roles).isEmpty();

        // And: JwtAuthenticationConverter で変換すると空の authorities
        Authentication convertedAuth = jwtAuthenticationConverter.convert(jwt);
        assertThat(convertedAuth.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("hasRole('ADMIN') が ROLE_ADMIN を持つ認証で true を返す")
    void hasRoleAdmin_WithAdminAuthority_ShouldPass() {
        // Given: ADMIN ロールを持つ認証情報
        Authentication mockAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "user-admin-001",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // When: JWT トークンを生成してデコード
        String token = jwtService.generateToken(mockAuth);
        Jwt jwt = jwtService.decodeToken(token);
        Authentication convertedAuth = jwtAuthenticationConverter.convert(jwt);

        // Then: ROLE_ADMIN を持っている
        assertThat(convertedAuth.getAuthorities())
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
