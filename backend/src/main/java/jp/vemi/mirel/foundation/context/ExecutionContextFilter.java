/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.foundation.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense;
import jp.vemi.mirel.foundation.abst.dao.entity.Tenant;
import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.repository.ApplicationLicenseRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.TenantRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ExecutionContext解決Filter.
 * リクエストごとにユーザ、テナント、ライセンス情報を解決してExecutionContextに設定
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ExecutionContextFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionContextFilter.class);

    @Autowired
    private ExecutionContext executionContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ApplicationLicenseRepository licenseRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // リクエストメタデータ設定
            executionContext.setRequestId(UUID.randomUUID().toString());
            executionContext.setIpAddress(getClientIpAddress(request));
            executionContext.setUserAgent(request.getHeader("User-Agent"));
            executionContext.setRequestTime(Instant.now());

            // Spring SecurityからユーザIDを取得
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String userId = auth.getName();
                logger.debug("ExecutionContextFilter: Resolving context for user: {}", userId);

                // ユーザ情報取得
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    executionContext.setCurrentUser(user);

                    // カレントテナント解決
                    String tenantId = resolveTenantId(request, auth, user);
                    logger.debug("ExecutionContextFilter: Resolved tenant: {}", tenantId);

                    if (StringUtils.hasText(tenantId)) {
                        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
                        executionContext.setCurrentTenant(tenant);

                        // 有効ライセンス取得（USER/TENANTスコープ両方）
                        List<ApplicationLicense> licenses = licenseRepository
                            .findEffectiveLicenses(userId, tenantId, Instant.now());
                        executionContext.setEffectiveLicenses(licenses);
                        logger.debug("ExecutionContextFilter: Found {} effective licenses", 
                            licenses != null ? licenses.size() : 0);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error in ExecutionContextFilter", e);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * テナントIDを解決
     * 優先順位: Header > JWT Claim > User Default > "default"
     */
    private String resolveTenantId(HttpServletRequest request, Authentication auth, User user) {
        // 1. X-Tenant-ID ヘッダー
        String headerTenantId = request.getHeader("X-Tenant-ID");
        if (StringUtils.hasText(headerTenantId)) {
            return headerTenantId;
        }

        // 2. JWT ClaimからtenantId
        if (auth instanceof JwtAuthenticationToken) {
            Jwt jwt = ((JwtAuthenticationToken) auth).getToken();
            String jwtTenantId = jwt.getClaimAsString("tenant_id");
            if (StringUtils.hasText(jwtTenantId)) {
                return jwtTenantId;
            }
        }

        // 3. Userのデフォルトテナント
        if (user != null && StringUtils.hasText(user.getTenantId())) {
            return user.getTenantId();
        }

        // 4. システムデフォルト
        return "default";
    }

    /**
     * クライアントIPアドレス取得
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
