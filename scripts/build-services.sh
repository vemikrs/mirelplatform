#!/bin/bash

# mirelplatform ビルドスクリプト
# Backend (Spring Boot) と Frontend v3 (Vite) をビルド

set -e

# プロジェクトルートに移動（scriptsフォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/.. 
cd "$PROJECT_ROOT"

echo "🔨 mirelplatform ビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

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
echo "🎨 Frontend v3 (Vite) ビルド中..."
echo "   作業ディレクトリ: apps/frontend-v3/"
echo "   ログ: logs/build-frontend.log"

cd apps/frontend-v3

# 依存関係の確認・インストール（pnpm 優先、無ければ npm）
if [ ! -d "node_modules" ]; then
    echo "   📦 依存関係をインストール中..."
    if command -v pnpm >/dev/null 2>&1; then
        if pnpm install > ../../logs/build-frontend-install.log 2>&1; then
            echo "   ✅ pnpm install 成功"
        else
            echo "   ❌ pnpm install 失敗"
            echo "   エラーログ: logs/build-frontend-install.log"
            cd ../..
            exit 1
        fi
    else
        if npm ci --no-audit > ../../logs/build-frontend-install.log 2>&1; then
            echo "   ✅ npm ci 成功"
        else
            echo "   ❌ npm ci 失敗"
            echo "   エラーログ: logs/build-frontend-install.log"
            cd ../..
            exit 1
        fi
    fi
fi

echo "   タスク: build"
if command -v pnpm >/dev/null 2>&1; then
    BUILD_CMD="pnpm build"
else
    BUILD_CMD="npm run build"
fi

if bash -lc "$BUILD_CMD" > ../../logs/build-frontend.log 2>&1; then
    echo "✅ Frontend ビルド成功"
    
    # ビルド結果の情報を表示
    if [ -d "dist" ]; then
        echo "   生成されたディストリビューション: apps/frontend-v3/dist"
        echo "   サイズ: $(du -sh dist 2>/dev/null | cut -f1 || echo 'N/A')"
        echo "   ファイル数: $(find dist -type f 2>/dev/null | wc -l || echo '0')"
    fi
else
    echo "❌ Frontend ビルド失敗"
    echo "   エラーログ: logs/build-frontend.log"
    cd ../..
    exit 1
fi

cd ../..

echo ""
echo "✅ 全ビルド完了!"
echo "======================================"
echo "📊 ビルド結果:"
echo "   Backend JAR: $(find backend/build/libs -name "*.jar" -not -name "*-plain.jar" | head -1)"
echo "   Frontend Dist: apps/frontend-v3/dist"
echo ""
echo "📋 ビルドログ:"
echo "   Backend:  logs/build-backend.log"
echo "   Frontend: logs/build-frontend.log"
echo ""
echo "🚀 本番実行方法:"
echo "   Backend:  java -jar $(find backend/build/libs -name "*.jar" -not -name "*-plain.jar" | head -1)"
echo "   Frontend: 任意の静的サーバで apps/frontend-v3/dist を配信 (例: npx serve apps/frontend-v3/dist)"