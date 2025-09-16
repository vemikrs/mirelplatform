#!/bin/# プロジェクトルートに移動（scriptsフォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/.. 
cd "$PROJECT_ROOT"

echo "🧩 Backend (Spring Boot) クリーンビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logsMirel Platform Backend クリーンビルドスクリプト
# Spring Boot アプリケーションのクリーン＆ビルド

set -e

echo "🧹 Backend (Spring Boot) クリーンビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

# プロジェクトルートに移動
cd /workspaces/mirelplatform

# クリーンビルド実行
echo "   タスク: ./gradlew clean backend:build"
echo "   出力: リアルタイム表示 + logs/backend-clean-build.log"
echo ""

# クリーンビルド実行（リアルタイム表示しつつログも保存）
if ./gradlew clean backend:build 2>&1 | tee logs/backend-clean-build.log; then
    echo ""
    echo "✅ Backend クリーンビルド成功!"
    
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
    echo "❌ Backend クリーンビルド失敗"
    echo "   詳細ログ: logs/backend-clean-build.log"
    exit 1
fi

echo ""
echo "======================================"
echo "クリーンビルド完了 $(date '+%Y-%m-%d %H:%M:%S')"