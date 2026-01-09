# Mira RAG パイプライン設計

> **作成日**: 2026-01-02  
> **関連**: [overview.md](./overview.md), [context-engineering-plan.md](../05_context-engineering/context-engineering-plan.md)

---

## 1. 概要

Mira は **RAG (Retrieval-Augmented Generation)** を活用し、ナレッジベースから関連情報を検索してコンテキストに注入する。これにより、ハルシネーションを抑制し、正確で根拠のある回答を生成する。

### 1.1 パイプライン構成

```
┌──────────────────────────────────────────────────────────────────────┐
│                          User Query                                   │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     MiraHybridSearchService                           │
│  ┌────────────────┐    ┌────────────────┐    ┌─────────────────────┐ │
│  │  Vector Search │    │ Keyword Search │    │  Scope Filtering    │ │
│  │   (PGVector)   │    │   (SQL ILIKE)  │    │ (Tenant/User/System)│ │
│  └───────┬────────┘    └───────┬────────┘    └──────────┬──────────┘ │
│          │                     │                        │            │
│          └─────────────────────┴────────────────────────┘            │
│                                │                                      │
│                                ▼                                      │
│                    ┌───────────────────────┐                          │
│                    │ Reciprocal Rank Fusion │                          │
│                    │        (RRF)           │                          │
│                    └───────────┬───────────┘                          │
│                                │                                      │
│                                ▼                                      │
│                    ┌───────────────────────┐                          │
│                    │     RerankerService    │                          │
│                    │  (Vertex AI Reranker)  │                          │
│                    └───────────┬───────────┘                          │
│                                │                                      │
└────────────────────────────────┼──────────────────────────────────────┘
                                 │
                                 ▼
                    ┌───────────────────────┐
                    │    Final Documents    │
                    │   (TopN, Reranked)    │
                    └───────────────────────┘
                                 │
                                 ▼
                    ┌───────────────────────┐
                    │   Context Injection   │
                    │  (System Prompt)      │
                    └───────────────────────┘
```

---

## 2. ハイブリッド検索

### 2.1 ベクトル検索

PGVector を使用したセマンティック検索。

```java
// Spring AI VectorStore を使用
List<Document> vectorResults = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(query)
        .topK(30)
        .similarityThreshold(0.6)
        .build()
);
```

### 2.2 キーワード検索

SQL ILIKE を使用した全文検索。

```java
// JdbcTemplate を使用してキーワードマッチング
List<Document> keywordResults = performKeywordSearch(query, 20);
```

### 2.3 Reciprocal Rank Fusion (RRF)

2つの検索結果を統合ランキング。

```
RRF_score(d) = Σ 1 / (k + rank(d))
```

- `k = 60` (定数パラメータ)
- 両方の検索で上位にランクされたドキュメントがより高いスコアを獲得

---

## 3. Reranker

### 3.1 概要

RRF 後の候補に対して、**Vertex AI Discovery Engine Ranking API** でセマンティックリランキングを実行。

### 3.2 実行条件

| 条件           | デフォルト値 |
| -------------- | ------------ |
| 有効化フラグ   | `true`       |
| 最小候補数閾値 | 10件以上     |
| タイムアウト   | 5000ms       |

候補数が閾値未満の場合はスキップ（コスト最適化）。

### 3.3 実装クラス

| クラス             | 役割                            |
| ------------------ | ------------------------------- |
| `Reranker`         | 共通インターフェース            |
| `VertexAiReranker` | Vertex AI Discovery Engine 連携 |
| `NoOpReranker`     | フォールバック（順序維持）      |
| `RerankerService`  | オーケストレーション            |

### 3.4 API仕様

```
POST https://discoveryengine.googleapis.com/v1/projects/{PROJECT_ID}/locations/{LOCATION}/rankingConfigs/default_ranking_config:rank

{
  "model": "semantic-ranker-default@latest",
  "query": "ユーザークエリ",
  "records": [
    {"id": "doc1", "content": "ドキュメント内容..."},
    ...
  ],
  "topN": 5
}
```

レスポンス例:

```json
{
  "records": [
    {"id": "doc3", "score": 0.89},
    {"id": "doc1", "score": 0.72},
    ...
  ]
}
```

---

## 4. スコープフィルタリング

ドキュメントの可視性はスコープで制御。

| スコープ | 可視性         |
| -------- | -------------- |
| `SYSTEM` | 全ユーザー     |
| `TENANT` | 同一テナント内 |
| `USER`   | 本人のみ       |

---

## 5. 設定

```yaml
mira:
  ai:
    reranker:
      enabled: true
      provider: vertex-ai
      model: semantic-ranker-default@latest
      top-n: 5
      min-candidates: 10
      timeout-ms: 5000

    vector:
      search-threshold: 0.6
```

---

## 6. 関連ドキュメント

- [layer-design.md](./layer-design.md) — レイヤ設計詳細
- [context-engineering-plan.md](../05_context-engineering/context-engineering-plan.md) — コンテキストエンジニアリング
