/*
 * Copyright(c) 2015-2024 mirelplatform.
 */
package jp.vemi.mirel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import jp.vemi.framework.util.SanitizeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * リクエストとレスポンスのログを出力するインターセプター
 * 
 * @author Hiroki Kurosawa
 * @version 3.0
 */
public class RequestLoggingInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger("request");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("REQUEST: {} {} (IP: {})",
                    request.getMethod(),
                    SanitizeUtil.forLog(request.getRequestURI()),
                    SanitizeUtil.forLog(request.getRemoteAddr()));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        logger.info("RESPONSE: {} {} (Status: {})",
                request.getMethod(),
                SanitizeUtil.forLog(request.getRequestURI()),
                response.getStatus());
    }
}
