#!/bin/bash

# frontend-v3 (Vite) のクリーンビルドスクリプト

set -e

# プロジェクトルートに移動（scripts フォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/..
cd "$PROJECT_ROOT"

echo "🧩 Frontend v3 (Vite) クリーンビルド開始..."
echo "======================================"

# ログディレクトリの作成
mkdir -p logs

# 作業ディレクトリへ
cd apps/frontend-v3

# クリーン実行
echo "🗑️  既存ファイルをクリーン中..."
echo "   削除対象: node_modules, dist"
rm -rf node_modules dist
echo "✅ クリーン完了"
echo ""

# 依存関係の再インストール（pnpm 優先、無ければ npm）
echo "📦 依存関係を再インストール中..."
echo "   出力: リアルタイム表示 + logs/frontend-clean-install.log"
echo ""
if command -v pnpm >/dev/null 2>&1; then
    pnpm install 2>&1 | tee ../../logs/frontend-clean-install.log
else
    npm ci --no-audit 2>&1 | tee ../../logs/frontend-clean-install.log
fi

# Frontend ビルド実行
echo ""
echo "🏗️  Vite ビルド実行中..."
echo "   出力: リアルタイム表示 + logs/frontend-clean-build.log"
echo ""

if command -v pnpm >/dev/null 2>&1; then
    pnpm build 2>&1 | tee ../../logs/frontend-clean-build.log
else
    npm run build 2>&1 | tee ../../logs/frontend-clean-build.log
fi

echo ""
echo "✅ Frontend クリーンビルド成功!"
echo "📦 ビルド結果:"
if [ -d "dist" ]; then
    echo "   パス: apps/frontend-v3/dist"
    echo "   サイズ: $(du -sh dist 2>/dev/null | cut -f1 || echo 'N/A')"
    echo "   ファイル数: $(find dist -type f 2>/dev/null | wc -l || echo '0')"
fi

echo ""
echo "======================================"
echo "クリーンビルド完了 $(date '+%Y-%m-%d %H:%M:%S')"