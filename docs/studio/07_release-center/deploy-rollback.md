# mirel Studio Deploy ロールバック仕様

> 誤った Deploy からの復旧機能

---

## 1. 概要

mirel Studio の Deploy は Release を Runtime に適用する操作であり、誤った Deploy に備えるため、**ロールバック機能** を提供する。

---

## 2. ロールバックの対象

| 対象 | 説明 |
|------|------|
| Runtime のモデル | Release 構造 |
| Flow・Form の生成物 | 実行時定義 |
| API スキーマ | CRUD エンドポイント |
| データ構造 | 必要に応じて（将来） |

---

## 3. ロールバック方式

以下の 2 段階方式を採用：

| 方式 | 説明 |
|------|------|
| Release 単位（推奨） | 過去の Release を再 Deploy |
| Runtime Snapshot（将来） | 完全なスナップショット復元 |

---

## 4. Release 単位のロールバック

Runtime では、以下のメタデータを持つ：

```json
{
  "runtimeState": {
    "currentRelease": "1.3.0",
    "appliedReleases": ["1.0.0", "1.1.0", "1.2.0", "1.3.0"]
  }
}
```

**ロールバック手順：**

1. 現在の Release を確認
2. 一つ前の Release を選択
3. Deploy し直す（巻き戻し）

---

## 5. 制限事項

| 制限 | 説明 |
|------|------|
| Prod 環境 | ロールバックは Admin のみ |
| モデル構造の大幅変更 | データ互換が崩れる可能性あり |
| レコード削除 | 復元不可（Data Snapshot が必要） |

---

## 6. Runtime Snapshot（将来）

以下をスナップショットとして保持予定：

- Release
- Flow 実行構造
- Form レイアウト
- API スキーマ
- データ（オプション）

※ コストが高いので必要に応じて提供。

---

## 7. ロールバック画面 UI

```
Release Center
 └── Deploy History
        ├─ v1.3.0（現在）
        ├─ v1.2.0
        ├─ v1.1.0
        └─ [ロールバック] ボタン
```

---

## 8. 設計意図

- 本番環境での事故対策
- Dev/Stg での検証サイクルの強化
- 業務アプリの継続性確保

---

## 関連ドキュメント

- [差分検出アルゴリズム](./diff-algorithm.md)
- [環境管理](../09_operations/workspace-env.md)
- [Draft/Release データモデル](./draft-release-model.md)

---

*Powered by Copilot 🤖*
