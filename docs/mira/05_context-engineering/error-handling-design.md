# Mira エラー処理・フォールバック設計

> **Issue**: #50 Mira v1 実装  
> **作成日**: 2025-12-07  
> **関連**: [context-engineering-plan.md](context-engineering-plan.md)

---

## 1. エラー分類

### 1.1 エラーカテゴリ

| カテゴリ               | コード範囲 | 説明                        | リトライ可否 |
| ---------------------- | ---------- | --------------------------- | ------------ |
| **API接続エラー**      | MIRA-1xxx  | AI プロバイダーへの接続失敗 | ✅ 可        |
| **認証エラー**         | MIRA-2xxx  | API トークン無効・期限切れ  | ❌ 不可      |
| **レート制限**         | MIRA-3xxx  | リクエスト制限超過          | ✅ 待機後可  |
| **コンテキストエラー** | MIRA-4xxx  | コンテキスト構築失敗        | ❌ 不可      |
| **モデルエラー**       | MIRA-5xxx  | AI モデルの応答エラー       | ⚠️ 条件付き  |
| **内部エラー**         | MIRA-9xxx  | 予期しないシステムエラー    | ❌ 不可      |

### 1.2 エラーコード定義

```java
public enum MiraErrorCode {

    // API接続エラー (1xxx)
    API_CONNECTION_FAILED("MIRA-1001", "AI プロバイダーへの接続に失敗しました"),
    API_TIMEOUT("MIRA-1002", "AI プロバイダーからの応答がタイムアウトしました"),
    API_UNAVAILABLE("MIRA-1003", "AI サービスが一時的に利用できません"),

    // 認証エラー (2xxx)
    AUTH_TOKEN_INVALID("MIRA-2001", "API トークンが無効です"),
    AUTH_TOKEN_EXPIRED("MIRA-2002", "API トークンの有効期限が切れています"),
    AUTH_PERMISSION_DENIED("MIRA-2003", "API へのアクセス権限がありません"),

    // レート制限 (3xxx)
    RATE_LIMIT_EXCEEDED("MIRA-3001", "リクエスト制限を超過しました"),
    QUOTA_EXCEEDED("MIRA-3002", "利用可能なクォータを超過しました"),

    // コンテキストエラー (4xxx)
    CONTEXT_BUILD_FAILED("MIRA-4001", "コンテキストの構築に失敗しました"),
    CONTEXT_TOO_LARGE("MIRA-4002", "コンテキストがトークン制限を超過しています"),
    TEMPLATE_NOT_FOUND("MIRA-4003", "プロンプトテンプレートが見つかりません"),

    // モデルエラー (5xxx)
    MODEL_RESPONSE_INVALID("MIRA-5001", "AI モデルからの応答が不正です"),
    MODEL_CONTENT_FILTERED("MIRA-5002", "コンテンツフィルタにより応答がブロックされました"),
    MODEL_OVERLOADED("MIRA-5003", "AI モデルが過負荷状態です"),

    // 内部エラー (9xxx)
    INTERNAL_ERROR("MIRA-9001", "内部エラーが発生しました"),
    CONVERSATION_NOT_FOUND("MIRA-9002", "会話が見つかりません"),
    MESSAGE_SAVE_FAILED("MIRA-9003", "メッセージの保存に失敗しました");

    private final String code;
    private final String defaultMessage;

    MiraErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}
```

---

## 2. リトライ戦略

### 2.1 Exponential Backoff

```java
@Configuration
public class MiraRetryConfiguration {

    @Bean
    public RetryTemplate miraRetryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .exponentialBackoff(
                Duration.ofMillis(500),   // 初期待機時間
                2.0,                       // 倍率
                Duration.ofSeconds(10)     // 最大待機時間
            )
            .retryOn(MiraRetryableException.class)
            .build();
    }
}
```

### 2.2 リトライ対象例外

```java
public class MiraRetryableException extends MiraException {
    // API_CONNECTION_FAILED, API_TIMEOUT, RATE_LIMIT_EXCEEDED 等
}

public class MiraNonRetryableException extends MiraException {
    // AUTH_TOKEN_INVALID, CONTEXT_BUILD_FAILED 等
}
```

### 2.3 レート制限対応

```java
@Component
public class RateLimitHandler {

    private final AtomicLong nextAllowedRequestTime = new AtomicLong(0);

    public void handleRateLimitResponse(int retryAfterSeconds) {
        long nextTime = System.currentTimeMillis() + (retryAfterSeconds * 1000L);
        nextAllowedRequestTime.set(nextTime);
    }

    public boolean canMakeRequest() {
        return System.currentTimeMillis() >= nextAllowedRequestTime.get();
    }

    public long getWaitTimeMillis() {
        return Math.max(0, nextAllowedRequestTime.get() - System.currentTimeMillis());
    }
}
```

---

## 3. フォールバック戦略

### 3.1 フォールバックチェーン

```
┌─────────────────────────────────────────────────────────────┐
│                  Primary: Gemini 2.5 Flash                   │
│             (Vertex AI - gemini-2.5-flash) 推奨              │
└─────────────────────────────────────────────────────────────┘
                              │
                         失敗時 ▼
┌─────────────────────────────────────────────────────────────┐
│                   Fallback 1: GPT-4o                         │
│               (Azure OpenAI / OpenAI)                        │
└─────────────────────────────────────────────────────────────┘
                              │
                         失敗時 ▼
┌─────────────────────────────────────────────────────────────┐
│                  Fallback 2: Static Response                 │
│               (事前定義された応答テンプレート)                 │
└─────────────────────────────────────────────────────────────┘
                              │
                         失敗時 ▼
┌─────────────────────────────────────────────────────────────┐
│                  Fallback 3: Error Message                   │
│             (ユーザーフレンドリーなエラー表示)                 │
└─────────────────────────────────────────────────────────────┘
```

> ⚠️ **GitHub Models (Llama 3.3)** は不安定なため、フォールバックチェーンに含めていません。

### 3.2 フォールバック実装

```java
@Service
@RequiredArgsConstructor
public class MiraChatServiceWithFallback {

    private final List<AiProviderClient> providers;  // 優先度順
    private final StaticResponseService staticResponseService;

    public MiraChatResponse chat(MiraContext context, String userMessage) {
        // 1. プライマリプロバイダーを試行
        for (AiProviderClient provider : providers) {
            try {
                String response = provider.chat(context, userMessage);
                return MiraChatResponse.success(response, provider.getName());
            } catch (MiraRetryableException e) {
                log.warn("Provider {} failed, trying next: {}", provider.getName(), e.getMessage());
            }
        }

        // 2. 静的応答にフォールバック
        Optional<String> staticResponse = staticResponseService.findResponse(
            context.getMode(),
            context.getScreenId()
        );

        if (staticResponse.isPresent()) {
            return MiraChatResponse.fallback(
                staticResponse.get(),
                "static-response",
                "AI サービスが一時的に利用できないため、事前定義された応答を表示しています。"
            );
        }

        // 3. エラーメッセージ
        return MiraChatResponse.error(
            MiraErrorCode.API_UNAVAILABLE,
            buildUserFriendlyErrorMessage(context.getLocale())
        );
    }

    private String buildUserFriendlyErrorMessage(String locale) {
        if ("ja".equals(locale)) {
            return """
                申し訳ございません。AI アシスタントが現在利用できません。

                しばらく時間をおいてから再度お試しください。
                問題が続く場合は、システム管理者にお問い合わせください。
                """;
        }
        return """
            Sorry, the AI assistant is currently unavailable.

            Please try again later.
            If the problem persists, contact your system administrator.
            """;
    }
}
```

### 3.3 静的応答サービス

```java
@Service
public class StaticResponseService {

    private final Map<String, String> screenHelpResponses = Map.of(
        "studio/modeler", """
            ## Modeler 画面について

            この画面では、エンティティ（データモデル）の設計を行います。

            ### 主な機能
            - **エンティティ作成**: 新しいデータモデルを定義
            - **属性追加**: フィールドの追加と型の設定
            - **リレーション設定**: エンティティ間の関係を定義

            詳細はヘルプドキュメントをご参照ください。
            """,
        "studio/form-designer", """
            ## Form Designer 画面について

            この画面では、フォーム（入力画面）のデザインを行います。

            ### 主な機能
            - **フィールド配置**: ドラッグ&ドロップでレイアウト
            - **バリデーション設定**: 入力チェックルールの定義
            - **条件付き表示**: 条件に応じたフィールド表示制御
            """
    );

    public Optional<String> findResponse(MiraMode mode, String screenId) {
        if (mode == MiraMode.CONTEXT_HELP && screenHelpResponses.containsKey(screenId)) {
            return Optional.of(screenHelpResponses.get(screenId));
        }
        return Optional.empty();
    }
}
```

---

## 4. Circuit Breaker

### 4.1 設定

```java
@Configuration
public class MiraCircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)                    // 50% 失敗でオープン
            .waitDurationInOpenState(Duration.ofSeconds(30))  // 30秒待機
            .slidingWindowSize(10)                       // 直近10リクエストで判定
            .minimumNumberOfCalls(5)                     // 最低5回のコールが必要
            .permittedNumberOfCallsInHalfOpenState(3)    // ハーフオープン時のテストコール数
            .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public CircuitBreaker miraCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("mira-ai");
    }
}
```

### 4.2 使用

```java
@Service
@RequiredArgsConstructor
public class MiraChatServiceWithCircuitBreaker {

    private final CircuitBreaker circuitBreaker;
    private final AiProviderClient primaryProvider;
    private final MiraChatServiceWithFallback fallbackService;

    public MiraChatResponse chat(MiraContext context, String userMessage) {
        Supplier<MiraChatResponse> decoratedCall = CircuitBreaker
            .decorateSupplier(circuitBreaker, () -> {
                String response = primaryProvider.chat(context, userMessage);
                return MiraChatResponse.success(response, primaryProvider.getName());
            });

        try {
            return decoratedCall.get();
        } catch (CallNotPermittedException e) {
            // Circuit Breaker がオープン状態
            log.warn("Circuit breaker is open, using fallback");
            return fallbackService.chat(context, userMessage);
        }
    }
}
```

---

## 5. エラーログ・通知

### 5.1 構造化ログ

```java
@Aspect
@Component
@RequiredArgsConstructor
public class MiraErrorLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger("mira.error");

    @AfterThrowing(
        pointcut = "execution(* jp.vemi.mirel.apps.mira.service.*.*(..))",
        throwing = "ex"
    )
    public void logError(JoinPoint joinPoint, Exception ex) {
        Map<String, Object> errorContext = new HashMap<>();
        errorContext.put("method", joinPoint.getSignature().toShortString());
        errorContext.put("errorType", ex.getClass().getSimpleName());
        errorContext.put("message", ex.getMessage());
        errorContext.put("timestamp", Instant.now().toString());

        if (ex instanceof MiraException miraEx) {
            errorContext.put("errorCode", miraEx.getErrorCode().getCode());
            errorContext.put("retryable", miraEx instanceof MiraRetryableException);
        }

        log.error("Mira error occurred: {}", objectMapper.writeValueAsString(errorContext), ex);
    }
}
```

### 5.2 Slack 通知（重大エラー用）

````java
@Component
@ConditionalOnProperty(name = "mira.notification.slack.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SlackNotificationService {

    private final SlackWebhookClient slackClient;
    private final MiraAiProperties properties;

    public void notifyCriticalError(MiraErrorCode errorCode, String details) {
        if (!isCriticalError(errorCode)) {
            return;
        }

        SlackMessage message = SlackMessage.builder()
            .channel(properties.getNotification().getSlack().getChannel())
            .username("Mira Alert")
            .iconEmoji(":warning:")
            .text(String.format("*[CRITICAL]* Mira エラー: %s\n```%s```",
                errorCode.getCode(), details))
            .build();

        slackClient.send(message);
    }

    private boolean isCriticalError(MiraErrorCode errorCode) {
        return Set.of(
            MiraErrorCode.AUTH_TOKEN_INVALID,
            MiraErrorCode.AUTH_TOKEN_EXPIRED,
            MiraErrorCode.QUOTA_EXCEEDED
        ).contains(errorCode);
    }
}
````

---

## 6. ユーザー向けエラー表示

### 6.1 エラーレスポンス形式

```java
@Data
@Builder
public class MiraErrorResponse {

    private String errorCode;
    private String message;
    private String suggestion;
    private boolean retryable;
    private Long retryAfterSeconds;

    public static MiraErrorResponse from(MiraException ex) {
        return MiraErrorResponse.builder()
            .errorCode(ex.getErrorCode().getCode())
            .message(ex.getLocalizedMessage())
            .suggestion(getSuggestion(ex.getErrorCode()))
            .retryable(ex instanceof MiraRetryableException)
            .retryAfterSeconds(getRetryAfter(ex))
            .build();
    }

    private static String getSuggestion(MiraErrorCode code) {
        return switch (code) {
            case RATE_LIMIT_EXCEEDED ->
                "しばらく待ってから再度お試しください。";
            case CONTEXT_TOO_LARGE ->
                "質問を短くするか、会話をリセットしてください。";
            case API_UNAVAILABLE ->
                "システム管理者に連絡してください。";
            default -> null;
        };
    }
}
```

### 6.2 フロントエンド表示

```typescript
// apps/frontend-v3/src/features/mira/components/MiraErrorDisplay.tsx

interface MiraErrorDisplayProps {
  error: MiraError;
  onRetry?: () => void;
}

export function MiraErrorDisplay({ error, onRetry }: MiraErrorDisplayProps) {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 p-4">
      <div className="flex items-start gap-3">
        <AlertCircle className="h-5 w-5 text-red-500 mt-0.5" />
        <div className="flex-1">
          <p className="text-sm font-medium text-red-800">
            {error.message}
          </p>
          {error.suggestion && (
            <p className="mt-1 text-sm text-red-600">
              {error.suggestion}
            </p>
          )}
          {error.retryable && onRetry && (
            <Button
              variant="outline"
              size="sm"
              className="mt-3"
              onClick={onRetry}
            >
              再試行
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
```

---

## 7. テスト戦略

### 7.1 エラーシナリオテスト

```java
@SpringBootTest
class MiraErrorHandlingTest {

    @MockBean
    private AiProviderClient aiProviderClient;

    @Autowired
    private MiraChatService miraChatService;

    @Test
    void shouldRetryOnConnectionFailure() {
        // Given
        when(aiProviderClient.chat(any(), any()))
            .thenThrow(new MiraRetryableException(MiraErrorCode.API_CONNECTION_FAILED))
            .thenThrow(new MiraRetryableException(MiraErrorCode.API_CONNECTION_FAILED))
            .thenReturn("Success on third attempt");

        // When
        MiraChatResponse response = miraChatService.chat(context, "test message");

        // Then
        assertThat(response.isSuccess()).isTrue();
        verify(aiProviderClient, times(3)).chat(any(), any());
    }

    @Test
    void shouldFallbackToStaticResponseWhenAllProvidersFail() {
        // Given
        when(aiProviderClient.chat(any(), any()))
            .thenThrow(new MiraRetryableException(MiraErrorCode.API_UNAVAILABLE));

        MiraContext context = MiraContext.builder()
            .mode(MiraMode.CONTEXT_HELP)
            .screenId("studio/modeler")
            .build();

        // When
        MiraChatResponse response = miraChatService.chat(context, "help");

        // Then
        assertThat(response.isFallback()).isTrue();
        assertThat(response.getContent()).contains("Modeler");
    }

    @Test
    void shouldOpenCircuitBreakerAfterMultipleFailures() {
        // Given
        when(aiProviderClient.chat(any(), any()))
            .thenThrow(new MiraRetryableException(MiraErrorCode.API_TIMEOUT));

        // When: Call 10 times to trigger circuit breaker
        for (int i = 0; i < 10; i++) {
            try {
                miraChatService.chat(context, "test");
            } catch (Exception ignored) {}
        }

        // Then: Next call should be blocked by circuit breaker
        assertThatThrownBy(() -> miraChatService.chat(context, "test"))
            .isInstanceOf(CallNotPermittedException.class);
    }
}
```

---

## 8. 参考資料

- [Spring Retry Documentation](https://docs.spring.io/spring-retry/docs/current/reference/html/)
- [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)
- [Error Handling Best Practices](https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc)
