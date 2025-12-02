# 45 Flow エラーハンドリング設計

Flow Designer は実行中の失敗に備え、  
包括的なエラーハンドリング機能を持つ。

---

# 1. エラー種別

```
① 入力エラー（ValidationError）
② データエラー（RecordNotFound / Duplicate）
③ 外部APIエラー（NetworkError / Timeout）
④ Script 実行エラー
⑤ 権限エラー
⑥ 不明エラー（UnexpectedException）
```

---

# 2. ハンドリングモデル

Flow には「エラーハンドラ」を設定できる。

```
Flow
├── Normal Path
└── Error Handler
```

---

# 3. エラーハンドラのノード

- メッセージ表示  
- リトライ  
- ログ出力  
- 補正処理  
- フォールバック処理  
- 強制終了  

---

# 4. エラーの伝播ルール

- ノード単位で捕捉 → Flow 全体へ伝播  
- 最初にヒットした ErrorHandler が適用  
- 捕捉されない場合は Flow が「失敗」状態で終了  

---

# 5. UI 例

```
[ActionNode]
│
├─ success → 次ノード
└─ error   → [ErrorHandler]
```

---

# 6. ログ出力

ErrorHandler が実行されるたびに以下を記録：

```
errorType
message
stackTrace（Scriptのみ）
nodeId
timestamp
```

---

# 7. 設計意図

- Runtime の安定動作  
- Builder が失敗パターンを制御可能  
- 外部 API 連携を安全に扱える設計  
