# MirelPlatform 本番デプロイガイド

## 概要

MirelPlatformを本番環境にデプロイする際の設定と考慮事項をまとめます。

## 必須環境変数

### データベース

```bash
DATABASE_URL=jdbc:postgresql://db-host:5432/mirelplatform
DATABASE_DRIVER=org.postgresql.Driver
DATABASE_USER=mirel_prod
DATABASE_PASS=<secure-password>
DATABASE_PLATFORM=org.hibernate.dialect.PostgreSQLDialect
```

### セキュリティ

```bash
JWT_SECRET=<32文字以上のランダム文字列>
```

### セッションストア（Redis推奨）

```bash
REDIS_HOST=redis-host
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password>
```

### ストレージ

```bash
MIREL_STORAGE_DIR=/var/lib/mirel/storage
```

## データベース初期化設定

```bash
# 初回のみシステムデータをシーディング（推奨）
MIREL_DB_INITIALIZE_MODE=ONCE

# サンプルデータは投入しない
MIREL_DB_SEED_SAMPLE=false
```

## CORS設定

```bash
CORS_ALLOWED_ORIGINS=https://your-domain.com,https://www.your-domain.com
```

## 設定ファイル

本番環境用の設定サンプルを参照してください：

- `backend/src/main/resources/config/application-prod.yml.sample`

## デプロイ手順

### 1. データベース準備

```sql
CREATE DATABASE mirelplatform;
CREATE USER mirel_prod WITH PASSWORD '<secure-password>';
GRANT ALL PRIVILEGES ON DATABASE mirelplatform TO mirel_prod;
```

### 2. ストレージディレクトリ準備

```bash
mkdir -p /var/lib/mirel/storage
chown mirel:mirel /var/lib/mirel/storage
chmod 750 /var/lib/mirel/storage
```

### 3. アプリケーション起動

```bash
java -jar mirelplatform.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

### 4. 初期セットアップ

[初期セットアップガイド](./INITIAL_SETUP.md)を参照してください。

## セキュリティチェックリスト

- [ ] JWT_SECRETは十分な長さ（32文字以上）でランダムに生成
- [ ] データベースパスワードは強力なものを使用
- [ ] CORS_ALLOWED_ORIGINSは必要最小限のドメインのみ許可
- [ ] Redisパスワードを設定
- [ ] ストレージディレクトリのパーミッションを適切に設定
- [ ] HTTPSを使用
- [ ] CSRFプロテクションを有効化

## 監視とログ

### ログレベル設定

本番環境では以下を推奨：

```yaml
logging:
  level:
    jp.vemi.mirel: INFO
    org.springframework: WARN
    org.springframework.security: WARN
```

### ヘルスチェック

```bash
curl https://your-domain.com/mipla2/actuator/health
```

## バックアップ

### データベース

定期的にpg_dumpでバックアップを取得してください。

```bash
pg_dump -h db-host -U mirel_prod mirelplatform > backup_$(date +%Y%m%d).sql
```

### ストレージ

ストレージディレクトリも定期的にバックアップしてください。

```bash
tar -czvf storage_backup_$(date +%Y%m%d).tar.gz /var/lib/mirel/storage
```

## トラブルシューティング

### アプリケーションが起動しない

1. データベース接続を確認
2. 環境変数が正しく設定されているか確認
3. ログを確認 (`--debug` オプションで詳細ログ出力)

### Bootstrap画面にアクセスできない

- 既にセットアップ完了済みの可能性があります
- `mir_system_migration` テーブルを確認してください
