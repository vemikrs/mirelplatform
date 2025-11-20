# E2E (packages/e2e)

## スコープ
Playwright 1.56 で ProMarker UI を E2E テストするパッケージ。`tests/specs/promarker-v3/` に React 版のシナリオを集約し、ページ操作は `tests/page-objects/` を経由させる。テスト実行時は Playwright がバックエンド/フロントエンドを自動起動するため、手動でサーバーを立てない。

## コマンド
| 用途 | コマンド |
| --- | --- |
| 通常実行 | `pnpm test:e2e` (ルート script) |
| UIモード | `pnpm test:e2e:ui` |
| 個別Spec | `pnpm --filter e2e test tests/specs/promarker-v3/<file>.spec.ts` |
| ヘッドレス→レポート表示 | `pnpm --filter e2e test` → `pnpm --filter e2e test:report` |
| ブラウザ依存準備 | `pnpm --filter e2e install:browsers` |

## 実装ポリシー
- 新規テストは `page-objects` を通し、ロケーターの重複を避ける。
- `E2E_BASE_URL` を指定しない場合は config で Dev サーバーを自動起動する。既存サーバーが起動中の場合はポート衝突に注意。
- ファイルアップロードなどの秘密情報は `tests/fixtures` に置かず、必要ならモックする。
- 失敗時のスクリーンショット/trace を `packages/e2e/test-results/` から収集して Issue に添付。

## レビュー前チェックリスト
- [ ] `pnpm test:e2e` がローカルで緑になる。
- [ ] 新規シナリオは `docs/E2E_IMPLEMENTATION_SUMMARY.md` へ要約を追記。
- [ ] テスト名は日本語でユーザー操作が分かる表現にする。
