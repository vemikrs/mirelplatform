# mirel Studio 影響分析エンジン

> モデル変更が与える影響の自動分析

---

## 1. 概要

mirel Studio Modeler は、業務構造の変更が **フォーム・フロー・API にどのような影響を与えるか** を自動分析する仕組みを備える。

---

## 2. Impact Analysis の目的

- モデル変更時の破壊リスクを即時検出
- Builder が安全に編集できる環境の提供
- Release 作成時の自動整合性チェック

---

## 3. 対象となる依存関係

```
モデル → モデル（複合関係）
モデル → フォーム（ウィジェット配置）
モデル → フロー（条件式・データ参照）
モデル → API（CRUD の生成条件）
```

---

## 4. 分析する変更内容

| 変更 | 分析内容 |
|------|----------|
| フィールド追加 | Form の未配置箇所、Flow で参照されていないか |
| フィールド削除 | Form の孤立ウィジェット、Flow 条件式の破綻 |
| 型変更 | 不整合条件、フロー変数の型エラー |
| 必須化 | 既存データの不足、Flow onCreate の必須満たすか |
| 複合構造変更 | 下位フィールドの関係分岐破壊 |

---

## 5. 依存解析アルゴリズム

```
① Model 全体を走査し依存グラフを構築（DAG）
② 変更対象ノード（フィールド）を特定
③ ノードから逆方向に探索
④ 関連フォーム・フロー・他モデルを列挙
⑤ 影響タイプを分類
⑥ Builder UI に警告を表示
```

**擬似コード：**

```
function analyzeChange(node):
    affected = []
    for each dependency in graph[node]:
        affected.add(dependency)
        affected.add(analyzeChange(dependency))
    return affected
```

---

## 6. 影響度レベル

| 影響度 | 例 |
|--------|-----|
| 高 | 型変更、削除、複合構造の変更 |
| 中 | 必須化、relation の付与 |
| 低 | 名称変更、説明文 |

---

## 7. Builder への提示方法

- 右ペインに「影響箇所」一覧
- 各箇所へのジャンプリンク
- Release Center にて一覧可視化
- Deploy 前には強制チェック

**例：**

```
影響箇所（3件）
- Form: customer_detail.address → 削除により孤立
- Flow: customer_onCreate.IF1 → 条件式で型不一致
- API: customer/update → 必須項目不足
```

---

## 8. 設計意図

- モデル変更の安全性の確保
- Builder の判断負荷の軽減
- 組織開発での統制と透明性の向上

---

## 関連ドキュメント

- [差分検出アルゴリズム](./diff-algorithm.md)
- [Modeler エラー検出](../03_modeler/error-detection.md)
- [Draft/Release データモデル](./draft-release-model.md)

---

*Powered by Copilot 🤖*
