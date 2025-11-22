/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2認証成功ハンドラー
 * 
 * GitHub OAuth2認証成功後、JWTトークンを発行してフロントエンドにリダイレクトします。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final JwtService jwtService;
    private final SystemUserRepository systemUserRepository;
    
    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            try {
                // GitHubユーザー情報を取得
                Map<String, Object> attributes = oauth2User.getAttributes();
                String githubId = String.valueOf(attributes.get("id"));
                
                // SystemUserを取得
                Optional<SystemUser> systemUser = systemUserRepository
                        .findByOauth2ProviderAndOauth2ProviderId("github", githubId);
                
                if (systemUser.isEmpty()) {
                    log.error("SystemUser not found for GitHub ID: {}", githubId);
                    getRedirectStrategy().sendRedirect(request, response, appBaseUrl + "/login?error=user_not_found");
                    return;
                }
                
                // JWTトークンを生成
                String token = jwtService.generateToken(authentication);
                
                // フロントエンドにリダイレクト（トークンをクエリパラメータで渡す）
                String redirectUrl = appBaseUrl + "/auth/oauth2/success?token=" + token;
                log.info("OAuth2 authentication success: user={}, redirecting to {}", 
                        systemUser.get().getEmail(), redirectUrl);
                
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                
            } catch (Exception e) {
                log.error("Failed to process OAuth2 authentication", e);
                getRedirectStrategy().sendRedirect(request, response, appBaseUrl + "/login?error=oauth2");
            }
        } else {
            log.error("Unexpected principal type: {}", authentication.getPrincipal().getClass());
            getRedirectStrategy().sendRedirect(request, response, appBaseUrl + "/login?error=oauth2");
        }
    }
}
