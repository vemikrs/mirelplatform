# mirel Studio 画面遷移図

> Workspace から各機能への導線設計

---

## 1. Top-Level Navigation（上位階層）

```
[Workspace一覧]
      │
      ▼
[Workspaceダッシュボード]
      │
      ├─ Modeler
      ├─ Form Designer
      ├─ Flow Designer
      ├─ Data Browser
      └─ Release Center
```

---

## 2. 各機能の遷移パターン

### 2.1 Modeler（モデル編集）

```
Modeler
├─ エンティティ一覧
│      └─ エンティティ編集（フィールド／属性）
│             └─ 関連モデル表示
├─ 関係ビュー（リレーション図）
└─ コード体系（値ラベル管理）
```

### 2.2 Form Designer（フォーム編集）

```
Form Designer
├─ 一覧ビュー一覧
│      └─ 一覧ビュー編集
│            └─ プレビュー
├─ 詳細フォーム一覧
│      └─ 詳細フォーム編集
│            └─ プレビュー
└─ レイアウト設定（共通コンポーネント）
```

### 2.3 Flow Designer（フロー編集）

```
Flow Designer
├─ イベント一覧（onCreate/onUpdate/onLoad/cron）
│      └─ イベント編集
│             └─ フロー定義へ遷移
└─ フローデザイナ
      ├─ ノード追加
      ├─ 条件分岐
      ├─ 外部連携
      └─ 実行プレビュー（将来）
```

### 2.4 Data Browser（データ）

```
Data Browser
├─ エンティティ選択
│      ├─ テーブル表示
│      ├─ 詳細表示（詳細フォーム表示）
│      └─ Saved View 保存
└─ Saved View 一覧
```

### 2.5 Release Center（公開管理）

```
Release Center
├─ Draft 一覧
│      └─ Draft詳細（差分確認）
├─ Release 一覧
│      └─ Release詳細（変更内容可視化）
└─ Deploy 管理
      ├─ Dev環境
      ├─ Stg環境
      └─ Prod環境
```

---

## 3. 全体遷移図（統合）

```
[Workspace一覧]
      ▼
[Workspaceダッシュボード]
├─ Modeler ── エンティティ一覧 ── エンティティ編集
│                         └─ 関係ビュー
│                         └─ コード体系
│
├─ Form Designer ── フォーム一覧 ── フォーム編集 ── プレビュー
│
├─ Flow Designer ── イベント一覧 ── イベント編集 ── フローデザイナ
│
├─ Data Browser ── エンティティ選択 ── データ閲覧
│
└─ Release Center ─ Draft / Release / Deploy
```

---

## 4. 設計の特徴

| 特徴 | 説明 |
|------|------|
| 一貫性 | 「業務の構造 → 表現 → ロジック → データ → 公開」の順に並ぶ |
| シンプルな階層 | 各機能に入った後の階層が 1〜2 段階で収まる |
| 分離された構造 | Builder と Runtime が混ざらない |
| 大規模対応 | 大規模な業務アプリでも迷わない導線 |

---

## 関連ドキュメント

- [UI 情報アーキテクチャ](./ui-information-architecture.md)
- [UX 原則](../01_concept/ux-principles.md)
- [Workspace/環境管理](../09_operations/workspace-env.md)

---

*Powered by Copilot 🤖*
