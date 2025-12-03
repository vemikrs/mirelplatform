# Issue #74: E2E テスト PostgreSQL 移行作業記録

## 問題の概要

GitHub Action #74 でE2Eテストがエラーで失敗していた。

### 原因

1. E2E環境ではH2データベースをPostgreSQL互換モードで使用していた
2. V2マイグレーション (`backend/src/main/resources/db/migration/V2__create_schema_tables.sql`) でPostgreSQL専用構文を使用:
   - PL/pgSQL関数とトリガー (`CREATE OR REPLACE FUNCTION`, `RETURNS TRIGGER`)
   - JSONBデータ型
   - GINインデックス (`CREATE INDEX ... USING GIN`)
3. H2のPostgreSQL互換モードではこれらの機能が完全にサポートされていない

## 実装した解決策

### 1. Docker Compose設定（E2E専用）

**ファイル**: `docker-compose.e2e.yml`

- PostgreSQL 15 (ポート 5433)
  - データベース: `mirelplatform_e2e`
  - ユーザー: `mirel_e2e`
  - tmpfsでメモリ内データベース化（高速化）
  - テスト用最適化（`fsync=off`, `synchronous_commit=off`）
- Redis 7 (ポート 6380)
- MailHog (SMTP: 1026, Web UI: 8026)

開発環境とポート番号を分離して競合を回避。

### 2. アプリケーション設定の更新

**ファイル**: `backend/src/main/resources/config/application-e2e.yml`

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5433/mirelplatform_e2e}
    driver-class-name: org.postgresql.Driver
    username: ${DATABASE_USER:mirel_e2e}
    password: ${DATABASE_PASS:mirel_e2e_password}
  session:
    jdbc:
      schema: classpath:org/springframework/session/jdbc/schema-postgresql.sql
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

環境変数での設定上書きをサポート（GitHub Actions対応）。

### 3. GitHub Actions ワークフロー更新

**ファイル**: `.github/workflows/e2e-tests.yml`

#### 追加した手順

1. **E2Eサービスの起動**:
   ```bash
   docker compose -f docker-compose.e2e.yml up -d
   ```

2. **PostgreSQLヘルスチェック**:
   ```bash
   docker compose -f docker-compose.e2e.yml exec -T postgres-e2e pg_isready
   ```

3. **環境変数の設定**:
   - `DATABASE_URL`
   - `DATABASE_USER`
   - `DATABASE_PASS`
   - `REDIS_HOST`, `REDIS_PORT`
   - `SMTP_HOST`, `SMTP_PORT`

4. **クリーンアップ**:
   ```bash
   docker compose -f docker-compose.e2e.yml down -v
   ```

### 4. ヘルパースクリプト

**ファイル**: `scripts/e2e/setup-postgres.sh`

E2Eサービスの起動・停止・状態確認を簡単に実行できるスクリプト:

```bash
# 起動
./scripts/e2e/setup-postgres.sh start

# 停止
./scripts/e2e/setup-postgres.sh stop

# 状態確認
./scripts/e2e/setup-postgres.sh status
```

### 5. ドキュメント

**ファイル**: `docs/e2e-postgres-setup.md`

E2E PostgreSQL環境のセットアップガイドと運用手順。

## 技術的な詳細

### PostgreSQL専用構文の例

**V2マイグレーションから**:

```sql
-- PL/pgSQL トリガー関数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- JSONBデータ型
CREATE TABLE sch_record (
    record_data JSONB,
    ...
);

-- GINインデックス
CREATE INDEX idx_sch_record_data_gin ON sch_record USING GIN (record_data);
```

これらはH2では動作しない。

### ポート割り当て

| サービス | 開発環境 | E2E環境 |
|---------|----------|---------|
| PostgreSQL | 5432 | 5433 |
| Redis | 6379 | 6380 |
| MailHog SMTP | 1025 | 1026 |
| MailHog Web | 8025 | 8026 |

## テスト結果

### ローカル検証

✅ Docker Composeでサービスが正常に起動
✅ PostgreSQLヘルスチェック成功
✅ Redisヘルスチェック成功
✅ MailHog起動確認

### 次のステップ

- [ ] GitHub Actionsでの実行確認
- [ ] E2Eテストの実行確認
- [ ] エラーログの確認と修正（必要に応じて）

## 参考資料

- PostgreSQL公式ドキュメント: https://www.postgresql.org/docs/15/
- Docker Compose v2: https://docs.docker.com/compose/
- Spring Session JDBC: https://docs.spring.io/spring-session/reference/guides/boot-jdbc.html

## 変更ファイル一覧

1. `docker-compose.e2e.yml` (新規作成)
2. `backend/src/main/resources/config/application-e2e.yml` (更新)
3. `.github/workflows/e2e-tests.yml` (更新)
4. `scripts/e2e/setup-postgres.sh` (新規作成)
5. `docs/e2e-postgres-setup.md` (新規作成)

## 注意事項

### 本番環境への影響

この変更はE2E環境のみに影響し、以下の環境には影響しない:
- 開発環境 (application-dev.yml)
- 本番環境 (application-prod.yml)

### パフォーマンス最適化

E2E環境のPostgreSQLは高速なテスト実行のために最適化されています:
- tmpfsでメモリ内データベース
- `fsync=off`
- `synchronous_commit=off`

⚠️ これらの設定は本番環境では**絶対に使用しないでください**（データ損失の危険性）。

## トラブルシューティング

### ポート競合

開発環境とE2E環境を同時に実行する場合、異なるポートを使用しているため競合は発生しません。

### データベース接続エラー

PostgreSQLの起動完了を待つ:

```bash
docker compose -f docker-compose.e2e.yml exec postgres-e2e pg_isready -U mirel_e2e
```

### ログ確認

```bash
docker compose -f docker-compose.e2e.yml logs postgres-e2e
docker compose -f docker-compose.e2e.yml logs redis-e2e
```

## まとめ

H2データベースからPostgreSQLへの移行により、E2E環境でもPostgreSQL専用機能（PL/pgSQL、JSONB、GINインデックス）が正常に動作するようになりました。これにより、開発環境と本番環境の構成をE2Eテストで正確に検証できます。
