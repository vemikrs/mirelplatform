# mirel Studio Modeler データ型定義

> バックエンド・フロントエンド双方のデータ構造定義

---

## 1. 概要

mirel Studio Modeler のデータモデルは、**Entity / View / Code** の 3 種類を統一的に管理する。本ドキュメントでは、バックエンド・フロントエンド双方のデータ構造を定義する。

---

## 2. コアコンセプト

### 2.1 モデルタイプ

| タイプ     | 説明                       | 用途                 |
| ---------- | -------------------------- | -------------------- |
| **Entity** | 実データを持つテーブル相当 | 顧客、注文、商品など |
| **View**   | 複数 Entity の結合・投影   | 一覧表示、レポート   |
| **Code**   | コードマスタ（選択肢）     | ステータス、区分など |

### 2.2 階層構造（Recursive Domain Structure）

```
Model (Entity/View)
  └── Field[]
        ├── Primitive Field   ← 単一値
        └── Domain Field      ← 複合値（ネスト可、再帰的構造）
              └── Field[]
```

---

## 3. バックエンド Entity（Java）

### 3.1 StuModelHeader（モデルヘッダー）

```java
@Entity
@Table(name = "stu_model_header")
public class StuModelHeader {
    @Id
    private String modelId;           // モデルID
    private String modelName;          // 表示名
    private String description;        // 説明
    private String modelType;          // 'entity' | 'view'
    private String modelCategory;      // 'transaction' | 'master'
    private Boolean isHidden;          // 非表示フラグ
    private String primaryKeyField;    // PKフィールドID

    // Draft/Publish 状態
    private String status;             // 'draft' | 'published'
    private Integer draftVersion;
    private Integer publishedVersion;

    // マルチテナント・監査
    private String tenantId;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
```

### 3.2 StuField（フィールド定義）

```java
@Entity
@Table(name = "stu_dic_model")
public class StuModel {
    @EmbeddedId
    private PK pk;  // modelId + fieldId

    // 基本情報
    private String fieldName;          // 表示名
    private String description;        // 説明
    private String dataType;           // string | number | date | boolean | domain
    private Integer sort;              // 表示順

    // 階層構造
    private String parentFieldId;      // 親フィールドID
    private Integer depth;             // 階層深度

    // 制約
    private Boolean isKey;
    private Boolean isRequired;
    private Boolean isArray;

    // 表示
    private String widgetType;
    private Integer displayWidth;
    private Boolean isHeader;
    private String format;

    // バリデーション
    private Integer minLength;
    private Integer maxLength;
    private String regexPattern;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer decimalPlaces;

    // 関連
    private String relationCodeGroup;
    private String relationEntityId;
    private String relationFieldId;
    private String relationType;

    // デフォルト
    private String defaultValue;
    private String function;

    // Draft/Publish
    private String status;
    private Integer draftVersion;

    // マルチテナント・監査
    private String tenantId;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    @Data
    @Embeddable
    public static class PK implements Serializable {
        private String modelId;
        private String fieldId;
    }
}
```

---

## 4. フロントエンド型定義（TypeScript）

### 4.1 基本型

```typescript
// モデルタイプ
type ModelType = "entity" | "view";
type ModelCategory = "transaction" | "master";
type ModelStatus = "draft" | "published";

// フィールドタイプ
type DataType = "string" | "number" | "date" | "boolean" | "domain";
type WidgetType =
  | "text"
  | "textarea"
  | "number"
  | "select"
  | "radio"
  | "checkbox"
  | "datepicker"
  | "datetimepicker"
  | "file"
  | "hidden";

// リレーションタイプ
type RelationType = "many-to-one" | "one-to-many" | "many-to-many";
```

### 4.2 モデル定義

```typescript
interface ModelHeader {
  modelId: string;
  modelName: string;
  description?: string;
  modelType: ModelType;
  modelCategory: ModelCategory;
  isHidden: boolean;
  primaryKeyField?: string;
  status: ModelStatus;
  draftVersion: number;
  publishedVersion: number;
  tenantId: string;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

interface ModelDefinition extends ModelHeader {
  fields: FieldDefinition[];
  relations: RelationDefinition[];
}
```

### 4.3 フィールド定義

```typescript
interface FieldDefinition {
  fieldId: string;
  fieldName: string;
  description?: string;
  dataType: DataType;
  sort: number;

  // 階層
  parentFieldId?: string;
  depth: number;
  children?: FieldDefinition[];

  // 制約
  isKey: boolean;
  isRequired: boolean;
  isArray: boolean;

  // 表示
  widgetType: WidgetType;
  displayWidth?: number;
  isHeader: boolean;
  format?: string;
  placeholder?: string;
  helpText?: string;

  // バリデーション
  validation?: ValidationRule;

  // 関連
  relationCodeGroup?: string;
  relationEntityId?: string;
  relationFieldId?: string;
  relationType?: RelationType;

  // デフォルト
  defaultValue?: any;
  function?: string;

  // 状態
  status: "draft" | "published" | "deleted";
  draftVersion?: number;
}

interface ValidationRule {
  minLength?: number;
  maxLength?: number;
  regexPattern?: string;
  minValue?: number;
  maxValue?: number;
  decimalPlaces?: number;
  customValidator?: string;
}
```

---

## 5. ツリー構造の変換

### 5.1 フラット → ツリー変換

```typescript
function buildFieldTree(fields: FieldDefinition[]): FieldDefinition[] {
  const fieldMap = new Map<string, FieldDefinition>();
  const roots: FieldDefinition[] = [];

  fields.forEach((field) => {
    fieldMap.set(field.fieldId, { ...field, children: [] });
  });

  fields.forEach((field) => {
    const node = fieldMap.get(field.fieldId)!;
    if (field.parentFieldId) {
      const parent = fieldMap.get(field.parentFieldId);
      if (parent) {
        parent.children = parent.children || [];
        parent.children.push(node);
      }
    } else {
      roots.push(node);
    }
  });

  return roots;
}
```

---

## 6. ウィジェットタイプマッピング

| DataType | 推奨 WidgetType | 代替 WidgetType         |
| -------- | --------------- | ----------------------- |
| string   | text            | textarea, select, radio |
| number   | number          | text                    |
| date     | datepicker      | text                    |
| boolean  | checkbox        | radio, select           |
| domain   | -               | （子フィールドで決定）  |

---

## 関連ドキュメント

- [Modeler 概要](./overview.md)
- [API 仕様](./api-spec.md)
- [アーキテクチャ データモデル](../02_architecture/data-model.md)

---

_Powered by Copilot 🤖_
