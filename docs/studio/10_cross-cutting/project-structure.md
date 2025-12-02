# mirel Studio プロジェクト構成標準

> Builder 環境と Runtime 環境のファイル構成規約

---

## 1. 概要

mirel Studio は、Builder 環境と Runtime 環境で異なる構造を用いるが、基本の整理軸は共通である。

---

## 2. Studio Workspace 内フォルダ構成（Builder 側）

```
workspace/
├─ drafts/
│    └─ draft-20251120-103000.json
├─ releases/
│    └─ release-1.2.0.json
├─ model/
│    └─ entities.json
├─ forms/
│    └─ customer_detail.json
├─ flows/
│    └─ customer_onCreate.json
├─ codes/
│    └─ OrderStatus.json
└─ metadata/
     └─ workspace.json
```

---

## 3. Draft/Release ファイルの基本規約

| ルール | 説明 |
|--------|------|
| ファイル名 | タイムスタンプまたはバージョンを付与 |
| metadata | JSON のトップレベルに持つ |
| 差分 | Release のみ diff キーに統合 |

**命名例：**
```
draft-20251121-235500.json
release-1.3.0.json
```

---

## 4. Builder アプリ側のプロジェクト構成（フロント）

```
src/
├─ components/
├─ pages/
├─ editors/
│    ├─ modeler/
│    ├─ form-designer/
│    ├─ flow-designer/
│    └─ code-editor/
├─ store/
└─ services/
```

---

## 5. Runtime プロジェクト構成（バックエンド）

```
runtime/
├─ api/
│    ├─ generated/      // CRUD API 自動生成領域
│    └─ router/
├─ engine/
│    ├─ flow/
│    └─ validation/
├─ domain/
├─ persistence/
└─ infra/
```

---

## 6. プラグイン構成（将来拡張）

```
plugins/
├─ flow-nodes/
│    └─ WebhookCall.json
└─ widgets/
     └─ custom-datepicker.json
```

---

## 7. ソース生成物とユーザー編集領域の分離

```
generated/     ← 自動生成、上書きOK
custom/        ← ユーザー編集、上書き禁止
```

Builder の self-managed な環境を維持するための必須ルール。

---

## 8. 構成の利点

| 利点 | 説明 |
|------|------|
| 拡張性と整合性の両立 | プラグイン対応 |
| 衝突しにくい構造 | チーム開発対応 |
| 明快な役割分離 | Builder/Runtime |

---

## 関連ドキュメント

- [アーキテクチャ概要](../02_architecture/overview.md)
- [Draft/Release データモデル](../07_release-center/draft-release-model.md)
- [7 層レイヤー](../02_architecture/seven-layer.md)

---

*Powered by Copilot 🤖*
