# 運用・監視 機能設計書

## 1. 概要

SaaSプラットフォームの本番運用に必要な監視・通知・バックアップ機能。
サービスの可用性確保、障害検知、データ保護を実現する。

---

## 2. 機能一覧と実装状況

| # | 機能名 | 説明 | 現状 | 優先度 |
|---|--------|------|------|--------|
| 5.1 | ヘルスチェック強化 | 詳細なシステム状態確認 | ⚠️ 基本のみ | P1 |
| 5.2 | メトリクス収集 | Prometheus形式メトリクス | ❌ 未 | P1 |
| 5.3 | エラー通知 | Slack/メール通知 | ❌ 未 | P1 |
| 5.4 | バックアップ/リストア | DB自動バックアップ | ❌ 未 | P2 |
| 5.5 | ログ集約 | 構造化ログ・外部転送 | ⚠️ 基本のみ | P2 |
| 5.6 | Blue-Greenデプロイ | 無停止デプロイ対応 | ❌ 未 | P3 |

---

## 3. 機能詳細設計

### 5.1 ヘルスチェック強化

#### 5.1.1 現状分析

```
【既存実装】
✅ Spring Boot Actuator 依存追加済み
✅ /actuator/health エンドポイント（基本）
✅ スクリプト内でcurlによるヘルスチェック実行

【未実装】
❌ カスタムヘルスインジケーター
❌ 外部サービス（Redis, SMTP）の接続確認
❌ データベース詳細状態
❌ ディスク使用量チェック
```

#### 5.1.2 ヘルスチェック設計

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        ヘルスチェック項目                                   │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【/actuator/health レスポンス例】                                          │
│  {                                                                          │
│    "status": "UP",                                                          │
│    "components": {                                                          │
│      "db": {                                                                │
│        "status": "UP",                                                      │
│        "details": {                                                         │
│          "database": "PostgreSQL",                                          │
│          "validationQuery": "isValid()"                                     │
│        }                                                                    │
│      },                                                                     │
│      "redis": {                                                             │
│        "status": "UP",                                                      │
│        "details": { "version": "7.0.0" }                                    │
│      },                                                                     │
│      "diskSpace": {                                                         │
│        "status": "UP",                                                      │
│        "details": {                                                         │
│          "total": 100GB,                                                    │
│          "free": 60GB,                                                      │
│          "threshold": 10GB                                                  │
│        }                                                                    │
│      },                                                                     │
│      "mail": {                                                              │
│        "status": "UP",                                                      │
│        "details": { "location": "smtp://localhost:1025" }                   │
│      },                                                                     │
│      "executionContext": {                                                  │
│        "status": "UP",                                                      │
│        "details": { "activeRequests": 5 }                                   │
│      }                                                                      │
│    }                                                                        │
│  }                                                                          │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

#### 5.1.3 カスタムヘルスインジケーター実装

```java
// Redis ヘルスチェック
@Component
public class RedisHealthIndicator implements HealthIndicator {
    
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    
    @Override
    public Health health() {
        if (redisTemplate == null) {
            return Health.unknown().withDetail("reason", "Redis not configured").build();
        }
        
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection().ping();
            return Health.up()
                .withDetail("response", pong)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}

// SMTP ヘルスチェック
@Component
public class MailHealthIndicator implements HealthIndicator {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Override
    public Health health() {
        try {
            ((JavaMailSenderImpl) mailSender).testConnection();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

#### 5.1.4 application.yml 設定

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when_authorized  # 認証済みの場合のみ詳細表示
      show-components: when_authorized
  health:
    redis:
      enabled: true
    mail:
      enabled: true
    diskspace:
      enabled: true
      threshold: 10GB
```

---

### 5.2 メトリクス収集 (Prometheus)

#### 5.2.1 収集対象メトリクス

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        メトリクス一覧                                       │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【JVM メトリクス（自動）】                                                  │
│  - jvm_memory_used_bytes                                                    │
│  - jvm_gc_pause_seconds                                                     │
│  - jvm_threads_live                                                         │
│                                                                             │
│  【HTTP メトリクス（自動）】                                                 │
│  - http_server_requests_seconds_count                                       │
│  - http_server_requests_seconds_sum                                         │
│  - http_server_requests_seconds_max                                         │
│                                                                             │
│  【カスタム ビジネスメトリクス】                                             │
│  - mirel_auth_login_total{status="success|failure"}                         │
│  - mirel_auth_signup_total                                                  │
│  - mirel_otp_request_total                                                  │
│  - mirel_otp_verify_total{status="success|failure"}                         │
│  - mirel_license_check_total{tier="FREE|PRO|MAX"}                           │
│  - mirel_promarker_generation_total                                         │
│  - mirel_promarker_generation_duration_seconds                              │
│  - mirel_active_users_gauge                                                 │
│  - mirel_active_tenants_gauge                                               │
│                                                                             │
│  【エラー メトリクス】                                                       │
│  - mirel_error_total{type="rate_limit|auth|license|internal"}               │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

#### 5.2.2 カスタムメトリクス実装

```java
@Component
public class AuthMetrics {
    
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter signupCounter;
    
    public AuthMetrics(MeterRegistry registry) {
        this.loginSuccessCounter = Counter.builder("mirel_auth_login_total")
            .tag("status", "success")
            .description("Total successful logins")
            .register(registry);
        
        this.loginFailureCounter = Counter.builder("mirel_auth_login_total")
            .tag("status", "failure")
            .description("Total failed logins")
            .register(registry);
        
        this.signupCounter = Counter.builder("mirel_auth_signup_total")
            .description("Total signups")
            .register(registry);
    }
    
    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }
    
    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }
    
    public void recordSignup() {
        signupCounter.increment();
    }
}

// 使用例
@Service
public class AuthenticationServiceImpl {
    
    @Autowired
    private AuthMetrics authMetrics;
    
    public AuthResponse login(LoginRequest request) {
        try {
            AuthResponse response = doLogin(request);
            authMetrics.recordLoginSuccess();
            return response;
        } catch (AuthenticationException e) {
            authMetrics.recordLoginFailure();
            throw e;
        }
    }
}
```

#### 5.2.3 Prometheus設定例

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'mirelplatform'
    metrics_path: '/mipla2/actuator/prometheus'
    static_configs:
      - targets: ['localhost:3000']
```

#### 5.2.4 Grafanaダッシュボード

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        Grafana ダッシュボード構成                           │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【Overview パネル】                                                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐              │
│  │ Active     │ │ Request    │ │ Error      │ │ Avg        │              │
│  │ Users: 150 │ │ Rate: 50/s │ │ Rate: 0.1% │ │ Latency: 45ms             │
│  └────────────┘ └────────────┘ └────────────┘ └────────────┘              │
│                                                                             │
│  【Charts】                                                                  │
│  ┌──────────────────────────────┐ ┌──────────────────────────────┐        │
│  │ Request Rate (5m avg)        │ │ Error Rate by Type           │        │
│  │ ▁▂▃▅▇█▇▅▃▂▁▂▃▅▇█▇▅▃        │ │ ▁▁▂▁▁▁▂▁▁▁▁▂▁▁▁▁▂▁▁         │        │
│  └──────────────────────────────┘ └──────────────────────────────┘        │
│                                                                             │
│  ┌──────────────────────────────┐ ┌──────────────────────────────┐        │
│  │ JVM Memory Usage             │ │ Login Success/Failure        │        │
│  │ ▅▅▅▅▆▆▆▇▇▇▇▇▆▆▆▅▅▅▅         │ │ █▅█▆█▅█▆█▅█▆█▅█▆█▅█          │        │
│  └──────────────────────────────┘ └──────────────────────────────┘        │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

---

### 5.3 エラー通知

#### 5.3.1 通知ポリシー

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        エラー通知ポリシー                                   │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【重大度レベル】                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ CRITICAL (即時通知)                                                   │  │
│  │  - サービス停止                                                       │  │
│  │  - データベース接続不可                                               │  │
│  │  - 認証システム障害                                                   │  │
│  │  → Slack + メール + PagerDuty (将来)                                  │  │
│  ├──────────────────────────────────────────────────────────────────────┤  │
│  │ ERROR (5分以内通知)                                                   │  │
│  │  - エラー率閾値超過 (>1%)                                             │  │
│  │  - レート制限大量発生                                                 │  │
│  │  - 支払い失敗                                                         │  │
│  │  → Slack                                                              │  │
│  ├──────────────────────────────────────────────────────────────────────┤  │
│  │ WARNING (日次サマリー)                                                │  │
│  │  - ディスク使用率 80%超過                                             │  │
│  │  - メモリ使用率 90%超過                                               │  │
│  │  - ライセンス期限切れ間近                                             │  │
│  │  → メール (日次レポート)                                              │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

#### 5.3.2 Slack通知実装

```java
@Component
public class SlackNotificationService {
    
    @Value("${notification.slack.webhook-url:}")
    private String webhookUrl;
    
    @Value("${notification.slack.channel:#alerts}")
    private String channel;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public void sendCriticalAlert(String title, String message, Map<String, String> details) {
        if (!StringUtils.hasText(webhookUrl)) {
            log.warn("Slack webhook URL not configured");
            return;
        }
        
        SlackMessage slackMessage = SlackMessage.builder()
            .channel(channel)
            .username("mirelplatform-alert")
            .iconEmoji(":rotating_light:")
            .attachments(List.of(
                Attachment.builder()
                    .color("danger")
                    .title(title)
                    .text(message)
                    .fields(details.entrySet().stream()
                        .map(e -> Field.of(e.getKey(), e.getValue(), true))
                        .collect(Collectors.toList()))
                    .ts(Instant.now().getEpochSecond())
                    .build()
            ))
            .build();
        
        restTemplate.postForEntity(webhookUrl, slackMessage, String.class);
    }
    
    public void sendErrorAlert(String title, String message) {
        // 簡略版
    }
}
```

#### 5.3.3 GlobalExceptionHandler統合

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @Autowired
    private SlackNotificationService slackNotification;
    
    @Autowired
    private ErrorMetrics errorMetrics;
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        
        // メトリクス記録
        errorMetrics.recordError("internal");
        
        // 重大エラーの場合は通知
        if (isCriticalError(ex)) {
            slackNotification.sendCriticalAlert(
                "Internal Server Error",
                ex.getMessage(),
                Map.of(
                    "Path", request.getRequestURI(),
                    "Method", request.getMethod(),
                    "Exception", ex.getClass().getSimpleName()
                )
            );
        }
        
        return ResponseEntity.status(500)
            .body(new ErrorResponse("INTERNAL_ERROR", "内部エラーが発生しました"));
    }
    
    private boolean isCriticalError(Exception ex) {
        return ex instanceof DataAccessException
            || ex instanceof AuthenticationServiceException;
    }
}
```

---

### 5.4 バックアップ/リストア

#### 5.4.1 バックアップ戦略

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        バックアップ戦略                                     │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【バックアップ種別】                                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 1. フルバックアップ                                                    │  │
│  │    - 頻度: 週1回（日曜深夜）                                           │  │
│  │    - 保持期間: 30日                                                    │  │
│  │    - 方式: pg_dump --format=custom                                     │  │
│  │                                                                       │  │
│  │ 2. 増分バックアップ                                                    │  │
│  │    - 頻度: 日次（毎日深夜）                                            │  │
│  │    - 保持期間: 7日                                                     │  │
│  │    - 方式: WAL アーカイブ                                              │  │
│  │                                                                       │  │
│  │ 3. ポイントインタイムリカバリ (PITR)                                   │  │
│  │    - WAL連続アーカイブ                                                 │  │
│  │    - 任意時点へのリストア可能                                          │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  【ストレージ】                                                              │
│  - ローカル: /backup/db/ (一時保存)                                         │
│  - リモート: S3 / Azure Blob Storage                                       │
│  - 暗号化: AES-256 (保存時暗号化)                                           │
│                                                                             │
│  【リストア手順】                                                            │
│  1. バックアップファイル取得                                                │
│  2. サービス停止                                                            │
│  3. pg_restore 実行                                                         │
│  4. データ整合性確認                                                        │
│  5. サービス再開                                                            │
│                                                                             │
└────────────────────────────────────────────────────────────────────────────┘
```

#### 5.4.2 バックアップスクリプト

```bash
#!/bin/bash
# scripts/backup-database.sh

set -e

BACKUP_DIR="/backup/db"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/mirel_${TIMESTAMP}.dump"
S3_BUCKET="s3://mirelplatform-backups/db"

# フルバックアップ
pg_dump \
  --host=${DATABASE_HOST} \
  --port=${DATABASE_PORT} \
  --username=${DATABASE_USER} \
  --format=custom \
  --file=${BACKUP_FILE} \
  ${DATABASE_NAME}

# 圧縮
gzip ${BACKUP_FILE}

# S3アップロード
aws s3 cp ${BACKUP_FILE}.gz ${S3_BUCKET}/

# 古いローカルバックアップ削除（7日以上）
find ${BACKUP_DIR} -name "*.dump.gz" -mtime +7 -delete

# 完了通知
echo "Backup completed: ${BACKUP_FILE}.gz"
```

#### 5.4.3 リストア手順書

```markdown
## データベースリストア手順

### 前提条件
- バックアップファイルへのアクセス権
- 管理者権限

### 手順

1. **サービス停止**
   ```bash
   ./scripts/stop-services.sh
   ```

2. **バックアップ取得**
   ```bash
   aws s3 cp s3://mirelplatform-backups/db/mirel_YYYYMMDD_HHMMSS.dump.gz .
   gunzip mirel_YYYYMMDD_HHMMSS.dump.gz
   ```

3. **既存DB退避（オプション）**
   ```bash
   pg_dump -Fc -f backup_before_restore.dump mirel
   ```

4. **リストア実行**
   ```bash
   pg_restore \
     --clean \
     --if-exists \
     --host=${DATABASE_HOST} \
     --port=${DATABASE_PORT} \
     --username=${DATABASE_USER} \
     --dbname=${DATABASE_NAME} \
     mirel_YYYYMMDD_HHMMSS.dump
   ```

5. **整合性確認**
   ```sql
   SELECT COUNT(*) FROM mir_user;
   SELECT COUNT(*) FROM mir_tenant;
   SELECT COUNT(*) FROM mir_application_license;
   ```

6. **サービス再開**
   ```bash
   ./scripts/start-services.sh
   ```

7. **動作確認**
   - ログイン可能か
   - データ表示されるか
```

---

### 5.5 ログ集約

#### 5.5.1 構造化ログ設定

```yaml
# logback-spring.xml
<configuration>
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"service":"mirelplatform"}</customFields>
        </encoder>
    </appender>
    
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/mirel.json.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/mirel.%d{yyyy-MM-dd}.json.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
    </root>
</configuration>
```

#### 5.5.2 ログ出力例

```json
{
  "@timestamp": "2025-11-28T10:30:00.000Z",
  "service": "mirelplatform",
  "level": "INFO",
  "logger": "jp.vemi.mirel.foundation.web.api.auth.controller.AuthenticationController",
  "thread": "http-nio-3000-exec-1",
  "message": "Login successful",
  "userId": "user-123",
  "tenantId": "tenant-abc",
  "requestId": "req-xyz",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "durationMs": 45
}
```

---

## 4. docker-compose 拡張 (監視スタック)

```yaml
# docker-compose.monitoring.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:v2.45.0
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.retention.time=15d'
    
  grafana:
    image: grafana/grafana:10.0.0
    ports:
      - "3030:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_ADMIN_PASSWORD:-admin}
      - GF_USERS_ALLOW_SIGN_UP=false
    depends_on:
      - prometheus
    
  alertmanager:
    image: prom/alertmanager:v0.25.0
    ports:
      - "9093:9093"
    volumes:
      - ./monitoring/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'

volumes:
  prometheus_data:
  grafana_data:
```

---

## 5. 実装タスクまとめ

```
【Phase 1: ヘルスチェック (Week 1)】
□ カスタムHealthIndicator実装
  - Redis, Mail, DiskSpace
□ application.yml 設定
□ /actuator/health 動作確認

【Phase 2: メトリクス (Week 1-2)】
□ Micrometer依存追加
□ カスタムメトリクス実装
  - AuthMetrics, LicenseMetrics, ProMarkerMetrics
□ Prometheus設定
□ Grafanaダッシュボード作成

【Phase 3: エラー通知 (Week 2)】
□ SlackNotificationService 実装
□ GlobalExceptionHandler 統合
□ 通知テスト

【Phase 4: バックアップ (Week 3)】
□ バックアップスクリプト作成
□ cronジョブ設定
□ リストア手順書作成
□ リストアテスト

【Phase 5: ログ集約 (Week 3)】
□ logstash-logback-encoder 追加
□ logback-spring.xml 設定
□ ログローテーション設定
```

---

## 6. 工数見積もり

| 機能 | 見積もり | 備考 |
|------|----------|------|
| ヘルスチェック強化 | 2日 | HealthIndicator実装 |
| メトリクス収集 | 4日 | カスタム + Grafana |
| エラー通知 | 2日 | Slack連携 |
| バックアップ/リストア | 3日 | スクリプト + 手順書 |
| ログ集約 | 1日 | 設定のみ |
| **合計** | **12日** | |

---

## 7. 環境変数追加

```bash
# .env.example 追加

# Slack Notifications
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx
SLACK_CHANNEL=#mirel-alerts

# Monitoring
PROMETHEUS_ENABLED=true
GRAFANA_ADMIN_PASSWORD=secure_password

# Backup
BACKUP_S3_BUCKET=mirelplatform-backups
BACKUP_ENCRYPTION_KEY=xxxxx
```

---

**作成日**: 2025年11月28日  
**作成者**: GitHub Copilot 🤖
