# UI Package (@mirel/ui)

## スコープ
`packages/ui/` は Radix UI + shadcn/ui をベースにしたデザインシステム。アプリ側はここで公開されるコンポーネントを import するだけでスタイル・アクセシビリティを担保する。CSS トークンは `src/theme/index.css` に集約され、Frontend から `@mirel/ui/theme/index.css` を取り込む。

## ビルド/検証
| 用途 | コマンド |
| --- | --- |
| 型チェック | `pnpm --filter @mirel/ui typecheck` |
| ユニットテスト (Vitest) | `pnpm --filter @mirel/ui test` |
| Lint | フロント側 eslint を流用するため、変更後は `pnpm --filter frontend-v3 lint` も実行 |

> **依存 or コンポーネント更新時チェック**: `pnpm --filter @mirel/ui typecheck` → `pnpm --filter @mirel/ui test` → Frontend 側で `pnpm --filter frontend-v3 build` を走らせ、破壊的変更がないか確認。

## 実装ポリシー
- Radix primitive を wrap する際は props を `React.ComponentPropsWithoutRef<typeof Primitive>` として透過させる。
- DOM 構造を変更する場合、アクセシビリティ属性 (`role`, `aria-*`, `data-state`) を失わないようテストを書く。
- テーマトークンは HSL 値を格納し、消費側は `hsl(var(--token))` で参照する。直接 HEX/rgba を埋め込まない。
- 新規コンポーネントは Story/MDX 未導入のため `packages/ui/README.md` に使い方を追記する。

## レビュー前チェックリスト
- [ ] `pnpm --filter @mirel/ui typecheck` / `test` が成功。
- [ ] 破壊的変更の場合は `apps/frontend-v3` の利用箇所をすべて洗い出し、移行ガイドを `docs/architecture/` に記載。
- [ ] バンドル不要。`src/index.ts` からのエクスポートを更新し忘れていないか確認。
