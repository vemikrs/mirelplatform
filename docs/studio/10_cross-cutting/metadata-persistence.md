# 43 メタデータ永続化方針

mirel Studio のすべての構成情報は  
「メタデータ（Metadata）」として一元管理される。

---

# 1. メタデータの分類

```
① モデル（entities.json / fields.json）
② フォーム（formLayout.json）
③ フロー（flow.json）
④ コード（codes.json）
⑤ Draft 情報
⑥ Release 情報
⑦ Workspace 情報
```

---

# 2. 永続化先

- PostgreSQL（JSONB）  
- 将来：S3 / Blob Storage へのアーカイブ  

---

# 3. 格納形式（例）

```json
{
  "workspaceId": "ws-001",
  "models": {...},
  "forms": {...},
  "flows": {...}
}
```

メタデータは常に **JSON 構造** として保持。

---

# 4. メタデータのバージョニング

* Draft ごとにメタデータスナップショット
* Release ごとにメタデータ固定化（Immutable）
* 過去版には書き込み不可

---

# 5. 更新トランザクション

```
Draft メタデータ更新  
 → EventBus に通知  
 → UI 各ツールに反映
```

---

# 6. 復旧／バックアップ

* Release がそのままバックアップとなる
* Export/Import も JSON ベースで可能

---

# 7. 設計意図

* 全データの一元管理
* 差分管理と復旧の容易化
* Deployment との連動を容易にする
