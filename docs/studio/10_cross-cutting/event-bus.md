# mirel Studio アプリ内イベントバス設計

> ツール間のリアクティブ更新を支える基盤

---

## 1. 概要

mirel Studio は UI 全体が複数ツール（Modeler / Form Designer / Flow Designer / Release Center）で構成されるため、同期と一貫性を保つために「アプリ内イベントバス」を採用する。

---

## 2. イベントバスの役割

| 役割 | 説明 |
|------|------|
| Modeler → Form/Flow | 更新通知 |
| Flow 条件式 | 更新検知 |
| Draft/Release | 状態遷移 |
| 統一更新 | Studio 全体のリアクティブ更新 |

---

## 3. イベントの分類

| イベント | 説明 |
|---------|------|
| ModelUpdated | モデル変更 |
| FormUpdated | フォーム変更 |
| FlowUpdated | フロー変更 |
| DraftChanged | Draft 状態変更 |
| ReleaseGenerated | Release 生成 |
| PermissionChanged | 権限変更 |

---

## 4. イベントの構造

```json
{
  "eventType": "ModelUpdated",
  "timestamp": "2025-11-21T10:00:00Z",
  "payload": {
    "entityId": "customer",
    "changes": { ... }
  }
}
```

---

## 5. イベント伝播のルール

| ルール | 説明 |
|--------|------|
| 選択的伝達 | 同期が必要なツールだけに伝達 |
| 効率的更新 | 不必要な再描画を避ける |
| スロットリング | イベントループを自動停止 |

---

## 6. UI の反応例

### ModelUpdated

- Form Designer: 対象フィールドの再配置を提案
- Flow Designer: 条件式の再評価を起動

### DraftChanged

- Draft バッジ更新
- Release Center に差分通知表示

---

## 7. 技術的前提

| 技術 | 用途 |
|------|------|
| React Context | 状態管理 |
| RxJS / EventEmitter | イベント配信 |
| WebSocket | 複数ユーザー同時編集（将来） |

---

## 8. 設計意図

- Studio 全体の状態統合
- ツール間の一貫性確保
- 更新の反映漏れを防止

---

## 関連ドキュメント

- [アーキテクチャ概要](../02_architecture/overview.md)
- [7 層レイヤー](../02_architecture/seven-layer.md)
- [UI 情報アーキテクチャ](./ui-information-architecture.md)

---

*Powered by Copilot 🤖*
