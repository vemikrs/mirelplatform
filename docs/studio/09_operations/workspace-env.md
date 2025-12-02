# mirel Studio Workspace／環境管理仕様

> Workspace × Environment による組織向け管理

---

## 1. 概要

mirel Studio は、組織での利用を前提とし **Workspace × Environment** という 2 軸で管理される。

---

## 2. Workspace 概念

Workspace は「業務アプリの集合」を表す単位。

**含まれるもの：**

| 項目 | 説明 |
|------|------|
| Model | 業務構造 |
| Forms | UI |
| Flows | 業務ロジック |
| Codes | 値ラベル |
| Draft/Release | 版管理 |
| Deploy 履歴 | 適用履歴 |

**用途：**
- 部署単位の業務アプリ
- システムごとの分離管理
- マルチテナント構成（将来）

---

## 3. Environment（環境）概念

環境は Runtime の実行先を分離する。

**標準提供される三階層：**

| 環境 | 説明 |
|------|------|
| Dev（開発） | Studio の編集内容はここへ Deploy |
| Stg（検証） | 動作検証用 |
| Prod（本番） | 本番稼働（安定性が最優先） |

---

## 4. Workspace × Environment の対応表

```
Workspace A
├─ Dev  ← Studio の編集内容はここへ Deploy
├─ Stg  ← 動作検証用
└─ Prod ← 本番稼働（安定性が最優先）

Workspace B
├─ Dev
├─ Stg
└─ Prod
```

---

## 5. Release の適用ルール

| ルール | 説明 |
|--------|------|
| Workspace ごと | Release は Workspace ごとに管理 |
| 環境単位 | Deploy は環境単位で制御 |
| 権限制御 | Prod への Deploy は承認制 |

---

## 6. 権限管理

| 権限 | Dev | Stg | Prod |
|------|-----|------|-------|
| 編集（Builder） | ○ | × | × |
| Deploy | ○ | ○ | △（承認制） |
| データ閲覧（Browser） | ○ | ○ | ○ |
| Flow 実行 | ○ | ○ | ○ |

---

## 7. 環境間のデータ分離

Runtime のデータ（JSONB 等）は環境間で完全分離する。

```
Dev DB  ≠  Stg DB  ≠  Prod DB
```

---

## 8. 環境切替 UI の仕様

トップバーにて選択可能：

```
[ Dev ▼ ]
├─ Dev
├─ Stg
└─ Prod
```

**切替後の動作：**
- Data Browser は対象環境のデータを表示
- Flow は対象環境で実行
- 各種ログも環境別に管理

---

## 9. 設計意図

- 組織利用に耐えうる統制
- Builder の編集と本番の安全性を分離
- Release/Deploy の整合性管理
- 抜け漏れを防ぐガバナンスの確保

---

## 関連ドキュメント

- [権限モデル（RBAC）](./rbac-model.md)
- [ログモデル](./log-model.md)
- [Deploy ロールバック](../07_release-center/deploy-rollback.md)

---

*Powered by Copilot 🤖*
