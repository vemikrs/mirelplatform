# ProMarker E2E Testing

Playwright による ProMarker (React 19 + Vite) の E2E テストパッケージ。

## Prerequisites

- Node.js 22.x / pnpm 9+
- Java 21（バックエンド用）
- Playwright ブラウザ（Chromium）

## Setup

```bash
# ブラウザインストール
pnpm --filter e2e install:browsers
```

## Running Tests

```bash
# 全テスト実行（バックエンド・フロントエンドを自動起動）
pnpm test:e2e

# UI モード（対話的）
pnpm test:e2e:ui

# 個別 Spec
pnpm --filter e2e test tests/specs/promarker-v3/<file>.spec.ts

# レポート表示
pnpm --filter e2e test:report
```

> **Note**: `webServer` 設定により Spring Boot (port 3000) と Vite (port 5173) が自動起動されます。
> 既に起動済みの場合は `reuseExistingServer: true` が適用されます。

## Test Structure

```
tests/
├── specs/promarker-v3/    # React v3 テスト（アクティブ）
├── pages/                 # Page Object Model
├── fixtures/              # テストデータ
├── utils/                 # ユーティリティ
├── global-setup.ts
└── global-teardown.ts
```

## Configuration

`playwright.config.ts` で管理:
- **Locale**: ja-JP / Asia/Tokyo
- **Browser**: Chromium（デフォルト）
- **Workers**: 2（ローカル）/ 1（CI）
- **Retries**: 1（ローカル）/ 2（CI）
- **Timeout**: 30s / Expect: 10s

## Troubleshooting

| 症状 | 対処 |
|------|------|
| `ERR_CONNECTION_REFUSED` | ポート 3000/5173 が空いているか確認 |
| API タイムアウト | バックエンドのログを確認。timeout は 30s に設定済み |
| Flaky テスト | retry が有効。根本原因は Issue に起票 |

## Reports

- HTML: `playwright-report/index.html`
- JUnit XML: `test-results/junit.xml`（CI用）

## 関連ドキュメント

- Copilot 向け実装ポリシー: `.github/copilot/e2e.md`
- CI 設定: `docs/dev/CI_CONFIGURATION.md`
- Ensure frontend is running on port 8080
- Check E2E_BASE_URL is set correctly

### Browser not found
- Run `npm run install:browsers`

### Node version errors
- Use Node 18.x: `nvm use 18` or check `.nvmrc`
