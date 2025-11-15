# Spring Boot Service Generator (Sample)

## 概要

このステンシルは、Spring Boot Service層の典型的なコード構成を自動生成するサンプルテンプレートです。
ProMarkerのvalidation機能の包括的なテスト用途として使用できます。

## 特徴

- **実践的なSpring Boot構成**: Controller、Service、Model、Mapperの標準的な層構造
- **包括的なvalidation定義**: 9種類のパラメータに対する多様なvalidationパターン
- **明確なパラメータ命名**: 省略形を避けた分かりやすいパラメータID

## パラメータ定義

このステンシルでは、以下の9つのパラメータを使用します：

| パラメータID | 名称 | デフォルト値 | validation |
|-------------|------|-------------|------------|
| `packageGroup` | パッケージグループ | `com.example` | 必須、Javaパッケージ形式 |
| `applicationId` | アプリケーションID | `sampleApp` | 必須、ローワーキャメルケース |
| `serviceId` | サービスID | `userService` | 必須、ローワーキャメルケース |
| `serviceName` | サービス名 | `ユーザーサービス` | 必須、2-100文字 |
| `eventId` | イベントID | `get` | 必須、ローワーキャメルケース |
| `eventName` | イベント名 | `取得` | 必須、1-50文字 |
| `version` | バージョン | `1.0` | 任意、バージョン形式 |
| `author` | 作成者 | `mirelplatform` | 任意、最大100文字 |
| `vendor` | ベンダー | `Open Source Community` | 任意、最大100文字 |

### Validation定義の詳細

### Validation定義の詳細

各パラメータには適切なvalidation定義が設定されています：

1. **packageGroup** - Javaパッケージ形式
   - `pattern: ^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$`
   - 小文字英数字とドット（.）のみ使用可能

2. **applicationId, serviceId, eventId** - ローワーキャメルケース
   - `pattern: ^[a-z][a-zA-Z0-9]*$`
   - 小文字で始まり、英数字のみ使用可能

3. **version** - バージョン形式
   - `pattern: ^[0-9]+\\.[0-9]+(\\.[0-9]+)?$`
   - セマンティックバージョニング形式（例：1.0、1.0.0）

4. **serviceName, eventName** - 日本語名称
   - 文字数制限のみ（正規表現なし）

5. **author, vendor** - 任意フィールド
   - `required: false`
   - 最大文字数制限のみ

## 生成されるファイル構造

```
src/
└── main/
    └── java/
        └── {packageGroup}/
            └── {applicationId}/
                ├── domain/
                │   ├── service/
                │   │   └── {ServiceId}Service.java
                │   ├── mapper/
                │   │   └── {ServiceId}Mapper.java
                │   └── model/
                │       ├── {ServiceId}{EventId}ParamModel.java
                │       └── {ServiceId}{EventId}ResultModel.java
                └── app/
                    ├── controller/
                    │   └── {ServiceId}Controller.java
                    ├── request/
                    │   └── {ServiceId}{EventId}Request.java
                    └── response/
                        └── {ServiceId}{EventId}Response.java
```

## テスト用途

### 1. Validation定義のテスト

このステンシルは以下のvalidationパターンをカバーします：

- ✅ **必須チェック** (`required: true`)
- ✅ **最小文字数** (`minLength`)
- ✅ **最大文字数** (`maxLength`)
- ✅ **正規表現パターン** (`pattern`)
- ✅ **カスタムエラーメッセージ** (`errorMessage`)
- ✅ **任意フィールド** (`required: false`)

### 2. 実践的なバリデーション例

- **Javaパッケージ名**: `^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$`
- **キャメルケース**: `^[a-z][a-zA-Z0-9]*$`
- **バージョン番号**: `^[0-9]+\\.[0-9]+(\\.[0-9]+)?$`

### 3. E2Eテストシナリオ

```typescript
// 正常系テスト
test('有効な入力でコード生成成功', async () => {
  await fillParameter('packageGroup', 'com.example');
  await fillParameter('applicationId', 'myApp');
  await fillParameter('serviceId', 'userService');
  // ... 他のパラメータ
  
  await clickGenerate();
  await expectSuccess();
});

// 異常系テスト
test('パッケージ名が不正な形式でエラー', async () => {
  await fillParameter('packageGroup', 'Com.Example'); // 大文字はNG
  await expectError('小文字英数字とドット（.）のみ使用可能です');
});

test('キャメルケースが不正でエラー', async () => {
  await fillParameter('serviceId', 'UserService'); // 先頭大文字はNG
  await expectError('ローワーキャメルケースで入力してください');
});

test('バージョン番号「1.0」が有効', async () => {
  await fillParameter('version', '1.0');
  await clickGenerate();
  await expectSuccess(); // min(3)削除により成功するはず
});
```

## 使用方法

### ステンシルマスタのリロード

```bash
curl -X POST http://localhost:3000/mipla2/apps/mste/api/reloadStencilMaster \
  -H "Content-Type: application/json" \
  -d '{"content": {}}'
```

### コード生成の実行

```bash
curl -X POST http://localhost:3000/mipla2/apps/mste/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "content": {
      "stencilCategoy": "/samples/springboot",
      "stencilCanonicalName": "/samples/springboot/spring-boot-service",
      "serialNo": "250101A",
      "packageGroup": "com.example",
      "applicationId": "sampleApp",
      "serviceId": "userService",
      "serviceName": "ユーザーサービス",
      "eventId": "get",
      "eventName": "取得",
      "version": "1.0",
      "author": "mirelplatform",
      "vendor": "Open Source Community"
    }
  }'
```

### 生成結果

上記のパラメータで生成すると、以下のようなファイルが生成されます：

- `com/example/sampleApp/domain/service/UserServiceService.java`
- `com/example/sampleApp/app/controller/UserServiceController.java`
- `com/example/sampleApp/domain/model/UserServiceGetParamModel.java`
- `com/example/sampleApp/domain/model/UserServiceGetResultModel.java`
- ... その他6ファイル

## 実装計画書への反映

validation-improvement-plan.md の以下のセクションで参照：

- **Step 2**: Backend YAML validation追加の実例
- **Step 4**: Frontend validation統合の実例
- **Step 6**: E2Eテストの包括的なテストケース

## 注意事項

⚠️ **このステンシルはサンプルです**

- 本番環境での使用は想定していません
- validation機能のテスト・デモンストレーション用途です
- 実際のプロジェクトでは、要件に応じてカスタマイズしてください

## 関連ドキュメント

- `/docs/issue/#33/validation-improvement-plan.md` - Validation機能改善計画
- `/.github/docs/api-reference.md` - API仕様書
- `/README.md` - mirelplatform概要
