#!/bin/bash

# Mirel Platform Backend ビルドスクリプト
# Spring Boot アプリケーションのビルド

set -e

echo "🔧 Backend (Spring Boot) ビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

# プロジェクトルートに移動
cd /workspaces/mirelplatform

# Backend ビルド実行
echo "   タスク: ./gradlew backend:build"
echo "   出力: リアルタイム表示 + logs/backend-build.log"
echo ""

# ビルド実行（リアルタイム表示しつつログも保存）
if ./gradlew backend:build 2>&1 | tee logs/backend-build.log; then
    echo ""
    echo "✅ Backend ビルド成功!"
    
    # JARファイルの情報を表示
    JAR_FILE=$(find backend/build/libs -name "*.jar" -not -name "*-plain.jar" 2>/dev/null | head -1)
    if [ -n "$JAR_FILE" ]; then
        echo "📦 生成されたJAR:"
        echo "   パス: $JAR_FILE"
        echo "   サイズ: $(du -h "$JAR_FILE" | cut -f1)"
        echo "   作成日時: $(date -r "$JAR_FILE" '+%Y-%m-%d %H:%M:%S')"
        echo ""
        echo "🚀 実行方法:"
        echo "   java -jar $JAR_FILE"
        echo "   または"
        echo "   ./gradlew backend:bootRun"
    fi
else
    echo ""
    echo "❌ Backend ビルド失敗"
    echo "   詳細ログ: logs/backend-build.log"
    exit 1
fi

echo ""
echo "======================================"
echo "ビルド完了 $(date '+%Y-%m-%d %H:%M:%S')"