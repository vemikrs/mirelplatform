# mirel Studio Modeler コード体系

> 業務アプリ内の定数（選択肢・分類項目）を一元管理する Code System

---

## 1. Code System の役割

Code System は以下の機能を提供する：

- 列挙値（ステータス・種別など）の管理
- フォームの SelectBox に自動適用
- Flow の条件式で参照可能
- データ更新の整合性を保証（不正値排除）

---

## 2. Code Group 構造

```
CodeGroup（例：OrderStatus）
├─ code: "NEW",      text: "新規"
├─ code: "APPROVED", text: "承認済"
├─ code: "SHIPPED",  text: "出荷済"
└─ ...
```

---

## 3. JSON モデル

```jsonc
"codes": {
  "OrderStatus": [
    { "code": "NEW", "text": "新規", "sort": 1 },
    { "code": "APPROVED", "text": "承認済", "sort": 2 },
    { "code": "SHIPPED", "text": "出荷済", "sort": 3 }
  ]
}
```

---

## 4. DB テーブル設計

```sql
CREATE TABLE stu_code (
  group_id VARCHAR(100) NOT NULL,
  code VARCHAR(100) NOT NULL,
  group_text VARCHAR(200),      -- グループ表示名
  text VARCHAR(200),            -- コード表示名
  sort INT DEFAULT 0,           -- 表示順
  delete_flag BOOLEAN DEFAULT FALSE,
  
  -- 追加属性
  attribute1 VARCHAR(500),
  attribute2 VARCHAR(500),
  attribute3 VARCHAR(500),
  
  -- Draft/Publish
  status VARCHAR(20) DEFAULT 'published',
  draft_version INT DEFAULT 0,
  
  -- マルチテナント・監査
  tenant_id VARCHAR(50) NOT NULL,
  version INT DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  created_by VARCHAR(100) NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  updated_by VARCHAR(100) NOT NULL,
  
  PRIMARY KEY (group_id, code)
);
```

---

## 5. Code System 管理 UI

```
┌─────────────────────────────────────────────────────────────┐
│ 左：コードグループ一覧                                        │
├─────────────────────────────────────────────────────────────┤
│ 中央：コード一覧（編集）                                      │
├─────────────────────────────────────────────────────────────┤
│ 右：プロパティ（ソート・説明・無効化）                         │
└─────────────────────────────────────────────────────────────┘
```

### 操作

| 操作 | 説明 |
|------|------|
| コード追加 | 新規コード値を追加 |
| テキスト編集 | 表示名を変更 |
| 並び順変更 | ドラッグ＆ドロップで順序変更 |
| 無効化 | 削除しない運用を推奨（論理削除） |

---

## 6. Form Designer との連携

- フィールドが CodeGroup を参照している場合 → SelectBox を自動割り当て
- CodeGroup の変更は Form に即時反映
- 無効化コードは表示しない（設定で切替可）

### ウィジェット選択ロジック

| コード数 | 推奨ウィジェット |
|---------|----------------|
| 5 件以下 | Radio Button |
| 6 件以上 | SelectBox |

---

## 7. Flow Designer との連携

条件式で `codeGroup.code` を参照可能。バリデーションも自動で型安全。

**例：**
```
IF OrderStatus == "APPROVED"
```

---

## 8. API との連携

Runtime API は不正値を reject する。

**例：** 登録時に存在しないコードを指定するとエラー

```json
{
  "errors": ["OrderStatus: INVALID_VALUE"],
  "errorCode": "VALIDATION_ERROR"
}
```

---

## 9. 目的

- 組織全体でのコード管理の一本化
- UI・Flow・データ・API の整合性確保
- モデル駆動の業務アプリに必須となる要素

---

## 関連ドキュメント

- [Modeler 概要](./overview.md)
- [Form Designer Widget 仕様](../04_form-designer/widget-spec.md)
- [API 仕様](./api-spec.md)

---

*Powered by Copilot 🤖*
