# mirel Studio 変更履歴

> ドキュメント構成の変更履歴

---

## バージョン 1.0.0（2025-01-XX）

### 初版リリース

既存の `grand-design/draft/` 配下の 50 ドキュメントを、カテゴリ別に再構成。

---

### 新規ディレクトリ構造

```
docs/studio/
├── 00_INDEX.md
├── 00_GLOSSARY.md
├── 01_concept/
├── 02_architecture/
├── 03_modeler/
├── 04_form-designer/
├── 05_flow-designer/
├── 06_data-browser/
├── 07_release-center/
├── 08_runtime/
├── 09_operations/
├── 10_cross-cutting/
├── 99_reference/
└── grand-design/ (レガシー)
```

---

### 主な変更点

| カテゴリ | 変更内容 |
|---------|---------|
| 命名規約 | `mirel` プレフィックスを廃止、DB テーブルのみ `stu_` を使用 |
| Java パッケージ | `jp.vemi.mirel.apps.studio` に統一 |
| API エンドポイント | `/mapi/studio/*` に統一 |
| コンポーネント名 | `mirel Studio Modeler` 等の形式に統一 |

---

### ドキュメント移行マッピング

| 旧ファイル | 新ファイル |
|-----------|-----------|
| draft/01 | 01_concept/design-philosophy.md |
| draft/02 | 01_concept/concept.md |
| draft/03 | 01_concept/lp-copy.md |
| draft/04 | 02_architecture/overview.md |
| draft/05 | 02_architecture/seven-layer.md |
| draft/06 | 02_architecture/builder-runtime.md |
| ... | ... |

完全なマッピングは [REVIEW-AND-REFACTORING-PLAN.md](../grand-design/REVIEW-AND-REFACTORING-PLAN.md) を参照。

---

## 今後の予定

- [ ] 31-50 の結合ファイルを個別ファイルに分離
- [ ] grand-design/draft の重複ファイル削除
- [ ] schema/ のドキュメントを各カテゴリに統合

---

*Powered by Copilot 🤖*
