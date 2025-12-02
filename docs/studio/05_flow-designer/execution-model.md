# mirel Studio Flow 実行モデル

> Runtime におけるフロー実行エンジンの仕様

---

## 1. 概要

mirel Studio Flow Designer で定義されたフローは、Runtime において **安全性・再現性・一貫性** を担保しつつ実行される。

---

## 2. 実行単位（Flow Execution Unit）

1つの「フロー定義」に対して 1つの実行単位が生成される。

**含まれる要素：**
- イベント情報
- グローバル変数コンテキスト
- ノード実行スタック
- エラーハンドラ
- ログコンテキスト

---

## 3. 実行シーケンス

```
(1) イベントが発火
    ↓
(2) Flow Execution Unit の生成
    ↓
(3) Start ノード実行
    ↓
(4) ノードチェーンを順次評価（BFS）
    ↓
(5) 条件分岐がある場合は条件を評価
    ↓
(6) 外部呼出・データ操作ノードを実行
    ↓
(7) 最終ノードに到達したら終了
```

---

## 4. ノードの実行方式

### 4.1 シングルスレッド実行

- 1フローにつき1実行コンテキスト
- 並列実行（Parallel Split）は将来拡張
- 副作用を局所化しやすくするため

### 4.2 実行順序

- エッジの優先度順
- arrival-order（定義順）を採用
- IF ノードは true → false の順

---

## 5. データ評価モデル

Flow Execution Unit は以下のスコープを持つ。

```
① イベントコンテキスト
② フロー内変数
③ ノード出力
④ エンティティデータ
```

**優先順位：**

```
ノード出力 > フロー変数 > イベントコンテキスト > エンティティデータ
```

---

## 6. データ操作ノードの実行

### Create / Update / Delete

- Modeler のスキーマ定義を参照
- バリデーションは実行前に再評価
- トランザクションは基本シリアライズ

### Get（検索）

- Entity + 条件式で実行
- 結果は Flow variables に格納

---

## 7. 外部連携ノードの実行

Runtime が提供する HTTP クライアントを利用。

| 項目 | 設定 |
|------|------|
| Timeout | デフォルト 3秒 |
| Retry | 将来実装 |
| Error | Catch ハンドラへ遷移 |

---

## 8. エラーハンドリング

### Try/Catch モデル

- ノード単位で try 区間を設置
- 例外発生 → Catch へ遷移
- Catch 実行後は「フロー続行」または「フロー終了」を選択

---

## 9. ログモデル

```typescript
interface FlowExecutionLog {
  flowId: string;
  executionId: string;
  eventType: string;
  startTime: Date;
  endTime: Date;
  nodes: Array<{
    nodeId: string;
    start: Date;
    end: Date;
    result: string;
    error?: string;
  }>;
}
```

---

## 10. Runtime の保証事項

- すべての実行パスのバリデーションを Builder 側でチェック
- Runtime ではパス実行に特化
- モデルに沿った整合性を常に維持

---

## 関連ドキュメント

- [ノード体系仕様](./node-spec.md)
- [条件式エディタ](./condition-editor.md)
- [テスト実行モード](./test-mode.md)

---

*Powered by Copilot 🤖*
