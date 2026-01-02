# Mira モニタリング・可観測性設計

> **Issue**: #50 Mira v1 実装  
> **作成日**: 2025-12-07  
> **関連**: [context-engineering-plan.md](context-engineering-plan.md)

---

## 1. モニタリング概要

### 1.1 観測対象

| カテゴリ           | メトリクス                         | 目的                       |
| ------------------ | ---------------------------------- | -------------------------- |
| **パフォーマンス** | レスポンス時間、スループット       | SLA 監視、ボトルネック検出 |
| **コスト**         | トークン使用量、API コール数       | 予算管理、最適化           |
| **品質**           | エラー率、フォールバック率         | サービス品質維持           |
| **利用状況**       | アクティブユーザー、モード別利用率 | 機能改善の優先度決定       |

### 1.2 アーキテクチャ

```
┌─────────────────────────────────────────────────────────────┐
│                        Mira Service                          │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐         │
│  │ ChatSvc │  │PromptSvc│  │MemorySvc│  │ProviderC│         │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘         │
│       │            │            │            │               │
│       └────────────┴────────────┴────────────┘               │
│                            │                                 │
│                     MiraMetricsAspect                        │
└─────────────────────────────────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
┌─────────────────┐ ┌─────────────┐ ┌─────────────────┐
│   Micrometer    │ │   Logging   │ │   Database      │
│  (Prometheus)   │ │   (JSON)    │ │ (mir_mira_*)    │
└────────┬────────┘ └──────┬──────┘ └────────┬────────┘
         │                 │                 │
         ▼                 ▼                 ▼
┌─────────────────┐ ┌─────────────┐ ┌─────────────────┐
│    Grafana      │ │     ELK     │ │   Dashboard     │
│   Dashboard     │ │    Stack    │ │   (Admin UI)    │
└─────────────────┘ └─────────────┘ └─────────────────┘
```

---

## 2. メトリクス定義

### 2.1 Micrometer メトリクス

```java
@Component
@RequiredArgsConstructor
public class MiraMetrics {

    private final MeterRegistry meterRegistry;

    // ========== カウンター ==========

    /** チャットリクエスト総数 */
    public Counter chatRequestsTotal(String mode, String status) {
        return Counter.builder("mira_chat_requests_total")
            .description("Total number of chat requests")
            .tag("mode", mode)
            .tag("status", status)  // success, error, fallback
            .register(meterRegistry);
    }

    /** トークン使用量 */
    public Counter tokensUsedTotal(String direction) {
        return Counter.builder("mira_tokens_used_total")
            .description("Total tokens used")
            .tag("direction", direction)  // input, output
            .register(meterRegistry);
    }

    /** フォールバック発生回数 */
    public Counter fallbackTotal(String reason) {
        return Counter.builder("mira_fallback_total")
            .description("Total fallback occurrences")
            .tag("reason", reason)  // api_error, timeout, rate_limit
            .register(meterRegistry);
    }

    // ========== ゲージ ==========

    /** アクティブ会話数 */
    public Gauge activeConversations(Supplier<Number> supplier) {
        return Gauge.builder("mira_active_conversations", supplier)
            .description("Number of active conversations")
            .register(meterRegistry);
    }

    /** Circuit Breaker 状態 */
    public Gauge circuitBreakerState(String state, Supplier<Number> supplier) {
        return Gauge.builder("mira_circuit_breaker_state", supplier)
            .description("Circuit breaker state")
            .tag("state", state)  // closed, open, half_open
            .register(meterRegistry);
    }

    // ========== タイマー ==========

    /** レスポンス時間 */
    public Timer responseTime(String mode, String provider) {
        return Timer.builder("mira_response_time_seconds")
            .description("Response time in seconds")
            .tag("mode", mode)
            .tag("provider", provider)
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /** プロンプト生成時間 */
    public Timer promptBuildTime() {
        return Timer.builder("mira_prompt_build_time_seconds")
            .description("Time to build system prompt")
            .register(meterRegistry);
    }

    // ========== ヒストグラム ==========

    /** コンテキストサイズ（トークン数）*/
    public DistributionSummary contextSize() {
        return DistributionSummary.builder("mira_context_size_tokens")
            .description("Context size in tokens")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }
}
```

### 2.2 カスタムメトリクス収集

```java
@Aspect
@Component
@RequiredArgsConstructor
public class MiraMetricsAspect {

    private final MiraMetrics metrics;
    private final TokenCounter tokenCounter;

    @Around("execution(* jp.vemi.mirel.apps.mira.service.MiraChatService.chat(..))")
    public Object measureChatRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        MiraContext context = (MiraContext) joinPoint.getArgs()[0];
        String userMessage = (String) joinPoint.getArgs()[1];
        String mode = context.getMode().name();

        Timer.Sample sample = Timer.start();
        String status = "success";
        String provider = "unknown";

        try {
            MiraChatResponse response = (MiraChatResponse) joinPoint.proceed();
            provider = response.getProvider();

            // トークン使用量を記録
            int inputTokens = tokenCounter.count(userMessage);
            int outputTokens = tokenCounter.count(response.getContent());
            metrics.tokensUsedTotal("input").increment(inputTokens);
            metrics.tokensUsedTotal("output").increment(outputTokens);

            if (response.isFallback()) {
                status = "fallback";
                metrics.fallbackTotal(response.getFallbackReason()).increment();
            }

            return response;

        } catch (Exception e) {
            status = "error";
            throw e;
        } finally {
            sample.stop(metrics.responseTime(mode, provider));
            metrics.chatRequestsTotal(mode, status).increment();
        }
    }
}
```

---

## 3. トークン使用量管理

### 3.1 トークンカウンター

```java
@Component
public class TokenCounter {

    // Gemini / GPT-4o / Llama 等は tiktoken の cl100k_base と互換性あり
    private final Encoding encoding = Encodings.newDefaultEncodingRegistry()
        .getEncoding(EncodingType.CL100K_BASE);

    public int count(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return encoding.countTokens(text);
    }

    public TokenUsage estimate(String systemPrompt, String userMessage, int maxOutputTokens) {
        int inputTokens = count(systemPrompt) + count(userMessage);
        return new TokenUsage(inputTokens, maxOutputTokens, inputTokens + maxOutputTokens);
    }
}

@Data
@AllArgsConstructor
public class TokenUsage {
    private int inputTokens;
    private int outputTokens;
    private int totalTokens;
}
```

### 3.2 使用量追跡テーブル

```sql
CREATE TABLE mir_mira_token_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    conversation_id UUID,
    input_tokens INTEGER NOT NULL,
    output_tokens INTEGER NOT NULL,
    model VARCHAR(100) NOT NULL,
    usage_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 日次集計用インデックス
CREATE INDEX idx_token_usage_date ON mir_mira_token_usage(usage_date);
CREATE INDEX idx_token_usage_tenant ON mir_mira_token_usage(tenant_id, usage_date);
```

### 3.3 使用量制限

```java
@Service
@RequiredArgsConstructor
public class TokenQuotaService {

    private final TokenUsageRepository usageRepository;
    private final MiraAiProperties properties;

    public void checkQuota(String tenantId, int estimatedTokens) {
        LocalDate today = LocalDate.now();
        long usedToday = usageRepository.sumTokensByTenantAndDate(tenantId, today);

        long dailyLimit = properties.getQuota().getDailyTokenLimit();
        if (usedToday + estimatedTokens > dailyLimit) {
            throw new MiraNonRetryableException(
                MiraErrorCode.QUOTA_EXCEEDED,
                String.format("Daily token quota exceeded: %d / %d", usedToday, dailyLimit)
            );
        }
    }

    public TokenQuotaStatus getQuotaStatus(String tenantId) {
        LocalDate today = LocalDate.now();
        long usedToday = usageRepository.sumTokensByTenantAndDate(tenantId, today);
        long dailyLimit = properties.getQuota().getDailyTokenLimit();

        return TokenQuotaStatus.builder()
            .tenantId(tenantId)
            .usedTokens(usedToday)
            .dailyLimit(dailyLimit)
            .remainingTokens(Math.max(0, dailyLimit - usedToday))
            .usagePercentage((double) usedToday / dailyLimit * 100)
            .build();
    }
}
```

---

## 4. ログ設計

### 4.1 構造化ログ

```java
@Slf4j
@Component
public class MiraAuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("mira.audit");

    public void logChatRequest(MiraChatAuditEvent event) {
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("eventType", "CHAT_REQUEST");
        logData.put("timestamp", Instant.now().toString());
        logData.put("conversationId", event.getConversationId());
        logData.put("tenantId", event.getTenantId());
        logData.put("userId", event.getUserId());
        logData.put("mode", event.getMode());
        logData.put("screenId", event.getScreenId());
        logData.put("inputTokens", event.getInputTokens());
        logData.put("outputTokens", event.getOutputTokens());
        logData.put("responseTimeMs", event.getResponseTimeMs());
        logData.put("provider", event.getProvider());
        logData.put("status", event.getStatus());

        if (event.getErrorCode() != null) {
            logData.put("errorCode", event.getErrorCode());
        }

        auditLog.info(toJson(logData));
    }

    private String toJson(Map<String, Object> data) {
        try {
            return new ObjectMapper().writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return data.toString();
        }
    }
}
```

### 4.2 Logback 設定

```xml
<!-- backend/src/main/resources/logback-spring.xml -->
<configuration>

    <!-- Mira 監査ログ専用アペンダー -->
    <appender name="MIRA_AUDIT" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/mira-audit.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/mira-audit.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyNames>tenantId,userId,conversationId</includeMdcKeyNames>
        </encoder>
    </appender>

    <!-- Mira エラーログ -->
    <appender name="MIRA_ERROR" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/mira-error.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/mira-error.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>WARN</level>
        </filter>
    </appender>

    <logger name="mira.audit" level="INFO" additivity="false">
        <appender-ref ref="MIRA_AUDIT"/>
    </logger>

    <logger name="mira.error" level="WARN" additivity="false">
        <appender-ref ref="MIRA_ERROR"/>
    </logger>

</configuration>
```

---

## 5. ダッシュボード

### 5.1 Grafana ダッシュボード

```json
{
  "dashboard": {
    "title": "Mira AI Assistant Monitoring",
    "panels": [
      {
        "title": "Request Rate (per minute)",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(mira_chat_requests_total[1m])",
            "legendFormat": "{{mode}} - {{status}}"
          }
        ]
      },
      {
        "title": "Response Time (p95)",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(mira_response_time_seconds_bucket[5m]))",
            "legendFormat": "{{mode}}"
          }
        ]
      },
      {
        "title": "Token Usage",
        "type": "stat",
        "targets": [
          {
            "expr": "sum(increase(mira_tokens_used_total[24h]))",
            "legendFormat": "Daily Tokens"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "gauge",
        "targets": [
          {
            "expr": "sum(rate(mira_chat_requests_total{status='error'}[5m])) / sum(rate(mira_chat_requests_total[5m])) * 100"
          }
        ],
        "thresholds": {
          "mode": "absolute",
          "steps": [
            { "color": "green", "value": null },
            { "color": "yellow", "value": 1 },
            { "color": "red", "value": 5 }
          ]
        }
      },
      {
        "title": "Circuit Breaker Status",
        "type": "state-timeline",
        "targets": [
          {
            "expr": "mira_circuit_breaker_state",
            "legendFormat": "{{state}}"
          }
        ]
      },
      {
        "title": "Fallback Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(mira_fallback_total[5m])",
            "legendFormat": "{{reason}}"
          }
        ]
      }
    ]
  }
}
```

### 5.2 Admin UI ダッシュボード（React）

```typescript
// apps/frontend-v3/src/features/admin/mira/MiraDashboard.tsx

export function MiraDashboard() {
  const { data: stats } = useQuery({
    queryKey: ['mira', 'stats'],
    queryFn: () => miraApi.getStats(),
    refetchInterval: 30000,
  });

  return (
    <div className="grid grid-cols-4 gap-4">
      {/* リクエスト数 */}
      <StatCard
        title="今日のリクエスト"
        value={stats?.todayRequests ?? 0}
        trend={stats?.requestsTrend}
        icon={<MessageSquare />}
      />

      {/* トークン使用量 */}
      <StatCard
        title="トークン使用量"
        value={formatNumber(stats?.todayTokens ?? 0)}
        subtitle={`/ ${formatNumber(stats?.dailyLimit ?? 0)}`}
        progress={(stats?.todayTokens ?? 0) / (stats?.dailyLimit ?? 1) * 100}
        icon={<Coins />}
      />

      {/* 平均レスポンス時間 */}
      <StatCard
        title="平均レスポンス時間"
        value={`${stats?.avgResponseTime ?? 0}ms`}
        trend={stats?.responseTimeTrend}
        trendInverted
        icon={<Clock />}
      />

      {/* エラー率 */}
      <StatCard
        title="エラー率"
        value={`${(stats?.errorRate ?? 0).toFixed(2)}%`}
        status={stats?.errorRate > 5 ? 'error' : stats?.errorRate > 1 ? 'warning' : 'success'}
        icon={<AlertTriangle />}
      />

      {/* モード別利用率 */}
      <div className="col-span-2">
        <Card>
          <CardHeader>
            <CardTitle>モード別利用率</CardTitle>
          </CardHeader>
          <CardContent>
            <ModeUsageChart data={stats?.modeUsage} />
          </CardContent>
        </Card>
      </div>

      {/* 時間帯別リクエスト */}
      <div className="col-span-2">
        <Card>
          <CardHeader>
            <CardTitle>時間帯別リクエスト</CardTitle>
          </CardHeader>
          <CardContent>
            <HourlyRequestsChart data={stats?.hourlyRequests} />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
```

---

## 6. アラート設定

### 6.1 Prometheus Alerting Rules

```yaml
# prometheus/alerts/mira.yml
groups:
  - name: mira-alerts
    rules:
      # 高エラー率
      - alert: MiraHighErrorRate
        expr: |
          sum(rate(mira_chat_requests_total{status="error"}[5m])) 
          / sum(rate(mira_chat_requests_total[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Mira error rate is above 5%"
          description: "Error rate: {{ $value | humanizePercentage }}"

      # レスポンス時間劣化
      - alert: MiraSlowResponse
        expr: |
          histogram_quantile(0.95, rate(mira_response_time_seconds_bucket[5m])) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Mira p95 response time is above 10 seconds"
          description: "Current p95: {{ $value | humanizeDuration }}"

      # Circuit Breaker オープン
      - alert: MiraCircuitBreakerOpen
        expr: mira_circuit_breaker_state{state="open"} == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Mira circuit breaker is open"
          description: "AI service is experiencing issues"

      # トークンクォータ警告
      - alert: MiraTokenQuotaWarning
        expr: |
          sum(increase(mira_tokens_used_total[24h])) 
          / on() group_left mira_daily_token_limit > 0.8
        for: 0m
        labels:
          severity: warning
        annotations:
          summary: "Mira token usage is above 80% of daily limit"
          description: "Usage: {{ $value | humanizePercentage }}"

      # フォールバック頻発
      - alert: MiraFrequentFallback
        expr: rate(mira_fallback_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Mira is frequently falling back"
          description: "Fallback rate: {{ $value }} per second"
```

---

## 7. 設定

### 7.1 application.yml

```yaml
mira:
  ai:
    enabled: true
    provider: github-models

  metrics:
    enabled: true
    detailed-timing: true

  quota:
    enabled: true
    daily-token-limit: 1000000 # 100万トークン/日
    warning-threshold: 0.8 # 80%で警告

  monitoring:
    response-time-threshold-ms: 5000
    error-rate-threshold: 0.05

  audit:
    enabled: true
    log-content: false # プライバシー: 内容はログしない
    retention-days: 90

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: mirelplatform
      component: mira
```

---

## 8. 参考資料

- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/naming/)
- [Spring Boot Actuator Metrics](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics)
- [Grafana Dashboard Best Practices](https://grafana.com/docs/grafana/latest/dashboards/build-dashboards/best-practices/)
