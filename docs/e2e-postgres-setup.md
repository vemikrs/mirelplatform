# E2E PostgreSQL セットアップガイド

## 概要

E2Eテスト環境では、PostgreSQL専用構文（PL/pgSQL、JSONB、GINインデックスなど）を使用するため、H2データベースではなくPostgreSQLを使用します。

## 背景

- V2マイグレーション (`V2__create_schema_tables.sql`) でPostgreSQL専用機能を使用
  - PL/pgSQL関数とトリガー
  - JSONBデータ型
  - GINインデックス
- H2のPostgreSQL互換モードでは完全な互換性がなく、エラーが発生

## アーキテクチャ

```
E2E Test Environment (docker-compose.e2e.yml)
├── PostgreSQL (port 5433)
│   └── Database: mirelplatform_e2e
├── Redis (port 6380)
└── MailHog
    ├── SMTP: port 1026
    └── Web UI: port 8026
```

## セットアップ手順

### 1. E2Eサービスの起動

```bash
# プロジェクトルートから
./scripts/e2e/setup-postgres.sh start
```

または

```bash
docker compose -f docker-compose.e2e.yml up -d
```

### 2. サービス状態の確認

```bash
./scripts/e2e/setup-postgres.sh status
```

### 3. E2Eテストの実行

E2Eサービスが起動した状態で:

```bash
cd packages/e2e
pnpm exec playwright test
```

### 4. サービスの停止

```bash
./scripts/e2e/setup-postgres.sh stop
```

または

```bash
docker compose -f docker-compose.e2e.yml down -v
```

## 接続情報

### PostgreSQL

```
Host: localhost
Port: 5433
Database: mirelplatform_e2e
User: mirel_e2e
Password: mirel_e2e_password
JDBC URL: jdbc:postgresql://localhost:5433/mirelplatform_e2e
```

### Redis

```
Host: localhost
Port: 6380
```

### MailHog

```
SMTP: localhost:1026
Web UI: http://localhost:8026
```

## 環境変数

バックエンドアプリケーション起動時に以下の環境変数を設定:

```bash
SPRING_PROFILES_ACTIVE=e2e
SERVER_PORT=3000
DATABASE_URL=jdbc:postgresql://localhost:5433/mirelplatform_e2e
DATABASE_USER=mirel_e2e
DATABASE_PASS=mirel_e2e_password
REDIS_HOST=localhost
REDIS_PORT=6380
SMTP_HOST=localhost
SMTP_PORT=1026
```

## GitHub Actions

GitHub Actions ワークフロー (`.github/workflows/e2e-tests.yml`) では、以下の手順で実行:

1. Docker Compose で E2E サービス起動
2. PostgreSQL/Redis のヘルスチェック
3. バックエンド・フロントエンド起動
4. E2E テスト実行
5. サービス停止とクリーンアップ

## トラブルシューティング

### ポートが既に使用されている

開発環境のサービスとポート競合を避けるため、E2E環境は異なるポートを使用:
- PostgreSQL: 5433 (dev: 5432)
- Redis: 6380 (dev: 6379)
- MailHog SMTP: 1026 (dev: 1025)
- MailHog Web: 8026 (dev: 8025)

### データベース接続エラー

PostgreSQL が起動完了するまで待つ:

```bash
# ヘルスチェック
docker compose -f docker-compose.e2e.yml exec postgres-e2e pg_isready -U mirel_e2e
```

### サービスログの確認

```bash
# 全サービスのログ
docker compose -f docker-compose.e2e.yml logs

# 特定サービスのログ
docker compose -f docker-compose.e2e.yml logs postgres-e2e
docker compose -f docker-compose.e2e.yml logs redis-e2e
```

## パフォーマンス最適化

E2E環境のPostgreSQLは高速なテスト実行のために最適化:

- `tmpfs` でデータをメモリに保存
- `fsync=off` で同期書き込みを無効化
- `synchronous_commit=off` で同期コミットを無効化

⚠️ これらの設定は本番環境では使用しないでください。

## 関連ファイル

- `docker-compose.e2e.yml` - E2E環境のDocker Compose設定
- `backend/src/main/resources/config/application-e2e.yml` - E2E環境のSpring設定
- `.github/workflows/e2e-tests.yml` - GitHub Actions ワークフロー
- `scripts/e2e/setup-postgres.sh` - E2Eサービス管理スクリプト
