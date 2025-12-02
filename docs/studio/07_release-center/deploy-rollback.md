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

## 9. Hotfix（緊急修正）フロー

本番環境での致命的な不具合に対し、開発中の Draft（次期バージョン）を経由せずに修正を適用するフロー。

1. **Hotfix Branch 作成**: 現在の Prod Release から Hotfix 用の作業領域を作成。
2. **修正適用**: 最小限の修正（Flow/Form/Script）のみを適用。
3. **Hotfix Release 作成**: バージョン番号をパッチアップ（例: `1.3.0` → `1.3.1`）。
4. **Deploy**: Prod 環境へ即時適用。
5. **Merge Back**: 修正内容を現在の Draft（次期バージョン開発ライン）へ自動マージ。

---

## 関連ドキュメント

- [差分検出アルゴリズム](./diff-algorithm.md)
- [環境管理](../09_operations/workspace-env.md)
- [Draft/Release データモデル](./draft-release-model.md)

---

*Powered by Copilot 🤖*
