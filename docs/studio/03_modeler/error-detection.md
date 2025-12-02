# mirel Studio Modeler エラー検出体系

> モデル破壊を未然に防ぐためのエラー検出仕様

---

## 1. エラー検出の目的

Modeler は業務の表示・ロジック・データの中心であるため、エラー検出を重視した仕組みを備える。

- モデル破壊を未然に防ぐ
- Form / Flow 側の矛盾を早期に発見
- Release 作成時の事故を防止

---

## 2. エラー階層

| レベル | 説明 | アイコン |
|--------|------|---------|
| 致命的エラー（Critical） | Release 作成不可 | 🔴 |
| 重大エラー（Error） | 動作に影響あり | 🔴 |
| 警告（Warning） | 推奨されない状態 | 🟡 |
| 情報（Info） | 参考情報 | 🔵 |

### 表示場所

- モデルツリーのノードにバッジ
- 右ペインの「問題」タブ
- Release Center の差分画面

---

## 3. 主なエラー種別

### 3.1 フィールド定義エラー

| エラー | 説明 |
|--------|------|
| 型未指定 | データ型が設定されていない |
| 必須項目の不備 | 必須フラグがあるが初期値なし |
| 無効なコードグループ | 存在しない CodeGroup を参照 |
| 循環参照 | A → B → A の参照ループ |
| 重複フィールド ID | 同一モデル内で ID が重複 |

### 3.2 モデル構造エラー

| エラー | 説明 |
|--------|------|
| 曖昧な複合関係 | 参照先が不明確 |
| 100 階層以上の再帰 | 安全装置による制限 |
| Domain → 自身への再帰 | 無限ループの可能性 |

### 3.3 依存エラー（Impact Analysis 連携）

| エラー | 説明 |
|--------|------|
| Form に残留する孤立ウィジェット | 削除済みフィールドを参照 |
| Flow 条件式の型不一致 | 型変更後に条件式が無効 |
| CRUD API の破壊的変更 | 既存 API との互換性喪失 |

---

## 4. UI 表示仕様

### バッジ表示

```
エラー → 赤バッジ 🔴
警告 → 黄バッジ 🟡
情報 → 青バッジ 🔵
```

### 右ペインの問題タブ

```
[エラー] address.zip が Form に未配置です
[警告] order.amount が Flow 'order_onCreate' の条件式で型不一致です
```

---

## 5. 修正誘導（Fix Suggestion）

Studio は次のアクションを提案する：

| 状況 | 提案 |
|------|------|
| フィールド削除時 | 「削除したフィールドを Form から除去しますか？」 |
| 型変更時 | 「型変更に合わせて条件式を修正しますか？」 |
| 依存確認 | 「依存するフローを一覧表示しますか？」 |

---

## 6. Release 作成時の強制チェック

Release 作成前に必ず以下を走査：

1. モデルエラー
2. Form エラー
3. Flow エラー

**致命的エラーがある場合は Release 作成不可。**

### 検証 API

```typescript
interface ValidateReleaseResponse {
  valid: boolean;
  errors: ValidationIssue[];
  warnings: ValidationIssue[];
  breakingChanges: BreakingChange[];
}
```

---

## 7. 設計意図

- Builder の作業負荷軽減
- モデル駆動の整合性を担保
- 組織での大規模利用に不可欠な仕組み

---

## 関連ドキュメント

- [Modeler 概要](./overview.md)
- [Release Center 概要](../07_release-center/overview.md)
- [影響分析エンジン](../07_release-center/impact-analysis.md)

---

*Powered by Copilot 🤖*
