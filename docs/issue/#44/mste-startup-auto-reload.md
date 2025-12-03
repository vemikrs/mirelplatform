# ProMarker起動時ステンシルマスタ自動リロード実装

**Issue**: #44 (OAuth2 Device Flow実装の一環)  
**実装日**: 2025年12月3日  
**担当**: GitHub Copilot

---

## 問題の背景

開発環境での初回 `/suggest` API呼び出し時にエラーが発生する問題が報告されました。

### 根本原因

- Spring Boot起動時、ステンシルマスタ（`mste_stencil`テーブル）が空の状態
- 初回 `/suggest` 呼び出し時、ステンシル候補リストが空で返される
- フロントエンド側でドロップダウンが正常に表示されない

### 従来の運用フロー（問題あり）

1. Spring Boot起動
2. **手動で** `/apps/mste/api/reloadStencilMaster` を呼び出し
3. その後 `/suggest` が正常動作

→ 開発者が毎回手動でリロードする必要があり、非効率

---

## 実装内容

### 1. ProMarker専用起動リスナー

**ファイル**: [MsteStartupListener.java](backend/src/main/java/jp/vemi/mirel/apps/mste/application/config/MsteStartupListener.java)

```java
@Component
public class MsteStartupListener {

    @Autowired
    private ReloadStencilMasterService reloadStencilMasterService;

    @Value("${mirel.apps.mste.auto-reload-stencil-on-startup:true}")
    private boolean autoReloadOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (!autoReloadOnStartup) {
            logger.info("[MSTE Startup] Stencil master auto-reload is DISABLED");
            return;
        }

        logger.info("[MSTE Startup] Starting stencil master auto-reload...");
        
        // ReloadStencilMasterServiceを呼び出し
        ApiRequest<ReloadStencilMasterParameter> request = ...;
        ApiResponse<?> response = reloadStencilMasterService.invoke(request);
        
        logger.info("[MSTE Startup] Reload completed successfully in {}ms", elapsedTime);
    }
}
```

### 設計のポイント

- **基盤機能に依存しない**: ProMarker (`mste`) パッケージ内に配置
- **ApplicationReadyEvent**: 全Bean初期化完了後に実行
- **設定で制御可能**: `mirel.apps.mste.auto-reload-stencil-on-startup` フラグ

---

## 設定ファイル

### 2. application.yml（基本設定）

```yaml
mirel:
  apps:
    mste:
      # ProMarker起動時のステンシルマスタ自動リロード設定
      auto-reload-stencil-on-startup: ${MIREL_MSTE_AUTO_RELOAD_ON_STARTUP:true}
```

### 3. application-dev.yml（開発環境）

```yaml
mirel:
  apps:
    mste:
      # 開発環境: 起動時に必ずステンシルマスタをリロード
      auto-reload-stencil-on-startup: true
```

### 4. application-e2e.yml（E2E環境）

```yaml
mirel:
  apps:
    mste:
      # E2E環境: テストケース実行前にステンシルが利用可能な状態にする
      auto-reload-stencil-on-startup: true
```

### 5. application-ci.yml（CI環境）

```yaml
mirel:
  apps:
    mste:
      # CI環境: テスト実行のため起動時にリロードしない
      # 各テストケースで必要に応じて手動リロード
      auto-reload-stencil-on-startup: false
```

---

## 動作確認

### 起動ログ

```
2025-12-03 19:51:10.214 [main] INFO  MsteStartupListener - [MSTE Startup] Application ready - Starting stencil master auto-reload...
2025-12-03 19:51:10.910 [main] INFO  MsteStartupListener - [MSTE Startup] Stencil master reload completed successfully in 695ms
2025-12-03 19:51:10.910 [main] INFO  MsteStartupListener - [MSTE Startup] ProMarker is ready to serve /suggest and /generate requests
```

### リロード処理の内容

1. `mste_stencil` テーブルをクリア
2. `file_management` テーブルをクリア（重複エラー回避）
3. 以下のレイヤーからステンシル定義を収集:
   - **User Layer**: `data/storage/apps/promarker/stencil/user`
   - **Standard Layer**: `data/storage/apps/promarker/stencil/standard`
   - **Samples Layer**: `classpath:/promarker/stencil/samples`
4. 各ステンシルをデータベースに登録
5. カテゴリ情報も自動生成して登録

### 実行時間

- **695ms** (開発環境、サンプルステンシル2件)
- 本番環境では件数に応じて増加する可能性

---

## 環境別の動作

| 環境 | auto-reload | 理由 |
|---|---|---|
| **dev** | `true` | クラスパス変更・ファイル更新を即座に反映 |
| **e2e** | `true` | テストケース実行前にステンシルが利用可能 |
| **ci** | `false` | H2データベースとの互換性、テスト制御 |
| **prod** | `true` (デフォルト) | 本番環境では環境変数で `false` に変更可能 |

### 本番環境での無効化方法

```bash
export MIREL_MSTE_AUTO_RELOAD_ON_STARTUP=false
```

または `application-prod.yml`:

```yaml
mirel:
  apps:
    mste:
      auto-reload-stencil-on-startup: false
```

---

## メリット

1. **開発体験の向上**: 手動リロード不要
2. **エラーの削減**: 初回 `/suggest` 呼び出し前に必ずステンシルが準備される
3. **環境別制御**: 本番・CI環境では無効化可能
4. **基盤非依存**: ProMarkerパッケージ内で完結

---

## 今後の改善案

1. **キャッシュ機構**: リロード時間短縮
2. **差分リロード**: 変更されたステンシルのみ更新
3. **並列処理**: 複数レイヤーの並列読み込み
4. **プログレス表示**: 大量ステンシル時の進捗可視化

---

## 関連ファイル

- [MsteStartupListener.java](backend/src/main/java/jp/vemi/mirel/apps/mste/application/config/MsteStartupListener.java)
- [application.yml](backend/src/main/resources/config/application.yml)
- [application-dev.yml](backend/src/main/resources/config/application-dev.yml)
- [application-e2e.yml](backend/src/main/resources/config/application-e2e.yml)
- [application-ci.yml](backend/src/main/resources/config/application-ci.yml)
- [ReloadStencilMasterService.java](backend/src/main/java/jp/vemi/mirel/apps/mste/domain/service/ReloadStencilMasterService.java)

---

**Powered by Copilot 🤖**
