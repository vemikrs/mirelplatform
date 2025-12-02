/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2認証失敗ハンドラー
 * 
 * GitHub OAuth2認証失敗時、エラーメッセージを付けてログインページにリダイレクトします。
 */
@Component
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;
    
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        
        log.error("OAuth2 authentication failed", exception);
        
        String redirectUrl = appBaseUrl + "/login?error=oauth2";
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
