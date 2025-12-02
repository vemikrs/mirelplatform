# 47 モデルラベルの i18n ポリシー

モデルの表示名は Runtime 画面のユーザー体験に直結するため、  
多言語化方針を詳細に定義する。

---

# 1. 対象

- Entity 名  
- Field 名  
- セクション名  
- Flow メッセージ  
- Code 値のラベル  

---

# 2. 多言語入力 UI

Modeler の右ペインで以下を入力可能：

```
fieldName:
  ja: 顧客名
  en: Customer Name
  zh: 客户名称 （将来）
```

---

# 3. 未入力時の挙動

- 未入力言語 → 主言語（ja）の値を fallback  
- fallback なし → FieldId を表示  

---

# 4. 自動翻訳オプション（任意）

- Builder がチェックすると、英語ラベルを自動生成  
- 翻訳サービスは切替可能（Azure / DeepL / Local）  
- 自動翻訳した箇所には「翻訳済バッジ」を表示  

---

# 5. ラベル整合性チェック

- 全言語の入力状況を一覧表示  
- Release 作成時に「未翻訳」警告を出す（任意設定）

---

# 6. CodeGroup の i18n

Code 定義は以下の形式：

```
{
  "groupId": "OrderStatus",
  "code": "APPROVED",
  "label": {
    "ja": "承認済",
    "en": "Approved"
  }
}
```

---

# 7. 設計意図

- 海外／多拠点対応  
- Runtime UI の統一品質  
- Builder 負荷の軽減  
