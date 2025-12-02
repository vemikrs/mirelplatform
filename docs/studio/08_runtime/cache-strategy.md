# mirel Studio Runtime キャッシュ戦略

> パフォーマンス確保のためのキャッシュレイヤー

---

## 1. 概要

mirel Studio Runtime は、生成されたモデル・フォーム・フローをもとにアプリケーションを動作させる。パフォーマンス確保のため、キャッシュレイヤーを持つ。

---

## 2. キャッシュ化される要素

以下の生成物をキャッシュする：

| 対象 | 説明 |
|------|------|
| Release のメタデータ | バージョン情報 |
| Entity モデル構造 | フィールド定義 |
| フォーム構造 | Widget 配置 |
| Flow ノード構造 | ノード定義 |
| Code グループ一覧 | コード体系 |
| Validation 定義 | バリデーションルール |

**単位：Release ごとにキャッシュを保持**

```typescript
runtimeCache[releaseId] = {
  modelTree,
  formLayouts,
  flowNodes,
  codes,
  validationRules
}
```

---

## 3. キャッシュ更新タイミング

| タイミング | 説明 |
|-----------|------|
| Deploy 時 | 新規 Release 適用 |
| Draft → Release 生成時 | 新バージョン作成 |
| Code 定義更新時 | コード体系変更 |

即時反映ではなく、**Runtime の reload（ホットリロード）** で更新。

---

## 4. キャッシュ整合性

以下の仕組みで整合性を保つ：

| 仕組み | 説明 |
|--------|------|
| Release 単位管理 | 過去 Release のキャッシュ保持 |
| Deploy 時は完全再生成 | 差分ではなく全体更新 |
| ハッシュによる検証 | 依存関係のハッシュを持つ |

**例：**

```
modelTree.hash   = sha256(...)
formLayout.hash  = sha256(...)
flow.hash        = sha256(...)
```

ハッシュが一致しない要素のみ再生成する。

---

## 5. キャッシュ階層

```
L1: メモリキャッシュ
L2: 永続化キャッシュ（JSON）
```

| 階層 | 説明 |
|------|------|
| L1（メモリ） | 特定 Release の頻出アクセス向け（Read-heavy） |
| L2（永続化） | サーバ再起動時にも即座に復帰、JSON / 簡易 KV ストレージを想定 |

---

## 6. キャッシュの無効化ルール

| 条件 | 説明 |
|------|------|
| 新規 Release Deploy | 新バージョン適用 |
| モデル破壊的変更検出 | 型変更・削除等 |
| Code Group 更新 | コード体系変更 |

**破壊的変更例：**
- フィールド削除
- 型変更
- モデル構造の循環発生

---

## 7. 設計意図

- 高速レスポンスの確保
- Deploy 時の負荷を最小化
- 大規模モデルの安定動作

---

## 関連ドキュメント

- [API 自動生成仕様](./api-autogen.md)
- [API スキーマ生成](./api-schema.md)
- [Deploy ロールバック](../07_release-center/deploy-rollback.md)

---

*Powered by Copilot 🤖*
