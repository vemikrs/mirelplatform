/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

/**
 * OAuth2AuthenticationFailureHandler単体テスト
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2AuthenticationFailureHandler単体テスト")
class OAuth2AuthenticationFailureHandlerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private OAuth2AuthenticationFailureHandler handler;

    @BeforeEach
    void setUp() {
        // appBaseUrlを設定
        ReflectionTestUtils.setField(handler, "appBaseUrl", "http://localhost:5173");

        // encodeRedirectURLのスタブ化 (DefaultRedirectStrategyで使用される)
        org.mockito.Mockito.lenient().when(response.encodeRedirectURL(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("認証失敗: OAuth2エラーでリダイレクト")
    void testOnAuthenticationFailure_OAuth2Error() throws Exception {
        // Given: OAuth2認証エラー
        OAuth2Error error = new OAuth2Error("invalid_request", "Invalid request", null);
        AuthenticationException exception = new OAuth2AuthenticationException(error);

        // When: 認証失敗ハンドラーを実行
        handler.onAuthenticationFailure(request, response, exception);

        // Then: エラー付きログインページにリダイレクト
        verify(response).sendRedirect("http://localhost:5173/login?error=oauth2");
    }

    @Test
    @DisplayName("認証失敗: 一般的な認証エラー")
    void testOnAuthenticationFailure_GeneralAuthenticationError() throws Exception {
        // Given: 一般的な認証エラー
        AuthenticationException exception = new AuthenticationException("Authentication failed") {
        };

        // When: 認証失敗ハンドラーを実行
        handler.onAuthenticationFailure(request, response, exception);

        // Then: エラー付きログインページにリダイレクト
        verify(response).sendRedirect("http://localhost:5173/login?error=oauth2");
    }

    @Test
    @DisplayName("認証失敗: カスタムappBaseUrl")
    void testOnAuthenticationFailure_CustomBaseUrl() throws Exception {
        // Given: カスタムappBaseUrl
        ReflectionTestUtils.setField(handler, "appBaseUrl", "https://example.com");
        AuthenticationException exception = new AuthenticationException("Authentication failed") {
        };

        // When: 認証失敗ハンドラーを実行
        handler.onAuthenticationFailure(request, response, exception);

        // Then: カスタムURLでリダイレクト
        verify(response).sendRedirect("https://example.com/login?error=oauth2");
    }
}
