# mirel Studio Builder–Runtime 連携設計

> Builder（設計環境）と Runtime（実行環境）の分離とインタラクション

---

## 1. 概要

mirel Studio は **Builder（設計環境）** と **Runtime（実行環境）** を厳密に分離し、業務アプリケーションの変更を「安全に」反映する構造を採用する。

---

## 2. インタラクション全体像

### Step 1：設計フェーズ（Builder）

- mirel Studio Modeler で業務構造を編集
- mirel Studio Form Designer で UI を設計
- mirel Studio Flow Designer で業務ロジックを追加
- 全て JSON アーティファクトとして保存

### Step 2：Draft 作成

- 現在の編集状態をスナップショットとして固定

### Step 3：Release 作成

- 過去バージョンとの差分を自動解析
- モデル／UI／フローの変更を一つの「リリース単位」にまとめる

### Step 4：Deploy

- Dev / Stg / Prod 各環境へ適用
- Runtime はリリース内容を基に以下を再構築：
  - 画面生成
  - API 生成
  - フロー実行計画

### Step 5：Runtime 実行

- エンドユーザーは生成された UI を利用
- モデルに基づく整合性チェック
- 保存・更新・削除時は Execution Engine が処理
- イベントは Flow Engine が実行

---

## 3. インタラクション図

```
[ Builder (mirel Studio) ]
    Modeler
    Form Designer
    Flow Designer
         │
         │ Draft 作成
         ▼
    [ Draft ]
         │
         │ Release 作成
         ▼
    [ Release Bundle ]
         │
         │ Deploy
         ▼
    [ Runtime ]
    ├── Generated UI
    ├── Generated API
    ├── Flow Execution
    ├── Domain Ops
    └── Data Store
```

---

## 4. 各フェーズの詳細

| フェーズ | 環境 | 主な処理 |
|---------|------|---------|
| 設計 | Builder | モデル/フォーム/フロー編集 |
| Draft | Builder | 編集状態のスナップショット |
| Release | Builder | 差分解析、バージョン確定 |
| Deploy | Builder → Runtime | 環境への適用 |
| 実行 | Runtime | UI 表示、API 処理、フロー実行 |

---

## 5. 設計意図

- 建設中の構造（Builder）が、運用中の業務（Runtime）へ直接影響しない
- 業務改善の反復を「壊れない形式」で蓄積可能
- 大規模組織で必須となる統制モデル（Dev/Stg/Prod）に対応

---

## 6. 利点

| 項目 | 説明 |
|------|------|
| 安全性 | 編集中の変更が本番に即座に反映されない |
| 追跡性 | Release 単位で変更履歴を管理 |
| 可逆性 | ロールバックにより以前の状態に戻せる |
| 統制 | 環境ごとの承認フローを設定可能 |

---

## 関連ドキュメント

- [アーキテクチャ概要](./overview.md)
- [Release Center 概要](../07_release-center/overview.md)
- [データモデル全体像](./data-model.md)

---

*Powered by Copilot 🤖*
