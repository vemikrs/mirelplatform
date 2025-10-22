#!/bin/bash
# プロジェクトルートに移動（scriptsフォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/..
cd "$PROJECT_ROOT"

echo "🧩 Mirel Platform クリーンビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs
# Backend と Frontend のクリーンとビルドを実行

set -e

echo "🧹 Mirel Platform クリーンビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

# プロジェクトルートに移動（既に移動済み）
cd "$PROJECT_ROOT"

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
echo "🎨 Frontend v3 (Vite) クリーンビルド中..."
echo "   作業ディレクトリ: apps/frontend-v3/"
echo "   ログ: logs/clean-build-frontend.log"

cd apps/frontend-v3

# node_modules と dist をクリーン
echo "   🗑️  node_modules, dist をクリーン中..."
rm -rf node_modules dist

# 依存関係の再インストール
echo "   📦 依存関係を再インストール中..."
if command -v pnpm >/dev/null 2>&1; then
    if pnpm install > ../../logs/clean-build-frontend-install.log 2>&1; then
        echo "   ✅ pnpm install 成功"
    else
        echo "   ❌ pnpm install 失敗"
        echo "   エラーログ: logs/clean-build-frontend-install.log"
        cd ../..
        exit 1
    fi
else
    if npm ci --no-audit > ../../logs/clean-build-frontend-install.log 2>&1; then
        echo "   ✅ npm ci 成功"
    else
        echo "   ❌ npm ci 失敗"
        echo "   エラーログ: logs/clean-build-frontend-install.log"
        cd ../..
        exit 1
    fi
fi

echo "   タスク: build"
if command -v pnpm >/dev/null 2>&1; then
    BUILD_CMD="pnpm build"
else
    BUILD_CMD="npm run build"
fi

if bash -lc "$BUILD_CMD" > ../../logs/clean-build-frontend.log 2>&1; then
    echo "✅ Frontend クリーンビルド成功"
    # ビルド結果の情報を表示
    if [ -d "dist" ]; then
        echo "   生成されたディストリビューション: apps/frontend-v3/dist"
        echo "   サイズ: $(du -sh dist 2>/dev/null | cut -f1 || echo 'N/A')"
    fi
else
    echo "❌ Frontend クリーンビルド失敗"
    echo "   エラーログ: logs/clean-build-frontend.log"
    cd ../..
    exit 1
fi

cd ../..

echo ""
echo "✅ 全クリーンビルド完了!"
echo "======================================"
echo "📊 ビルド結果:"
echo "   Backend JAR: $(find backend/build/libs -name "*.jar" -not -name "*-plain.jar" | head -1)"
echo "   Frontend Dist: apps/frontend-v3/dist"
echo ""
echo "📋 ビルドログ:"
echo "   Backend:  logs/clean-build-backend.log"
echo "   Frontend: logs/clean-build-frontend.log"