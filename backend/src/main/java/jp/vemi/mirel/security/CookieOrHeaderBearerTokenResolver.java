/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.util.StringUtils;

/**
 * Cookie または Authorization ヘッダーからJWTを読み取るBearerTokenResolver.
 * 
 * 優先順位:
 * 1. Authorization: Bearer <token>
 * 2. Cookie: accessToken=<token>
 */
public class CookieOrHeaderBearerTokenResolver implements BearerTokenResolver {

    private static final Logger logger = LoggerFactory.getLogger(CookieOrHeaderBearerTokenResolver.class);
    private static final String COOKIE_NAME = "accessToken";

    @Override
    public String resolve(HttpServletRequest request) {
        // 1. Authorization ヘッダーから取得（優先）
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.debug("JWT resolved from Authorization header");
            return token;
        }

        // 2. Cookie から取得（フォールバック）
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        logger.debug("JWT resolved from Cookie: {}", COOKIE_NAME);
                        return token;
                    }
                }
            }
        }

        logger.debug("No JWT found in Authorization header or Cookie");
        return null;
    }
}
