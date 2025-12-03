# mirel Studio 実装計画書

> mirel Studio の段階的実装ロードマップとマイルストーン (refs #43)

---

## 1. 実装フェーズ概要

mirel Studio は巨大なシステムであるため、以下の 4 フェーズに分割して開発を進める。
**「Model-Driven (モデル駆動)」** の原則に従い、まずはデータ構造の定義と Runtime への反映を最優先とする。

| フェーズ    | 名称                   | ゴール                                                     | 期間（目安） |
| ----------- | ---------------------- | ---------------------------------------------------------- | ------------ |
| **Phase 1** | **Core Foundation**    | モデル定義ができ、CRUD API が自動生成されて動く            | 2ヶ月        |
| **Phase 2** | **UI Construction**    | 定義したモデルに基づいて画面（フォーム）が生成・操作できる | 1.5ヶ月      |
| **Phase 3** | **Logic & Automation** | 業務ロジック（フロー）を定義し、イベント駆動で実行できる   | 2ヶ月        |
| **Phase 4** | **Governance & Ops**   | 権限管理、リリース管理、本番運用機能の完備                 | 1.5ヶ月      |

---

## 2. Phase 1: Core Foundation (Modeler & Runtime Basic)

**目的**: 業務データの構造化と、それを扱うための API 基盤の確立。

### 2.1 Backend (Spring Boot)

- [x] **Metadata Entities**: `StuModelHeader`, `StuField`, `StuRelation` 等のメタデータ管理テーブル実装
- [x] **Dynamic Schema Engine**: メタデータから物理 DB (PostgreSQL) へのマッピング・DDL 発行機能
- [x] **Runtime Data Access**: `DynamicEntityService` (JSON ベースでの動的 CRUD 処理)
- [x] **Draft/Release Management**: Draft 保存と Release スナップショット化の基本ロジック

### 2.2 Frontend (React/Vite)

- [x] **Studio Shell**: Studio 全体のレイアウト、サイドバー、プロジェクト切り替え
- [x] **Modeler UI**:
  - Entity 一覧・作成
  - Field 定義エディタ（型、バリデーション設定）
  - Relation 定義
- [x] **API Preview**: 定義したモデルに対する API テストコンソール

### 2.3 成果物

- Modeler で Entity を定義し、Publish すると `/mapi/studio/{entity}/list` 等が叩ける状態。

---

## 3. Phase 2: UI Construction (Form Designer)

**目的**: ノンコーディングでの UI 生成と、エンドユーザーが利用可能な画面の提供。

### 3.1 Frontend (Builder)

- [x] **Form Designer**:
  - Grid Layout Editor (Drag & Drop) <!-- Implemented -->
  - Widget Palette (Text, Number, Date, Select...) <!-- Partially Implemented -->
  - Property Editor (ラベル、必須、表示条件) <!-- Implemented -->
- [ ] **Preview Mode**: 編集中のフォームを即時プレビュー <!-- Implemented -->

### 3.2 Frontend (Runtime)

- [ ] **Dynamic Form Renderer**: Form JSON 定義を解釈して React コンポーネントを展開するエンジン <!-- Implemented -->
- [ ] **Data Binding**: Runtime API とフォーム入力値の双方向バインディング <!-- Implemented -->
- [ ] **Validation**: モデル定義およびフォーム定義に基づくクライアントサイドバリデーション <!-- Partially Implemented -->

### 3.3 成果物

- 定義したモデルに対して、自動生成＋カスタマイズされた入力画面でデータ登録ができる状態。

---

## 4. Phase 3: Logic & Automation (Flow Designer)

**目的**: 複雑な業務ロジックの実装と、イベント駆動アーキテクチャの実現。

### 4.1 Frontend (Builder)

- [ ] **Flow Designer**:
  - Node-based Editor (React Flow 採用想定)
  - Node Palette (Start, End, If, Update, Email, API Call)
  - Condition Editor (条件式ビルダー)

### 4.2 Backend (Runtime)

- [ ] **Flow Execution Engine**:
  - フロー定義 JSON の解析と実行
  - トランザクション管理機構
  - 変数スコープ管理
- [ ] **Event Bus Integration**: `onCreate`, `onUpdate` 等のイベントフック実装

### 4.3 成果物

- 「データ登録時に条件によって値を書き換える」「メールを送る」等のロジックが動く状態。

---

## 5. Phase 4: Governance & Operations

**目的**: 組織利用に耐えうる統制機能と、ライフサイクル管理の完成。

### 5.1 Release Center

- [ ] **Diff Engine**: Draft と Release、Release 間の差分表示
- [ ] **Deploy Pipeline**: Dev → Stg → Prod への環境適用フロー
- [ ] **Hotfix Flow**: 緊急修正用パッチフローの実装

### 5.2 Security & RBAC

- [ ] **Permission Enforcement**: Runtime API / Form Renderer への権限チェック組み込み
- [ ] **Field Level Security**: フィールド単位の読み書き制御実装

### 5.3 Data Browser

- [ ] **Data Grid**: 管理者向けの高機能データ閲覧・編集グリッド
- [ ] **Import/Export**: CSV/Excel によるデータ入出力

---

## 6. 技術スタック選定

### Frontend

- **Framework**: React 19, Vite
- **State Management**: Zustand (Builder/Runtime 共通)
- **UI Components**: @mirel/ui (Radix UI + shadcn/ui)
- **Diagramming**: React Flow (Flow Designer 用)
- **Form**: React Hook Form + Zod
- **Drag & Drop**: dnd-kit (Form Designer 用)

### Backend

- **Framework**: Spring Boot 3.3
- **Language**: Java 21
- **DB**: PostgreSQL (JSONB を活用して動的スキーマに対応)
- **Template Engine**: FreeMarker (コード生成が必要な場合)
- **Validation**: Hibernate Validator

---

## 7. リスクと対策

| リスク                           | 対策                                                                                         |
| -------------------------------- | -------------------------------------------------------------------------------------------- |
| **動的スキーマのパフォーマンス** | JSONB インデックスの活用と、View テーブル（Materialized View）の併用を検討                   |
| **Flow 実行のデバッグ難易度**    | 実行ログ（Execution Trace）を詳細に記録し、Designer 上で再生できる「リプレイ機能」を将来実装 |
| **フロントエンドのビルド肥大化** | Builder と Runtime を別バンドルに分割し、Lazy Loading を徹底する                             |
| **複雑なバリデーション**         | Zod スキーマを動的生成するロジックを共通化し、前後で二重管理を防ぐ                           |

---

_Powered by Copilot 🤖_
