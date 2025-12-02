# mirel Studio UI 情報アーキテクチャ（IA）

> 業務構造に沿った直感的な UI 設計

---

## 1. 概要

mirel Studio の IA は、**「業務構造 → 画面 → ロジック → データ → 公開」** の順に理解・操作できるよう設計されている。

情報量が多い LCAP/NCAP で迷わないため、明確な階層と一貫した操作導線を採用している。

---

## 2. グローバル UI 構造

### 左ナビゲーション（主要機能）

```
Workspace
├─ Dashboard
├─ Modeler
├─ Form Designer
├─ Flow Designer
├─ Data Browser
└─ Release Center
```

役割が明確で、上から下へ向かって **モデル → 表現 → 振る舞い → データ → 公開** の順に学習できる構造。

### トップバー

| 項目 | 説明 |
|------|------|
| 現在の環境 | Dev / Stg / Prod |
| Workspace 名 | 作業対象 |
| グローバル検索 | 横断検索 |
| ユーザーメニュー | プロファイル・設定 |

### メインキャンバス

コンテキストに応じて表示が変わる主要作業領域。

| モジュール | 表示内容 |
|-----------|---------|
| Modeler | エンティティ構造ツリー / フィールド編集 |
| Form Designer | レイアウトキャンバス |
| Flow Designer | ノード接続グラフ |
| Data Browser | テーブルビュー |
| Release Center | 差分とリリース履歴 |

### 右ペイン（プロパティ）

選択した要素に応じて動的に切り替わる。

- フィールド属性
- コンポーネント設定
- フローノードのパラメータ
- 依存関係の警告
- バリデーション情報

---

## 3. 詳細ナビゲーション構造

```
Workspace
├─ Dashboard
├─ Modeler
│    ├─ エンティティ一覧
│    ├─ 関係ビュー
│    └─ コード体系
├─ Form Designer
│    ├─ 一覧ビュー
│    ├─ 詳細フォーム
│    └─ レイアウト設定
├─ Flow Designer
│    ├─ イベント一覧
│    └─ フロー定義
├─ Data Browser
│    ├─ エンティティデータ
│    └─ Saved Views
└─ Release Center
     ├─ Draft
     ├─ Release
     └─ Deployments
```

---

## 4. IA 設計の指針

| 指針 | 説明 |
|------|------|
| 明確な位置 | 「どこに何があるか」が一目で分かる |
| 自然な順序性 | Model → Form → Flow |
| 集中できる環境 | Builder ユーザーが作業に集中 |
| 分離された構造 | Runtime と混ざらない |
| バランスのとれた情報量 | 大規模業務でも迷わない |

---

## 5. 設計意図

mirel Studio の IA は **業務アプリ構築の思考順序そのものを UI に反映した構造** である。

- 複雑な LCAP を扱う上での迷いを排除
- 各機能が一貫した情報配置で連動
- モデル中心の設計思想をそのまま UI に昇華

---

## 関連ドキュメント

- [画面遷移図](./screen-transition.md)
- [UX 原則](../01_concept/ux-principles.md)
- [Modeler UI コンポーネント](../03_modeler/ui-components.md)

---

*Powered by Copilot 🤖*
