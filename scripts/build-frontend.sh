#!/bin/bash

# Mirel Platform Frontend ビルドスクリプト
# frontend-v3 (Vite + React) のビルド

set -e

# プロジェクトルートに移動（scriptsフォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/.. 
cd "$PROJECT_ROOT"

echo "🎨 Frontend v3 (Vite) ビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

# apps/frontend-v3 ディレクトリに移動
cd apps/frontend-v3

# npm依存関係の確認・インストール
if [ ! -d "node_modules" ]; then
    echo "📦 依存関係をインストール中..."
    echo "   出力: リアルタイム表示 + logs/frontend-install.log"
    echo ""
    if command -v pnpm >/dev/null 2>&1; then
        if pnpm install 2>&1 | tee ../../logs/frontend-install.log; then
            echo ""
            echo "✅ pnpm install 成功"
        else
            echo ""
            echo "❌ pnpm install 失敗"
            echo "   詳細ログ: logs/frontend-install.log"
            exit 1
        fi
    else
        if npm ci --no-audit 2>&1 | tee ../../logs/frontend-install.log; then
            echo ""
            echo "✅ npm ci 成功"
        else
            echo ""
            echo "❌ npm ci 失敗"
            echo "   詳細ログ: logs/frontend-install.log"
            exit 1
        fi
    fi
    echo ""
fi

# Frontend ビルド実行
echo "🏗️  Vite ビルド実行中..."
echo "   タスク: build"
echo "   出力: リアルタイム表示 + logs/frontend-build.log"
echo ""

# ビルド実行（リアルタイム表示しつつログも保存）
if command -v pnpm >/dev/null 2>&1; then
    BUILD_CMD="pnpm build"
else
    BUILD_CMD="npm run build"
fi

if bash -lc "$BUILD_CMD" 2>&1 | tee ../../logs/frontend-build.log; then
    echo ""
    echo "✅ Frontend ビルド成功!"
    
    # ビルド結果の情報を表示
    echo "📦 ビルド結果:"
    if [ -d "dist" ]; then
        echo "   パス: apps/frontend-v3/dist (静的生成)"
        echo "   サイズ: $(du -sh dist 2>/dev/null | cut -f1 || echo 'N/A')"
        echo "   ファイル数: $(find dist -type f 2>/dev/null | wc -l || echo '0')"
    fi
    
    echo ""
    echo "🚀 実行方法:"
    if [ -d "dist" ]; then
        echo "   静的ホスティング: apps/frontend-v3/dist フォルダを配信"
        echo "   ローカル確認: npx serve dist"
    fi
else
    echo ""
    echo "❌ Frontend ビルド失敗"
    echo "   詳細ログ: logs/frontend-build.log"
    exit 1
fi

echo ""
echo "======================================"
echo "ビルド完了 $(date '+%Y-%m-%d %H:%M:%S')"