# GraalVM Native Image ビルド影響調査レポート（修正版）

## 調査概要

Spring Boot 3.3.0 + Java 21 ベースの `mirelplatform` バックエンドを GraalVM Native Image でビルドする際の影響調査。ユーザーフィードバックに基づき修正。

---

## 現在の技術スタック

| カテゴリ       | 技術                            | Native Image対応 |
| :------------- | :------------------------------ | :--------------: |
| フレームワーク | Spring Boot 3.3.0               |        ✅        |
| JDK            | Java 21                         |        ✅        |
| ORM            | Hibernate 6.6.1 + JPA           |        ✅        |
| テンプレート   | **FreeMarker**（ProMarker使用） |    ⚠️ 開発中     |
| ユーティリティ | **Groovy Tuple2/Tuple3**        |   ⚠️ 設定必要    |
| スケジューラ   | **Quartz + @Scheduled**         |   ⚠️ 設定必要    |
| AI             | **Spring AI + Vertex AI SDK**   |   ⚠️ 設定必要    |
| ブラウザ自動化 | Selenide 7.5.1                  |    ❌ 非対応     |
| ドキュメント   | Apache POI / Tika               |   ⚠️ 設定必要    |
| キャッシュ     | Redis (Lettuce)                 |        ✅        |

---

## 必須機能の使用状況

### FreeMarker（ProMarker）- 4ファイル

- [TemplateEngineProcessor.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/ste/domain/engine/TemplateEngineProcessor.java) - メインテンプレート処理
- [TemplateParser.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/extension/function_resolver/main/TemplateParser.java)
- [StencilEditorServiceImp.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/apps/mste/domain/service/StencilEditorServiceImp.java)
- [EmailTemplateService.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/foundation/service/EmailTemplateService.java)

### Groovy Tuple2/Tuple3 - 6ファイル

- [StructureReader.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/ste/domain/engine/StructureReader.java) - Tuple2
- [TemplateEngineProcessor.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/ste/domain/engine/TemplateEngineProcessor.java) - Tuple3
- [DownloadController.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/foundation/web/api/DownloadController.java) - Tuple3
- [FileDownloadResult.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/foundation/feature/files/dto/FileDownloadResult.java) - Tuple3
- [FileDownloadServiceImpl.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/foundation/feature/files/service/FileDownloadServiceImpl.java) - Tuple3
- [FileUploadResult.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/foundation/feature/files/dto/FileUploadResult.java) - Tuple3

### Quartz / @Scheduled - 2ファイル

- [DeviceAuthService.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/security/device/DeviceAuthService.java)
- [OtpService.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/foundation/service/OtpService.java)

### Vertex AI SDK - 2ファイル

- [VertexAiGeminiClient.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/apps/mira/infrastructure/ai/VertexAiGeminiClient.java)
- [VectorStoreConfig.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/apps/mira/infrastructure/config/VectorStoreConfig.java)

---

## 対処方針の整理

### 1. 廃止（コメントアウト）

| 対象                   | ファイル数 | 対処           |
| :--------------------- | :--------- | :------------- |
| **Selenide関連コード** | 3ファイル  | コメントアウト |

対象ファイル:

- [SelenideAgent.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/apps/selenade/agent/SelenideAgent.java)
- [SelenideSuite.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/apps/selenade/agent/SelenideSuite.java)
- [RunTestServiceImp.java](file:///home/user/dev/mirel-local/backend/src/main/java/jp/vemi/mirel/apps/selenade/domain/service/RunTestServiceImp.java)

### 2. 事前ロード設定（リフレクション設定）

以下のクラス/ライブラリに対するリフレクション設定が必要：

| 対象                                     | 理由                                 |        設定難易度        |
| :--------------------------------------- | :----------------------------------- | :----------------------: |
| **FreeMarker**                           | テンプレート処理で動的クラスアクセス | 中（開発中サポートあり） |
| **Groovy Tuple2/Tuple3**                 | Groovyランタイムの一部               |            低            |
| **Quartz Scheduler**                     | Job定義の動的ロード                  |            中            |
| **ToStringBuilder.reflectionToString()** | 10クラスで使用                       |      低（代替推奨）      |
| **Class.forName("PGobject")**            | 1箇所                                |            低            |
| **Apache POI**                           | Excel処理                            |            中            |
| **Apache Tika**                          | ドキュメント読み込み                 |            中            |
| **Vertex AI SDK**                        | gRPC/Protobuf動的生成                |            高            |
| **hypersistence-utils**                  | Hibernateカスタム型                  |            中            |

設定ファイル配置場所:

```
backend/src/main/resources/META-INF/native-image/jp.vemi/mirelplatform/
├── reflect-config.json
├── resource-config.json
├── proxy-config.json
└── native-image.properties
```

### 3. 代替ライブラリ変更

| 現行                                   | 推奨対処                          | 備考                           |
| :------------------------------------- | :-------------------------------- | :----------------------------- |
| `ToStringBuilder.reflectionToString()` | Lombok `@ToString` または手動実装 | 10クラス修正必要               |
| `Groovy Tuple2/Tuple3`                 | 維持（リフレクション設定で対応）  | Javaレコードへの移行も検討可能 |

---

## 試験ビルド手順

### Step 1: build.gradle への変更

```groovy
plugins {
    // 既存プラグイン...
    id 'org.graalvm.buildtools.native' version '0.10.4'
}

graalvmNative {
    binaries {
        main {
            imageName = 'mirelplatform'
            mainClass = 'jp.vemi.mirel.MiplaApplication'
            buildArgs.addAll([
                '-H:+ReportExceptionStackTraces',
                '--initialize-at-build-time=org.slf4j',
                '--initialize-at-run-time=io.netty'
            ])
        }
    }
    metadataRepository {
        enabled = true  // GraalVM Reachability Metadata Repository使用
    }
}
```

### Step 2: Selenideコードのコメントアウト

```java
// SelenideAgent.java - 一時的にコメントアウト
/*
public class SelenideAgent {
    // ... 全コード
}
*/
```

### Step 3: ビルド実行

```bash
cd /home/user/dev/mirel-local
./gradlew :backend:nativeCompile 2>&1 | tee native-build.log
```

### Step 4: エラー分析

ビルドエラーから追加設定が必要なクラスを特定し、`reflect-config.json`に追加。

---

## 総合判断

> [!IMPORTANT]
> **Native Image 移行には相当な作業が必要**

| 項目               |         評価          |
| :----------------- | :-------------------: |
| フレームワーク対応 |        ✅ 良好        |
| 必須ライブラリ対応 |     ⚠️ 設定工数大     |
| Selenide対応       | ❌ コメントアウト必要 |
| Vertex AI SDK対応  |      ⚠️ 高リスク      |
| 運用メリット       |  △ 起動時間短縮のみ   |

> [!TIP]
> **推奨アプローチ**
>
> 1. まずSelenideコードをコメントアウトして試験ビルドを実行
> 2. エラーログから必要な設定を特定
> 3. 設定工数と効果を比較して最終判断

---

## 次のステップ

1. **試験ビルドを実施するか？**  
   → Selenideコメントアウト + Native Imageプラグイン追加 + ビルド実行

2. **ToStringBuilder.reflectionToString()の修正を先に行うか？**  
   → 10クラスをLombok @ToStringに変更（Native Image関係なく改善）
