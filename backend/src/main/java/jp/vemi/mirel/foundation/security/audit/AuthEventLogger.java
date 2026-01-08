/*
 * Copyright(c) 2015-2026 mirelplatform.
 */
package jp.vemi.mirel.foundation.security.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import jp.vemi.framework.util.SanitizeUtil;

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
            MDC.put("username", SanitizeUtil.forLog(username));
            MDC.put("ipAddress", SanitizeUtil.forLog(ipAddress));
            MDC.put("userAgent", SanitizeUtil.forLog(userAgent));

            // lgtm[java/log-injection] - all user inputs are sanitized with
            // SanitizeUtil.forLog()
            logger.info("Login successful: userId={}, username={}, ip={}",
                    userId, SanitizeUtil.forLog(username), SanitizeUtil.forLog(ipAddress));
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
            MDC.put("usernameOrEmail", SanitizeUtil.forLog(usernameOrEmail));
            MDC.put("reason", SanitizeUtil.forLog(reason));
            MDC.put("ipAddress", SanitizeUtil.forLog(ipAddress));
            MDC.put("userAgent", SanitizeUtil.forLog(userAgent));

            // lgtm[java/log-injection] - all user inputs are sanitized with
            // SanitizeUtil.forLog()
            logger.warn("Login failed: usernameOrEmail={}, reason={}, ip={}",
                    SanitizeUtil.forLog(usernameOrEmail), SanitizeUtil.forLog(reason), SanitizeUtil.forLog(ipAddress));
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
            MDC.put("email", SanitizeUtil.forLog(email));
            MDC.put("success", String.valueOf(success));
            MDC.put("purpose", SanitizeUtil.forLog(purpose));
            MDC.put("ipAddress", SanitizeUtil.forLog(ipAddress));

            if (success) {
                logger.info("OTP verified: email={}, purpose={}, ip={}",
                        SanitizeUtil.forLog(email), SanitizeUtil.forLog(purpose), SanitizeUtil.forLog(ipAddress));
            } else {
                logger.warn("OTP verification failed: email={}, purpose={}, ip={}",
                        SanitizeUtil.forLog(email), SanitizeUtil.forLog(purpose), SanitizeUtil.forLog(ipAddress));
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
            MDC.put("ipAddress", SanitizeUtil.forLog(ipAddress));

            if (success) {
                logger.debug("Token refreshed: userId={}, ip={}", userId, SanitizeUtil.forLog(ipAddress));
            } else {
                logger.warn("Token refresh failed: userId={}, ip={}", userId, SanitizeUtil.forLog(ipAddress));
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
            MDC.put("ipAddress", SanitizeUtil.forLog(ipAddress));

            logger.info("User logged out: userId={}, ip={}", userId, SanitizeUtil.forLog(ipAddress));
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
            MDC.put("oldKeyId", SanitizeUtil.forLog(oldKeyId));
            MDC.put("newKeyId", SanitizeUtil.forLog(newKeyId));
            MDC.put("reason", SanitizeUtil.forLog(reason));

            logger.info("JWT key rotated: oldKeyId={}, newKeyId={}, reason={}",
                    SanitizeUtil.forLog(oldKeyId), SanitizeUtil.forLog(newKeyId), SanitizeUtil.forLog(reason));
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
            MDC.put("usernameOrEmail", SanitizeUtil.forLog(usernameOrEmail));
            MDC.put("ipAddress", SanitizeUtil.forLog(ipAddress));

            // lgtm[java/log-injection] - all user inputs are sanitized with
            // SanitizeUtil.forLog()
            logger.warn("Account locked due to multiple failed attempts: usernameOrEmail={}, ip={}",
                    SanitizeUtil.forLog(usernameOrEmail), SanitizeUtil.forLog(ipAddress));
        } finally {
            clearMDC();
        }
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
