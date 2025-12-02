# mirel Studio Modeler 概要

> mirel Studio の中核コンポーネント：業務モデル定義ツール

---

## 1. mirel Studio における位置付け

mirel Studio Modeler は **mirel Studio** の中核コンポーネントであり、業務構造をモデルとして記述し、システム全体の **Single Source of Truth（SSOT）** を担う。

```
┌────────────────────────────────────────────────────────────────────┐
│                         mirel Studio (Builder)                      │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                      Modeler                                  │  │
│  │   - Entity（実体）/ View（投影）/ Code（コード体系）           │  │
│  │   - Domain Tree（再帰的ドメイン構造）                         │  │
│  │   - Relation / Validation / Lint                              │  │
│  └───────────────────────────┬──────────────────────────────────┘  │
│                              │                                      │
│  ┌───────────────────────────┼──────────────────────────────────┐  │
│  │  Form Designer │ Flow Designer │ Data Browser │ Release Center│  │
│  │  （モデルに基づき UI・ロジック・データ操作・公開を管理）        │  │
│  └───────────────────────────┴──────────────────────────────────┘  │
│                                                                    │
├────────────────────────────────────────────────────────────────────┤
│                          Runtime (実行環境)                         │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Generated UI │ Generated API │ Execution Engine │ Data Store │  │
│  │  （モデルから自動生成されたフォーム・API・ビジネスロジック実行）  │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

---

## 2. 設計思想とコアコンセプト

### 2.1 Model-Driven Everything（MDE）

Modeler では **モデルが単一の情報源** となる。

- データ構造 → Entity / View / Code として定義
- フォーム → モデルから派生（Form Designer が参照）
- ビジネスロジック → モデルイベントに紐付け（Flow Designer）
- API → モデル定義から自動生成
- バリデーション → モデル内制約として一元管理

**UI からデータが生まれるのではなく、データ構造から UI・ロジック・API が派生する。**

### 2.2 Recursive Domain Structure（再帰的ドメインモデル）

業務を構成する情報は **階層・関係・複合性** を持つ。

- ドメインは階層構造を持つ（ネストドメイン）
- 複合エンティティを直接扱える
- 再帰的な依存を安全に解決できる
- 一貫性を保ったまま UI とデータへ展開できる

### 2.3 Consistency First（一貫性の優先）

モデル → UI → ロジック → データ の各要素間で **整合性の欠落が生じない** ことを最重要とする。

- 型変更 → コンポーネントの自動再構成
- フィールド削除 → UI の自動検知
- API がモデルから外れない

### 2.4 Release as a Unit（リリース単位での変化管理）

変更は **バージョン** としてまとめて取り扱う（Release Center と連携）。

| 状態 | 説明 |
|------|------|
| **Draft** | 作業中（Builder 内で編集中） |
| **Release** | 確定したバージョン（公開準備完了） |
| **Deploy** | 環境への適用（Release Center 経由） |
| **Rollback** | 巻き戻し（影響範囲を限定して復元） |

---

## 3. モデルアーキテクチャ

Modeler が管理するモデルは以下の 3 要素で構成される。

### 3.1 Entity（実体）

実データを持つテーブル相当の構造。

| 要素 | 説明 |
|------|------|
| キー | Primary Key |
| 属性 | プリミティブ・複合 |
| 制約 | 必須・ユニーク・バリデーション |
| 関係 | 参照/集約 |

### 3.2 View（投影／派生構造）

特定用途向けのデータ射影。

- 複数 Entity の結合
- 表示必要なフィールドの部分選択
- 集計・計算フィールド
- 条件付きフィールド

### 3.3 Code（コード体系）

選択肢・マスタデータを管理。

- 値／ラベルのペア
- 並び順
- 関連付け（親子階層など）
- 国際化対応（将来）

**階層構造 + 再帰的定義** を前提としており、モデルは木構造またはグラフ構造として解釈される。

---

## 4. 画面体系

```
mirel Studio Modeler
│
├── Model Explorer（モデル探索）
│     ├── Entity List
│     ├── View List
│     ├── Code List
│     └── Model Overview（Fields / Relations / Usage）
│
└── Model Designer（モデル定義編集）
      ├── Domain Tree（辞書ツリー）
      ├── Field Editor（Primitive）
      ├── Domain Editor（Nested Domain）
      └── Draft / Publish → Release Center 連携
```

- Explorer と Designer は分離しつつも、1 クリックで相互遷移可能
- Entity / View / Code が統一された体験で扱えることが核心

---

## 5. 他コンポーネントとの連携

| コンポーネント | 連携内容 |
|---------------|---------|
| **Form Designer** | モデルのフィールド定義を参照し、UI コンポーネントを自動生成 |
| **Flow Designer** | モデルのイベント（onCreate / onUpdate 等）にフローを紐付け |
| **Data Browser** | モデル定義に基づきデータを観測・軽微な操作 |
| **Release Center** | Draft/Release/Deploy/Rollback を管理。バージョン統制点 |

---

## 6. DB テーブル設計

| テーブル | 説明 |
|---------|------|
| `stu_model_header` | モデルヘッダー（Entity/View 定義） |
| `stu_field` | フィールド定義 |
| `stu_code` | コード定義 |
| `stu_relation` | リレーション定義 |
| `stu_draft` | Draft 管理 |

---

## 関連ドキュメント

- [UI コンポーネント](./ui-components.md)
- [データ型定義](./data-model.md)
- [API 仕様](./api-spec.md)
- [エラー検出](./error-detection.md)
- [コード体系](./code-system.md)

---

*Powered by Copilot 🤖*
