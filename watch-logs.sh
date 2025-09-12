#!/bin/bash

# Mirel Platform ログ監視スクリプト

echo "📊 Mirel Platform ログ監視"
echo "======================================"
echo "Ctrl+C で監視終了"
echo ""

# ログファイルが存在するかチェック
if [ ! -f "logs/backend.log" ] && [ ! -f "logs/frontend.log" ]; then
    echo "❌ ログファイルが見つかりません。"
    echo "先に ./start-services.sh を実行してください。"
    exit 1
fi

# 引数に応じてログを表示
case "${1:-all}" in
    "backend" | "be")
        echo "🔧 Backend ログ監視中..."
        echo "======================================"
        tail -f logs/backend.log
        ;;
    "frontend" | "fe")
        echo "🎨 Frontend ログ監視中..."
        echo "======================================"
        tail -f logs/frontend.log
        ;;
    "all" | *)
        echo "🔧🎨 Backend & Frontend ログ監視中..."
        echo "======================================"
        # 色付きでログを区別
        (tail -f logs/backend.log | sed 's/^/[BACKEND] /' | while read line; do echo -e "\033[34m$line\033[0m"; done) &
        (tail -f logs/frontend.log | sed 's/^/[FRONTEND] /' | while read line; do echo -e "\033[32m$line\033[0m"; done) &
        
        # Ctrl+C で両方を停止
        trap 'kill $(jobs -p) 2>/dev/null; exit' INT TERM
        wait
        ;;
esac