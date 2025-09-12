#!/bin/bash

# Mirel Platform サービス停止スクリプト

echo "🛑 Mirel Platform サービス停止中..."
echo "======================================"

# PIDファイルから停止
if [ -f "logs/backend.pid" ]; then
    BACKEND_PID=$(cat logs/backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        echo "🔧 Backend停止中 (PID: $BACKEND_PID)"
        kill $BACKEND_PID
    fi
    rm -f logs/backend.pid
fi

if [ -f "logs/frontend.pid" ]; then
    FRONTEND_PID=$(cat logs/frontend.pid)
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        echo "🎨 Frontend停止中 (PID: $FRONTEND_PID)"
        kill $FRONTEND_PID
    fi
    rm -f logs/frontend.pid
fi

# 確実にプロセスを停止
echo "📋 関連プロセスの確認・停止中..."
pkill -f "gradlew.*bootRun" 2>/dev/null || true
pkill -f "npm run dev" 2>/dev/null || true
pkill -f "nuxt" 2>/dev/null || true

# 起動状態ファイルを削除
rm -f logs/.backend_ready logs/.frontend_ready

sleep 2

echo "✅ サービス停止完了!"
echo ""
echo "📊 最新のログを確認:"
echo "   Backend:  tail logs/backend.log"
echo "   Frontend: tail logs/frontend.log"