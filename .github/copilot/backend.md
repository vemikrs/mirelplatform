# Backend (Spring Boot)

## スコープ
`backend/` 以下の Spring Boot 3.3 + Java 21 プロジェクト。`jp.vemi.mirel.apps.mste` 直下に Controller / Service / Domain / DTO を分割し、レスポンスは `ApiResponse<T>` で統一する。アプリは `server.servlet.context-path=/mipla2` で提供される。

## 起動・ビルド
| 用途 | VS Code Task | CLI (参考) |
| --- | --- | --- |
| 開発サーバー | `shell: Backend: Start Spring Boot` | `SPRING_PROFILES_ACTIVE=dev SERVER_PORT=3000 ./gradlew :backend:bootRun` |
| Devtools無効で起動 | `shell: Backend: Start WITHOUT Devtools` | `SPRING_DEVTOOLS_RESTART_ENABLED=false ./gradlew :backend:bootRun` |
| 停止 | `shell: Backend: Stop` | VS Code Task 内部で `pkill -f "gradlew.*bootRun"` を実行。手動停止が必要な場合は `fuser -k 3000/tcp` でポートを解放してから再起動する。 |
| ビルド/テスト | ― | `./gradlew :backend:build`, `./gradlew :backend:check`, `./gradlew :backend:test` |

> **依存更新時チェック**: `./gradlew :backend:clean :backend:build` を必ず実行し、`build/reports/tests/test/index.html` で失敗が無いか確認。

## 実装ポリシー
- DTO は `domain/dto`、Form/Request は `application/controller` 直下 `request` パッケージへ置く。
- `ApiRequest<T>.model` を経由して `/mapi` から渡る payload を受け取り、null チェックを徹底。
- 新規 API は `/mipla2/apps/mste/...` に揃え、Swagger (`/mipla2/swagger-ui.html`) に反映されるよう `@Operation` を記述。
- 永続化: Spring Data JPA。開発時は H2、CI では MySQL を想定。Flyway 等は未導入のため SQL 変更はドキュメント化。

### Mira AI (`jp.vemi.mirel.apps.mira`)

AI アシスタント機能を提供するモジュール。設定は `mira.ai.*` プレフィックスで管理。

**アーキテクチャ:**
- `application/controller/MiraApiController.java` - REST API エンドポイント (`/mipla2/apps/mira/api/*`)
- `domain/service/MiraChatService.java` - チャットロジック、プロンプト構築
- `infrastructure/ai/` - AI プロバイダ抽象化層
  - `AiProviderClient` - プロバイダインタフェース
  - `AiProviderFactory` - プロバイダ選択・フォールバック
  - `MockAiClient` - テスト/開発用モック
  - `AzureOpenAiClient` - Azure OpenAI 実装
- `infrastructure/config/MiraAiProperties.java` - 設定クラス

**設定例 (application.yml):**
```yaml
mira:
  ai:
    enabled: true
    provider: azure-openai  # or github-models
    mock:
      enabled: false        # true で開発時モック使用
```

**新規プロバイダ追加手順:**
1. `AiProviderClient` を実装したクラスを作成
2. `@Component` で Bean 登録
3. `getProviderName()` で識別子を返す
4. `MiraAiProperties` に設定クラスを追加

## レビュー前チェックリスト
- [ ] `./gradlew :backend:check` が成功。
- [ ] 例外ハンドリングは `GlobalExceptionHandler` 経由で `ApiError` に変換される。
- [ ] 新規設定は `backend/src/main/resources/config/application.yml` or `application-*.yml` に追記し、`.env` 参照時は `spring.config.import=optional:file:.env` を利用。
- [ ] REST API 変更は `docs/api-*` への反映を検討。
