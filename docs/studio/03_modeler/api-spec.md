# mirel Studio Modeler API 仕様

> Modeler の REST API 仕様書

---

## 1. 概要

mirel Studio Modeler の REST API 仕様を定義する。すべてのエンドポイントは `/mapi/studio/` をベースパスとする。

### 1.1 設計原則

| 原則 | API 設計への反映 |
|------|-----------------|
| **Model-Driven Everything** | Model 定義を取得すれば UI/バリデーション/データ構造が決定する |
| **Consistency First** | 型変更は Draft → Publish フローで整合性を検証 |
| **Release as a Unit** | Release Center 連携 API で一括デプロイ可能 |

---

## 2. 共通仕様

### 2.1 リクエスト形式

```typescript
interface ApiRequest<T = Record<string, any>> {
  content: T;
}
```

### 2.2 レスポンス形式

```typescript
interface ApiResponse<T = Record<string, any>> {
  data: T;
  messages: string[];
  errors: string[];
  errorCode?: string;
}
```

### 2.3 エラーコード

| コード | 説明 |
|-------|------|
| `VALIDATION_ERROR` | 入力検証エラー |
| `NOT_FOUND` | リソースが見つからない |
| `DUPLICATE_KEY` | キー重複 |
| `PERMISSION_DENIED` | 権限不足 |
| `DRAFT_CONFLICT` | Draft 競合 |
| `PUBLISH_FAILED` | 公開失敗 |

---

## 3. Model Explorer API

### 3.1 モデル一覧取得

**`POST /mapi/studio/listModels`**

```typescript
// Request
interface ListModelsRequest {
  includeHidden?: boolean;
  status?: 'all' | 'draft' | 'published';
}

// Response
interface ListModelsResponse {
  entities: ModelSummary[];
  views: ModelSummary[];
  codes: CodeGroupSummary[];
  draftCount: number;
}
```

### 3.2 モデル詳細取得

**`POST /mapi/studio/getModel`**

```typescript
// Request
interface GetModelRequest {
  modelId: string;
  includeRelations?: boolean;
  includeUsage?: boolean;
  includeDraft?: boolean;
}

// Response
interface GetModelResponse {
  header: ModelHeader;
  fields: FieldDefinition[];
  relations?: RelationInfo[];
  usage?: UsageInfo;
  draftChanges?: DraftChange[];
}
```

### 3.3 モデル検索

**`POST /mapi/studio/searchModels`**

```typescript
// Request
interface SearchModelsRequest {
  query: string;
  types?: ('entity' | 'view' | 'code')[];
  limit?: number;
}

// Response
interface SearchModelsResponse {
  results: SearchResult[];
  total: number;
}
```

---

## 4. Model Designer API

### 4.1 フィールド一覧取得

**`POST /mapi/studio/listSchema`**

```typescript
// Request
interface ListSchemaRequest {
  modelId: string;
}

// Response
interface ListSchemaResponse {
  schemas: FieldDefinition[];
}
```

### 4.2 モデル保存（Draft）

**`POST /mapi/studio/saveDraft`**

```typescript
// Request
interface SaveDraftRequest {
  modelId: string;
  header?: Partial<ModelHeader>;
  fields?: FieldDefinition[];
  relations?: RelationDefinition[];
}

// Response
interface SaveDraftResponse {
  modelId: string;
  draftVersion: number;
  changeCount: number;
}
```

### 4.3 モデル公開

**`POST /mapi/studio/publish`**

```typescript
// Request
interface PublishRequest {
  modelId: string;
  force?: boolean;
}

// Response
interface PublishResponse {
  modelId: string;
  publishedVersion: number;
  publishedAt: string;
}
```

### 4.4 Draft 破棄

**`POST /mapi/studio/discardDraft`**

```typescript
// Request
interface DiscardDraftRequest {
  modelId: string;
}

// Response
interface DiscardDraftResponse {
  modelId: string;
  discardedChangeCount: number;
}
```

### 4.5 モデル作成

**`POST /mapi/studio/createModel`**

```typescript
// Request
interface CreateModelRequest {
  modelId: string;
  modelName: string;
  modelType: 'entity' | 'view';
  modelCategory: 'transaction' | 'master';
  description?: string;
  copyFrom?: string;
}

// Response
interface CreateModelResponse {
  modelId: string;
  created: boolean;
}
```

### 4.6 モデル削除

**`POST /mapi/studio/deleteModel`**

```typescript
// Request
interface DeleteModelRequest {
  modelId: string;
  force?: boolean;
}

// Response
interface DeleteModelResponse {
  modelId: string;
  deleted: boolean;
  warnings?: string[];
}
```

---

## 5. Code API

### 5.1 コードグループ一覧

**`POST /mapi/studio/listCodeGroups`**

```typescript
// Request
interface ListCodeGroupsRequest {
  includeDeleted?: boolean;
}

// Response
interface ListCodeGroupsResponse {
  groups: string[];
  groupDetails?: CodeGroupSummary[];
}
```

### 5.2 コード一覧取得

**`POST /mapi/studio/listCode`**

```typescript
// Request
interface ListCodeRequest {
  id: string;  // groupId
  includeDeleted?: boolean;
}

// Response
interface ListCodeResponse {
  valueTexts: CodeValue[];
  groupName?: string;
  status?: 'draft' | 'published';
}
```

### 5.3 コード保存

**`POST /mapi/studio/saveCode`**

```typescript
// Request
interface SaveCodeRequest {
  groupId: string;
  groupName?: string;
  details: CodeValue[];
  publishImmediately?: boolean;
}

// Response
interface SaveCodeResponse {
  groupId: string;
  savedCount: number;
}
```

---

## 6. Record API（Runtime）

### 6.1 レコード一覧取得

**`POST /mapi/studio/list`**

```typescript
// Request
interface ListRecordsRequest {
  modelId: string;
  page?: number;
  size?: number;
  query?: string;
  sort?: string;
  order?: 'asc' | 'desc';
  filters?: Record<string, any>;
}

// Response
interface ListRecordsResponse {
  records: Record<string, any>[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}
```

### 6.2 レコード保存

**`POST /mapi/studio/save`**

```typescript
// Request
interface SaveRecordRequest {
  modelId: string;
  recordId?: string;
  record: Record<string, any>;
}

// Response
interface SaveRecordResponse {
  recordId: string;
  isNew: boolean;
}
```

---

## 7. バリデーション API

### 7.1 モデル検証

**`POST /mapi/studio/validate`**

```typescript
// Request
interface ValidateRequest {
  modelId: string;
  fields?: FieldDefinition[];
}

// Response
interface ValidateResponse {
  valid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
}
```

---

## 8. Release Center 連携 API

### 8.1 一括公開

**`POST /mapi/studio/publishAll`**

```typescript
// Request
interface PublishAllRequest {
  modelIds?: string[];
  description?: string;
}

// Response
interface PublishAllResponse {
  releaseId: string;
  releaseVersion: number;
  publishedModels: { modelId: string; version: number }[];
  failedModels: { modelId: string; error: string }[];
}
```

### 8.2 リリース検証

**`POST /mapi/studio/validateRelease`**

```typescript
// Request
interface ValidateReleaseRequest {
  modelIds?: string[];
}

// Response
interface ValidateReleaseResponse {
  valid: boolean;
  errors: ValidationIssue[];
  warnings: ValidationIssue[];
  breakingChanges: BreakingChange[];
}
```

---

## 関連ドキュメント

- [Modeler 概要](./overview.md)
- [データ型定義](./data-model.md)
- [Release Center 概要](../07_release-center/overview.md)

---

*Powered by Copilot 🤖*
