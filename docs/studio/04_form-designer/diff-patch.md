# mirel Studio Form Designer 差分適用アルゴリズム

> 軽量パッチ方式による効率的なレイアウト更新

---

## 1. 概要

mirel Studio Form Designer は頻繁に変更が発生するため、**差分だけを適用する軽量パッチ方式** を採用する。

これにより、大規模フォームでも高速に動作し、レイアウト全体の再レンダリングを防止する。

---

## 2. パッチ対象

| 対象 | 説明 |
|------|------|
| レイアウト | 行・列・タブの構造 |
| Widget の配置 | グリッド位置 |
| Widget の属性 | ラベル・プレースホルダ等 |
| セクション構造 | グループ分割 |

---

## 3. パッチ構造（差分の最小単位）

JSON Patch RFC6902 互換形式を参考とする。

```json
{
  "op": "add" | "remove" | "replace" | "move",
  "path": "/layout/1/2",
  "value": {...}
}
```

| オペレーション | 説明 |
|---------------|------|
| add | 新規要素の追加 |
| remove | 既存要素の削除 |
| replace | 既存要素の置換 |
| move | 要素の位置移動 |

---

## 4. パッチ生成

Draft と Release の差分から自動生成する。

```
Draft.FormLayout - Release.FormLayout = Patch[]
```

**生成例：**

```json
[
  { "op": "add", "path": "/layout/3", "value": {...} },
  { "op": "replace", "path": "/layout/1/field", "value": "customerName" }
]
```

---

## 5. パッチ適用順序

Runtime は以下の順序で適用し、依存関係の崩壊を避ける。

```
1. remove  （削除を先に）
2. replace （置換）
3. add     （追加）
4. move    （移動を最後に）
```

---

## 6. 衝突検出

以下のケースでは競合として検出される。

| ケース | 説明 |
|--------|------|
| 行削除 + Widget 更新 | 削除対象内の Widget を更新しようとした |
| タブ変更 + タブ内移動 | タブ構造変更と内部 Widget 移動が重複 |
| 別 Draft とのマージ結果 | 複数 Draft の統合時に矛盾が発生 |

競合時は **手動解決** が必要。

---

## 7. 設計意図

- レイアウト全体の再レンダリングを防止
- 差分のみを高速適用
- 大規模フォームでも高速動作
- Builder の編集体験を向上

---

## 関連ドキュメント

- [Widget 仕様](./widget-spec.md)
- [レイアウトアルゴリズム](./layout-algorithm.md)
- [Release Center 差分アルゴリズム](../07_release-center/diff-algorithm.md)

---

*Powered by Copilot 🤖*
