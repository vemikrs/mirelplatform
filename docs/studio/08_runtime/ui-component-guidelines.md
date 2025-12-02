# 49 Runtime UI コンポーネント指針

Runtime で使用される UI コンポーネントは、  
「一貫性」「アクセシビリティ」「拡張容易性」を指針とする。

---

# 1. コンポーネント分類

```
入力系：Text / Select / Date / Time / Checkbox / File / Tag
表示系：Label / Badge / Status / Timeline
構造系：Section / Tab / Collapse
通知系：Alert / Toast
```

---

# 2. コンポーネント共通ルール

- ラベル必須  
- クリアボタンは明示  
- エラーは項目下に掲載  
- 必須項目は「*」で明示しない（色依存を避ける）  
  → ラベル横に「必須」バッジを付与  

---

# 3. タブ構造

- 2〜7 タブが推奨  
- 8 以上の場合は折りたたみ必須  
- タブ切替時はスクロール位置をリセット  

---

# 4. ファイルアップロード

- プレビュー（画像のみ）  
- 5MB 超過は警告  
- 複数ファイルは将来対応  

---

# 5. Form レンダリングとの整合

- Form Designer のレイアウト定義を忠実に再現  
- Embedded モデルは自動的にセクション化  
- 深い階層は collapse で折りたたみ  

---

# 6. 状態表示コンポーネント

```
status: APPROVED → 青バッジ
status: REJECTED → 赤バッジ
status: PENDING  → 黄バッジ
```

色弱対策のため、アイコン形状でも区別：

```
APPROVED → ✔
REJECTED → ✖
PENDING  → …
```

---

# 7. 設計意図

- Runtime アプリの統一品質  
- フォーム生成と矛盾しない UI  
- アクセシビリティの強化  
