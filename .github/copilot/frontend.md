# Frontend (apps/frontend-v3)

## スコープ
React 19 + Vite 7 で実装された ProMarker フロントエンド。ルーティング設定は `src/app/router.config.tsx` / `routes.tsx` にまとまり、ユースケースは `src/features/stencil-editor`, `src/features/promarker`, `src/features/catalog` など機能単位のフォルダで管理される。共通 UI は `@mirel/ui` を利用し、API コールは `src/lib/api/client.ts` から作成した axios instance を通じて `/mapi/*` に集約する。

## 起動・ビルド
| 用途 | VS Code Task | CLI (参考) |
| --- | --- | --- |
| 開発サーバー | `shell: Frontend-v3: Start Vite` | `pnpm --filter frontend-v3 dev` |
| 静的ビルド + 型チェック (tsc -b + vite build) | ― | `pnpm --filter frontend-v3 build` |
| Lint (eslint.config.js) | ― | `pnpm --filter frontend-v3 lint` |
| Unit Test (Vitest) | ― | `pnpm --filter frontend-v3 test` |
| 事前ビルドを確認したプレビュー | ― | `pnpm --filter frontend-v3 preview` |

> **依存更新時チェック**: `pnpm install` → `pnpm --filter frontend-v3 build` → `pnpm --filter frontend-v3 lint` → `pnpm --filter frontend-v3 test` の順で壊れないことを確認する。

## 実装ポリシー
- ルーティングは `src/app/router.config.tsx` で集中管理されるため、ページ追加時はそこへ登録する。
- `src/lib/api/` で axios instance と型 (`types.ts`) を定義しているので、新規 API もここで型付きにまとめてから `/mapi/...` を呼び出す。
- スタイルは `apps/frontend-v3/tailwind.config.js` と `@mirel/ui/theme/index.css` に定義された HSL トークンを使用し、独自カラーは可能な限り `var(--token)` ベースで指定する。
- モーダルやトーストなどの UI は `@mirel/ui` のコンポーネントを継承し、Radix の `data-state` / `aria-*` 属性を削除しない。レイヤー制御は Tailwind の `z-110` / `z-120` (Dialog用) を基準に設定する。
- Zustand / TanStack Query の状態管理は各機能ディレクトリ (`src/features/**/store.ts`) にまとめ、直接グローバルへ配置しない。

## レビュー前チェックリスト
- [ ] UI 変更はスクリーンショットまたはデモ動画を添付。
- [ ] ストア (Zustand) の更新は immer に依存せず immutable で実装。
- [ ] TanStack Query の cache key / staleTime を要件に合わせて設定。
- [ ] `pnpm --filter frontend-v3 build` と `lint` `test` が成功。
