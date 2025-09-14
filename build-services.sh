#!/bin/bash

# Mirel Platform ビルドスクリプト
# Backend (Spring Boot) と Frontend (Nuxt.js) をビルド

set -e

echo "🔨 Mirel Platform ビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

# プロジェクトルートに移動
cd /workspaces/mirelplatform

# Backend ビルド
echo "🔧 Backend (Spring Boot) ビルド中..."
echo "   タスク: ./gradlew backend:build"
echo "   ログ: logs/build-backend.log"

if ./gradlew backend:build > logs/build-backend.log 2>&1; then
    echo "✅ Backend ビルド成功"
    
    # JARファイルの情報を表示
    JAR_FILE=$(find backend/build/libs -name "*.jar" -not -name "*-plain.jar" | head -1)
    if [ -n "$JAR_FILE" ]; then
        echo "   生成されたJAR: $JAR_FILE"
        echo "   サイズ: $(du -h "$JAR_FILE" | cut -f1)"
    fi
else
    echo "❌ Backend ビルド失敗"
    echo "   エラーログ: logs/build-backend.log"
    exit 1
fi

# Frontend ビルド
echo ""
echo "🎨 Frontend (Nuxt.js) ビルド中..."
echo "   作業ディレクトリ: frontend/"
echo "   ログ: logs/build-frontend.log"

cd frontend

# npm依存関係の確認・インストール
if [ ! -d "node_modules" ]; then
    echo "   📦 npm依存関係をインストール中..."
    if npm install --legacy-peer-deps > ../logs/build-frontend-install.log 2>&1; then
        echo "   ✅ npm install 成功"
    else
        echo "   ❌ npm install 失敗"
        echo "   エラーログ: logs/build-frontend-install.log"
        cd ..
        exit 1
    fi
fi

echo "   タスク: npm run build"
if npm run build > ../logs/build-frontend.log 2>&1; then
    echo "✅ Frontend ビルド成功"
    
    # ビルド結果の情報を表示
    if [ -d "dist" ]; then
        echo "   生成されたディストリビューション: frontend/dist"
        echo "   サイズ: $(du -sh dist 2>/dev/null | cut -f1 || echo 'N/A')"
        echo "   ファイル数: $(find dist -type f 2>/dev/null | wc -l || echo '0')"
    elif [ -d ".nuxt" ]; then
        echo "   Nuxtビルド成果物: frontend/.nuxt"
    fi
else
    echo "❌ Frontend ビルド失敗"
    echo "   エラーログ: logs/build-frontend.log"
    cd ..
    exit 1
fi

cd ..

echo ""
echo "✅ 全ビルド完了!"
echo "======================================"
echo "📊 ビルド結果:"
echo "   Backend JAR: $(find backend/build/libs -name "*.jar" -not -name "*-plain.jar" | head -1)"
echo "   Frontend Dist: frontend/dist"
echo ""
echo "📋 ビルドログ:"
echo "   Backend:  logs/build-backend.log"
echo "   Frontend: logs/build-frontend.log"
echo ""
echo "🚀 本番実行方法:"
echo "   Backend:  java -jar $(find backend/build/libs -name "*.jar" -not -name "*-plain.jar" | head -1)"
echo "   Frontend: npm run start (frontend/dist から配信)"