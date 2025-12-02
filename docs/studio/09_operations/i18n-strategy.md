# mirel Studio 多言語化戦略（i18n）

> 国際展開を視野に入れた多言語対応

---

## 1. 概要

mirel Studio は国際展開を視野に入れ、UI・生成アプリの多言語化を段階的に進める。

---

## 2. 多言語対応の対象

| 対象 | 説明 |
|------|------|
| Studio（編集 UI） | Builder 向け |
| Runtime（利用者向け UI） | エンドユーザー向け |
| エラーメッセージ | システムメッセージ |
| コード一覧（label） | コード体系の表示名 |

---

## 3. ランタイム言語切替

ユーザー単位で以下を選択可能：

| 言語 | 対応状況 |
|------|---------|
| 日本語 | 対応 |
| 英語 | 対応 |
| 韓国語 | 将来 |
| 中国語 | 将来 |

---

## 4. リソース管理方式

```
i18n/
├─ ja.json
├─ en.json
└─ zh.json（将来）
```

Studio では React Intl / i18next を想定。

---

## 5. フォーム生成時の i18n

Modeler の「表示名」を多言語フィールドに：

```json
{
  "fieldName": {
    "ja": "顧客名",
    "en": "Customer Name"
  }
}
```

未入力の場合は自動翻訳（オプション）。

---

## 6. Flow メッセージ表示の多言語化

Flow の「メッセージ表示」ノードは次の形式：

```json
{
  "message": {
    "ja": "保存しました",
    "en": "Saved successfully"
  }
}
```

---

## 7. i18n ルール

| ルール | 説明 |
|--------|------|
| 文言のハードコード禁止 | キー管理必須 |
| エラーメッセージ | キー管理 |
| Modeler と Runtime の i18n | 別管理 |
| コードラベル | CodeGroup ごとに言語管理 |

---

## 8. 設計意図

- 海外利用・多拠点利用を想定
- 文言の一元管理による保守性向上
- Builder の負荷を軽減

---

## 関連ドキュメント

- [UX 原則](../01_concept/ux-principles.md)
- [コード体系](../03_modeler/code-system.md)
- [Workspace/環境管理](./workspace-env.md)

---

*Powered by Copilot 🤖*
