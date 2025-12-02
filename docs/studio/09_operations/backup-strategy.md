# 46 バックアップ／リストア戦略

mirel Studio は Workspace 単位で大量のメタデータを管理するため、  
バックアップとリストアの仕組みを公式に定義する。

---

# 1. バックアップ対象

```
① Model（Entity / Field）
② Form（Layout / Widgets）
③ Flow（Node / Condition）
④ Code 定義
⑤ Draft
⑥ Release
⑦ Workspace 設定
```

※ Runtime データは対象外（別運用）。

---

# 2. バックアップ方式

2 系統を採用する。

### 2.1 定期バックアップ（Scheduled）
- 1日1回  
- 最新 30 日分保持  
- JSON スナップショットとして保管  

### 2.2 手動バックアップ（On-demand）
- Builder / Admin が手動で実行  
- 任意のタイミングでスナップショット作成  

---

# 3. 格納形式

```
backup/
└── {workspaceId}/
    └── {yyyymmdd-HHmmss}/
        ├── model.json
        ├── forms.json
        ├── flows.json
        ├── codes.json
        ├── drafts.json
        └── releases.json
```

すべて JSON で統一。

---

# 4. リストア方式

### 4.1 部分リストア
- Model / Form / Flow など、特定モジュールのみリストア可能

### 4.2 完全リストア
- Workspace 全体を復元  
- Release 情報も含めて巻き戻し  

---

# 5. 制限事項

- 本番環境では Admin のみ使用可能  
- リストア後に差分が発生する場合、Draft 再同期が必要  

---

# 6. 設計意図

- 事故耐性の向上  
- Release ロールバック（30）との整合  
- 大規模環境での安全な運用  
