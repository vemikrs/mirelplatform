/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.security.device;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jp.vemi.mirel.foundation.abst.dao.entity.User;
import jp.vemi.mirel.foundation.abst.dao.repository.UserRepository;
import jp.vemi.mirel.security.device.dto.DeviceCodeRequest;
import jp.vemi.mirel.security.device.dto.DeviceCodeResponse;
import jp.vemi.mirel.security.device.dto.DeviceTokenResponse;
import jp.vemi.mirel.security.jwt.JwtService;

/**
 * デバイス認証サービス。
 * OAuth2 Device Authorization Grant (RFC 8628) の実装。
 */
@Service
public class DeviceAuthService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceAuthService.class);

    /**
     * ユーザーコード生成に使用する文字（紛らわしい文字を除外）
     * 除外: 0, O, 1, I, l, L
     */
    private static final String USER_CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    /**
     * デバイスコードの有効期限（秒）
     */
    private static final int DEVICE_CODE_EXPIRY_SECONDS = 900; // 15分

    /**
     * ポーリング最小間隔（秒）
     */
    private static final int MIN_POLLING_INTERVAL_SECONDS = 5;

    /**
     * CLIトークンの有効期限（秒）- 24時間
     */
    private static final long CLI_TOKEN_EXPIRY_SECONDS = 86400;

    /**
     * インメモリセッションストア
     * TODO: Redis対応時はRedisTemplateに置き換え
     */
    private final Map<String, DeviceAuthSession> sessionsByDeviceCode = new ConcurrentHashMap<>();
    private final Map<String, String> deviceCodeByUserCode = new ConcurrentHashMap<>();

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    @Autowired(required = false)
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    /**
     * デバイスコードとユーザーコードを発行します。
     * 
     * @param request
     *            デバイスコード発行リクエスト
     * @return デバイスコードレスポンス
     */
    public DeviceCodeResponse createDeviceCode(DeviceCodeRequest request) {
        String deviceCode = UUID.randomUUID().toString();
        String userCode = generateUserCode();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(DEVICE_CODE_EXPIRY_SECONDS);

        DeviceAuthSession session = DeviceAuthSession.builder()
                .deviceCode(deviceCode)
                .userCode(userCode)
                .clientId(request.getClientId())
                .scope(request.getScope())
                .status(DeviceAuthStatus.PENDING)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        sessionsByDeviceCode.put(deviceCode, session);
        deviceCodeByUserCode.put(userCode, deviceCode);

        logger.info("Device code created: deviceCode={}, userCode={}, clientId={}",
                deviceCode, userCode, request.getClientId());

        return DeviceCodeResponse.builder()
                .deviceCode(deviceCode)
                .userCode(userCode)
                .verificationUri(appBaseUrl + "/cli/auth")
                .expiresIn(DEVICE_CODE_EXPIRY_SECONDS)
                .interval(MIN_POLLING_INTERVAL_SECONDS)
                .build();
    }

    /**
     * デバイスコードに対応するセッションを取得します。
     * 
     * @param deviceCode
     *            デバイスコード
     * @return セッション（存在しない場合はempty）
     */
    public Optional<DeviceAuthSession> getSessionByDeviceCode(String deviceCode) {
        return Optional.ofNullable(sessionsByDeviceCode.get(deviceCode));
    }

    /**
     * ユーザーコードに対応するセッションを取得します。
     * 
     * @param userCode
     *            ユーザーコード
     * @return セッション（存在しない場合はempty）
     */
    public Optional<DeviceAuthSession> getSessionByUserCode(String userCode) {
        String deviceCode = deviceCodeByUserCode.get(userCode);
        if (deviceCode == null) {
            return Optional.empty();
        }
        return getSessionByDeviceCode(deviceCode);
    }

    /**
     * トークンポーリングを処理します。
     * レート制限をチェックし、認証状態に応じたレスポンスを返します。
     * 
     * @param deviceCode
     *            デバイスコード
     * @param clientId
     *            クライアントID
     * @return トークンレスポンス
     */
    public DeviceTokenResponse pollToken(String deviceCode, String clientId) {
        DeviceAuthSession session = sessionsByDeviceCode.get(deviceCode);

        if (session == null) {
            logger.warn("Device code not found: {}", deviceCode);
            return DeviceTokenResponse.expired();
        }

        // クライアントID検証
        if (!session.getClientId().equals(clientId)) {
            logger.warn("Client ID mismatch: expected={}, actual={}", session.getClientId(), clientId);
            return DeviceTokenResponse.builder()
                    .error("invalid_client")
                    .errorDescription("Client ID does not match")
                    .build();
        }

        // 有効期限チェック
        if (session.isExpired()) {
            logger.info("Device code expired: {}", deviceCode);
            removeSession(deviceCode);
            return DeviceTokenResponse.expired();
        }

        // レート制限チェック
        Instant now = Instant.now();
        if (session.getLastPolledAt() != null) {
            Duration elapsed = Duration.between(session.getLastPolledAt(), now);
            if (elapsed.getSeconds() < MIN_POLLING_INTERVAL_SECONDS) {
                logger.debug("Polling too fast: deviceCode={}, elapsed={}s", deviceCode, elapsed.getSeconds());
                return DeviceTokenResponse.slowDown();
            }
        }
        session.setLastPolledAt(now);

        // ステータスに応じたレスポンス
        switch (session.getStatus()) {
            case PENDING:
                return DeviceTokenResponse.pending();

            case AUTHORIZED:
                // JWTトークンを生成
                String accessToken = generateCliToken(session);
                DeviceTokenResponse response = DeviceTokenResponse.builder()
                        .status("authorized")
                        .accessToken(accessToken)
                        .tokenType("Bearer")
                        .expiresIn((int) CLI_TOKEN_EXPIRY_SECONDS)
                        .user(DeviceTokenResponse.UserInfo.builder()
                                .email(session.getUserEmail())
                                .name(session.getUserName())
                                .build())
                        .build();

                // セッションを削除（使い捨て）
                removeSession(deviceCode);
                logger.info("Token issued for device code: {}, userId={}", deviceCode, session.getUserId());

                return response;

            case DENIED:
                removeSession(deviceCode);
                return DeviceTokenResponse.denied();

            default:
                return DeviceTokenResponse.pending();
        }
    }

    /**
     * ユーザーコードを承認します。
     * 
     * @param userCode
     *            ユーザーコード
     * @param userId
     *            承認するユーザーのID
     * @param userName
     *            ユーザー名
     * @param userEmail
     *            ユーザーメールアドレス
     * @return 成功した場合true
     */
    public boolean authorize(String userCode, String userId, String userName, String userEmail) {
        String deviceCode = deviceCodeByUserCode.get(userCode);
        if (deviceCode == null) {
            logger.warn("User code not found: {}", userCode);
            return false;
        }

        DeviceAuthSession session = sessionsByDeviceCode.get(deviceCode);
        if (session == null || session.isExpired() || session.getStatus() != DeviceAuthStatus.PENDING) {
            logger.warn("Invalid session for user code: {}", userCode);
            return false;
        }

        session.setStatus(DeviceAuthStatus.AUTHORIZED);
        session.setUserId(userId);
        session.setUserName(userName);
        session.setUserEmail(userEmail);

        logger.info("User code authorized: userCode={}, userId={}", userCode, userId);
        return true;
    }

    /**
     * ユーザーコードを拒否します。
     * 
     * @param userCode
     *            ユーザーコード
     * @return 成功した場合true
     */
    public boolean deny(String userCode) {
        String deviceCode = deviceCodeByUserCode.get(userCode);
        if (deviceCode == null) {
            logger.warn("User code not found: {}", userCode);
            return false;
        }

        DeviceAuthSession session = sessionsByDeviceCode.get(deviceCode);
        if (session == null || session.isExpired() || session.getStatus() != DeviceAuthStatus.PENDING) {
            logger.warn("Invalid session for user code: {}", userCode);
            return false;
        }

        session.setStatus(DeviceAuthStatus.DENIED);

        logger.info("User code denied: {}", userCode);
        return true;
    }

    /**
     * ユーザーコードの検証（表示用）
     * 
     * @param userCode
     *            ユーザーコード
     * @return 有効な場合はセッション情報、無効な場合はempty
     */
    public Optional<DeviceAuthSession> validateUserCode(String userCode) {
        return getSessionByUserCode(userCode)
                .filter(session -> !session.isExpired() && session.getStatus() == DeviceAuthStatus.PENDING);
    }

    /**
     * セッションを削除します。
     */
    private void removeSession(String deviceCode) {
        DeviceAuthSession session = sessionsByDeviceCode.remove(deviceCode);
        if (session != null) {
            deviceCodeByUserCode.remove(session.getUserCode());
        }
    }

    /**
     * 8文字のユーザーコードを生成します（XXXX-XXXX形式）。
     */
    private String generateUserCode() {
        StringBuilder code = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                code.append('-');
            }
            int index = secureRandom.nextInt(USER_CODE_CHARS.length());
            code.append(USER_CODE_CHARS.charAt(index));
        }

        // 重複チェック
        String userCode = code.toString();
        if (deviceCodeByUserCode.containsKey(userCode)) {
            return generateUserCode(); // 再帰的に再生成
        }

        return userCode;
    }

    /**
     * CLI向けJWTトークンを生成します。
     */
    private String generateCliToken(DeviceAuthSession session) {
        if (jwtService == null) {
            throw new IllegalStateException("JWT service is not available");
        }

        // ユーザー情報からAuthenticationを構築してトークン生成
        // JwtServiceのgenerateCliTokenメソッドを使用
        return jwtService.generateCliToken(
                session.getUserId(),
                session.getScope(),
                session.getClientId(),
                buildRolesFromUser(session.getUserId()));
    }

    /**
     * ユーザーIDからロール一覧を取得
     */
    private List<String> buildRolesFromUser(String userId) {
        return userRepository.findById(userId)
                .map(User::getRoles)
                .map(roles -> {
                    if (roles == null || roles.isBlank()) {
                        return List.of("ROLE_USER");
                    }
                    return java.util.Arrays.stream(roles.split("[,|]"))
                            .map(String::trim)
                            .filter(role -> !role.isEmpty())
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase())
                            .collect(Collectors.toList());
                })
                .orElse(List.of("ROLE_USER"));
    }

    /**
     * 期限切れセッションを定期的にクリーンアップします。
     */
    @Scheduled(fixedRate = 60000) // 1分ごと
    public void cleanupExpiredSessions() {
        Instant now = Instant.now();
        int removedCount = 0;

        for (Map.Entry<String, DeviceAuthSession> entry : sessionsByDeviceCode.entrySet()) {
            if (entry.getValue().isExpired()) {
                removeSession(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            logger.debug("Cleaned up {} expired device auth sessions", removedCount);
        }
    }
}
