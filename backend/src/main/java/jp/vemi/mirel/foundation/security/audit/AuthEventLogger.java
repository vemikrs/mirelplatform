/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 認証イベント監査ログ.
 * <p>
 * 全ての認証関連イベントを構造化ログとして出力する。
 * ログは以下のイベントを記録:
 * <ul>
 * <li>AUTH_LOGIN_SUCCESS - ログイン成功</li>
 * <li>AUTH_LOGIN_FAILURE - ログイン失敗</li>
 * <li>AUTH_OTP_VERIFY - OTP検証</li>
 * <li>AUTH_REFRESH - トークンリフレッシュ</li>
 * <li>AUTH_LOGOUT - ログアウト</li>
 * <li>AUTH_KEY_ROTATION - 鍵ローテーション</li>
 * </ul>
 * </p>
 */
@Component
public class AuthEventLogger {

    private static final Logger logger = LoggerFactory.getLogger("AUDIT.AUTH");

    /**
     * ログイン成功をログ.
     */
    public void logLoginSuccess(String userId, String username, String ipAddress, String userAgent) {
        try {
            MDC.put("event", "AUTH_LOGIN_SUCCESS");
            MDC.put("userId", userId);
            MDC.put("username", sanitize(username));
            MDC.put("ipAddress", sanitize(ipAddress));
            MDC.put("userAgent", sanitize(userAgent));

            logger.info("Login successful: userId={}, username={}, ip={}",
                    userId, sanitize(username), sanitize(ipAddress));
        } finally {
            clearMDC();
        }
    }

    /**
     * ログイン失敗をログ.
     */
    public void logLoginFailure(String usernameOrEmail, String reason, String ipAddress, String userAgent) {
        try {
            MDC.put("event", "AUTH_LOGIN_FAILURE");
            MDC.put("usernameOrEmail", sanitize(usernameOrEmail));
            MDC.put("reason", sanitize(reason));
            MDC.put("ipAddress", sanitize(ipAddress));
            MDC.put("userAgent", sanitize(userAgent));

            logger.warn("Login failed: usernameOrEmail={}, reason={}, ip={}",
                    sanitize(usernameOrEmail), sanitize(reason), sanitize(ipAddress));
        } finally {
            clearMDC();
        }
    }

    /**
     * OTP検証をログ.
     */
    public void logOtpVerify(String email, boolean success, String purpose, String ipAddress) {
        try {
            MDC.put("event", "AUTH_OTP_VERIFY");
            MDC.put("email", sanitize(email));
            MDC.put("success", String.valueOf(success));
            MDC.put("purpose", sanitize(purpose));
            MDC.put("ipAddress", sanitize(ipAddress));

            if (success) {
                logger.info("OTP verified: email={}, purpose={}, ip={}",
                        sanitize(email), sanitize(purpose), sanitize(ipAddress));
            } else {
                logger.warn("OTP verification failed: email={}, purpose={}, ip={}",
                        sanitize(email), sanitize(purpose), sanitize(ipAddress));
            }
        } finally {
            clearMDC();
        }
    }

    /**
     * トークンリフレッシュをログ.
     */
    public void logRefresh(String userId, boolean success, String ipAddress) {
        try {
            MDC.put("event", "AUTH_REFRESH");
            MDC.put("userId", userId);
            MDC.put("success", String.valueOf(success));
            MDC.put("ipAddress", sanitize(ipAddress));

            if (success) {
                logger.debug("Token refreshed: userId={}, ip={}", userId, sanitize(ipAddress));
            } else {
                logger.warn("Token refresh failed: userId={}, ip={}", userId, sanitize(ipAddress));
            }
        } finally {
            clearMDC();
        }
    }

    /**
     * ログアウトをログ.
     */
    public void logLogout(String userId, String ipAddress) {
        try {
            MDC.put("event", "AUTH_LOGOUT");
            MDC.put("userId", userId);
            MDC.put("ipAddress", sanitize(ipAddress));

            logger.info("User logged out: userId={}, ip={}", userId, sanitize(ipAddress));
        } finally {
            clearMDC();
        }
    }

    /**
     * 鍵ローテーションをログ.
     */
    public void logKeyRotation(String oldKeyId, String newKeyId, String reason) {
        try {
            MDC.put("event", "AUTH_KEY_ROTATION");
            MDC.put("oldKeyId", sanitize(oldKeyId));
            MDC.put("newKeyId", sanitize(newKeyId));
            MDC.put("reason", sanitize(reason));

            logger.info("JWT key rotated: oldKeyId={}, newKeyId={}, reason={}",
                    sanitize(oldKeyId), sanitize(newKeyId), sanitize(reason));
        } finally {
            clearMDC();
        }
    }

    /**
     * アカウントロックをログ.
     */
    public void logAccountLocked(String usernameOrEmail, String ipAddress) {
        try {
            MDC.put("event", "AUTH_ACCOUNT_LOCKED");
            MDC.put("usernameOrEmail", sanitize(usernameOrEmail));
            MDC.put("ipAddress", sanitize(ipAddress));

            logger.warn("Account locked due to multiple failed attempts: usernameOrEmail={}, ip={}",
                    sanitize(usernameOrEmail), sanitize(ipAddress));
        } finally {
            clearMDC();
        }
    }

    /**
     * ログインジェクション対策のためサニタイズ.
     */
    private String sanitize(String value) {
        if (value == null) {
            return "null";
        }
        // 改行・タブ・制御文字を除去
        return value.replaceAll("[\\r\\n\\t]", "_")
                .replaceAll("[\\x00-\\x1F]", "");
    }

    private void clearMDC() {
        MDC.remove("event");
        MDC.remove("userId");
        MDC.remove("username");
        MDC.remove("usernameOrEmail");
        MDC.remove("email");
        MDC.remove("ipAddress");
        MDC.remove("userAgent");
        MDC.remove("reason");
        MDC.remove("success");
        MDC.remove("purpose");
        MDC.remove("oldKeyId");
        MDC.remove("newKeyId");
    }
}
