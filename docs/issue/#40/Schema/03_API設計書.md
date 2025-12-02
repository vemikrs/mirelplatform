# Mirel Schema API設計書

## 1. 文書情報

| 項目 | 内容 |
|------|------|
| システム名 | Mirel Schema（ミレル スキーマ） |
| 版数 | 3.0 |
| 作成日 | 2025年11月28日 |

---

## 2. API概要

### 2.1 共通仕様

#### ベースURL

```
/apps/schema/api
```

#### リクエスト形式

すべてのAPIはPOSTメソッドでJSON形式のリクエストを受け付けます。

```json
{
  "content": {
    // API固有のパラメータ
  }
}
```

#### レスポンス形式

```json
{
  "data": {
    // API固有のレスポンスデータ
  },
  "messages": ["情報メッセージ1", "情報メッセージ2"],
  "errors": ["エラーメッセージ1", "エラーメッセージ2"],
  "errorCode": "SCH-V-001"
}
```

| フィールド | 型 | 説明 |
|-----------|-----|------|
| data | Object | API固有のレスポンスデータ |
| messages | String[] | 情報メッセージ（正常終了時） |
| errors | String[] | エラーメッセージ（バリデーションエラー等） |
| errorCode | String | エラーコード（エラー時のみ） |

#### HTTPステータスコード

| コード | 説明 |
|--------|------|
| 200 OK | 正常終了（errors配列で業務エラーを判定） |
| 401 Unauthorized | 認証エラー |
| 403 Forbidden | 認可エラー |
| 500 Internal Server Error | システムエラー |

---

## 3. APIエンドポイント一覧

| No. | エンドポイント | 概要 | カテゴリ |
|-----|---------------|------|---------|
| 1 | `/list` | レコード一覧取得 | レコード |
| 2 | `/load` | レコード詳細取得 | レコード |
| 3 | `/save` | レコード保存 | レコード |
| 4 | `/deleteRecord` | レコード削除 | レコード |
| 5 | `/listSchema` | スキーマ定義一覧取得 | モデル |
| 6 | `/saveSchema` | スキーマ定義保存 | モデル |
| 7 | `/deleteModel` | モデル削除 | モデル |
| 8 | `/listCode` | コード取得 | コードマスタ |
| 9 | `/saveCode` | コード保存 | コードマスタ |
| 10 | `/deleteCode` | コード削除 | コードマスタ |
| 11 | `/getApps` | アプリケーション一覧取得 | アプリケーション |

---

## 4. レコード管理API

### 4.1 レコード一覧取得 (list)

指定されたモデルのレコード一覧を取得します。

#### エンドポイント

```
POST /apps/schema/api/list
```

#### リクエスト

```json
{
  "content": {
    "modelId": "order",
    "criteria": {
      "keyword": "サンプル",
      "filters": [
        { "field": "status", "operator": "eq", "value": "confirmed" },
        { "field": "orderDate", "operator": "gte", "value": "2024-01-01" }
      ]
    },
    "page": {
      "number": 1,
      "size": 20
    },
    "sort": {
      "field": "orderDate",
      "direction": "desc"
    }
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| modelId | String | ○ | 取得対象のモデルID |
| criteria | Object | - | 検索条件 |
| criteria.keyword | String | - | キーワード検索（全フィールド対象） |
| criteria.filters | Array | - | フィルタ条件の配列 |
| criteria.filters[].field | String | ○ | フィールドID |
| criteria.filters[].operator | String | ○ | 演算子（eq, ne, gt, gte, lt, lte, like, in） |
| criteria.filters[].value | Any | ○ | 検索値 |
| page | Object | - | ページネーション |
| page.number | Number | - | ページ番号（1始まり、デフォルト: 1） |
| page.size | Number | - | 1ページあたりの件数（デフォルト: 20、最大: 100） |
| sort | Object | - | ソート条件 |
| sort.field | String | - | ソート対象フィールドID |
| sort.direction | String | - | ソート順序（asc, desc） |

#### レスポンス

```json
{
  "data": {
    "dictionary": {
      "modelId": "order",
      "nodeType": "ROOT",
      "childs": [
        {
          "nodeType": "PRIMITIVE_TYPE",
          "fieldId": "orderId",
          "fieldName": "注文ID",
          "widgetType": "text",
          "isKey": true,
          "isRequired": true,
          "isHeader": true
        },
        {
          "nodeType": "PRIMITIVE_TYPE",
          "fieldId": "orderDate",
          "fieldName": "注文日",
          "widgetType": "text_date",
          "isRequired": true,
          "isHeader": true
        }
      ]
    },
    "records": [
      {
        "recordId": "uuid-1234-5678",
        "orderId": "ORD-001",
        "orderDate": "2024-01-15"
      },
      {
        "recordId": "uuid-2345-6789",
        "orderId": "ORD-002",
        "orderDate": "2024-01-20"
      }
    ],
    "totalCount": 150,
    "page": {
      "number": 1,
      "size": 20,
      "totalPages": 8
    }
  },
  "messages": [],
  "errors": []
}
```

| レスポンス項目 | 型 | 説明 |
|---------------|-----|------|
| dictionary | Object | モデル定義のツリー構造 |
| records | Array | レコードデータの配列 |
| totalCount | Number | 総件数（ページネーション表示用） |
| page.number | Number | 現在のページ番号 |
| page.size | Number | 1ページあたりの件数 |
| page.totalPages | Number | 総ページ数 |

---

### 4.2 レコード詳細取得 (load)

指定されたレコードの詳細データを取得します。

#### エンドポイント

```
POST /apps/schema/api/load
```

#### リクエスト

```json
{
  "content": {
    "modelId": "order",
    "recordId": "uuid-1234-5678"
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| modelId | String | △ | モデルID（省略時はrecordIdから逆引き） |
| recordId | String | △ | レコードID |

#### レスポンス

```json
{
  "data": {
    "dictionary": {
      "modelId": "order",
      "childs": [...]
    },
    "recordId": "uuid-1234-5678",
    "record": {
      "recordId": "uuid-1234-5678",
      "orderId": "ORD-001",
      "orderDate": "2024-01-15",
      "customerName": "株式会社サンプル",
      "status": "confirmed"
    },
    "valueTexts": {
      "model": [
        { "value": "order", "text": "注文" },
        { "value": "customer", "text": "顧客" }
      ],
      "status": [
        { "value": "draft", "text": "下書き" },
        { "value": "confirmed", "text": "確定" }
      ]
    }
  },
  "messages": [],
  "errors": []
}
```

---

### 4.3 レコード保存 (save)

レコードを新規登録または更新します。

#### エンドポイント

```
POST /apps/schema/api/save
```

#### リクエスト

```json
{
  "content": {
    "modelId": "order",
    "recordId": "",
    "record": {
      "orderId": "ORD-003",
      "orderDate": "2024-01-25",
      "customerName": "新規株式会社",
      "status": "draft"
    }
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| modelId | String | ○ | モデルID |
| recordId | String | - | レコードID（空の場合は新規登録） |
| record | Object | ○ | レコードデータ |

#### レスポンス（成功時）

```json
{
  "data": {
    "recordId": "uuid-3456-7890"
  },
  "messages": ["保存しました。"],
  "errors": []
}
```

#### レスポンス（バリデーションエラー時）

```json
{
  "data": null,
  "messages": [],
  "errors": [
    "注文IDは必須項目です。",
    "キーが重複しています。",
    "メールアドレスの形式が正しくありません。",
    "金額は0〜1000000の範囲で入力してください。"
  ],
  "errorCode": "SCH-V-001"
}
```

#### レスポンス（楽観的ロックエラー時）

```json
{
  "data": null,
  "messages": [],
  "errors": [
    "他のユーザーにより更新されました。再読み込みしてください。"
  ],
  "errorCode": "SCH-D-005"
}
```

---

### 4.4 レコード削除 (deleteRecord)

指定されたレコードを削除します。

#### エンドポイント

```
POST /apps/schema/api/deleteRecord
```

#### リクエスト

```json
{
  "content": {
    "recordId": "uuid-1234-5678"
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| recordId | String | ○ | 削除対象のレコードID |

#### レスポンス

```json
{
  "data": {},
  "messages": ["削除しました。"],
  "errors": []
}
```

---

## 5. モデル管理API

### 5.1 スキーマ定義一覧取得 (listSchema)

指定されたモデルのフィールド定義一覧を取得します。

#### エンドポイント

```
POST /apps/schema/api/listSchema
```

#### リクエスト

```json
{
  "content": {
    "modelId": "order",
    "criteria": {
      "keyword": ""
    },
    "page": {
      "number": 1,
      "size": 100
    },
    "sort": {
      "field": "sort",
      "direction": "asc"
    }
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| modelId | String | ○ | 取得対象のモデルID |
| criteria | Object | - | 検索条件 |
| page | Object | - | ページネーション |
| sort | Object | - | ソート条件 |

#### レスポンス

```json
{
  "data": {
    "columns": [
      { "field": "fieldId", "headerName": "フィールドID", "width": 180 },
      { "field": "fieldName", "headerName": "名称" },
      { "field": "widgetType", "headerName": "型", "width": 100 },
      { "field": "isKey", "headerName": "キー", "width": 75 },
      { "field": "isRequired", "headerName": "必須", "width": 75 },
      { "field": "isHeader", "headerName": "一覧", "width": 75 },
      { "field": "relationCodeGroup", "headerName": "コード" },
      { "field": "function", "headerName": "ファンクション" }
    ],
    "schemas": [
      {
        "modelId": "order",
        "fieldId": "orderId",
        "fieldName": "注文ID",
        "widgetType": "text",
        "isKey": true,
        "isRequired": true,
        "isHeader": true,
        "sort": 1,
        "regexPattern": "^ORD-[0-9]+$",
        "minLength": 5,
        "maxLength": 20
      },
      {
        "modelId": "order",
        "fieldId": "orderDate",
        "fieldName": "注文日",
        "widgetType": "text_date",
        "isRequired": true,
        "isHeader": true,
        "sort": 2
      },
      {
        "modelId": "order",
        "fieldId": "totalAmount",
        "fieldName": "合計金額",
        "widgetType": "text",
        "dataType": "number",
        "isHeader": true,
        "sort": 3,
        "minValue": 0,
        "maxValue": 100000000
      }
    ],
    "totalCount": 15
  },
  "messages": [],
  "errors": []
}
```

---

### 5.2 スキーマ定義保存 (saveSchema)

モデルのフィールド定義を保存します。

#### エンドポイント

```
POST /apps/schema/api/saveSchema
```

#### リクエスト

```json
{
  "content": {
    "modelId": "order",
    "modelName": "注文",
    "isHiddenModel": false,
    "fields": [
      {
        "fieldId": "orderId",
        "fieldName": "注文ID",
        "widgetType": "text",
        "isKey": true,
        "isRequired": true,
        "isHeader": true,
        "regexPattern": "^ORD-[0-9]+$",
        "minLength": 5,
        "maxLength": 20,
        "function": "${_seq(\"ORD\", 5)}"
      },
      {
        "fieldId": "orderDate",
        "fieldName": "注文日",
        "widgetType": "text_date",
        "isRequired": true,
        "isHeader": true,
        "function": "${_today()}"
      },
      {
        "fieldId": "totalAmount",
        "fieldName": "合計金額",
        "widgetType": "text",
        "dataType": "number",
        "isHeader": true,
        "minValue": 0,
        "maxValue": 100000000
      },
      {
        "fieldId": "status",
        "fieldName": "ステータス",
        "widgetType": "selectbox",
        "isHeader": true,
        "relationCodeGroup": "order_status"
      }
    ]
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| modelId | String | ○ | モデルID |
| modelName | String | - | モデル表示名 |
| isHiddenModel | Boolean | - | 非表示モデルか |
| fields | Array | ○ | フィールド定義の配列 |
| fields[].regexPattern | String | - | 正規表現パターン |
| fields[].minLength | Number | - | 最小文字数 |
| fields[].maxLength | Number | - | 最大文字数 |
| fields[].minValue | Number | - | 最小値（数値型） |
| fields[].maxValue | Number | - | 最大値（数値型） |

#### レスポンス

```json
{
  "data": {},
  "messages": ["保存しました。"],
  "errors": []
}
```

---

### 5.3 モデル削除 (deleteModel)

指定されたモデル定義を削除します。

#### エンドポイント

```
POST /apps/schema/api/deleteModel
```

#### リクエスト

```json
{
  "content": {
    "modelId": "order"
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| modelId | String | ○ | 削除対象のモデルID |

#### レスポンス

```json
{
  "data": {},
  "messages": [],
  "errors": []
}
```

---

## 6. コードマスタ管理API

### 6.1 コード取得 (listCode)

コードグループまたはコード値一覧を取得します。

#### エンドポイント

```
POST /apps/schema/api/listCode
```

#### リクエスト

```json
{
  "content": {
    "id": "order_status",
    "criteria": {
      "keyword": ""
    },
    "page": {
      "number": 1,
      "size": 100
    },
    "sort": {
      "field": "sort",
      "direction": "asc"
    }
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| id | String | - | コードグループID（特殊ID対応） |
| criteria | Object | - | 検索条件 |
| page | Object | - | ページネーション |
| sort | Object | - | ソート条件 |

#### 特殊ID一覧

| id | 動作 |
|-----|------|
| (空) | コードマスタのカラム定義を返却 |
| `model` | 非表示でないモデル一覧を返却 |
| `model_all` | 全モデル一覧を返却（非表示含む） |
| `codeGroup` | コードグループ一覧を返却 |
| (その他) | 指定グループIDのコード値一覧を返却 |

#### レスポンス（コード値取得時）

```json
{
  "data": {
    "columns": [
      { "field": "value", "headerName": "値" },
      { "field": "text", "headerName": "表示名" },
      { "field": "groupText", "headerName": "グループ表示名" }
    ],
    "valueTexts": [
      { "value": "draft", "text": "下書き", "groupText": "注文ステータス" },
      { "value": "confirmed", "text": "確定", "groupText": "注文ステータス" },
      { "value": "cancelled", "text": "キャンセル", "groupText": "注文ステータス" }
    ],
    "totalCount": 3
  },
  "messages": [],
  "errors": []
}
```

#### レスポンス（モデル一覧取得時）

```json
{
  "data": {
    "valueTexts": [
      { "value": "order", "text": "注文", "ext": { "isHidden": false } },
      { "value": "customer", "text": "顧客", "ext": { "isHidden": false } }
    ],
    "totalCount": 2
  },
  "messages": [],
  "errors": []
}
```

---

### 6.2 コード保存 (saveCode)

コードグループのコード値を一括保存します。

#### エンドポイント

```
POST /apps/schema/api/saveCode
```

#### リクエスト

```json
{
  "content": {
    "groupId": "order_status",
    "details": [
      { "value": "draft", "text": "下書き" },
      { "value": "confirmed", "text": "確定" },
      { "value": "cancelled", "text": "キャンセル" }
    ]
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| groupId | String | ○ | コードグループID |
| details | Array | ○ | コード値の配列 |

#### レスポンス

```json
{
  "data": {},
  "messages": ["保存しました。"],
  "errors": []
}
```

---

### 6.3 コード削除 (deleteCode)

指定されたコードグループを削除します。

#### エンドポイント

```
POST /apps/schema/api/deleteCode
```

#### リクエスト

```json
{
  "content": {
    "codeGroupId": "order_status"
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| codeGroupId | String | ○ | 削除対象のコードグループID |

#### レスポンス

```json
{
  "data": {},
  "messages": [],
  "errors": []
}
```

---

## 7. データ型定義

### 7.1 SearchCriteria

検索条件オブジェクト。

```typescript
interface SearchCriteria {
  keyword?: string;
  filters?: FilterCondition[];
}

interface FilterCondition {
  field: string;
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'like' | 'in';
  value: any;
}
```

### 7.2 Pagination

ページネーションオブジェクト。

```typescript
interface PageRequest {
  number?: number;  // ページ番号（1始まり）
  size?: number;    // 1ページあたりの件数
}

interface PageResponse {
  number: number;
  size: number;
  totalPages: number;
}
```

### 7.3 SortCondition

ソート条件オブジェクト。

```typescript
interface SortCondition {
  field: string;
  direction: 'asc' | 'desc';
}
```

### 7.4 FieldDefinition

フィールド定義オブジェクト。

```typescript
interface FieldDefinition {
  modelId: string;
  fieldId: string;
  fieldName: string;
  widgetType: WidgetType;
  dataType?: DataType;
  isKey?: boolean;
  isRequired?: boolean;
  isHeader?: boolean;
  sort?: number;
  displayWidth?: number;
  format?: string;
  maxDigits?: number;
  relationCodeGroup?: string;
  defaultValue?: string;
  function?: string;
  description?: string;
  // バリデーション属性
  regexPattern?: string;
  minLength?: number;
  maxLength?: number;
  minValue?: number;
  maxValue?: number;
}
```

### 7.5 ValueText

選択肢の値と表示テキスト。

```typescript
interface ValueText {
  value: string;
  text: string;
  groupId?: string;
  groupText?: string;
  constraintId?: string;
  ext?: Record<string, any>;
}
```

### 7.6 DictionaryTreeNode

ディクショナリのツリーノード。

```typescript
interface DictionaryTreeNode {
  nodeType: 'ROOT' | 'ABSTRACT_SCHEMA' | 'PRIMITIVE_TYPE';
  modelId?: string;
  fieldId?: string;
  fieldName?: string;
  widgetType?: string;
  childs?: DictionaryTreeNode[];
  // PrimitiveElementの全属性
  regexPattern?: string;
  minLength?: number;
  maxLength?: number;
  minValue?: number;
  maxValue?: number;
}
```

---

## 8. OpenAPI仕様

### 8.1 アノテーション例

```java
@RestController
@RequestMapping("/apps/schema/api")
@Tag(name = "Mirel Schema", description = "動的データ管理API")
public class SchemaApiController {

    @Operation(
        summary = "レコード一覧取得",
        description = "指定されたモデルのレコード一覧を取得します"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功"),
        @ApiResponse(responseCode = "401", description = "認証エラー")
    })
    @PostMapping("/list")
    public ResponseEntity<ApiResponse<ListResult>> list(
        @RequestBody ListRequest request
    ) {
        // ...
    }
}
```

### 8.2 Swagger UI

開発環境では以下のURLでAPI仕様を確認できます。

```
http://localhost:8080/swagger-ui/index.html
```

---

## 9. Webhook管理API

### 9.1 Webhook登録一覧取得 (listWebhook)

テナントに登録されているWebhook一覧を取得します。

#### エンドポイント

```
POST /apps/schema/api/webhook/list
```

#### リクエスト

```json
{
  "content": {
    "page": {
      "number": 1,
      "size": 20
    }
  }
}
```

#### レスポンス

```json
{
  "data": {
    "webhooks": [
      {
        "subscriptionId": "wh-001",
        "name": "注文通知",
        "url": "https://example.com/hooks/orders",
        "events": ["record.created", "record.updated"],
        "modelIds": ["order"],
        "isActive": true,
        "createdAt": "2024-11-01T10:00:00Z"
      }
    ],
    "totalCount": 5
  },
  "messages": [],
  "errors": []
}
```

---

### 9.2 Webhook登録 (saveWebhook)

新しいWebhookを登録または更新します。

#### エンドポイント

```
POST /apps/schema/api/webhook/save
```

#### リクエスト

```json
{
  "content": {
    "subscriptionId": "",
    "name": "注文通知",
    "url": "https://example.com/hooks/orders",
    "events": ["record.created", "record.updated"],
    "modelIds": ["order"],
    "headers": {
      "X-Custom-Header": "value"
    }
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| subscriptionId | String | - | 空=新規登録、値あり=更新 |
| name | String | ○ | Webhook名 |
| url | String | ○ | 配信先URL（HTTPS推奨） |
| events | String[] | ○ | 対象イベント |
| modelIds | String[] | - | 対象モデル（空=全モデル） |
| headers | Object | - | カスタムヘッダー |

#### 対象イベント一覧

| イベント | 説明 |
|----------|------|
| `record.created` | レコード作成時 |
| `record.updated` | レコード更新時 |
| `record.deleted` | レコード削除時 |
| `model.updated` | モデル定義変更時 |

#### レスポンス

```json
{
  "data": {
    "subscriptionId": "wh-001",
    "secret": "whsec_xxxxx..."
  },
  "messages": ["Webhookを登録しました。"],
  "errors": []
}
```

---

### 9.3 Webhook削除 (deleteWebhook)

Webhookを削除します。

#### エンドポイント

```
POST /apps/schema/api/webhook/delete
```

#### リクエスト

```json
{
  "content": {
    "subscriptionId": "wh-001"
  }
}
```

#### レスポンス

```json
{
  "data": {},
  "messages": ["削除しました。"],
  "errors": []
}
```

---

### 9.4 Webhook配信履歴取得 (listWebhookDelivery)

Webhookの配信履歴を取得します。

#### エンドポイント

```
POST /apps/schema/api/webhook/deliveries
```

#### リクエスト

```json
{
  "content": {
    "subscriptionId": "wh-001",
    "page": {
      "number": 1,
      "size": 20
    }
  }
}
```

#### レスポンス

```json
{
  "data": {
    "deliveries": [
      {
        "deliveryId": 12345,
        "eventType": "record.created",
        "status": "success",
        "responseStatus": 200,
        "durationMs": 150,
        "createdAt": "2024-11-28T10:30:00Z",
        "deliveredAt": "2024-11-28T10:30:00Z"
      },
      {
        "deliveryId": 12344,
        "eventType": "record.updated",
        "status": "failed",
        "responseStatus": 500,
        "errorMessage": "Connection timeout",
        "attempt": 3,
        "createdAt": "2024-11-28T10:25:00Z"
      }
    ],
    "totalCount": 100
  },
  "messages": [],
  "errors": []
}
```

---

### 9.5 Webhook再送 (retryWebhookDelivery)

失敗したWebhook配信を再送します。

#### エンドポイント

```
POST /apps/schema/api/webhook/retry
```

#### リクエスト

```json
{
  "content": {
    "deliveryId": 12344
  }
}
```

#### レスポンス

```json
{
  "data": {
    "deliveryId": 12346
  },
  "messages": ["再送をスケジュールしました。"],
  "errors": []
}
```

---

### 9.6 Webhookペイロード仕様

Webhookで配信されるペイロードの形式です。

#### ヘッダー

| ヘッダー | 説明 |
|----------|------|
| `X-Webhook-Id` | 配信ID |
| `X-Webhook-Event` | イベント種別 |
| `X-Webhook-Signature` | HMAC-SHA256署名 |
| `X-Webhook-Timestamp` | 送信タイムスタンプ |

#### 署名検証

```javascript
const crypto = require('crypto');

function verifyWebhookSignature(payload, signature, secret) {
  const timestamp = signature.split(',')[0].split('=')[1];
  const receivedSig = signature.split(',')[1].split('=')[1];
  
  const signedPayload = `${timestamp}.${JSON.stringify(payload)}`;
  const expectedSig = crypto
    .createHmac('sha256', secret)
    .update(signedPayload)
    .digest('hex');
    
  return crypto.timingSafeEqual(
    Buffer.from(receivedSig),
    Buffer.from(expectedSig)
  );
}
```

#### ペイロード例

```json
{
  "id": "evt_123456",
  "event": "record.created",
  "timestamp": "2024-11-28T10:30:00Z",
  "tenant_id": "tenant-001",
  "data": {
    "id": "rec-789",
    "schema": "order",
    "record_data": {
      "orderId": "ORD-001",
      "customerName": "株式会社サンプル",
      "orderDate": "2024-11-28",
      "status": "draft"
    },
    "created_at": "2024-11-28T10:30:00Z",
    "created_by": "user-123"
  }
}
```

---

## 10. 監査ログAPI

### 10.1 監査ログ検索 (listAuditLog)

監査ログを検索します。

#### エンドポイント

```
POST /apps/schema/api/audit/list
```

#### リクエスト

```json
{
  "content": {
    "criteria": {
      "resourceType": "record",
      "resourceId": "rec-789",
      "userId": "user-123",
      "action": "UPDATE",
      "dateFrom": "2024-11-01",
      "dateTo": "2024-11-30"
    },
    "page": {
      "number": 1,
      "size": 50
    },
    "sort": {
      "field": "createdAt",
      "direction": "desc"
    }
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| criteria.resourceType | String | - | リソース種別（record, model, code） |
| criteria.resourceId | String | - | リソースID |
| criteria.userId | String | - | 操作ユーザーID |
| criteria.action | String | - | アクション（CREATE, UPDATE, DELETE） |
| criteria.dateFrom | String | - | 期間開始日 |
| criteria.dateTo | String | - | 期間終了日 |

#### レスポンス

```json
{
  "data": {
    "logs": [
      {
        "auditId": 12345,
        "userId": "user-123",
        "action": "UPDATE",
        "resourceType": "record",
        "resourceId": "rec-789",
        "diff": {
          "status": { "from": "draft", "to": "confirmed" },
          "totalAmount": { "from": 10000, "to": 12000 }
        },
        "ipAddress": "192.168.1.100",
        "createdAt": "2024-11-28T10:30:00Z"
      }
    ],
    "totalCount": 150
  },
  "messages": [],
  "errors": []
}
```

---

### 10.2 監査ログ詳細取得 (loadAuditLog)

監査ログの詳細（変更前後のデータ）を取得します。

#### エンドポイント

```
POST /apps/schema/api/audit/load
```

#### リクエスト

```json
{
  "content": {
    "auditId": 12345
  }
}
```

#### レスポンス

```json
{
  "data": {
    "auditId": 12345,
    "userId": "user-123",
    "action": "UPDATE",
    "resourceType": "record",
    "resourceId": "rec-789",
    "oldValue": {
      "orderId": "ORD-001",
      "status": "draft",
      "totalAmount": 10000
    },
    "newValue": {
      "orderId": "ORD-001",
      "status": "confirmed",
      "totalAmount": 12000
    },
    "diff": {
      "status": { "from": "draft", "to": "confirmed" },
      "totalAmount": { "from": 10000, "to": 12000 }
    },
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "traceId": "abc-123-def-456",
    "createdAt": "2024-11-28T10:30:00Z"
  },
  "messages": [],
  "errors": []
}
```

---

## 11. 権限管理API

### 11.1 ロール一覧取得 (listRoles)

テナントのロール一覧を取得します。

#### エンドポイント

```
POST /apps/schema/api/roles/list
```

#### リクエスト

```json
{
  "content": {}
}
```

#### レスポンス

```json
{
  "data": {
    "roles": [
      {
        "roleId": "admin",
        "roleName": "管理者",
        "description": "すべての操作が可能",
        "isSystem": true,
        "permissions": [
          { "resourceType": "all", "action": "admin" }
        ]
      },
      {
        "roleId": "order_editor",
        "roleName": "注文編集者",
        "description": "注文の編集が可能",
        "isSystem": false,
        "permissions": [
          { "resourceType": "model", "resourceId": "order", "action": "read" },
          { "resourceType": "model", "resourceId": "order", "action": "create" },
          { "resourceType": "model", "resourceId": "order", "action": "update" }
        ]
      }
    ]
  },
  "messages": [],
  "errors": []
}
```

---

### 11.2 ロール保存 (saveRole)

ロールを作成または更新します。

#### エンドポイント

```
POST /apps/schema/api/roles/save
```

#### リクエスト

```json
{
  "content": {
    "roleId": "order_editor",
    "roleName": "注文編集者",
    "description": "注文の編集が可能",
    "version": 1,
    "permissions": [
      { "resourceType": "model", "resourceId": "order", "action": "read" },
      { "resourceType": "model", "resourceId": "order", "action": "create" },
      { "resourceType": "model", "resourceId": "order", "action": "update" }
    ]
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| roleId | String | ○ | ロールID |
| roleName | String | ○ | ロール名 |
| description | String | - | 説明 |
| version | Integer | - | 楽観的ロック用バージョン（更新時必須） |
| permissions | Array | - | 権限リスト |

#### レスポンス

```json
{
  "data": {
    "roleId": "order_editor",
    "version": 2
  },
  "messages": ["ロールを保存しました。"],
  "errors": []
}
```

---

### 11.3 ロール削除 (deleteRole)

ロールを論理削除します。

#### エンドポイント

```
POST /apps/schema/api/roles/delete
```

#### リクエスト

```json
{
  "content": {
    "roleId": "order_editor",
    "version": 2
  }
}
```

| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|:----:|------|
| roleId | String | ○ | 削除対象のロールID |
| version | Integer | ○ | 楽観的ロック用バージョン |

#### レスポンス

```json
{
  "data": {},
  "messages": ["ロールを削除しました。"],
  "errors": []
}
```

#### エラーレスポンス（システムロール削除時）

```json
{
  "data": null,
  "messages": [],
  "errors": ["システム定義ロールは削除できません。"],
  "errorCode": "SCH-A-002"
}
```

---

## 12. アプリケーション管理API

### 12.1 アプリケーション一覧取得 (getApps)

mirelplatformに登録されているアプリケーションの一覧を取得します。
ナビゲーションやサイドバーでのアプリ切り替え表示に使用します。

#### エンドポイント

```
POST /apps/schema/api/getApps
```

#### リクエスト

```json
{
  "content": {}
}
```

#### レスポンス

```json
{
  "data": {
    "apps": [
      {
        "appId": "schema",
        "appName": "Mirel Schema",
        "description": "動的データ管理",
        "icon": "database",
        "url": "/apps/schema",
        "isActive": true,
        "sort": 1
      },
      {
        "appId": "mste",
        "appName": "ProMarker",
        "description": "テンプレートエンジン",
        "icon": "code",
        "url": "/apps/mste",
        "isActive": true,
        "sort": 2
      },
      {
        "appId": "selenade",
        "appName": "Selenade",
        "description": "テスト自動化",
        "icon": "flask",
        "url": "/apps/selenade",
        "isActive": false,
        "sort": 3
      }
    ]
  },
  "messages": [],
  "errors": []
}
```

| レスポンス項目 | 型 | 説明 |
|---------------|-----|------|
| apps | Array | アプリケーションリスト |
| apps[].appId | String | アプリケーションID |
| apps[].appName | String | アプリケーション名 |
| apps[].description | String | 説明 |
| apps[].icon | String | アイコン名 |
| apps[].url | String | アプリケーションURL |
| apps[].isActive | Boolean | 現在アクティブかどうか |
| apps[].sort | Number | 表示順 |

---

## 13. APIエンドポイント一覧（完全版）

| No. | エンドポイント | 概要 | カテゴリ |
|-----|---------------|------|---------|
| 1 | `/list` | レコード一覧取得 | レコード |
| 2 | `/load` | レコード詳細取得 | レコード |
| 3 | `/save` | レコード保存 | レコード |
| 4 | `/deleteRecord` | レコード削除 | レコード |
| 5 | `/listSchema` | スキーマ定義一覧取得 | モデル |
| 6 | `/saveSchema` | スキーマ定義保存 | モデル |
| 7 | `/deleteModel` | モデル削除 | モデル |
| 8 | `/listCode` | コード取得 | コードマスタ |
| 9 | `/saveCode` | コード保存 | コードマスタ |
| 10 | `/deleteCode` | コード削除 | コードマスタ |
| 11 | `/getApps` | アプリケーション一覧取得 | アプリケーション |
| 12 | `/webhook/list` | Webhook一覧取得 | Webhook |
| 13 | `/webhook/save` | Webhook登録・更新 | Webhook |
| 14 | `/webhook/delete` | Webhook削除 | Webhook |
| 15 | `/webhook/deliveries` | 配信履歴取得 | Webhook |
| 16 | `/webhook/retry` | 配信再送 | Webhook |
| 17 | `/audit/list` | 監査ログ検索 | 監査 |
| 18 | `/audit/load` | 監査ログ詳細 | 監査 |
| 19 | `/roles/list` | ロール一覧取得 | 権限 |
| 20 | `/roles/save` | ロール保存 | 権限 |
| 21 | `/roles/delete` | ロール削除 | 権限 |
