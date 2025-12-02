/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * OAuth2AuthenticationSuccessHandler単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2AuthenticationSuccessHandler単体テスト")
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ObjectProvider<JwtService> jwtServiceProvider;

    @Mock
    private SystemUserRepository systemUserRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    private Map<String, Object> githubAttributes;
    private SystemUser systemUser;
    private String testToken;

    @BeforeEach
    void setUp() {
        // appBaseUrlを設定
        ReflectionTestUtils.setField(handler, "appBaseUrl", "http://localhost:5173");

        // encodeRedirectURLのスタブ化 (DefaultRedirectStrategyで使用される)
        org.mockito.Mockito.lenient().when(response.encodeRedirectURL(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // GitHubユーザー属性
        githubAttributes = new HashMap<>();
        githubAttributes.put("id", 12345L);
        githubAttributes.put("login", "testuser");
        githubAttributes.put("email", "test@example.com");
        githubAttributes.put("avatar_url", "https://github.com/avatar.jpg");

        // SystemUser
        systemUser = new SystemUser();
        systemUser.setId(UUID.randomUUID());
        systemUser.setEmail("test@example.com");
        systemUser.setOauth2Provider("github");
        systemUser.setOauth2ProviderId("12345");

        // テスト用JWTトークン
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    }

    @Test
    @DisplayName("認証成功: JWT発行とリダイレクト")
    void testOnAuthenticationSuccess_Success() throws Exception {
        // Given: OAuth2認証成功
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(githubAttributes);
        when(systemUserRepository.findByOauth2ProviderAndOauth2ProviderId("github", "12345"))
                .thenReturn(Optional.of(systemUser));
        when(jwtServiceProvider.getIfAvailable()).thenReturn(jwtService);
        when(jwtService.generateToken(any(Authentication.class))).thenReturn(testToken);

        // When: 認証成功ハンドラーを実行
        handler.onAuthenticationSuccess(request, response, authentication);

        // Then: リダイレクトURL検証
        verify(response).sendRedirect("http://localhost:5173/auth/oauth2/success?token=" + testToken);
    }

    @Test
    @DisplayName("認証失敗: SystemUserが見つからない")
    void testOnAuthenticationSuccess_UserNotFound() throws Exception {
        // Given: SystemUserが見つからない
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(githubAttributes);
        when(systemUserRepository.findByOauth2ProviderAndOauth2ProviderId(anyString(), anyString()))
                .thenReturn(Optional.empty());
        lenient().when(jwtServiceProvider.getIfAvailable()).thenReturn(jwtService);

        // When: 認証成功ハンドラーを実行
        handler.onAuthenticationSuccess(request, response, authentication);

        // Then: エラー付きログインページにリダイレクト
        verify(response).sendRedirect("http://localhost:5173/login?error=user_not_found");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("認証失敗: JWT生成エラー")
    void testOnAuthenticationSuccess_JwtGenerationError() throws Exception {
        // Given: JWT生成でエラー
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(githubAttributes);
        when(systemUserRepository.findByOauth2ProviderAndOauth2ProviderId("github", "12345"))
                .thenReturn(Optional.of(systemUser));
        when(jwtServiceProvider.getIfAvailable()).thenReturn(jwtService);
        when(jwtService.generateToken(any(Authentication.class)))
                .thenThrow(new RuntimeException("JWT generation failed"));

        // When: 認証成功ハンドラーを実行
        handler.onAuthenticationSuccess(request, response, authentication);

        // Then: エラー付きログインページにリダイレクト
        verify(response).sendRedirect("http://localhost:5173/login?error=oauth2");
    }

    @Test
    @DisplayName("認証失敗: Principal型が不正")
    void testOnAuthenticationSuccess_InvalidPrincipalType() throws Exception {
        // Given: OAuth2User以外のPrincipal
        when(authentication.getPrincipal()).thenReturn("invalid-principal");

        // When: 認証成功ハンドラーを実行
        handler.onAuthenticationSuccess(request, response, authentication);

        // Then: エラー付きログインページにリダイレクト
        verify(response).sendRedirect("http://localhost:5173/login?error=oauth2");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("認証成功: カスタムappBaseUrl")
    void testOnAuthenticationSuccess_CustomBaseUrl() throws Exception {
        // Given: カスタムappBaseUrl
        ReflectionTestUtils.setField(handler, "appBaseUrl", "https://example.com");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(githubAttributes);
        when(systemUserRepository.findByOauth2ProviderAndOauth2ProviderId("github", "12345"))
                .thenReturn(Optional.of(systemUser));
        when(jwtServiceProvider.getIfAvailable()).thenReturn(jwtService);
        when(jwtService.generateToken(any(Authentication.class))).thenReturn(testToken);

        // When: 認証成功ハンドラーを実行
        handler.onAuthenticationSuccess(request, response, authentication);

        // Then: カスタムURLでリダイレクト
        verify(response).sendRedirect("https://example.com/auth/oauth2/success?token=" + testToken);
    }

    @Test
    @DisplayName("認証失敗: JWTサービス未設定")
    void testOnAuthenticationSuccess_JwtServiceUnavailable() throws Exception {
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(githubAttributes);
        when(systemUserRepository.findByOauth2ProviderAndOauth2ProviderId("github", "12345"))
                .thenReturn(Optional.of(systemUser));
        when(jwtServiceProvider.getIfAvailable()).thenReturn(null);

        // When: 認証成功ハンドラーを実行
        handler.onAuthenticationSuccess(request, response, authentication);

        // Then: JWT無効エラーでリダイレクト
        verify(response).sendRedirect("http://localhost:5173/login?error=oauth2_jwt_disabled");
        verify(jwtService, never()).generateToken(any());
    }
}
