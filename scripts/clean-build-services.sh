#!/bin/# プロジェクトルートに移動（scriptsフォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/.. 
cd "$PROJECT_ROOT"

echo "🧩 Mirel Platform クリーンビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logsMirel Platform クリーンビルドスクリプト
# Backend と Frontend のクリーンとビルドを実行

set -e

echo "🧹 Mirel Platform クリーンビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

# プロジェクトルートに移動
cd /workspaces/mirelplatform

# Backend クリーンビルド
echo "🔧 Backend (Spring Boot) クリーンビルド中..."
echo "   タスク: ./gradlew clean backend:build"
echo "   ログ: logs/clean-build-backend.log"

if ./gradlew clean backend:build > logs/clean-build-backend.log 2>&1; then
    echo "✅ Backend クリーンビルド成功"
else
    echo "❌ Backend クリーンビルド失敗"
    echo "   エラーログ: logs/clean-build-backend.log"
    exit 1
fi

# Frontend クリーンビルド
echo ""
echo "🎨 Frontend (Nuxt.js) クリーンビルド中..."
echo "   作業ディレクトリ: frontend/"
echo "   ログ: logs/clean-build-frontend.log"

cd frontend

# node_modulesとdistをクリーン
echo "   🗑️  node_modules, dist, .nuxt をクリーン中..."
rm -rf node_modules dist .nuxt

# npm依存関係の再インストール
echo "   📦 npm依存関係を再インストール中..."
if npm install --legacy-peer-deps > ../logs/clean-build-frontend-install.log 2>&1; then
    echo "   ✅ npm install 成功"
else
    echo "   ❌ npm install 失敗"
    echo "   エラーログ: logs/clean-build-frontend-install.log"
    cd ..
    exit 1
fi

echo "   タスク: npm run build"
if npm run build > ../logs/clean-build-frontend.log 2>&1; then
    echo "✅ Frontend クリーンビルド成功"
    
    # ビルド結果の情報を表示
    if [ -d "dist" ]; then
        echo "   生成されたディストリビューション: frontend/dist"
        echo "   サイズ: $(du -sh dist 2>/dev/null | cut -f1 || echo 'N/A')"
    elif [ -d ".nuxt" ]; then
        echo "   Nuxtビルド成果物: frontend/.nuxt"
    fi
else
    echo "❌ Frontend クリーンビルド失敗"
    echo "   エラーログ: logs/clean-build-frontend.log"
    cd ..
    exit 1
fi

cd ..

echo ""
echo "✅ 全クリーンビルド完了!"
echo "======================================"
echo "📊 ビルド結果:"
echo "   Backend JAR: $(find backend/build/libs -name "*.jar" -not -name "*-plain.jar" | head -1)"
echo "   Frontend Dist: frontend/dist"
echo ""
echo "📋 ビルドログ:"
echo "   Backend:  logs/clean-build-backend.log"
echo "   Frontend: logs/clean-build-frontend.log"