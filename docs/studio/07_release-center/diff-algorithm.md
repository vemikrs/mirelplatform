# mirel Studio Release Center 差分検出アルゴリズム

> Draft → Release → Deploy における変更差分の自動抽出

---

## 1. 概要

mirel Studio Release Center は Draft → Release → Deploy の流れの中で **変更差分の自動抽出と安全性検証** を行う。

---

## 2. 差分対象

以下の 3 つが比較対象となる：

| 対象 | 説明 |
|------|------|
| Model | エンティティ・フィールド・関係 |
| Form | レイアウト・コンポーネント配置 |
| Flow | ノード・条件・入出力 |

---

## 3. 差分検出の基本原則

- JSON アーティファクトを基準に構造比較
- 「ID」「型」「属性」「構造」を比較
- 差分は階層的に分類

---

## 4. 差分種別（分類）

| 種別 | 説明 |
|------|------|
| Add | 新規追加 |
| Remove | 削除 |
| Update | フィールド・属性の変更 |
| Move | 並び順（位置）の変更 |

---

## 5. 差分抽出アルゴリズム

```
function diff(old, new):
    result = []
    scan(old, new, path="$", result)
    return result

function scan(a, b, path, result):
    if both primitive:
        if a != b:
            result.add({type:"Update", path, old:a, new:b})
        return
    
    keys = union(a.keys, b.keys)
    for k in keys:
        newPath = path + "." + k
        if k not in a:
            result.add({type:"Add", path:newPath, new:b[k]})
        else if k not in b:
            result.add({type:"Remove", path:newPath, old:a[k]})
        else:
            scan(a[k], b[k], newPath, result)
```

---

## 6. 差分のビジュアル化

Release Center では以下のヒートマップ表示を採用：

| 種別 | 色 |
|------|-----|
| Add | 緑 |
| Remove | 赤 |
| Update | 青 |
| Move | 黄色（将来） |

---

## 7. リスク評価（重要度）

差分ごとに「影響度」を自動評価する。

| 影響度 | 例 |
|--------|-----|
| 高 | 型変更・関係変更・既存フローの破壊 |
| 中 | 必須化・UI 表示制御の変更 |
| 低 | 名称変更・説明文の変更 |

---

## 8. Deploy 前の整合性チェック

| チェック項目 | 説明 |
|-------------|------|
| Model ↔ Form | 矛盾チェック |
| Model ↔ Flow | 破壊チェック |
| Form ↔ Flow | 整合性チェック（入力変数一致など） |

全て問題がない場合のみ Deploy が可能。

---

## 9. 設計意図

- Builder 作業の透明性向上
- Deploy 時の事故を防止
- チーム開発での統制を確保
- モデル駆動の LCAP としての再現性確保

---

## 関連ドキュメント

- [Draft/Release データモデル](./draft-release-model.md)
- [影響分析エンジン](./impact-analysis.md)
- [Deploy ロールバック](./deploy-rollback.md)

---

*Powered by Copilot 🤖*
