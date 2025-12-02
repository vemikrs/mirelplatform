# mirel Studio 七層アーキテクチャ

> 業務モデルを起点とした一貫性のあるアプリケーション構築のための責務層

---

## 1. 概要

mirel Studio は、**業務モデルを起点とした一貫性のあるアプリケーション構築** を実現するため、七つの責務層で構成される。

この構造により、変更・拡張・運用が明確な境界を保ちながら安全に行える。

---

## 2. 七層モデル

### Layer 1：モデル層（Model Layer）

- 業務構造（エンティティ／ビュー／コード体系）
- 階層構造・複合構造・依存関係
- すべての源泉（SSOT）

### Layer 2：モデルサービス層（Model Service Layer）

- モデルの解釈・整合性チェック
- 依存解決・再帰構造展開
- UI/Flow/API への派生情報を提供

### Layer 3：プレゼンテーション定義層（Presentation Definition Layer）

- フォーム定義
- レイアウト（12 グリッド）
- データバインディング
- 条件表示・バリデーション定義

### Layer 4：オートメーション層（Automation Layer）

- イベント（データ／フォーム／スケジュール）
- 条件分岐・外部連携
- フローの実行計画

### Layer 5：Runtime API 層（Runtime API Layer）

- モデルから生成される REST/GraphQL API
- UI、Flow、外部連携が参照する単一インターフェース

### Layer 6：実行層（Execution Layer）

- データ保存／更新／削除の業務ルール処理
- フロー実行
- トランザクション管理
- 実行時バリデーション

### Layer 7：永続化層（Persistence Layer）

- PostgreSQL を中心としたデータ永続化
- モデル自体の JSON 保存
- 実データの JSONB or 正規化テーブル

---

## 3. 層構造図

```
Layer 1  モデル層
    ↓
Layer 2  モデルサービス層
    ↓
Layer 3  プレゼンテーション定義層
    ↓
Layer 4  オートメーション層
    ↓
Layer 5  Runtime API 層
    ↓
Layer 6  実行層
    ↓
Layer 7  永続化層
```

---

## 4. 各層の責務一覧

| 層 | 責務 | 主な成果物 |
|----|------|-----------|
| L1 | 業務構造の定義 | Entity, View, Code JSON |
| L2 | モデル解釈・整合性 | 派生情報、依存グラフ |
| L3 | UI 定義 | Form JSON |
| L4 | ロジック定義 | Flow JSON |
| L5 | API 提供 | REST/GraphQL エンドポイント |
| L6 | 実行制御 | トランザクション、バリデーション |
| L7 | データ保存 | PostgreSQL テーブル |

---

## 5. 特徴

- 各層は一方向に責務を流し、循環依存を禁止
- モデル（Layer 1）が全層の根源となる構造
- 複雑な業務ロジックでも破綻しにくい拡張性を確保

---

## 関連ドキュメント

- [アーキテクチャ概要](./overview.md)
- [Builder/Runtime 連携](./builder-runtime.md)
- [モジュール構造](./module-structure.md)

---

*Powered by Copilot 🤖*
