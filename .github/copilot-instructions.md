# ProMarker Platform - GitHub Copilot Instructions

**Allways answer in Japanese.**

## Project Overview

ProMarker is a comprehensive code generation and template management platform built on the mirelplatform framework. This application provides automated development scaffolding, template processing, and code generation capabilities for rapid application development.

### Architecture
- **Backend**: Spring Boot 3.3 with Java 21, mirelplatform framework
- **Frontend**: React 19+ with Vite, Tailwind CSS 4, Radix UI, shadcn/ui components (@mirel/ui)
- **State Management**: Zustand + TanStack Query (React Query) for server state
- **Database**: H2 (development), MySQL (production)
- **Template Engine**: FreeMarker with custom function resolvers
- **Monorepo**: pnpm workspace with packages (ui, e2e) + apps (frontend-v3)
- **Testing**: Playwright E2E, Vitest for unit tests
- **Container**: DevContainer support for Codespaces and local development

## Copilot Workflow（作業報告ルール）

Copilotが作業を行う際のルール：

### Issue/PRへのコメント
* 各Phaseの作業開始時・完了時に関連Issue/PRへ進捗報告を投稿し、対応する `docs/issue/#<Issue>/*.md` を更新
* コメントの末尾に必ず **"Powered by Copilot 🤖"** を明記
* 作業内容・変更点・次のステップを簡潔に記載

### コミットメッセージ

コミットメッセージは以下のルールに従う：

#### **形式**

```
<type>(<scope>): <subject> (refs #<issue-number>)
```

#### **Type（必須）**

- **feat**: 新機能追加
- **fix**: バグ修正
- **docs**: ドキュメント変更のみ
- **style**: コードの意味に影響しない変更（空白、フォーマット、セミコロン等）
- **refactor**: リファクタリング（バグ修正も機能追加もしない）
- **perf**: パフォーマンス改善
- **test**: テスト追加・修正
- **chore**: ビルドプロセスや補助ツールの変更
- **ci**: CI/CD設定の変更
- **build**: ビルドシステムや外部依存関係の変更
- **revert**: 以前のコミットを取り消す

#### **Scope（オプション）**

変更の範囲を示す（括弧内）：
- `ci`: CI/CD関連
- `deps`: 依存関係
- `modal`: モーダルコンポーネント
- `nav`: ナビゲーション
- `seo`: SEO関連
- `liquid`: Liquid Design関連
- 等

#### **Subject（必須）**

- 50文字以内を目安
- 日本語OK
- 文末にピリオド不要

#### **Issue参照（推奨）**

- `(refs #<issue>)`: 作業中のIssue参照
- `(closes #<issue>)`: Issueをクローズする場合
- 複数Issue: `(refs #12, #34)`

#### **例**

```bash
# 機能追加
feat(modal): プロダクト詳細モーダルを追加 (refs #25)

# バグ修正
fix(nav): スクロール時のNavbar表示バグを修正 (closes #34)

# CI/CD改善
chore(ci): Yarn Cacheを有効化してビルド時間短縮 (refs #45)

# ドキュメント更新
docs: copilot-instructionsにコミットルールを追記 (refs #45)

```

#### **コミット前の確認**
* `git status` で変更ファイル一覧を確認
* `git diff` で意図しない差分がないかチェック
* 特に以下に注意：
  - README.mdの意図しない上書き
  - 既存ファイルの削除・移動漏れ
  - ビルド成果物（`dist/`等）のコミット防止
  - `copilot-instructions.md` の意図しない変更
* 確認後に `git add` してコミット

### 進捗の可視化
* 複数ファイルの変更は、変更内容を箇条書きで報告
* ビルドエラーや問題発生時は即座に報告し、解決策を提示

### PRレビューコメントの取得
* GitHub PRのレビューコメント（インラインコメント）を取得する場合：
  ```bash
  gh api /repos/{owner}/{repo}/pulls/{pr_number}/comments --jq '.[] | {path, line, body, user: .user.login, created_at}'
  ```
* レビュー全体のサマリーを取得する場合：
  ```bash
  gh pr view {pr_number} --json reviews,comments
  ```

## Development Environment

### Quick Start

**⚠️ 重要: サービス起動にはVS Code Tasksを使用すること**
シェルスクリプトを直接実行すると、ターミナルがキャンセルされた際にプロセスが残り続けます。
必ず `create_and_run_task` ツールを使用してバックグラウンドタスクとして起動してください。

```bash
# ❌ 非推奨: シェルスクリプト直接実行
./scripts/start-services.sh  # プロセスが残る可能性がある

# ✅ 推奨: VS Code Tasksまたはgradlewコマンド直接実行
# バックエンド起動
cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'

# フロントエンド起動
pnpm --filter frontend-v3 dev  # または: cd apps/frontend-v3 && npm run dev
```

**プロセスの強制停止**
- **停止**：**Port→PID→Kill**。例：`fuser -k 5173/tcp`  
- **禁止**：`pkill -f node` / `killall node` / `kill $(pgrep node)`

**GitHub Copilotへの指示:**
- サービス起動時は必ずVS Code Tasksを作成・使用する
- `run_in_terminal` で `isBackground=true` は使用しない（プロセスが残る）
- 長時間実行プロセスは `create_and_run_task` でタスク化する

### Service URLs
- Frontend v3: http://localhost:5173/
- Backend API: http://localhost:3000/mipla2
- Swagger UI: http://localhost:3000/mipla2/swagger-ui.html
- OpenAPI JSON: http://localhost:3000/mipla2/api-docs
- ProMarker UI (v3): http://localhost:5173/promarker
- H2 Console: http://localhost:3000/mipla2/h2-console

### Key Configuration Files
- `backend/src/main/resources/config/application.yml` - Main configuration
- `apps/frontend-v3/vite.config.ts` - Frontend build and proxy settings
- `settings.gradle` - Multi-project Gradle configuration
- `.devcontainer/devcontainer.json` - Development container setup

### API Proxy Configuration
Frontend development server (Vite) proxies API calls:
```typescript
// vite.config.ts proxy configuration
server: {
  proxy: {
    '/mapi': {
      target: 'http://localhost:3000/mipla2',
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/mapi/, ''),
    },
  },
}
```

**Critical Pattern**: 
- Frontend API calls: `POST /mapi/apps/mste/api/suggest`
- Backend receives: `POST /mipla2/apps/mste/api/suggest`
- Spring Boot context path: `/mipla2`
- Always use `/mapi/` prefix in frontend code for proxy routing

## Core Components

### Backend Structure
```
backend/
├── src/main/java/jp/vemi/
│   ├── mirel/apps/mste/        # ProMarker core functionality
│   │   ├── domain/service/     # Business logic
│   │   ├── application/controller/ # REST controllers
│   │   └── domain/dto/         # Data transfer objects
│   ├── framework/              # Base framework utilities
│   └── ste/                   # Stencil Template Engine
├── src/main/resources/
│   ├── config/                # Configuration files
│   ├── stencil-samples/       # Built-in template samples
│   └── templates/             # Web templates
```

### Frontend Structure
```
apps/frontend-v3/
├── src/
│   ├── app/routes/            # React Router pages
│   ├── features/promarker/    # ProMarker feature module
│   ├── components/            # Shared components
│   └── lib/                   # Utilities and API client
├── packages/ui/               # Design system (@mirel/ui)
└── vite.config.ts            # Vite configuration
```

## Development Guidelines

### Code Style
- Java: Follow Spring Boot conventions, use Lombok for boilerplate reduction
- React: Use functional components with hooks, TypeScript strict mode, maintain immutable state patterns
- Ensure proper null safety checks, especially for API response handling

### API Response Patterns
- Most APIs return `ApiResponse<T>` structure with `data`, `messages`, `errors` fields
- Special case: SuggestService uses ModelWrapper for frontend compatibility
- Always check for both success data and error conditions

### Database Access
- Primary: JPA repositories with Spring Data patterns
- Development: Framework debug endpoint `/framework/db/query` (localhost only)
- Use proper transaction boundaries for multi-step operations

### Template System
- Stencil templates use YAML configuration + FreeMarker templates
- Templates stored in `backend/src/main/resources/stencil-samples/`
- Support for hierarchical category organization

## Key Features Implementation

### ProMarker (MSTE) - Main Template Engine
- **Purpose**: Dynamic code generation from templates
- **Main UI**: `/apps/frontend-v3/src/features/promarker/` (React)
- **Core Service**: `SuggestServiceImp`, `GenerateServiceImp`
- **Workflow**: Category selection → Stencil selection → Parameter input → Generation

### Stencil Management
- **Configuration**: YAML-based stencil settings
- **Templates**: FreeMarker (.ftl) files for code generation
- **Storage**: Classpath-bundled samples + database-managed custom stencils
- **Categories**: Hierarchical organization with user/standard/sample levels

### File Management
- **Upload/Download**: Secure temporary file handling
- **Batch Operations**: ZIP compression for multi-file downloads
- **Integration**: Template parameter file references

## Related Documentation

For detailed information on specific aspects, refer to:

- **[API Reference](./docs/api-reference.md)** - Complete API endpoint documentation
- **[Frontend Architecture](./docs/frontend-architecture.md)** - React implementation details (Note: Some content may reference legacy Vue.js)
- **[Development Guide](./docs/development-guide.md)** - Advanced development patterns
- **[Troubleshooting](./docs/troubleshooting.md)** - Common issues and solutions

## Common Development Tasks

### Adding New API Endpoints
1. Create DTO classes in `domain/dto/`
2. Implement service in `domain/service/`
3. Create API wrapper in `domain/api/`
4. Register with Spring's component scanning

### Frontend Component Development
1. Follow React functional component patterns with TypeScript strict mode
2. Use @mirel/ui components (shadcn/ui + Radix UI wrapper) for design system consistency
3. Use Tailwind CSS 4 for styling with class-variance-authority for component variants
4. Implement proper error handling with Radix Toast notifications
5. Use TanStack Query for server state, Zustand for client state
6. Follow feature-based architecture: `apps/frontend-v3/src/features/{feature}/`

### Template Development
1. Create YAML configuration in `stencil-samples/`
2. Implement FreeMarker templates (.ftl files)
3. Test with ProMarker UI workflow
4. Ensure proper parameter validation

## Monorepo Workspace Structure

### Packages Organization
```
packages/
├── ui/              # @mirel/ui design system (shadcn/ui wrapper)
├── e2e/             # Playwright E2E tests
└── configs/         # Shared configurations

apps/
└── frontend-v3/     # React app with Vite
```

### Package Management Commands
```bash
# Install dependencies for all packages
pnpm install

# Run command in specific package
pnpm --filter frontend-v3 dev
pnpm --filter e2e test

# Run command in all packages
pnpm -r build
pnpm -r typecheck
```

## Testing Strategy

### E2E Testing with Playwright
- **Location**: `packages/e2e/tests/`
- **Configuration**: `packages/e2e/playwright.config.ts`
- **Auto-start**: Configured to start backend + frontend automatically
- **Parallel execution**: Limited to 2 workers (local) / 1 worker (CI) for stability
- **Localization**: Japanese locale (ja-JP), Asia/Tokyo timezone

### Key E2E Test Commands
```bash
# Run all E2E tests
pnpm test:e2e

# Run with UI mode (interactive)
pnpm test:e2e:ui

# Run specific test file
pnpm --filter e2e test tests/specs/promarker-v3/form-validation.spec.ts
```

### Test Organization
- **Archived tests**: `tests/specs/_archived-vue-frontend/` (ignored in config)
- **Active tests**: Focus on `tests/specs/promarker-v3/` (React frontend)
- **Page Objects**: Organized by feature in `tests/page-objects/`

## Security Considerations

- **Authentication**: JWT-based with session management
- **API Access**: Most endpoints require authentication
- **File Security**: Temporary file cleanup and access control
- **Database**: Development debug access restricted to localhost only
- **Template Security**: Proper input validation for template parameters

### Performance Guidelines

- **Frontend**: Use React.memo, useMemo, useCallback appropriately; leverage TanStack Query caching
- **Backend**: Implement proper caching for template metadata, use efficient database queries
- **File Operations**: Stream large files, implement proper cleanup for temporary files
- **E2E Testing**: Use Playwright with resource-aware parallelization (workers: 2 local, 1 CI)

---

This document provides the foundation for working with the ProMarker platform. Refer to the detailed documentation in the `docs/` directory for specific implementation guidance.