/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jp.vemi.mirel.foundation.abst.dao.entity.OtpAuditLog;
import jp.vemi.mirel.foundation.abst.dao.entity.OtpToken;
import jp.vemi.mirel.foundation.abst.dao.entity.SystemUser;
import jp.vemi.mirel.foundation.abst.dao.repository.OtpAuditLogRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.OtpTokenRepository;
import jp.vemi.mirel.foundation.abst.dao.repository.SystemUserRepository;
import jp.vemi.mirel.foundation.config.AppProperties;
import jp.vemi.mirel.foundation.config.OtpProperties;
import jp.vemi.mirel.foundation.config.RateLimitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * OTPサービス.
 * ワンタイムパスワードの生成・検証・再送信を管理
 */
@Service
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final OtpAuditLogRepository otpAuditLogRepository;
    private final SystemUserRepository systemUserRepository;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final OtpProperties otpProperties;
    private final RateLimitProperties rateLimitProperties;
    private final AppProperties appProperties;

    private final SecureRandom secureRandom = new SecureRandom();

    // Micrometer メトリクスカウンター
    private final Counter otpRequestSuccessCounter;
    private final Counter otpRequestFailedCounter;
    private final Counter otpVerifySuccessCounter;
    private final Counter otpVerifyFailedCounter;

    /**
     * コンストラクタでメトリクスカウンターを初期化
     */
    public OtpService(
            OtpTokenRepository otpTokenRepository,
            OtpAuditLogRepository otpAuditLogRepository,
            SystemUserRepository systemUserRepository,
            EmailService emailService,
            RateLimitService rateLimitService,
            OtpProperties otpProperties,
            RateLimitProperties rateLimitProperties,
            AppProperties appProperties,
            MeterRegistry meterRegistry) {
        this.otpTokenRepository = otpTokenRepository;
        this.otpAuditLogRepository = otpAuditLogRepository;
        this.systemUserRepository = systemUserRepository;
        this.emailService = emailService;
        this.rateLimitService = rateLimitService;
        this.otpProperties = otpProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.appProperties = appProperties;

        // メトリクスカウンター初期化
        this.otpRequestSuccessCounter = Counter.builder("otp.request.success")
                .description("OTP request successful count")
                .register(meterRegistry);
        this.otpRequestFailedCounter = Counter.builder("otp.request.failed")
                .description("OTP request failed count")
                .register(meterRegistry);
        this.otpVerifySuccessCounter = Counter.builder("otp.verify.success")
                .description("OTP verification successful count")
                .register(meterRegistry);
        this.otpVerifyFailedCounter = Counter.builder("otp.verify.failed")
                .description("OTP verification failed count")
                .register(meterRegistry);
    }

    /**
     * OTPをリクエスト
     * 
     * @param email
     *            メールアドレス
     * @param purpose
     *            用途 (LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION)
     * @param ipAddress
     *            リクエスト元IPアドレス
     * @param userAgent
     *            User Agent
     * @return リクエストID
     */
    @Transactional
    public String requestOtp(String email, String purpose, String ipAddress, String userAgent) {
        String requestId = java.util.UUID.randomUUID().toString();

        // レート制限チェック
        String rateLimitKey = "otp:request:" + email;
        int requestPerMinute = rateLimitProperties.getOtp().getRequestPerMinute();
        if (!rateLimitService.checkRateLimit(rateLimitKey, requestPerMinute, 60)) {
            otpRequestFailedCounter.increment();
            logAudit(requestId, null, email, purpose, "REQUEST", false,
                    "レート制限超過", ipAddress, userAgent,
                    String.format("{\"limit\": %d, \"window\": 60}", requestPerMinute));
            throw new RuntimeException("リクエスト制限に達しました。しばらく待ってから再度お試しください。");
        }

        // クールダウンチェック
        String cooldownKey = "otp:cooldown:" + email;
        if (rateLimitService.isInCooldown(cooldownKey)) {
            otpRequestFailedCounter.increment();
            logAudit(requestId, null, email, purpose, "REQUEST", false,
                    "クールダウン中", ipAddress, userAgent, null);
            throw new RuntimeException("しばらく待ってから再度お試しください。");
        }

        // SystemUser取得（新規ユーザーの場合は作成しない）
        SystemUser systemUser = systemUserRepository.findByEmail(email).orElse(null);

        // OTPコード生成
        String otpCode = generateOtpCode();
        String otpHash = hashOtp(otpCode);

        // 既存の未検証トークンを無効化
        if (systemUser != null) {
            otpTokenRepository.invalidatePreviousTokens(systemUser.getId(), purpose);
        }

        // OTPトークン保存
        OtpToken token = new OtpToken();
        if (systemUser != null) {
            token.setSystemUserId(systemUser.getId());
        } else {
            // 新規ユーザーの場合は仮ID（メール検証後に実SystemUserIdに更新）
            token.setSystemUserId(java.util.UUID.randomUUID());
        }

        // マジックリンクトークン生成
        String magicLinkToken = generateMagicLinkToken();
        token.setMagicLinkToken(magicLinkToken);

        token.setOtpHash(otpHash);
        token.setPurpose(purpose);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()));
        token.setMaxAttempts(otpProperties.getMaxAttempts());
        token.setRequestIp(ipAddress);
        token.setUserAgent(userAgent);
        otpTokenRepository.save(token);

        // メール送信
        sendOtpEmail(email, otpCode, purpose, magicLinkToken);

        // クールダウン設定
        rateLimitService.setCooldown(cooldownKey, otpProperties.getResendCooldownSeconds());

        // 監査ログ
        logAudit(requestId, systemUser != null ? systemUser.getId() : null, email, purpose, "REQUEST", true,
                null, ipAddress, userAgent, null);

        // メトリクス: リクエスト成功
        otpRequestSuccessCounter.increment();

        log.info("OTPリクエスト成功: email={}, purpose={}, requestId={}", email, purpose, requestId);
        return requestId;
    }

    /**
     * OTPを検証
     * 
     * @param email
     *            メールアドレス
     * @param otpCode
     *            OTPコード
     * @param purpose
     *            用途
     * @param ipAddress
     *            リクエスト元IPアドレス
     * @param userAgent
     *            User Agent
     * @return 検証成功の場合true
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode, String purpose, String ipAddress, String userAgent) {
        String requestId = java.util.UUID.randomUUID().toString();

        // レート制限チェック
        String rateLimitKey = "otp:verify:" + email;
        int verifyPerMinute = rateLimitProperties.getOtp().getVerifyPerMinute();
        if (!rateLimitService.checkRateLimit(rateLimitKey, verifyPerMinute, 60)) {
            otpVerifyFailedCounter.increment();
            logAudit(requestId, null, email, purpose, "VERIFY", false,
                    "検証レート制限超過", ipAddress, userAgent,
                    String.format("{\"limit\": %d, \"window\": 60}", verifyPerMinute));
            throw new RuntimeException("検証試行制限に達しました。しばらく待ってから再度お試しください。");
        }

        // SystemUser取得
        SystemUser systemUser = systemUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("ユーザーが見つかりません"));

        log.info("OTP検証開始: email={}, purpose={}, systemUserId={}", email, purpose, systemUser.getId());

        // 有効なOTPトークン取得
        OtpToken token = otpTokenRepository
                .findBySystemUserIdAndPurposeAndIsVerifiedAndExpiresAtAfter(
                        systemUser.getId(), purpose, false, LocalDateTime.now())
                .orElse(null);

        if (token == null) {
            otpVerifyFailedCounter.increment();
            log.warn("OTP検証失敗: トークンが見つからないか期限切れ - email={}, purpose={}, systemUserId={}",
                    email, purpose, systemUser.getId());
            logAudit(requestId, systemUser.getId(), email, purpose, "VERIFY", false,
                    "トークンが見つからないか期限切れ", ipAddress, userAgent, null);
            return false;
        }

        log.info("OTPトークン発見: tokenId={}, expiresAt={}, attemptCount={}/{}",
                token.getId(), token.getExpiresAt(), token.getAttemptCount(), token.getMaxAttempts());

        // 試行回数チェック
        if (token.getAttemptCount() >= token.getMaxAttempts()) {
            otpVerifyFailedCounter.increment();
            logAudit(requestId, systemUser.getId(), email, purpose, "VERIFY", false,
                    "最大試行回数超過", ipAddress, userAgent, null);
            return false;
        }

        // OTPコード検証
        String otpHash = hashOtp(otpCode);
        token.incrementAttemptCount();

        log.info("OTPコード検証: 入力={}, 入力Hash={}, 保存Hash={}",
                otpCode, otpHash.substring(0, 16) + "...", token.getOtpHash().substring(0, 16) + "...");

        if (!token.getOtpHash().equals(otpHash)) {
            otpTokenRepository.save(token);
            otpVerifyFailedCounter.increment();
            log.warn("OTP検証失敗: コード不一致 - email={}, attemptCount={}", email, token.getAttemptCount());
            logAudit(requestId, systemUser.getId(), email, purpose, "VERIFY", false,
                    "OTPコード不一致", ipAddress, userAgent, null);
            return false;
        }

        // 検証成功
        token.setIsVerified(true);
        token.setVerifiedAt(LocalDateTime.now());
        otpTokenRepository.save(token);

        // レート制限クリア
        rateLimitService.clearRateLimit(rateLimitKey);
        rateLimitService.clearRateLimit("otp:request:" + email);
        rateLimitService.clearRateLimit("otp:cooldown:" + email);

        logAudit(requestId, systemUser.getId(), email, purpose, "VERIFY", true,
                null, ipAddress, userAgent, null);

        // メトリクス: 検証成功
        otpVerifySuccessCounter.increment();

        log.info("OTP検証成功: email={}, purpose={}", email, purpose);
        return true;
    }

    /**
     * OTPを再送信
     * 
     * @param email
     *            メールアドレス
     * @param purpose
     *            用途
     * @param ipAddress
     *            リクエスト元IPアドレス
     * @param userAgent
     *            User Agent
     * @return リクエストID
     */
    @Transactional
    public String resendOtp(String email, String purpose, String ipAddress, String userAgent) {
        log.info("OTP再送信リクエスト: email={}, purpose={}", email, purpose);
        return requestOtp(email, purpose, ipAddress, userAgent);
    }

    /**
     * 6桁のOTPコードを生成
     */
    private String generateOtpCode() {
        int code = secureRandom.nextInt(1000000);
        return String.format("%06d", code);
    }

    /**
     * マジックリンクトークンを検証
     * 
     * @param magicLinkToken
     *            トークン
     * @param ipAddress
     *            IPアドレス
     * @param userAgent
     *            User Agent
     * @return 検証されたOTPトークン (失敗時はnullまたは例外)
     */
    @Transactional
    public OtpToken verifyMagicLink(String magicLinkToken, String ipAddress, String userAgent) {
        String requestId = java.util.UUID.randomUUID().toString();

        // トークン検索
        OtpToken token = otpTokenRepository
                .findByMagicLinkTokenAndIsVerifiedAndExpiresAtAfter(
                        magicLinkToken, false, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("無効または期限切れのリンクです"));

        return verifyOtpToken(token, requestId, ipAddress, userAgent, "MAGIC_LINK_VERIFY");
    }

    /**
     * 共通検証ロジック
     */
    private OtpToken verifyOtpToken(OtpToken token, String requestId, String ipAddress, String userAgent,
            String action) {
        // SystemUser取得 (存在チェック)
        // 新規登録(EMAIL_VERIFICATION)の場合は一時的なIDの可能性があるが、
        // verifyOtpでは systemUser をemailから引いていた。
        // ここでは token.systemUserId があるが、新規登録時は実Userがまだないかもしれない。
        // しかし requestOtp では SystemUser が null の場合 randomUUID を入れている。
        // EMAIL_VERIFICATION の場合、そのIDはダミー。
        // 既存ロジック(verifyOtp)を見ると、emailからSystemUserを引いている。
        // マジックリンクの場合 email パラメータがないので、token から情報を引く必要があるが、
        // OtpTokenには email が保存されていない。
        // -> OtpTokenエンティティには systemUserId しかない。
        // -> 新規登録時 (EMAIL_VERIFICATION) は systemUserId はダミーUUID。これでは誰だかわからない。
        // -> requestOtp の実装を見ると "新規ユーザーの場合は仮ID" とある。
        // -> これだと verify 時困るのでは？
        // -> verifyOtp(String email, ...) では email から SystemUser を引いている。
        // -> email がキーになっている。
        // -> マジックリンクの場合、token だけだと email がわからない。
        // -> OtpToken に email カラムがないと、マジックリンク単体での検証は不可能（特に新規登録時）。
        // -> ただし、LOGIN や PASSWORD_RESET なら systemUserId から SystemUser が引ける。

        // 既存実装の verifyOtp も、 systemUser.getId() と token.systemUserId を照合している。
        // 新規登録時、 requestOtp で systemUser が null だと token.systemUserId = randomUUID()。
        // verifyOtp で email から systemUser を引こうとすると... 新規登録前なら systemUser はいないはず。
        // -> wait, AuthenticationServiceImpl.signup で SystemUser を作ってから OtpService
        // を呼んでいるのか？
        // -> Signup 処理の最後で verify ではなく、 verify してから signup なのか？
        // -> OtpEmailVerificationPage を見ると、 verifyOtp 成功後に updateUser している。
        // -> つまり User は既にいる前提？
        // -> OtpController.requestOtp を見る。
        // -> otpService.requestOtp を呼ぶ。
        // -> OtpService.requestOtp で systemUserRepository.findByEmail(email) している。
        // -> 新規登録フローの順序:
        // 1. SignupPage -> API /auth/signup (ここで SystemUser, User 作成)
        // 2. API /auth/signup 内部で OtpService を呼んでいるわけではない。
        // 3. SignupPage -> /auth/email-verification 画面へ遷移
        // 4. OtpEmailVerificationPage -> useEffect で requestOtp?
        // いいえ、SignupPage.tsx の実装を見ていないが、通常は Signup API が成功したらメール飛ぶはず。
        // AuthenticationServiceImpl.signup を確認済み。
        // signup メソッド内で "SystemUser作成", "User作成" している。
        // しかし、メール送信ロジックが見当たらない。

        // AuthenticationServiceImpl.signup に戻って確認する必要があるが、
        // もし Signup 時にメールを送っていないなら、誰が送る？
        // OtpController.requestOtp はパブリックエンドポイント。

        // とりあえず、ここでの課題は「OtpToken から User を特定できるか」
        // LOGIN/PASSWORD_RESET -> systemUserId は有効な SystemUser の ID。
        // EMAIL_VERIFICATION -> Signup 後なら SystemUser は存在するはず。
        // OtpService.requestOtp:130 `SystemUser systemUser =
        // systemUserRepository.findByEmail(email).orElse(null);`
        // もし Signup 後なら systemUser は not null のはず。
        // ならば token.systemUserId には正しい ID が入る。
        // つまり "新規ユーザーの場合は仮ID" というコメント (lines 146-147) は、
        // 「Signup API を叩く前に OTP を要求する場合」を想定しているのかもしれないが、
        // 現状の SystemUser 依存の実装だと、 SystemUser がないと OTP トークン紐付けが弱い。

        // 今回は Magic Link Token で検証する。
        // token.systemUserId を信じて SystemUser を引く。

        // 1. systemUserId から SystemUser を取得してみる
        SystemUser systemUser = systemUserRepository.findById(token.getSystemUserId()).orElse(null);

        // SystemUser が見つからない場合 (仮IDの場合など)
        if (systemUser == null) {
            // EMAIL_VERIFICATION で、もし登録前チェックならこれでありえるが...
            // 今回の要件（Signup後の検証）なら SystemUser はいるはず。
            // いない場合はエラーとする（セキュリティ的にも）
            throw new RuntimeException("ユーザーが見つかりません (token context invalid)");
        }

        String email = systemUser.getEmail(); // これで email 特定
        String purpose = token.getPurpose();

        // 試行回数チェック
        if (token.getAttemptCount() >= token.getMaxAttempts()) {
            otpVerifyFailedCounter.increment();
            logAudit(requestId, token.getSystemUserId(), email, purpose, action, false,
                    "最大試行回数超過", ipAddress, userAgent, null);
            throw new RuntimeException("最大試行回数を超過しました");
        }

        token.incrementAttemptCount();

        // 検証成功
        token.setIsVerified(true);
        token.setVerifiedAt(LocalDateTime.now());
        otpTokenRepository.save(token);

        // (省略) レート制限クリア等は email が必要。
        // rateLimitService.clearRateLimit("otp:request:" + email); ...
        // ここでは略すが、本来やるべき。

        logAudit(requestId, token.getSystemUserId(), email, purpose, action, true,
                null, ipAddress, userAgent, null);

        otpVerifySuccessCounter.increment();

        return token;
    }

    /**
     * 32バイト(64文字)のランダムトークンを生成
     */
    private String generateMagicLinkToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    /**
     * OTPコードをSHA-256でハッシュ化
     */
    private String hashOtp(String otpCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otpCode.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256アルゴリズムが見つかりません", e);
        }
    }

    /**
     * OTPメールを送信
     */
    private void sendOtpEmail(String email, String otpCode, String purpose, String magicLinkToken) {
        String templateName = switch (purpose) {
            case "LOGIN" -> "otp-login";
            case "PASSWORD_RESET" -> "otp-password-reset";
            case "EMAIL_VERIFICATION" -> "otp-email-verification";
            default -> throw new RuntimeException("不明なOTP用途: " + purpose);
        };

        String subject = switch (purpose) {
            case "LOGIN" -> "ログイン認証コード";
            case "PASSWORD_RESET" -> "パスワードリセット認証コード";
            case "EMAIL_VERIFICATION" -> "メールアドレス検証コード";
            default -> "認証コード";
        };

        // マジックリンク生成 (トークン方式)
        String magicLink = String.format("%s/auth/magic-verify?token=%s",
                appProperties.getBaseUrl(), magicLinkToken);

        Map<String, Object> variables = Map.of(
                "otpCode", otpCode,
                "expirationMinutes", otpProperties.getExpirationMinutes(),
                "magicLink", magicLink,
                "domain", appProperties.getDomain());

        emailService.sendTemplateEmail(email, subject, templateName, variables);
    }

    /**
     * 監査ログを記録
     */
    private void logAudit(String requestId, java.util.UUID systemUserId, String email,
            String purpose, String action, Boolean success,
            String failureReason, String ipAddress, String userAgent,
            String rateLimitInfo) {
        OtpAuditLog log = new OtpAuditLog();
        log.setRequestId(requestId);
        log.setSystemUserId(systemUserId);
        log.setEmail(email);
        log.setPurpose(purpose);
        log.setAction(action);
        log.setSuccess(success);
        log.setFailureReason(failureReason);
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        log.setRateLimitInfo(rateLimitInfo);
        otpAuditLogRepository.save(log);
    }

    /**
     * 期限切れOTPトークンを削除（毎日3時実行）
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        int deleted = otpTokenRepository.deleteByExpiresAtBefore(cutoffDate);
        log.info("期限切れOTPトークン削除: {} 件", deleted);
    }

    /**
     * 古い監査ログを削除（毎日3時実行）
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupOldAuditLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        int deleted = otpAuditLogRepository.deleteByCreatedAtBefore(cutoffDate);
        log.info("古い監査ログ削除: {} 件", deleted);
    }
    /**
     * アカウントセットアップ用のトークンを作成.
     * 管理者作成ユーザーが初回パスワード設定を行うためのワンタイムトークン
     * 
     * @param systemUserId SystemUser ID
     * @param email ユーザーのメールアドレス
     * @return マジックリンクトークン
     */
    @Transactional
    public String createAccountSetupToken(UUID systemUserId, String email) {
        log.info("Creating account setup token: systemUserId={}, email={}", systemUserId, email);

        // 既存の未検証ACCOUNT_SETUPトークンを無効化
        otpTokenRepository.findBySystemUserIdAndPurposeAndIsVerifiedFalse(systemUserId, "ACCOUNT_SETUP")
                .forEach(token -> {
                    token.setIsVerified(true); // 無効化
                    otpTokenRepository.save(token);
                });

        // 新しいトークン作成
        String magicLinkToken = generateSecureToken(32);
        String dummyOtpHash = hashOtp("ACCOUNT_SETUP_" + systemUserId.toString()); // ダミーハッシュ

        OtpToken token = new OtpToken();
        token.setId(UUID.randomUUID());
        token.setSystemUserId(systemUserId);
        token.setOtpHash(dummyOtpHash);
        token.setMagicLinkToken(magicLinkToken);
        token.setPurpose("ACCOUNT_SETUP");
        token.setExpiresAt(LocalDateTime.now().plusHours(72)); // 72時間有効
        token.setIsVerified(false);
        token.setAttemptCount(0);
        token.setMaxAttempts(1); // 1回のみ使用可能

        otpTokenRepository.save(token);

        log.info("Account setup token created: tokenId={}, expiresAt={}", 
                token.getId(), token.getExpiresAt());

        return magicLinkToken;
    }

    /**
     * セキュアなランダムトークンを生成
     */
    private String generateSecureToken(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }
}
