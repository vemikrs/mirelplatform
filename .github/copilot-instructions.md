# mirelplatform - GitHub Copilot Instructions

> **基本ルール**: すべての応答は日本語。指示ファイルの内容は常に最新のベストプラクティス（2025/11時点）に沿ってメンテすること。[GitHub公式ブログの指針](https://github.blog/ai-and-ml/github-copilot/5-tips-for-writing-better-custom-instructions-for-copilot/)や[Monorepo向けCopilot運用事例](https://github.com/orgs/community/discussions/179916)を参考に、各ドメインの文脈を明示する。

## 1. プロジェクト全体像

このリポジトリの主役は、汎用性の高い業務アプリケーション基盤である **mirelplatform** そのもの。ProMarker は mirelplatform にバンドルされているアプリケーション群の一つであり、その代表的なフロントエンド実装として本モノレポ（pnpm workspace）内で管理されている。

以下の4ドメインは連携はしているが疎結合であるため、作業前に対象範囲（mirelplatform コアか、その上で動作する ProMarker などのアプリか）を必ず明示する。

| ドメイン | 役割 | 主な技術 | 起動/ビルド | 備考 |
|---|---|---|---|---|
| `apps/frontend-v3` | React 19 + Vite アプリ | TypeScript, Tailwind 4, Radix UI, Zustand, TanStack Query | `pnpm --filter frontend-v3 dev` (VS Code Task経由) | `/promarker` からアクセス。APIは `/mapi/*` を必ず経由。|
| `packages/ui` | @mirel/ui デザインシステム | Radix + shadcn, Storybook相当のユーティリティ | 直接ビルド不要。`tsc --noEmit` に通ること | ここで Dialog 等の共通実装を定義。|
| `packages/e2e` | Playwright E2E | Playwright 1.57+, Vite test server bootstrapping | `pnpm test:e2e` | 自動で backend/frontend を立ち上げる。|
| `backend` | Spring Boot 3.3 API | Java 21, Gradle 8, FreeMarker | `./gradlew :backend:bootRun --args='--spring.profiles.active=dev'` | `mipla2` コンテキスト配下で API 提供。|

## 2. 回答/コミュニケーション指針

- 日本語で簡潔に回答。成果物は Markdown で整理し、コード変更がある場合はファイル名と意図を先に提示。
- ユーザー要求が複数ある場合は TODO リストを活用し、段階ごとに更新（Copilot Spaces運用の推奨に従いコンテキストを明確化）。
- **PR・GitHub Actions はすべて GitHub** で管理する。ADOのリポジトリ・パイプライン機能は使用しない。
- **Issue はハイブリッド運用中**。GitHub Issue（現行）と Azure Boards Work Item（今後統合予定）の両者を使用する。作業参照は GitHub Issue または ADO WI のどちらかを文脈に応じて使い分ける。
- Issue/PRへ投稿する際は開始/完了それぞれでコメントし、末尾に **"Powered by Copilot 🤖"** を付記。
- `docs/issue/#<Issue>/*.md` を作業ログとして更新。アーキテクチャ判断・障害の暫定措置も必ず追記。

## 3. コーディング規約とコミット

### 3.1 コミットメッセージ形式

```
<type>(<scope>): <subject> (refs #<issue>)
```

Type: `feat|fix|docs|style|refactor|perf|test|chore|ci|build|revert`

Scope例: `frontend`, `backend`, `ui`, `e2e`, `infra`, `modal`, `deps`

Subjectは50文字以内、日本語可。Issue連携必須。

### 3.2 コミット前チェック

- `git status` / `git diff` で意図しない差分がないか確認。
- 生成物 (`dist/`, `.next/`, `pnpm-lock.yaml` 手動編集 等) をコミットしない。pnpm v9 の `workspace-lock.yaml` は **変更禁止**。
- Java系は `./gradlew :backend:check`, フロントは `pnpm --filter frontend-v3 lint` を可能な限り実行。

## 4. モノレポのフォルダガイド

```
apps/
  frontend-v3/         # Reactアプリ (Vite)
packages/
  ui/                  # デザインシステム (@mirel/ui)
  e2e/                 # Playwright テスト
backend/               # Spring Boot サービス
docs/                  # 仕様・検証ログ・Issue別記録
scripts/               # サービス起動/停止/ビルドスクリプト (直接実行は避ける)
```

### ドメイン別ベストプラクティス

- **Frontend**: React19, `src/app`(ルーティング), `src/features/promarker`(機能単位)。API呼び出しは `src/lib/api` 経由で `/mapi/*` のみ使用。UIコンポーネントは可能な限り `@mirel/ui` を利用し、Tailwindは `@apply` 禁止・`class-variance-authority` でバリアント管理。
- **Packages/ui**: Radixコンポーネントを拡張。DOM直書き禁止、アクセシビリティ属性を維持。`Dialog` 等のスタイル調整はここで集約し、アプリ側では props で調整。
- **Backend**: `jp.vemi.mirel.apps.mste` 配下に API/Service/Domain を分離。リクエストDTOは `domain/dto`, Service戻り値は `ApiResponse` で統一。`/mapi` から流入したパラメータは `ApiRequest<*>.model` で受ける。
- **E2E**: Playwright は `packages/e2e/tests/specs/promarker-v3/` に集約。テストは `apps/frontend-v3` を起動せず、Playwright config が `frontend-v3 dev` タスクを自動起動する。新規テストは `page-objects` を必ず経由。
- **Mira AI**: `jp.vemi.mirel.apps.mira` 配下で AI アシスタント機能を提供。プロバイダは `AiProviderClient` インタフェースで抽象化し、`AiProviderFactory` でプロバイダ選択。設定は `mira.ai.*` プレフィックスで管理。

## 5. 開発環境とタスク実行

### 5.1 VS Code Tasks (必須)

- 長時間動くサーバー・ウォッチャーは **必ず** VS Code Tasks (`create_and_run_task`) で起動。`run_in_terminal` の `isBackground=true` は使用禁止。
- 代表タスク:
  - `shell: Backend: Start Spring Boot`
  - `shell: Frontend-v3: Start Vite`
  - `shell: Start All Services` (両方必要な場合のみ)
  - `shell: Watch Logs` (監視向け)

### 5.2 プロセス終了

- Port→PID→Kill を徹底 (`fuser -k 5173/tcp` 等)。`pkill -f node` や `killall` 系コマンドは禁止。

### 5.3 環境別メモ

- **DevContainer/Codespaces**: `pnpm install` 後、`pnpm dlx husky` 等のセットアップは不要（すでに済み）。VS Code の `Java: Clean Workspace` を使う際は `backend/.gradle` を削除しない。
- **ローカル**: Node v22.20.0 (nvm利用), Java 21 (SDKMAN!) を想定。`pnpm env use --global 22.20.0` 済みか確認。

## 6. ビルド & テスト マトリクス

| 種別 | コマンド (タスク経由) | 目的 |
|---|---|---|
| Frontend Dev | `pnpm --filter frontend-v3 dev` | Vite開発サーバー |
| Frontend Test | `pnpm --filter frontend-v3 test` / `pnpm --filter frontend-v3 lint` | Vitest + ESLint |
| UI Package | `pnpm --filter @mirel/ui typecheck` | `tsc --noEmit`。Storybookは未導入 |
| Backend Dev | `./gradlew :backend:bootRun --args='--spring.profiles.active=dev'` | Spring Boot |
| Backend Test | `./gradlew :backend:test` / `:backend:check` | JUnit |
| E2E | `pnpm test:e2e` | Playwright、必要サービス自動起動 |

成果確認後は `pnpm prune` や `./gradlew --stop` でプロセスを解放する。

## 7. API/フロント間プロキシ規約

- Vite 側では `/mapi` → `http://localhost:3000/mipla2` へ rewrite。React から直接 `http://localhost:3000` を叩かない。
- 例: `POST /mapi/apps/mste/api/generate` → Backend: `/mipla2/apps/mste/api/generate`
- Spring の `server.servlet.context-path=/mipla2` を変更しないこと。

## 8. セキュリティ & データ管理

- JWT/セッション情報を含むファイルは Git 追跡禁止。`.env` ではなく `config/application.yml` の `spring.config.import=optional:file:.env` を利用。
- 一時ファイルは `data/storage/` 配下に生成され、72時間でクリーンアップ。テストでは `FileManagementService` をモックする。
- Playwright でアップロードする秘密情報は `packages/e2e/tests/fixtures` に保存しない。

## 9. 作業手順テンプレート

1. GitHub Issue または ADO Work Item から TODO を切り出し、`manage_todo_list` で追跡。
2. 関連ファイルを読み込み (`read_file`, `grep_search`) → 必要なら `file_search`。
3. 変更は `apply_patch` or `edit_notebook_file`。複数ファイル修正時は差分を小さく保つ。
4. `run_task` 経由でビルド/テスト。失敗時はログ抜粋を共有し、再現手順と暫定策を記録。
5. 変更後は `get_errors` or ツール出力でエラー確認。必要に応じ `pnpm lint` / `gradlew check`。
6. PR（GitHub）へのコメントと、Issue（GitHub Issue または ADO WI）への結果要約を行い、`docs/issue/#<id>/` に詳細を追記。コメント末尾は **"Powered by Copilot 🤖"**。

## 10. 参考リンク

- GitHub Copilot カスタムインストラクションのベストプラクティス（プロジェクト概要・フォルダ構成を明示することが推奨）[^1]
- Monorepo での Copilot コンテキスト分割とナレッジ共有の重要性[^2]

[^1]: GitHub Blog “5 tips for writing better custom instructions for Copilot” (2025). https://github.blog/ai-and-ml/github-copilot/5-tips-for-writing-better-custom-instructions-for-copilot/
[^2]: GitHub Community Discussion “Taming Your Monorepo with GitHub Copilot” (2025). https://github.com/orgs/community/discussions/179916

## 11. スコープ別インストラクション

Copilot Chat / Coding Agent に対象ドメインを明確に伝えるため、以下の補助ファイルを参照すること。

| ファイル | 役割 |
| --- | --- |
| `.github/copilot/frontend.md` | `apps/frontend-v3` 向け。Viteの起動手順や build/lint/test コマンドを記載。 |
| `.github/copilot/backend.md` | `backend/` 向け。Gradle タスクや API 構成ポリシー、プロファイル設定を記載。 |
| `.github/copilot/ui-package.md` | `packages/ui` の Radix/shadcn ラッパーに関するガイド。型チェック・テスト必須。 |
| `.github/copilot/e2e.md` | `packages/e2e` Playwright テスト実装用。自動起動やレポート確認手順を記載。 |

パッケージやモジュールの変更時は、まず該当ファイルを読み込み、要求されたビルド・テストコマンドを必ず実行すること。

## 12. Azure DevOps 連携

> **用途の明確化**: ADO は Work Item（Boards）とWikiの管理にのみ使用する。PR・Actions・リポジトリ管理はすべて **GitHub** を使用すること。ADO MCP ツールは WI/トラッキング/Wiki に関するリクエストにのみ使用し、PR操作やコード参照に対してはGitHub MCPツールを使用する。

- MCP サーバー: `ado`（`.vscode/mcp.json` で定義済み）
- 組織: `vemijp` / デフォルトプロジェクト: `mirelplatform`
- **使用対象**: Work Item の参照・作成・更新、Wiki（Boards/トラッキング目的のみ）
- **使用しない対象**: リポジトリ閲覧、パイプライン操作（これらはGitHubで行う）
- 認証: `az login` 済みの Azure CLI 資格情報を利用（テナント: `vemi`）
- 特定テナントにアクセスする場合は `az login --tenant <TENANT_ID>` で切り替え
- 機密性の高い別プロジェクトへのアクセスは VS Code ユーザー設定の `ado-confidential` サーバーを使用すること（本ファイルには記載しない）