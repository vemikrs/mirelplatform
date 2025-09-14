#!/bin/bash

# Mirel Platform Frontend ビルドスクリプト
# Nuxt.js アプリケーションのビルド

set -e

echo "🎨 Frontend (Nuxt.js) ビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

# frontendディレクトリに移動
cd /workspaces/mirelplatform/frontend

# npm依存関係の確認・インストール
if [ ! -d "node_modules" ]; then
    echo "📦 npm依存関係をインストール中..."
    echo "   タスク: npm install --legacy-peer-deps"
    echo "   出力: リアルタイム表示 + logs/frontend-install.log"
    echo ""
    
    if npm install --legacy-peer-deps --no-audit 2>&1 | tee ../logs/frontend-install.log; then
        if [ "$?" -eq 0 ] && [ -d "node_modules" ]; then
            echo ""
            echo "✅ npm install 成功"
        else
            echo ""
            echo "❌ npm install 失敗 (フォルダ作成失敗)"
            echo "   詳細ログ: logs/frontend-install.log"
            exit 1
        fi
    else
        echo ""
        echo "❌ npm install 失敗"
        echo "   詳細ログ: logs/frontend-install.log"
        exit 1
    fi
    echo ""
fi

# Frontend ビルド実行
echo "🏗️  Nuxt.js ビルド実行中..."
echo "   タスク: npm run build"
echo "   出力: リアルタイム表示 + logs/frontend-build.log"
echo ""

# ビルド実行（リアルタイム表示しつつログも保存）
if npm run build 2>&1 | tee ../logs/frontend-build.log; then
    echo ""
    echo "✅ Frontend ビルド成功!"
    
    # ビルド結果の情報を表示
    echo "📦 ビルド結果:"
    if [ -d "dist" ]; then
        echo "   パス: frontend/dist (静的生成)"
        echo "   サイズ: $(du -sh dist 2>/dev/null | cut -f1 || echo 'N/A')"
        echo "   ファイル数: $(find dist -type f 2>/dev/null | wc -l || echo '0')"
    elif [ -d ".nuxt" ]; then
        echo "   パス: frontend/.nuxt (SSR/SPA)"
        echo "   サイズ: $(du -sh .nuxt 2>/dev/null | cut -f1 || echo 'N/A')"
    fi
    
    echo ""
    echo "🚀 実行方法:"
    if [ -d "dist" ]; then
        echo "   静的ホスティング: frontend/dist フォルダを配信"
        echo "   ローカル確認: npx serve dist"
    else
        echo "   開発サーバー: npm run dev"
        echo "   本番サーバー: npm run start"
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