# mirel Studio 用語集（Glossary）

> mirel Studio で使用される用語の定義と説明

---

## A

| 用語 | 英語 | 説明 |
|------|------|------|
| アーティファクト | Artifact | Builder が生成・管理する成果物（JSON等） |

---

## B

| 用語 | 英語 | 説明 |
|------|------|------|
| ビルダー | Builder | mirel Studio 全体の設計環境。Modeler/Form/Flow/Release を含む |

---

## C

| 用語 | 英語 | 説明 |
|------|------|------|
| コードグループ | Code Group | 選択肢・分類項目を管理する列挙値の集合 |
| コード体系 | Code System | CodeGroup の集合と管理機能 |

---

## D

| 用語 | 英語 | 説明 |
|------|------|------|
| ドメイン | Domain | 業務領域。または複合フィールド（Embedded）の別名 |
| ドメインツリー | Domain Tree | モデルの階層構造を表現するツリー |
| ドラフト | Draft | 編集中の未公開バージョン |
| デプロイ | Deploy | 環境（Dev/Stg/Prod）への適用 |

---

## E

| 用語 | 英語 | 説明 |
|------|------|------|
| エンティティ | Entity | 実データを持つモデルの単位（テーブル相当） |
| 埋め込みモデル | Embedded Model | 他モデル内にネストされた複合構造 |

---

## F

| 用語 | 英語 | 説明 |
|------|------|------|
| フィールド | Field | エンティティを構成する属性の最小単位 |
| フロー | Flow | 業務ロジック・ワークフローの定義 |
| フォーム | Form | UIレイアウトの定義 |
| フォームデザイナー | Form Designer | フォームを視覚的に設計するツール。正式名: mirel Studio Form Designer |
| フローデザイナー | Flow Designer | フローを視覚的に設計するツール。正式名: mirel Studio Flow Designer |

---

## M

| 用語 | 英語 | 説明 |
|------|------|------|
| モデル | Model | Entity / View / Code の総称。業務構造の定義 |
| モデラー | Modeler | 業務モデルを定義する中核コンポーネント。旧称「Schema」。正式名: mirel Studio Modeler |
| MDE | Model-Driven Everything | モデルを単一情報源とする設計原則 |

---

## R

| 用語 | 英語 | 説明 |
|------|------|------|
| リリース | Release | 公開されたバージョン。Draft の確定版 |
| リリースセンター | Release Center | Draft からリリースを管理するツール。正式名: mirel Studio Release Center |
| ランタイム | Runtime | 設計されたアプリの実行環境 |
| RBAC | Role-Based Access Control | ロールに基づくアクセス制御 |

---

## S

| 用語 | 英語 | 説明 |
|------|------|------|
| SSOT | Single Source of Truth | 単一の情報源。mirel では Model が該当 |

---

## V

| 用語 | 英語 | 説明 |
|------|------|------|
| ビュー | View | 複数 Entity の結合・投影を定義したモデル |

---

## W

| 用語 | 英語 | 説明 |
|------|------|------|
| ワークスペース | Workspace | 最上位コンテナ。モデル・フォーム・フロー等を統合管理 |
| ウィジェット | Widget | フォーム上の入力コンポーネント |

---

## 命名規則

### コンポーネント正式名称

| 正式名称 | 短縮形（コード内） | 説明 |
|---------|--------|------|
| mirel Studio Modeler | Modeler | モデル定義ツール |
| mirel Studio Form Designer | FormDesigner | フォーム定義ツール |
| mirel Studio Flow Designer | FlowDesigner | フロー定義ツール |
| mirel Studio Data Browser | DataBrowser | データ閲覧ツール |
| mirel Studio Release Center | ReleaseCenter | リリース管理ツール |

### 技術的命名規則

| 対象 | 規則 | 例 |
|------|------|------|
| DBテーブル | `stu_` 接頭辞 | `stu_model_header`, `stu_field` |
| Javaパッケージ | `jp.vemi.mirel.apps.studio` | - |
| APIエンドポイント | `/mapi/studio/*` | `/mapi/studio/models` |

---

*Powered by Copilot 🤖*
