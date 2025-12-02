# OTP認証基盤 実装計画書 v1.0

**作成日**: 2025年11月23日  
**対象Issue**: #40 - SaaS multi-tenant infrastructure  
**ステータス**: Draft - レビュー待ち

---

## 1. Executive Summary

### 1.1 概要

mirelplatformにパスワードレスOTP(One-Time Password)認証をデフォルト実装し、エンタープライズグレードのセキュリティ・可用性・監査機能を提供します。GitHub OAuth2統合、Redis分散レート制限、メールドメイン検証、招待制テナント管理、ユーザーアバター機能を含む包括的な認証基盤を構築します。

### 1.2 主要機能

| 機能 | 説明 | 優先度 |
|---|---|---|
| **OTPメール認証** | 6桁コードをメールで送信するパスワードレス認証 | P0 |
| **GitHub OAuth2** | GitHubアカウントでのソーシャルログイン | P0 |
| **Redis分散レート制限** | 水平スケーリング対応のAPI保護 | P0 |
| **メールドメイン検証** | テナント別の許可ドメイン管理 | P1 |
| **招待制テナント** | 管理者による招待トークン発行 | P1 |
| **ユーザーアバター** | プロフィール画像のアップロード・表示 | P1 |
| **監査ログ** | 全OTP操作の追跡・分析 | P0 |

### 1.3 技術スタック

- **Backend**: Spring Boot 3.3, Spring Security OAuth2, Redis (Lettuce), Azure Communication Services
- **Frontend**: React 19, Zustand, TanStack Query, Vite
- **Infrastructure**: Redis 7 (分散キャッシュ), MailHog (開発), Azure (本番)

---

## 2. Architecture Overview

### 2.1 認証フロー全体像

```
┌─────────────────────────────────────────────────────────────────┐
│                        Authentication Flow                       │
└─────────────────────────────────────────────────────────────────┘

【パスワードレスOTP認証】
1. User → Frontend: メールアドレス入力
2. Frontend → Backend: POST /auth/otp/request {email, purpose: "LOGIN"}
3. Backend → Redis: レート制限チェック (3回/分)
4. Backend → DB: OtpToken生成・保存 (SHA-256ハッシュ化)
5. Backend → Azure Email: OTPコード送信 (6桁)
6. User → Frontend: メールからOTPコード入力
7. Frontend → Backend: POST /auth/otp/verify {email, otpCode, purpose}
8. Backend → DB: OTP検証 (有効期限・試行回数チェック)
9. Backend → User: JWT発行 (accessToken + refreshToken)

【GitHub OAuth2認証】
1. User → Frontend: 「GitHubでログイン」クリック
2. Frontend → GitHub: OAuth2 認可リクエスト
3. GitHub → User: 認証画面表示
4. User → GitHub: 認証許可
5. GitHub → Backend: コールバック (/login/oauth2/code/github)
6. Backend → GitHub: アクセストークン取得
7. Backend → GitHub API: ユーザー情報取得 (email, name, avatar_url)
8. Backend → DB: SystemUser検索 or 新規作成
9. Backend → User: JWT発行 + プロフィール補完リダイレクト
```

### 2.2 システムコンポーネント図

```
┌──────────────┐
│   Frontend   │ (React 19 + Vite)
│              │
│ - LoginPage  │
│ - OtpVerify  │
│ - Signup     │
└──────┬───────┘
       │ HTTPS
       ▼
┌──────────────────────────────────────────┐
│         Backend (Spring Boot)             │
│                                           │
│  ┌────────────────────────────────────┐  │
│  │  OtpController                     │  │
│  │  - POST /auth/otp/request          │  │
│  │  - POST /auth/otp/verify           │  │
│  │  - POST /auth/otp/resend           │  │
│  └───────────┬────────────────────────┘  │
│              ▼                            │
│  ┌────────────────────────────────────┐  │
│  │  OtpService                        │  │
│  │  - requestOtp()                    │  │
│  │  - verifyOtp()                     │  │
│  │  - resendOtp()                     │  │
│  └───┬───────────┬────────────────────┘  │
│      │           │                        │
│      ▼           ▼                        │
│  ┌─────────┐  ┌────────────────────────┐ │
│  │ OtpToken│  │  RateLimitService      │ │
│  │ Entity  │  │  (Redis distributed)   │ │
│  └─────────┘  └────────────────────────┘ │
│      │                                    │
│      ▼                                    │
│  ┌────────────────────────────────────┐  │
│  │  EmailService (Azure)              │  │
│  │  - sendOtpEmail()                  │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
       │                    │
       ▼                    ▼
┌──────────────┐    ┌──────────────┐
│  PostgreSQL  │    │   Redis 7    │
│  (OtpToken,  │    │  (RateLimit, │
│   AuditLog)  │    │   Sessions)  │
└──────────────┘    └──────────────┘
       │
       ▼
┌──────────────┐
│ Azure Comm   │
│ Services     │
│ (Email)      │
└──────────────┘
```

---

## 3. Database Schema Design

### 3.1 新規テーブル

#### 3.1.1 `mir_otp_token` - OTPトークン管理

```sql
CREATE TABLE mir_otp_token (
    id UUID PRIMARY KEY,
    system_user_id UUID NOT NULL REFERENCES mir_system_user(id) ON DELETE CASCADE,
    otp_hash VARCHAR(64) NOT NULL,  -- SHA-256ハッシュ
    purpose VARCHAR(50) NOT NULL,   -- LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION
    expires_at TIMESTAMP NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    request_ip VARCHAR(45),  -- IPv6対応
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_purpose_verified (system_user_id, purpose, is_verified, expires_at),
    INDEX idx_expires_at (expires_at)  -- クリーンアップ用
);
```

#### 3.1.2 `mir_otp_audit_log` - OTP監査ログ

```sql
CREATE TABLE mir_otp_audit_log (
    id UUID PRIMARY KEY,
    request_id VARCHAR(36),  -- ExecutionContext.requestId連携
    system_user_id UUID REFERENCES mir_system_user(id) ON DELETE SET NULL,
    email VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    action VARCHAR(20) NOT NULL,  -- REQUEST, VERIFY, RESEND, EXPIRE, RATE_LIMIT
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(500),
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    rate_limit_info JSONB,  -- {remaining: 2, resetAt: "2025-11-23T..."}
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_created (system_user_id, created_at),
    INDEX idx_email_created (email, created_at),
    INDEX idx_created_at (created_at)  -- 保持期限管理用
);
```

#### 3.1.3 `mir_tenant_email_domain_rule` - テナント別メールドメインルール

```sql
CREATE TABLE mir_tenant_email_domain_rule (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL REFERENCES mir_tenant(tenant_id) ON DELETE CASCADE,
    domain VARCHAR(255) NOT NULL,
    rule_type VARCHAR(10) NOT NULL,  -- ALLOW, BLOCK
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by UUID REFERENCES mir_system_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE (tenant_id, domain),
    INDEX idx_tenant_active (tenant_id, is_active)
);
```

#### 3.1.4 `mir_invitation_token` - 招待トークン

```sql
CREATE TABLE mir_invitation_token (
    id UUID PRIMARY KEY,
    token VARCHAR(64) NOT NULL UNIQUE,  -- URL-safe random token
    email VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL REFERENCES mir_tenant(tenant_id) ON DELETE CASCADE,
    invited_by UUID NOT NULL REFERENCES mir_system_user(id),
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMP,
    used_by UUID REFERENCES mir_system_user(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_token (token),
    INDEX idx_email_tenant (email, tenant_id),
    INDEX idx_expires_at (expires_at)
);
```

### 3.2 既存テーブル変更

#### 3.2.1 `mir_system_user` - OAuth2・アバター対応

```sql
ALTER TABLE mir_system_user 
ADD COLUMN oauth_provider VARCHAR(20),           -- GITHUB, GOOGLE, APPLE
ADD COLUMN oauth_provider_id VARCHAR(255),       -- GitHub User ID等
ADD COLUMN avatar_url VARCHAR(500),              -- プロフィール画像URL
ADD COLUMN avatar_storage_path VARCHAR(500),     -- ローカルストレージパス
ADD COLUMN profile_completed BOOLEAN DEFAULT TRUE;

CREATE UNIQUE INDEX idx_oauth_provider ON mir_system_user(oauth_provider, oauth_provider_id) 
WHERE oauth_provider IS NOT NULL;
```

#### 3.2.2 `mir_user` - アバター同期

```sql
ALTER TABLE mir_user
ADD COLUMN avatar_url VARCHAR(500);
```

---

## 4. Backend Implementation Details

### 4.1 依存関係追加

#### `backend/build.gradle`

```gradle
dependencies {
    // 既存依存関係...
    
    // Redis (分散レート制限・セッション管理)
    implementation 'org.springframework.boot:spring-boot-starter-data-redis:3.3.0'
    implementation 'io.lettuce:lettuce-core:6.3.2.RELEASE'
    
    // Azure Communication Services (メール送信)
    implementation 'com.azure:azure-communication-email:1.0.0'
    
    // SecureRandom強化 (OTPコード生成)
    implementation 'org.bouncycastle:bcprov-jdk18on:1.77'
}
```

### 4.2 設定ファイル

#### `backend/src/main/resources/config/application.yml`

```yaml
# OTP設定
otp:
  enabled: true
  length: 6                       # OTPコード桁数
  expiration-minutes: 5           # 有効期限 (分)
  max-attempts: 3                 # 最大検証試行回数
  resend-cooldown-seconds: 60     # 再送信クールダウン (秒)

# Redis設定
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379
      password: ${REDIS_PASSWORD:}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms

# Azure Communication Services
azure:
  communication:
    connection-string: ${AZURE_COMMUNICATION_CONNECTION_STRING:}
    email:
      from: noreply_mirel@vemi.jp
      from-name: mirelplatform

# メール送信設定 (開発環境はMailHog)
email:
  provider: ${EMAIL_PROVIDER:azure}  # azure | smtp
  smtp:
    host: ${SMTP_HOST:localhost}
    port: ${SMTP_PORT:1025}
    username: ${SMTP_USERNAME:}
    password: ${SMTP_PASSWORD:}

# GitHub OAuth2
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID:}
            client-secret: ${GITHUB_CLIENT_SECRET:}
            scope:
              - user:email
              - read:user
        provider:
          github:
            user-name-attribute: login

# レート制限設定
rate-limit:
  otp:
    request-per-minute: 3   # OTP要求: 3回/分
    verify-per-minute: 5    # OTP検証: 5回/分
  redis:
    fallback-to-memory: true  # Redis障害時インメモリ使用
```

#### `backend/src/main/resources/config/application-dev.yml`

```yaml
# 開発環境設定
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ""

email:
  provider: smtp
  smtp:
    host: localhost
    port: 1025  # MailHog

otp:
  expiration-minutes: 10  # 開発環境は長め

rate-limit:
  otp:
    request-per-minute: 10  # 開発環境は緩和
    verify-per-minute: 20
```

### 4.3 Entity実装

#### `OtpToken.java`

```java
package jp.vemi.mirel.foundation.abst.dao.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mir_otp_token")
@Getter
@Setter
public class OtpToken {
    
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    @Column(name = "system_user_id", nullable = false, columnDefinition = "UUID")
    private UUID systemUserId;
    
    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;
    
    @Column(name = "purpose", nullable = false, length = 50)
    private String purpose;  // LOGIN, PASSWORD_RESET, EMAIL_VERIFICATION
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;
    
    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 3;
    
    @Column(name = "request_ip", length = 45)
    private String requestIp;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (isVerified == null) isVerified = false;
        if (attemptCount == null) attemptCount = 0;
        if (maxAttempts == null) maxAttempts = 3;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isValid() {
        return !isVerified && !isExpired() && attemptCount < maxAttempts;
    }
    
    public void incrementAttemptCount() {
        this.attemptCount++;
    }
}
```

### 4.4 Service Layer実装

#### `OtpService.java` - コアロジック

```java
package jp.vemi.mirel.foundation.web.api.auth.service;

import jp.vemi.mirel.foundation.abst.dao.entity.*;
import jp.vemi.mirel.foundation.abst.dao.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {
    
    private final OtpTokenRepository otpTokenRepository;
    private final SystemUserRepository systemUserRepository;
    private final EmailService emailService;
    private final OtpProperties otpProperties;
    private final OtpAuditLogRepository auditLogRepository;
    private final RateLimitService rateLimitService;
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * OTPコード要求
     */
    @Transactional
    public void requestOtp(String email, String purpose, String requestIp, String userAgent) {
        // レート制限チェック
        RateLimitResult rateLimit = rateLimitService.checkRateLimit(
            "otp:request:" + requestIp, 
            otpProperties.getRequestPerMinute(), 
            60
        );
        
        if (!rateLimit.isAllowed()) {
            auditLog(null, email, purpose, "REQUEST", false, 
                "Rate limit exceeded", requestIp, userAgent, rateLimit);
            throw new RateLimitExceededException("Too many OTP requests. Please try again later.");
        }
        
        // SystemUser検索
        SystemUser systemUser = systemUserRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // 古い未検証トークンを無効化
        otpTokenRepository.invalidatePreviousTokens(systemUser.getId(), purpose);
        
        // 6桁OTPコード生成
        String otpCode = generateOtpCode(otpProperties.getLength());
        String otpHash = hashOtp(otpCode);
        
        // OtpToken保存
        OtpToken otpToken = new OtpToken();
        otpToken.setSystemUserId(systemUser.getId());
        otpToken.setOtpHash(otpHash);
        otpToken.setPurpose(purpose);
        otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(otpProperties.getExpirationMinutes()));
        otpToken.setRequestIp(requestIp);
        otpToken.setUserAgent(userAgent);
        otpToken.setMaxAttempts(otpProperties.getMaxAttempts());
        otpTokenRepository.save(otpToken);
        
        // メール送信
        emailService.sendOtpEmail(email, otpCode, purpose);
        
        // 監査ログ
        auditLog(systemUser.getId(), email, purpose, "REQUEST", true, 
            null, requestIp, userAgent, rateLimit);
        
        log.info("OTP requested for user: {} (purpose: {})", email, purpose);
    }
    
    /**
     * OTP検証
     */
    @Transactional
    public UUID verifyOtp(String email, String otpCode, String purpose, String requestIp, String userAgent) {
        // レート制限チェック
        RateLimitResult rateLimit = rateLimitService.checkRateLimit(
            "otp:verify:" + email, 
            otpProperties.getVerifyPerMinute(), 
            60
        );
        
        if (!rateLimit.isAllowed()) {
            auditLog(null, email, purpose, "VERIFY", false, 
                "Rate limit exceeded", requestIp, userAgent, rateLimit);
            throw new RateLimitExceededException("Too many verification attempts.");
        }
        
        SystemUser systemUser = systemUserRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        String otpHash = hashOtp(otpCode);
        
        // 有効なOTPトークン検索
        OtpToken otpToken = otpTokenRepository
            .findBySystemUserIdAndPurposeAndIsVerifiedFalseAndExpiresAtAfter(
                systemUser.getId(), purpose, LocalDateTime.now())
            .orElseThrow(() -> {
                auditLog(systemUser.getId(), email, purpose, "VERIFY", false, 
                    "Invalid or expired OTP", requestIp, userAgent, rateLimit);
                return new IllegalArgumentException("Invalid or expired OTP");
            });
        
        // 試行回数チェック
        if (otpToken.getAttemptCount() >= otpToken.getMaxAttempts()) {
            auditLog(systemUser.getId(), email, purpose, "VERIFY", false, 
                "Max attempts exceeded", requestIp, userAgent, rateLimit);
            throw new IllegalStateException("OTP verification attempts exceeded");
        }
        
        // ハッシュ比較
        if (!otpHash.equals(otpToken.getOtpHash())) {
            otpToken.incrementAttemptCount();
            otpTokenRepository.save(otpToken);
            auditLog(systemUser.getId(), email, purpose, "VERIFY", false, 
                "Invalid OTP code", requestIp, userAgent, rateLimit);
            throw new IllegalArgumentException("Invalid OTP code");
        }
        
        // 検証成功
        otpToken.setIsVerified(true);
        otpToken.setVerifiedAt(LocalDateTime.now());
        otpTokenRepository.save(otpToken);
        
        // 監査ログ
        auditLog(systemUser.getId(), email, purpose, "VERIFY", true, 
            null, requestIp, userAgent, rateLimit);
        
        log.info("OTP verified successfully for user: {}", email);
        return systemUser.getId();
    }
    
    /**
     * OTP再送信
     */
    @Transactional
    public void resendOtp(String email, String purpose, String requestIp, String userAgent) {
        // クールダウンチェック (Redisで管理)
        String cooldownKey = "otp:cooldown:" + email + ":" + purpose;
        if (rateLimitService.isInCooldown(cooldownKey, otpProperties.getResendCooldownSeconds())) {
            throw new IllegalStateException("Please wait before requesting a new OTP");
        }
        
        // 既存のrequestOtpを再利用
        requestOtp(email, purpose, requestIp, userAgent);
        
        // クールダウン設定
        rateLimitService.setCooldown(cooldownKey, otpProperties.getResendCooldownSeconds());
        
        auditLog(null, email, purpose, "RESEND", true, 
            null, requestIp, userAgent, null);
    }
    
    private String generateOtpCode(int length) {
        int bound = (int) Math.pow(10, length);
        int code = secureRandom.nextInt(bound);
        return String.format("%0" + length + "d", code);
    }
    
    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    private void auditLog(UUID userId, String email, String purpose, String action, 
                         boolean success, String failureReason, String ip, String userAgent,
                         RateLimitResult rateLimitInfo) {
        OtpAuditLog log = new OtpAuditLog();
        log.setSystemUserId(userId);
        log.setEmail(email);
        log.setPurpose(purpose);
        log.setAction(action);
        log.setSuccess(success);
        log.setFailureReason(failureReason);
        log.setIpAddress(ip);
        log.setUserAgent(userAgent);
        if (rateLimitInfo != null) {
            log.setRateLimitInfo(String.format("{\"remaining\":%d,\"resetAt\":\"%s\"}", 
                rateLimitInfo.getRemaining(), rateLimitInfo.getResetAt()));
        }
        auditLogRepository.save(log);
    }
}
```

#### `RateLimitService.java` - Redis分散レート制限

```java
package jp.vemi.mirel.foundation.web.api.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, RateLimitCounter> memoryFallback = new ConcurrentHashMap<>();
    
    /**
     * レート制限チェック (Redis分散)
     */
    public RateLimitResult checkRateLimit(String key, int limit, int windowSeconds) {
        try {
            String redisKey = "ratelimit:" + key;
            Long current = redisTemplate.opsForValue().increment(redisKey);
            
            if (current == 1) {
                // 初回アクセス: TTL設定
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
            
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            Instant resetAt = Instant.now().plusSeconds(ttl != null ? ttl : windowSeconds);
            
            return RateLimitResult.builder()
                .allowed(current <= limit)
                .remaining(Math.max(0, limit - current.intValue()))
                .resetAt(resetAt)
                .build();
                
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to in-memory rate limiting", e);
            return checkRateLimitMemory(key, limit, windowSeconds);
        }
    }
    
    /**
     * クールダウン中かチェック
     */
    public boolean isInCooldown(String key, int cooldownSeconds) {
        try {
            String redisKey = "cooldown:" + key;
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * クールダウン設定
     */
    public void setCooldown(String key, int cooldownSeconds) {
        try {
            String redisKey = "cooldown:" + key;
            redisTemplate.opsForValue().set(redisKey, "1", cooldownSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to set cooldown in Redis", e);
        }
    }
    
    /**
     * インメモリフォールバック (単一インスタンスのみ有効)
     */
    private RateLimitResult checkRateLimitMemory(String key, int limit, int windowSeconds) {
        RateLimitCounter counter = memoryFallback.compute(key, (k, v) -> {
            if (v == null || v.isExpired()) {
                return new RateLimitCounter(limit, windowSeconds);
            }
            v.increment();
            return v;
        });
        
        return RateLimitResult.builder()
            .allowed(counter.isAllowed())
            .remaining(counter.getRemaining())
            .resetAt(counter.getResetAt())
            .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RateLimitResult {
        private boolean allowed;
        private int remaining;
        private Instant resetAt;
    }
    
    private static class RateLimitCounter {
        private int count = 0;
        private final int limit;
        private final Instant resetAt;
        
        RateLimitCounter(int limit, int windowSeconds) {
            this.limit = limit;
            this.resetAt = Instant.now().plus(Duration.ofSeconds(windowSeconds));
        }
        
        void increment() { count++; }
        boolean isAllowed() { return count <= limit; }
        int getRemaining() { return Math.max(0, limit - count); }
        Instant getResetAt() { return resetAt; }
        boolean isExpired() { return Instant.now().isAfter(resetAt); }
    }
}
```

### 4.5 Email Service実装

#### `EmailService.java` - インターフェース

```java
package jp.vemi.mirel.foundation.web.api.auth.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode, String purpose);
    void sendPasswordResetOtp(String toEmail, String otpCode);
    void sendWelcomeEmail(String toEmail, String displayName);
    void sendInvitationEmail(String toEmail, String inviterName, String tenantName, String invitationUrl);
}
```

#### `AzureEmailServiceImpl.java` - Azure実装

```java
package jp.vemi.mirel.foundation.web.api.auth.service;

import com.azure.communication.email.*;
import com.azure.communication.email.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "email.provider", havingValue = "azure")
@RequiredArgsConstructor
@Slf4j
public class AzureEmailServiceImpl implements EmailService {
    
    @Value("${azure.communication.connection-string}")
    private String connectionString;
    
    @Value("${azure.communication.email.from}")
    private String fromEmail;
    
    @Value("${azure.communication.email.from-name}")
    private String fromName;
    
    private final EmailTemplateService templateService;
    
    @Override
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        try {
            EmailClient emailClient = new EmailClientBuilder()
                .connectionString(connectionString)
                .buildClient();
            
            String subject = "mirelplatform - ログイン認証コード";
            String htmlContent = templateService.renderOtpEmail(otpCode, purpose);
            
            EmailMessage message = new EmailMessage()
                .setSenderAddress(fromEmail)
                .setToRecipients(toEmail)
                .setSubject(subject)
                .setBodyHtml(htmlContent);
            
            EmailSendResult result = emailClient.send(message);
            log.info("OTP email sent successfully to: {} (messageId: {})", toEmail, result.getMessageId());
            
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
    
    // 他のメソッド実装...
}
```

---

## 5. Frontend Implementation Details

### 5.1 Auth Store拡張

#### `authStore.ts`

```typescript
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface User {
  userId: string;
  username: string;
  email: string;
  displayName: string;
  firstName?: string;
  lastName?: string;
  avatarUrl?: string;  // 追加
  isActive: boolean;
  emailVerified: boolean;
  profileCompleted: boolean;  // 追加
  oauthProvider?: string;  // 追加
}

interface OtpState {
  email: string;
  purpose: string;
  expiresAt: string;
  attemptsRemaining: number;
}

interface AuthState {
  user: User | null;
  currentTenant: Tenant | null;
  tokens: Tokens | null;
  isAuthenticated: boolean;
  otpState: OtpState | null;

  // OTP Actions
  requestOtp: (email: string, purpose: 'LOGIN' | 'PASSWORD_RESET' | 'EMAIL_VERIFICATION') => Promise<void>;
  verifyOtp: (email: string, otpCode: string, purpose: string) => Promise<void>;
  resendOtp: (email: string, purpose: string) => Promise<void>;
  
  // Password Actions
  loginWithPassword: (email: string, password: string) => Promise<void>;
  
  // OAuth2 Actions
  loginWithGitHub: () => void;
  
  // 既存Actions
  signup: (data: SignupData) => Promise<void>;
  logout: () => Promise<void>;
  switchTenant: (tenantId: string) => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      currentTenant: null,
      tokens: null,
      isAuthenticated: false,
      otpState: null,

      requestOtp: async (email: string, purpose: string) => {
        const response = await fetch('/mapi/auth/otp/request', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, purpose }),
        });

        if (!response.ok) {
          const error = await response.json();
          throw new Error(error.message || 'Failed to request OTP');
        }

        const data = await response.json();
        set({
          otpState: {
            email,
            purpose,
            expiresAt: data.expiresAt,
            attemptsRemaining: data.maxAttempts,
          },
        });
      },

      verifyOtp: async (email: string, otpCode: string, purpose: string) => {
        const response = await fetch('/mapi/auth/otp/verify', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, otpCode, purpose }),
        });

        if (!response.ok) {
          const error = await response.json();
          throw new Error(error.message || 'OTP verification failed');
        }

        const data = await response.json();
        set({
          user: data.user,
          currentTenant: data.currentTenant,
          tokens: data.tokens,
          isAuthenticated: true,
          otpState: null,
        });
      },

      resendOtp: async (email: string, purpose: string) => {
        const response = await fetch('/mapi/auth/otp/resend', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, purpose }),
        });

        if (!response.ok) {
          const error = await response.json();
          throw new Error(error.message || 'Failed to resend OTP');
        }

        const data = await response.json();
        set({
          otpState: {
            email,
            purpose,
            expiresAt: data.expiresAt,
            attemptsRemaining: data.maxAttempts,
          },
        });
      },

      loginWithGitHub: () => {
        window.location.href = '/mapi/oauth2/authorization/github';
      },

      // 他のメソッド実装...
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        currentTenant: state.currentTenant,
        tokens: state.tokens,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
```

### 5.2 OTP Verification Page

#### `OtpVerificationPage.tsx`

```typescript
import { useState, useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/stores/authStore';
import { Button, Card, CardContent, CardDescription, CardHeader, CardTitle, Input } from '@mirel/ui';

export function OtpVerificationPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  
  const email = searchParams.get('email') || '';
  const purpose = searchParams.get('purpose') || 'LOGIN';
  
  const [otpCode, setOtpCode] = useState(['', '', '', '', '', '']);
  const [error, setError] = useState('');
  const [countdown, setCountdown] = useState(300); // 5分
  const [canResend, setCanResend] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);
  const { verifyOtp, resendOtp, otpState } = useAuthStore();
  
  useEffect(() => {
    // カウントダウンタイマー
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 0) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    
    // 1分後に再送信可能
    const resendTimer = setTimeout(() => setCanResend(true), 60000);
    
    return () => {
      clearInterval(timer);
      clearTimeout(resendTimer);
    };
  }, []);
  
  useEffect(() => {
    // 初回フォーカス
    inputRefs.current[0]?.focus();
  }, []);
  
  const handleInputChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return; // 数字のみ許可
    
    const newCode = [...otpCode];
    newCode[index] = value.slice(-1); // 最後の1文字のみ
    setOtpCode(newCode);
    
    // 自動フォーカス移動
    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
    
    // 6桁入力完了で自動検証
    if (newCode.every(digit => digit !== '')) {
      handleVerify(newCode.join(''));
    }
  };
  
  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otpCode[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };
  
  const handleVerify = async (code: string) => {
    setIsVerifying(true);
    setError('');
    
    try {
      await verifyOtp(email, code, purpose);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Invalid OTP code');
      setOtpCode(['', '', '', '', '', '']);
      inputRefs.current[0]?.focus();
    } finally {
      setIsVerifying(false);
    }
  };
  
  const handleResend = async () => {
    setError('');
    try {
      await resendOtp(email, purpose);
      setCanResend(false);
      setCountdown(300);
      setTimeout(() => setCanResend(true), 60000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to resend OTP');
    }
  };
  
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };
  
  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <Card className="w-full max-w-md p-8">
        <CardHeader>
          <CardTitle>認証コードを入力</CardTitle>
          <CardDescription>
            {email} に送信された6桁のコードを入力してください
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* OTP入力フィールド */}
          <div className="flex justify-center gap-2">
            {otpCode.map((digit, index) => (
              <Input
                key={index}
                ref={(el) => (inputRefs.current[index] = el)}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) => handleInputChange(index, e.target.value)}
                onKeyDown={(e) => handleKeyDown(index, e)}
                disabled={isVerifying}
                className="w-12 h-14 text-center text-2xl font-bold"
              />
            ))}
          </div>
          
          {/* カウントダウン */}
          <div className="text-center text-sm text-muted-foreground">
            有効期限: {formatTime(countdown)}
          </div>
          
          {/* 試行回数 */}
          {otpState && (
            <div className="text-center text-sm text-muted-foreground">
              残り試行回数: {otpState.attemptsRemaining}回
            </div>
          )}
          
          {/* エラーメッセージ */}
          {error && (
            <div className="bg-red-50 dark:bg-red-950 text-red-600 dark:text-red-400 p-3 rounded text-sm text-center">
              {error}
            </div>
          )}
          
          {/* 再送信ボタン */}
          <div className="text-center">
            <Button
              variant="ghost"
              onClick={handleResend}
              disabled={!canResend || isVerifying}
            >
              {canResend ? 'コードを再送信' : '1分後に再送信可能'}
            </Button>
          </div>
          
          {/* キャンセル */}
          <div className="text-center">
            <Button variant="link" onClick={() => navigate('/login')}>
              ログイン画面に戻る
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
```

---

## 6. Testing Strategy

### 6.1 E2Eテスト実装

#### `packages/e2e/tests/specs/promarker-v3/auth/otp-passwordless-login.spec.ts`

```typescript
import { test, expect } from '@playwright/test';
import axios from 'axios';

const MAILHOG_API = 'http://localhost:8025/api/v2';

test.describe('OTP Passwordless Login', () => {
  test.beforeEach(async ({ page }) => {
    // MailHogメール削除
    await axios.delete(`${MAILHOG_API}/messages`);
  });
  
  test('should complete OTP login flow', async ({ page }) => {
    const email = 'test@example.com';
    
    // 1. ログインページ表示
    await page.goto('http://localhost:5173/login');
    
    // 2. メールアドレス入力
    await page.fill('input[type="email"]', email);
    await page.click('button:has-text("コードを送信")');
    
    // 3. OTP検証ページへ遷移確認
    await expect(page).toHaveURL(/\/otp-verify\?email=/);
    
    // 4. MailHogからOTPコード取得
    await page.waitForTimeout(2000); // メール送信待機
    const response = await axios.get(`${MAILHOG_API}/messages`);
    const latestMessage = response.data.items[0];
    const emailBody = latestMessage.Content.Body;
    const otpMatch = emailBody.match(/\b(\d{6})\b/);
    
    expect(otpMatch).toBeTruthy();
    const otpCode = otpMatch![1];
    
    // 5. OTPコード入力
    for (let i = 0; i < 6; i++) {
      await page.fill(`input[type="text"]:nth-child(${i + 1})`, otpCode[i]);
    }
    
    // 6. 自動検証 & ダッシュボードリダイレクト
    await expect(page).toHaveURL('http://localhost:5173/');
    
    // 7. 認証状態確認
    const user = await page.evaluate(() => {
      const auth = localStorage.getItem('auth-storage');
      return auth ? JSON.parse(auth).state.user : null;
    });
    
    expect(user).toBeTruthy();
    expect(user.email).toBe(email);
  });
  
  test('should handle invalid OTP code', async ({ page }) => {
    // テスト実装...
  });
  
  test('should enforce rate limiting', async ({ page }) => {
    // テスト実装...
  });
});
```

### 6.2 Unit Test実装

#### `OtpServiceTest.java`

```java
@SpringBootTest
class OtpServiceTest {
    
    @Autowired
    private OtpService otpService;
    
    @MockBean
    private EmailService emailService;
    
    @Test
    void testRequestOtp_Success() {
        String email = "test@example.com";
        String purpose = "LOGIN";
        
        otpService.requestOtp(email, purpose, "127.0.0.1", "Test Agent");
        
        verify(emailService, times(1)).sendOtpEmail(eq(email), anyString(), eq(purpose));
    }
    
    @Test
    void testVerifyOtp_Success() {
        // テスト実装...
    }
    
    @Test
    void testRateLimit_Exceeded() {
        // テスト実装...
    }
}
```

---

## 7. Security Considerations

### 7.1 セキュリティ対策一覧

| 脅威 | 対策 | 実装箇所 |
|---|---|---|
| **ブルートフォース攻撃** | レート制限 (3回/分)、試行回数制限 (3回)、アカウントロック | `RateLimitService`, `OtpService` |
| **OTPコード推測** | SecureRandom使用、SHA-256ハッシュ化、5分有効期限 | `OtpService.generateOtpCode()` |
| **リプレイ攻撃** | ワンタイム使用強制 (`isVerified`フラグ) | `OtpToken.verifyOtp()` |
| **中間者攻撃** | HTTPS強制、Secure Cookie、HSTS | `WebSecurityConfig` |
| **セッションハイジャック** | JWT有効期限 (1時間)、RefreshToken rotation | `JwtService` |
| **メールスプーフィング** | SPF/DKIM/DMARC設定 | Azure Communication Services |
| **DDoS攻撃** | Redis分散レート制限、Cloudflare Protection | `RateLimitService` |

### 7.2 GDPR/個人情報保護対応

#### 監査ログ保持ポリシー

```java
@Scheduled(cron = "0 0 2 * * *")  // 毎日2:00AMに実行
public void cleanupExpiredLogs() {
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
    int deleted = otpAuditLogRepository.deleteByCreatedAtBefore(cutoffDate);
    log.info("Deleted {} expired audit logs", deleted);
}
```

#### ユーザー削除時のデータ処理

```java
@Transactional
public void deleteUser(UUID userId) {
    // SystemUser削除
    systemUserRepository.deleteById(userId);
    
    // 関連OTPトークン削除 (CASCADE)
    // 監査ログは匿名化して保持
    otpAuditLogRepository.anonymizeBySystemUserId(userId);
}
```

---

## 8. Deployment & Operations

### 8.1 Infrastructure Requirements

#### Docker Compose (開発環境)

```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3
  
  mailhog:
    image: mailhog/mailhog:latest
    ports:
      - "1025:1025"  # SMTP
      - "8025:8025"  # Web UI
  
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: mirelplatform
      POSTGRES_USER: mirel
      POSTGRES_PASSWORD: mirel
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  redis-data:
  postgres-data:
```

### 8.2 環境変数設定

#### `.env.example`

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/mirelplatform
DATABASE_USER=mirel
DATABASE_PASS=mirel

# Redis
REDIS_HOST=localhost
REDIS_PASSWORD=

# Azure Communication Services
AZURE_COMMUNICATION_CONNECTION_STRING=endpoint=https://...;accesskey=...
EMAIL_PROVIDER=azure

# GitHub OAuth2
GITHUB_CLIENT_ID=your_client_id
GITHUB_CLIENT_SECRET=your_client_secret

# JWT
JWT_SECRET=your-256-bit-secret-key-here

# OTP Settings
OTP_EXPIRATION_MINUTES=5
RATE_LIMIT_OTP_REQUEST_PER_MINUTE=3
```

### 8.3 Monitoring & Alerting

#### 監視項目

1. **OTP送信成功率**: 95%以上を維持
2. **平均OTP検証時間**: 2秒以内
3. **Redis可用性**: 99.9%以上
4. **レート制限発動回数**: 異常な増加を検知
5. **監査ログ異常パターン**: 同一IPからの大量失敗

#### ログ出力例

```
2025-11-23 10:15:30.123 INFO  [OtpService] OTP requested for user: user@example.com (purpose: LOGIN)
2025-11-23 10:15:45.456 INFO  [OtpService] OTP verified successfully for user: user@example.com
2025-11-23 10:16:00.789 WARN  [RateLimitService] Rate limit exceeded for IP: 192.168.1.100
2025-11-23 10:17:15.012 ERROR [EmailService] Failed to send OTP email to: user@example.com (retrying...)
```

---

## 9. Migration Plan

### 9.1 段階的ロールアウト

| Phase | 内容 | 期間 | リスク |
|---|---|---|---|
| **Phase 0** | 技術検証・PoC | 1週間 | 低 |
| **Phase 1** | Backend基盤実装 (OTP, Redis, Email) | 2週間 | 中 |
| **Phase 2** | Frontend実装 (OTP画面, GitHub OAuth2) | 1週間 | 中 |
| **Phase 3** | E2Eテスト・バグ修正 | 1週間 | 高 |
| **Phase 4** | Staging環境デプロイ・負荷テスト | 3日 | 高 |
| **Phase 5** | Production デプロイ (カナリアリリース) | 1日 | 高 |

### 9.2 ロールバック計画

1. **OTP無効化**: `otp.enabled=false` で従来のパスワード認証に即座に切り替え
2. **Redis障害時**: インメモリフォールバック自動適用
3. **Azure障害時**: SMTP バックアップ (`email.provider=smtp`)

---

## 10. Open Questions & Decisions Needed

### 10.1 要決定事項

1. **GitHub OAuth2アプリ登録**: Client ID/Secret発行の担当者は？
2. **Azure Communication Services契約**: すでに契約済み？セットアップ必要？
3. **ドメイン検証ルール初期設定**: デフォルトで全ドメイン許可 or 制限？
4. **本番Redis構成**: 単一インスタンス or Sentinel or Cluster？
5. **監査ログ保持期間**: 90日で確定？法的要件確認済み？

### 10.2 今後の拡張予定

- Apple Sign In 追加
- Google OAuth2 追加
- SMS OTP (Twilio統合)
- 生体認証 (WebAuthn/Passkey)
- SSO (SAML 2.0)

---

## 11. Appendix

### 11.1 参考資料

- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [Azure Communication Services - Email](https://learn.microsoft.com/azure/communication-services/concepts/email/email-overview)
- [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html)
- [Redis Rate Limiting](https://redis.io/docs/manual/patterns/rate-limiter/)

### 11.2 変更履歴

| 日付 | バージョン | 変更内容 | 作成者 |
|---|---|---|---|
| 2025-11-23 | 1.0 | 初版作成 | GitHub Copilot |

---

**Powered by Copilot 🤖**
