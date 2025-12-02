# mirel Studio ドキュメント目次

> mirel Studio は、ノーコード/ローコードでビジネスアプリケーションを構築するための統合開発環境です。

---

## 📚 ドキュメント構成

### [01. コンセプト](./01_concept/)
設計思想、UX原則、製品コンセプトに関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [design-philosophy.md](./01_concept/design-philosophy.md) | mirel Studio の設計哲学 |
| [concept.md](./01_concept/concept.md) | 製品コンセプト |
| [lp-copy.md](./01_concept/lp-copy.md) | ランディングページコピー |
| [ux-principles.md](./01_concept/ux-principles.md) | UX設計原則 |

---

### [02. アーキテクチャ](./02_architecture/)
システム全体の技術アーキテクチャに関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [overview.md](./02_architecture/overview.md) | 技術アーキテクチャ概要 |
| [seven-layer.md](./02_architecture/seven-layer.md) | 七層アーキテクチャ |
| [builder-runtime.md](./02_architecture/builder-runtime.md) | Builder/Runtime 連携 |
| [module-structure.md](./02_architecture/module-structure.md) | モジュール構造 |
| [event-bus.md](./02_architecture/event-bus.md) | イベントバス設計 |
| [data-model.md](./02_architecture/data-model.md) | データモデル全体像 |

---

### [03. Modeler](./03_modeler/)
mirel Studio Modeler（旧 Schema）に関するドキュメント。業務モデルの定義を担う中核コンポーネント。

| ファイル | 概要 |
|---------|------|
| [overview.md](./03_modeler/overview.md) | Modeler 概要 |
| [ui-components.md](./03_modeler/ui-components.md) | UI コンポーネント仕様 |
| [data-model.md](./03_modeler/data-model.md) | データ型定義 |
| [api-spec.md](./03_modeler/api-spec.md) | API 仕様 |
| [error-detection.md](./03_modeler/error-detection.md) | エラー検出機能 |
| [code-system.md](./03_modeler/code-system.md) | コード体系 |
| [usecases/](./03_modeler/usecases/) | ユースケース集 |

---

### [04. Form Designer](./04_form-designer/)
mirel Studio Form Designer に関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [widget-spec.md](./04_form-designer/widget-spec.md) | Widget 仕様 |
| [layout-algorithm.md](./04_form-designer/layout-algorithm.md) | レイアウトアルゴリズム |
| [modeler-sync.md](./04_form-designer/modeler-sync.md) | Modeler 連携 |
| [accessibility.md](./04_form-designer/accessibility.md) | アクセシビリティ対応 |
| [diff-patch.md](./04_form-designer/diff-patch.md) | 差分パッチ処理 |

---

### [05. Flow Designer](./05_flow-designer/)
mirel Studio Flow Designer に関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [node-spec.md](./05_flow-designer/node-spec.md) | ノード仕様 |
| [execution-model.md](./05_flow-designer/execution-model.md) | 実行モデル |
| [condition-editor.md](./05_flow-designer/condition-editor.md) | 条件エディタ |
| [palette-ext.md](./05_flow-designer/palette-ext.md) | パレット拡張 |
| [parallel-execution.md](./05_flow-designer/parallel-execution.md) | 並列実行 |
| [test-mode.md](./05_flow-designer/test-mode.md) | テストモード |
| [error-handling.md](./05_flow-designer/error-handling.md) | エラーハンドリング |

---

### [06. Data Browser](./06_data-browser/)
mirel Studio Data Browser に関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [spec.md](./06_data-browser/spec.md) | Data Browser 仕様 |

---

### [07. Release Center](./07_release-center/)
mirel Studio Release Center に関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [overview.md](./07_release-center/overview.md) | Release Center 概要 |
| [diff-algorithm.md](./07_release-center/diff-algorithm.md) | 差分アルゴリズム |
| [draft-release-json.md](./07_release-center/draft-release-json.md) | Draft/Release JSON 構造 |
| [draft-versioning.md](./07_release-center/draft-versioning.md) | 版管理ポリシー |
| [diff-merge.md](./07_release-center/diff-merge.md) | 差分マージ |
| [deploy-rollback.md](./07_release-center/deploy-rollback.md) | デプロイ/ロールバック |
| [impact-analysis.md](./07_release-center/impact-analysis.md) | 影響分析エンジン |

---

### [08. Runtime](./08_runtime/)
Runtime 環境に関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [autogen-api.md](./08_runtime/autogen-api.md) | API 自動生成 |
| [api-schema.md](./08_runtime/api-schema.md) | API スキーマ |
| [cache-strategy.md](./08_runtime/cache-strategy.md) | キャッシュ戦略 |
| [ui-component-guidelines.md](./08_runtime/ui-component-guidelines.md) | UI コンポーネント指針 |
| [data-persistence.md](./08_runtime/data-persistence.md) | データ永続化 |

---

### [09. 運用・ガバナンス](./09_operations/)
運用管理、セキュリティ、ガバナンスに関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [workspace-env.md](./09_operations/workspace-env.md) | Workspace 環境管理 |
| [workspace-model.md](./09_operations/workspace-model.md) | Workspace モデル |
| [rbac-model.md](./09_operations/rbac-model.md) | RBAC モデル |
| [audit-log.md](./09_operations/audit-log.md) | 監査ログ |
| [backup-strategy.md](./09_operations/backup-strategy.md) | バックアップ戦略 |
| [log-model.md](./09_operations/log-model.md) | ログモデル |

---

### [10. 横断的関心事](./10_cross-cutting/)
複数コンポーネントにまたがる横断的な設計に関するドキュメント。

| ファイル | 概要 |
|---------|------|
| [i18n-strategy.md](./10_cross-cutting/i18n-strategy.md) | 多言語化戦略 |
| [label-i18n.md](./10_cross-cutting/label-i18n.md) | ラベル多言語化 |
| [embedded-depth.md](./10_cross-cutting/embedded-depth.md) | 複合モデル深度ガイドライン |
| [metadata-persistence.md](./10_cross-cutting/metadata-persistence.md) | メタデータ永続化 |
| [project-structure.md](./10_cross-cutting/project-structure.md) | プロジェクト構造標準 |

---

### [99. 参考資料](./99_reference/)
画面遷移図、IA などの参考資料。

| ファイル | 概要 |
|---------|------|
| [screen-transition-map.md](./99_reference/screen-transition-map.md) | 画面遷移図 |
| [ui-info-architecture.md](./99_reference/ui-info-architecture.md) | UI 情報アーキテクチャ |

---

## 📖 関連ドキュメント

- [用語集 (Glossary)](./00_GLOSSARY.md)
- [レビュー＆修正計画](./REVIEW-AND-REFACTORING-PLAN.md)

---

## 🏷️ 命名規則

### コンポーネント名

| 正式名称 | 短縮形 | 説明 |
|---------|--------|------|
| mirel Studio Modeler | Modeler | モデル定義ツール |
| mirel Studio Form Designer | FormDesigner | フォーム定義ツール |
| mirel Studio Flow Designer | FlowDesigner | フロー定義ツール |
| mirel Studio Data Browser | DataBrowser | データ閲覧ツール |
| mirel Studio Release Center | ReleaseCenter | リリース管理ツール |

### DB テーブル接頭辞

DB テーブル名には `stu_` 接頭辞を使用します（例: `stu_model_header`, `stu_field`）。

---

*Powered by Copilot 🤖*
