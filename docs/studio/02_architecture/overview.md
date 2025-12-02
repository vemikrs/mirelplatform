# mirel Studio 技術アーキテクチャ概要

> mirel Studio の構造原理と技術的中核

---

## 1. Architectural Principles（アーキテクチャ原則）

mirel Studio は以下の原則を中心に設計される。

| 原則 | 説明 |
|------|------|
| **Model-Driven Everything** | モデル（業務構造）が UI / Flow / API / Data の源泉となる |
| **Separation of Builder & Runtime** | 設計フェーズと運用フェーズを物理的・論理的に分離する |
| **Consistency & Type Safety** | モデルの変更が UI・フロー・API へ自動的に整合される |
| **Layered Responsibility** | Model → Presentation → Automation → Persistence の責務境界を曖昧にしない |
| **Versioned Change Management** | Release Center によるバージョン単位の差分管理・反映・ロールバック |

---

## 2. Component Architecture（コンポーネント構成）

```
mirel Studio
├── Modeler            … Domain Model Definition
├── Form Designer      … UI Layout & Data Binding
├── Flow Designer      … Event/Logic/Integration Flows
├── Data Browser       … Data Observation & Inline Ops
└── Release Center     … Version & Deployment Control

Runtime
├── Generated UI       … Final Application Screens
├── Generated API      … Application API (REST/GraphQL)
├── Execution Engine   … Data Ops / Rules / Events
└── Data Store         … Managed Persistent Storage
```

Builder（Studio）と Runtime は明確に分離し、反映は Release Center を経由する。

---

## 3. Model Architecture（モデル構造）

モデルは以下の 3 要素で構成される。

### 3.1 Entity（実体）

- キー
- 属性（プリミティブ・複合）
- 制約
- 関係（参照/集約）

### 3.2 View（投影／派生構造）

- 特定用途向けのデータ射影
- 表示必要なフィールドの部分選択
- 条件付きフィールド

### 3.3 Code（コード体系）

- 値／ラベル
- 並び順
- 関連付け
- 国際化（将来）

**階層構造 + 再帰的定義** を前提としており、モデルは木構造またはグラフ構造として解釈される。

---

## 4. Presentation Architecture（UI 生成）

Form Designer の構造は以下のとおり。

```
Form Definition (JSON)
├── Layout (Grid)
├── Components
│    ├── Bound Components (Field Binding)
│    └── Non-Bound Components (Label, Section, Button)
└── Actions (Save / Load / API / Flow Trigger)
```

UI はモデルと同期されるため：

- 型変更 → コンポーネントの自動再構成
- フィールド削除 → UI の自動検知
- 依存項目の表示条件更新 → UI へ反映

**Presentation と Model の乖離を防ぐ構造になっている。**

---

## 5. Automation Architecture（Flow Designer）

Flow はイベント駆動の抽象ワークフローとして設計される。

### 5.1 イベントタイプ

- onCreate / onUpdate / onDelete
- onLoad / onButtonClick
- scheduled(cron)

### 5.2 ノード

- 条件分岐
- データ操作
- 外部 API 呼び出し
- 変数操作
- 通知
- エラー処理パス

### 5.3 実行環境

Flow は Runtime の「Execution Engine」で処理される。

- トランザクション管理
- データ整合性チェック
- リトライ／補償処理（将来）

---

## 6. Data Architecture

推奨バックエンド構成は以下の通り。

| 項目 | 技術 |
|------|------|
| DB | PostgreSQL |
| モデル保存 | JSON 構造（Model/Forms/Flows/Versions） |
| 実データ | JSONB or normalized table（選択式） |

データ操作は以下のレイヤに分離。

```
Data Access Layer
├── Entity Store
├── View Projection Engine
└── Code Provider
```

---

## 7. Release & Deployment Architecture

Release Center は業務アプリ改変の "統制点" として機能する。

### 7.1 変更検出

- モデル差分
- UI 差分
- Flow 差分

### 7.2 バージョン生成

- Release ID
- 差分のスナップショット
- メタ情報（作成者・日時・環境互換）

### 7.3 デプロイ

Dev → Stg → Prod のマルチ環境へ適用。

### 7.4 ロールバック

影響範囲を限定した巻き戻しが可能。

---

## 8. Security & Governance

- RBAC（Role-Based Access Control）
- Workspace 単位での権限付与
- Builder 操作ログ
- Runtime 監査ログ
- バージョンの改ざん防止

エンタープライズ利用に必要な統制基盤を備える。

---

## 9. Integration Architecture

外部サービス連携は Flow Designer から扱える。

- REST API
- Webhook
- OAuth 2.0 / API Key
- 外部 DB（将来）
- SaaS コネクタ（将来）

拡張ポイントを明確にすることで、ローコード環境でありながら開発者の介入余地を残す。

---

## 10. Runtime Architecture（実行時）

```
Generated UI
├── Next.js / React ベース
└── Client-Side Validation / Conditional Rendering

Generated API
└── Spring Boot (or Quarkus) REST/GraphQL

Execution Engine
└── Domain Ops / Event Handling / Flow Execution

Data Store
└── PostgreSQL
```

Builder の成果物は Runtime に変換され、高性能な Web アプリケーションとして提供される。

---

## 11. Summary

mirel Studio のアーキテクチャは次を実現するために設計されている：

- **モデル駆動の一貫性**
- **複雑業務に耐える構造的表現**
- **安全な変更管理**
- **現場とエンジニアの協働**
- **永続的な業務改善サイクル**

mirel Studio は、単なるローコード/ノーコードではなく、**業務構造を扱うための "設計基盤"** として位置付けられる。

---

## 関連ドキュメント

- [七層アーキテクチャ](./seven-layer.md)
- [Builder/Runtime 連携](./builder-runtime.md)
- [データモデル全体像](./data-model.md)

---

*Powered by Copilot 🤖*
