/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api.auth.service;

import jp.vemi.mirel.foundation.abst.dao.entity.*;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.LicenseTier;
import jp.vemi.mirel.foundation.abst.dao.entity.ApplicationLicense.SubjectType;
import jp.vemi.mirel.foundation.abst.dao.repository.*;
import jp.vemi.mirel.foundation.web.api.auth.dto.*;
import jp.vemi.mirel.security.jwt.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 認証サービス実装.
 */
@Service
public class AuthenticationServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private ApplicationLicenseRepository licenseRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JwtService jwtService;

    @Autowired
    private jp.vemi.mirel.config.properties.AuthProperties authProperties;

    /**
     * ログイン処理
     */
    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        logger.info("Login attempt for username or email: {}", request.getUsernameOrEmail());

        // SystemUserでusernameまたはemailを検索
        SystemUser systemUser = systemUserRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> systemUserRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));

        // アクティブチェック
        if (systemUser.getIsActive() == null || !systemUser.getIsActive()) {
            throw new RuntimeException("User account is not active");
        }

        // アカウントロックチェック
        if (systemUser.getAccountLocked() != null && systemUser.getAccountLocked()) {
            throw new RuntimeException("User account is locked");
        }

        // パスワード検証
        if (!passwordEncoder.matches(request.getPassword(), systemUser.getPasswordHash())) {
            logger.warn("Invalid password for user: {}", request.getUsernameOrEmail());

            // ログイン失敗回数をインクリメント
            Integer failedAttempts = systemUser.getFailedLoginAttempts() == null ? 0
                    : systemUser.getFailedLoginAttempts();
            systemUser.setFailedLoginAttempts(failedAttempts + 1);

            // 5回失敗でアカウントロック
            if (failedAttempts + 1 >= 5) {
                systemUser.setAccountLocked(true);
                logger.warn("Account locked due to multiple failed login attempts: {}", request.getUsernameOrEmail());
            }

            systemUserRepository.save(systemUser);
            throw new RuntimeException("Invalid username/email or password");
        }

        // ログイン成功：失敗回数リセット
        systemUser.setFailedLoginAttempts(0);
        systemUserRepository.save(systemUser);

        // Userエンティティを取得（ApplicationデータにアクセスするためにsystemUserIdで検索）
        User user = userRepository.findBySystemUserId(systemUser.getId())
                .orElseThrow(() -> new RuntimeException("User profile not found"));

        // 最終ログイン時刻更新
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // テナント解決
        Tenant tenant = resolveTenant(user, request.getTenantId());

        // トークン生成（JWT有効な場合のみ）
        String accessToken;
        boolean isJwtEnabled = authProperties.getJwt().isEnabled();
        if (isJwtEnabled && jwtService != null) {
            accessToken = jwtService.generateToken(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            user.getUserId(), null, buildAuthoritiesFromUser(user)));
        } else {
            accessToken = "session-based-auth-token";
            logger.warn("JWT is disabled. Using session-based authentication placeholder.");
        }

        // RefreshToken作成
        RefreshToken refreshToken = createRefreshToken(user);

        // 有効ライセンス取得
        List<ApplicationLicense> licenses = licenseRepository.findEffectiveLicenses(
                user.getUserId(), tenant != null ? tenant.getTenantId() : null, Instant.now());

        logger.info("Login successful for user: {}", user.getUserId());

        return buildAuthenticationResponse(user, tenant, accessToken, refreshToken.getTokenHash(), licenses);
    }

    /**
     * ユーザー指定ログイン処理 (OTP等から利用)
     */
    @Transactional
    public AuthenticationResponse loginWithUser(User user) {
        logger.info("Login with user object: {}", user.getUserId());

        // 最終ログイン時刻更新
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // テナント解決
        Tenant tenant = resolveTenant(user, null);

        // トークン生成（JWT有効な場合のみ）
        String accessToken;
        boolean isJwtEnabled = authProperties.getJwt().isEnabled();
        if (isJwtEnabled && jwtService != null) {
            accessToken = jwtService.generateToken(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            user.getUserId(), null, buildAuthoritiesFromUser(user)));
        } else {
            accessToken = "session-based-auth-token";
            logger.warn("JWT is disabled. Using session-based authentication placeholder.");
        }

        // RefreshToken作成
        RefreshToken refreshToken = createRefreshToken(user);

        // 有効ライセンス取得
        List<ApplicationLicense> licenses = licenseRepository.findEffectiveLicenses(
                user.getUserId(), tenant != null ? tenant.getTenantId() : null, Instant.now());

        logger.info("Login successful for user: {}", user.getUserId());

        return buildAuthenticationResponse(user, tenant, accessToken, refreshToken.getTokenHash(), licenses);
    }

    /**
     * サインアップ処理
     */
    @Transactional
    public AuthenticationResponse signup(SignupRequest request) {
        logger.info("Signup attempt for username: {}, email: {}", request.getUsername(), request.getEmail());

        // ユーザー名重複チェック
        if (systemUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // メール重複チェック
        if (systemUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // SystemUser作成
        SystemUser systemUser = new SystemUser();
        systemUser.setId(UUID.randomUUID());
        systemUser.setUsername(request.getUsername());
        systemUser.setEmail(request.getEmail());
        systemUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        systemUser.setIsActive(true);
        systemUser.setEmailVerified(false);
        systemUser = systemUserRepository.save(systemUser);

        // User作成（Applicationレベル）
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setSystemUserId(systemUser.getId());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsActive(true);
        user.setEmailVerified(false);
        user.setRoles("USER");
        user = userRepository.save(user);

        // デフォルトテナント作成または割り当て
        Tenant defaultTenant = getOrCreateDefaultTenant();
        user.setTenantId(defaultTenant.getTenantId());
        userRepository.save(user);

        // UserTenant作成
        UserTenant userTenant = new UserTenant();
        userTenant.setUserId(user.getUserId());
        userTenant.setTenantId(defaultTenant.getTenantId());
        userTenant.setRoleInTenant("MEMBER");
        userTenant.setIsDefault(true);
        userTenantRepository.save(userTenant);

        // デフォルトライセンス付与（FREE tier）
        ApplicationLicense license = new ApplicationLicense();
        license.setSubjectType(SubjectType.USER);
        license.setSubjectId(user.getUserId());
        license.setApplicationId("promarker");
        license.setTier(LicenseTier.FREE);
        license.setGrantedBy("system");
        licenseRepository.save(license);

        // トークン生成
        String accessToken;
        boolean isJwtEnabled = authProperties.getJwt().isEnabled();
        if (isJwtEnabled && jwtService != null) {
            accessToken = jwtService.generateToken(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            user.getUserId(), null, List.of()));
        } else {
            accessToken = "session-based-auth-token";
            logger.warn("JWT is disabled. Using session-based authentication placeholder.");
        }

        RefreshToken refreshToken = createRefreshToken(user);

        logger.info("Signup successful for user: {}", user.getUserId());

        return buildAuthenticationResponse(user, defaultTenant, accessToken,
                refreshToken.getTokenHash(), List.of(license));
    }

    /**
     * OAuth2サインアップ処理
     */
    @Transactional
    public AuthenticationResponse signupWithOAuth2(OAuth2SignupRequest request, String systemUserIdStr) {
        logger.info("OAuth2 signup attempt for username: {}, systemUserId: {}", request.getUsername(), systemUserIdStr);

        UUID systemUserId = UUID.fromString(systemUserIdStr);
        SystemUser systemUser = systemUserRepository.findById(systemUserId)
                .orElseThrow(() -> new RuntimeException("System user not found"));

        // ユーザー名重複チェック (自分自身は除く...といってもUserはまだないはず)
        // SystemUserのusernameとは別管理だが、一応チェック
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // User作成
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setSystemUserId(systemUser.getId());
        user.setUsername(request.getUsername());
        user.setEmail(systemUser.getEmail()); // SystemUserのメールを使用
        user.setDisplayName(request.getDisplayName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        // パスワードは設定しない（OAuth2認証のみ）
        // ただしDB制約がある場合はダミーを入れる必要があるが、Userエンティティの定義を確認するとnullableではない可能性がある
        // User.javaを見ると password カラムはあるが nullable 制約は @Column に書いてない。DB定義次第。
        // 安全のためダミーを入れる
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

        user.setIsActive(true);
        user.setEmailVerified(systemUser.getEmailVerified());
        user.setRoles("USER");
        user = userRepository.save(user);

        // デフォルトテナント作成または割り当て
        Tenant defaultTenant = getOrCreateDefaultTenant();
        user.setTenantId(defaultTenant.getTenantId());
        userRepository.save(user);

        // UserTenant作成
        UserTenant userTenant = new UserTenant();
        userTenant.setUserId(user.getUserId());
        userTenant.setTenantId(defaultTenant.getTenantId());
        userTenant.setRoleInTenant("MEMBER");
        userTenant.setIsDefault(true);
        userTenantRepository.save(userTenant);

        // デフォルトライセンス付与（FREE tier）
        ApplicationLicense license = new ApplicationLicense();
        license.setSubjectType(SubjectType.USER);
        license.setSubjectId(user.getUserId());
        license.setApplicationId("promarker");
        license.setTier(LicenseTier.FREE);
        license.setGrantedBy("system");
        licenseRepository.save(license);

        // トークン生成
        String accessToken;
        boolean isJwtEnabled = authProperties.getJwt().isEnabled();
        if (isJwtEnabled && jwtService != null) {
            accessToken = jwtService.generateToken(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            user.getUserId(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        } else {
            accessToken = "session-based-auth-token";
        }

        RefreshToken refreshToken = createRefreshToken(user);

        logger.info("OAuth2 signup successful for user: {}", user.getUserId());

        return buildAuthenticationResponse(user, defaultTenant, accessToken,
                refreshToken.getTokenHash(), List.of(license));
    }

    /**
     * OTPベースサインアップ処理
     * メールアドレス検証済みのユーザーを作成し、ログイン状態にする
     */
    @Transactional
    public AuthenticationResponse signupWithOtp(jp.vemi.mirel.foundation.web.api.auth.dto.OtpSignupRequest request) {
        logger.info("OTP-based signup attempt for username: {}, email: {}", request.getUsername(), request.getEmail());

        // ユーザー名重複チェック
        if (systemUserRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // メール重複チェック
        if (systemUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // パスワードプレースホルダー（OTP認証のため実際には使用されない）
        String dummyPassword = passwordEncoder.encode("OTP_AUTH_USER");

        // SystemUser作成（パスワードはプレースホルダー）
        SystemUser systemUser = new SystemUser();
        systemUser.setId(UUID.randomUUID());
        systemUser.setUsername(request.getUsername());
        systemUser.setEmail(request.getEmail());
        systemUser.setPasswordHash(dummyPassword);
        systemUser.setIsActive(true);
        systemUser.setEmailVerified(request.getEmailVerified() != null ? request.getEmailVerified() : true);
        systemUser = systemUserRepository.save(systemUser);

        // User作成（Applicationレベル）
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setSystemUserId(systemUser.getId());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        // パスワードハッシュ（プレースホルダー）
        user.setPasswordHash(dummyPassword);
        user.setIsActive(true);
        user.setEmailVerified(request.getEmailVerified() != null ? request.getEmailVerified() : true);
        user.setRoles("USER");
        // 最終ログイン時刻を設定（初回サインアップ時にも記録）
        user.setLastLoginAt(Instant.now());
        user = userRepository.save(user);

        // デフォルトテナント作成または割り当て
        Tenant defaultTenant = getOrCreateDefaultTenant();
        user.setTenantId(defaultTenant.getTenantId());
        userRepository.save(user);

        // UserTenant作成
        UserTenant userTenant = new UserTenant();
        userTenant.setUserId(user.getUserId());
        userTenant.setTenantId(defaultTenant.getTenantId());
        userTenant.setRoleInTenant("MEMBER");
        userTenant.setIsDefault(true);
        userTenantRepository.save(userTenant);

        // デフォルトライセンス付与（FREE tier）
        ApplicationLicense license = new ApplicationLicense();
        license.setSubjectType(SubjectType.USER);
        license.setSubjectId(user.getUserId());
        license.setApplicationId("promarker");
        license.setTier(LicenseTier.FREE);
        license.setGrantedBy("system");
        licenseRepository.save(license);

        // トークン生成
        String accessToken;
        boolean isJwtEnabled = authProperties.getJwt().isEnabled();
        if (isJwtEnabled && jwtService != null) {
            accessToken = jwtService.generateToken(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            user.getUserId(), null, List.of()));
        } else {
            accessToken = "session-based-auth-token";
            logger.warn("JWT is disabled. Using session-based authentication placeholder.");
        }

        RefreshToken refreshToken = createRefreshToken(user);

        logger.info("OTP-based signup successful for user: {}", user.getUserId());

        return buildAuthenticationResponse(user, defaultTenant, accessToken,
                refreshToken.getTokenHash(), List.of(license));
    }

    /**
     * トークンリフレッシュ
     */
    @Transactional
    public AuthenticationResponse refresh(RefreshTokenRequest request) {
        boolean isJwtEnabled = authProperties.getJwt().isEnabled();
        if (!isJwtEnabled || jwtService == null) {
            throw new IllegalStateException("JWT is disabled. Refresh token not supported.");
        }
        logger.info("Token refresh attempt");

        // RefreshToken検証
        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new RuntimeException("Refresh token is expired or revoked");
        }

        // ユーザー取得
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // テナント取得
        Tenant tenant = user.getTenantId() != null ? tenantRepository.findById(user.getTenantId()).orElse(null) : null;

        // 新しいアクセストークン生成
        String accessToken;
        if (isJwtEnabled && jwtService != null) {
            accessToken = jwtService.generateToken(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            user.getUserId(), null, List.of()));
        } else {
            accessToken = "session-based-auth-token";
            logger.warn("JWT is disabled. Using session-based authentication placeholder.");
        }

        // 有効ライセンス取得
        List<ApplicationLicense> licenses = licenseRepository.findEffectiveLicenses(
                user.getUserId(), tenant != null ? tenant.getTenantId() : null, Instant.now());

        logger.info("Token refresh successful for user: {}", user.getUserId());

        return buildAuthenticationResponse(user, tenant, accessToken, request.getRefreshToken(), licenses);
    }

    /**
     * ログアウト
     */
    @Transactional
    public void logout(String refreshToken) {
        logger.info("Logout attempt");

        if (refreshToken != null && !refreshToken.isEmpty()) {
            String tokenHash = hashToken(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
                logger.info("Refresh token revoked for user: {}", token.getUserId());
            });
        }
    }

    /**
     * テナント切替
     */
    @Transactional
    public UserContextDto switchTenant(String userId, String tenantId) {
        logger.info("Tenant switch attempt: user={}, tenant={}", userId, tenantId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ユーザーがテナントに所属しているか確認
        UserTenant userTenant = userTenantRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this tenant"));

        // デフォルトテナント更新
        // まず全てのUserTenantのisDefaultをfalseに
        userTenantRepository.findByUserId(userId).forEach(ut -> {
            ut.setIsDefault(false);
            userTenantRepository.save(ut);
        });

        // 選択したテナントをデフォルトに
        userTenant.setIsDefault(true);
        userTenantRepository.save(userTenant);

        // User のtenantIdも更新
        user.setTenantId(tenantId);
        userRepository.save(user);

        // Tenant取得
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        logger.info("Tenant switch successful: user={}, new tenant={}", userId, tenantId);

        return UserContextDto.builder()
                .user(UserDto.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .isActive(user.getIsActive())
                        .emailVerified(user.getEmailVerified())
                        .build())
                .currentTenant(TenantContextDto.builder()
                        .tenantId(tenant.getTenantId())
                        .tenantName(tenant.getTenantName())
                        .displayName(tenant.getDisplayName())
                        .build())
                .build();
    }

    /**
     * テナント解決
     */
    private Tenant resolveTenant(User user, String requestedTenantId) {
        String tenantId = requestedTenantId != null ? requestedTenantId : user.getTenantId();

        if (tenantId == null) {
            // デフォルトテナント取得
            UserTenant defaultUserTenant = userTenantRepository.findDefaultByUserId(user.getUserId())
                    .orElse(null);
            if (defaultUserTenant != null) {
                tenantId = defaultUserTenant.getTenantId();
            } else {
                tenantId = "default";
            }
        }

        return tenantRepository.findById(tenantId).orElse(null);
    }

    /**
     * デフォルトテナント取得または作成
     */
    private Tenant getOrCreateDefaultTenant() {
        return tenantRepository.findById("default").orElseGet(() -> {
            Tenant tenant = new Tenant();
            tenant.setTenantId("default");
            tenant.setTenantName("Default Workspace");
            tenant.setDisplayName("Default Workspace");
            tenant.setIsActive(true);
            return tenantRepository.save(tenant);
        });
    }

    /**
     * RefreshToken作成
     */
    private RefreshToken createRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        String tokenHash = hashToken(tokenValue);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getUserId());
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshToken.setDeviceInfo("web");
        refreshTokenRepository.save(refreshToken);

        // tokenHashにtokenValueを一時的に保存（レスポンス用）
        refreshToken.setTokenHash(tokenValue);
        return refreshToken;
    }

    /**
     * トークンハッシュ生成
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    /**
     * AuthenticationResponse構築
     */
    private AuthenticationResponse buildAuthenticationResponse(User user, Tenant tenant,
            String accessToken, String refreshToken, List<ApplicationLicense> licenses) {

        return AuthenticationResponse.builder()
                .user(UserDto.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .isActive(user.getIsActive())
                        .emailVerified(user.getEmailVerified())
                        .build())
                .tokens(TokenDto.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .expiresIn(authProperties.getJwt().getExpiration())
                        .build())
                .currentTenant(tenant != null ? TenantContextDto.builder()
                        .tenantId(tenant.getTenantId())
                        .tenantName(tenant.getTenantName())
                        .displayName(tenant.getDisplayName())
                        .build() : null)
                .build();
    }

    /**
     * ユーザーのロール文字列からSpring Security権限リストを生成
     * ロールはカンマ(,)またはパイプ(|)で区切られた文字列
     * 
     * @param user
     *            ユーザー
     * @return 権限リスト
     */
    private List<SimpleGrantedAuthority> buildAuthoritiesFromUser(User user) {
        String roles = user.getRoles();
        if (roles == null || roles.isBlank()) {
            // デフォルトでUSERロールを付与
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // カンマまたはパイプで区切り
        return Arrays.stream(roles.split("[,|]"))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> {
                    // ROLE_ プレフィックスがない場合は追加
                    if (!role.startsWith("ROLE_")) {
                        return new SimpleGrantedAuthority("ROLE_" + role.toUpperCase());
                    }
                    return new SimpleGrantedAuthority(role.toUpperCase());
                })
                .collect(Collectors.toList());
    }
}
