#!/bin/bash

# mirelplatform 一括起動スクリプト
# Backend (Spring Boot) と Frontend v3 (Vite) を同時起動

echo "🚀 mirelplatform サービス起動中..."
echo "======================================"

# プロジェクトルートに移動（scriptsフォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/.. 
cd "$PROJECT_ROOT"

# ログディレクトリの作成
mkdir -p logs

# 既存のプロセスを停止
echo "📋 既存プロセスの停止中..."
pkill -f "gradlew.*bootRun" 2>/dev/null || true
pkill -f "pnpm --filter frontend-v3 dev" 2>/dev/null || true
pkill -f "vite" 2>/dev/null || true
pkill -f "nuxt" 2>/dev/null || true
sleep 2

# Backend起動
echo "🔧 Backend (Spring Boot) 起動中..."
echo "   ポート: 3000"
echo "   プロファイル: dev"
echo "   ログ: logs/backend.log"

# nohup を使ってプロセスを完全に切り離す（macOS/Linux両対応）
nohup bash -c "cd '$PROJECT_ROOT' && SPRING_PROFILES_ACTIVE=dev SERVER_PORT=3000 ./gradlew --console=plain :backend:bootRun" > logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "   Backend PID: $BACKEND_PID"

# Frontend 起動
echo "🎨 Frontend v3 (Vite) 起動中..."
echo "   ポート: 5173"
echo "   ホスト: 0.0.0.0"
echo "   ログ: logs/frontend.log"

# 依存関係インストール（必要時）
if [ ! -d "apps/frontend-v3/node_modules" ]; then
    echo "   📦 依存関係をインストール中..."
    if command -v pnpm >/dev/null 2>&1; then
        (cd apps/frontend-v3 && pnpm install) || true
    else
        (cd apps/frontend-v3 && npm ci --no-audit) || true
    fi
fi

# Vite 開発サーバ起動（nohup で完全に切り離し - macOS/Linux両対応）
if command -v pnpm >/dev/null 2>&1; then
    FRONTEND_CMD="cd '$PROJECT_ROOT/apps/frontend-v3' && pnpm dev"
else
    FRONTEND_CMD="cd '$PROJECT_ROOT/apps/frontend-v3' && npm run dev"
fi
nohup bash -c "$FRONTEND_CMD" > logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "   Frontend PID: $FRONTEND_PID"

# プロセス情報を保存
echo "$BACKEND_PID" > logs/backend.pid
echo "$FRONTEND_PID" > logs/frontend.pid

echo ""
echo "✅ サービス起動完了!"
echo "======================================"
echo "🌐 アクセスURL:"
echo "   Frontend: http://localhost:5173/"
echo "   Backend API: http://localhost:3000"
echo ""
echo "📊 リアルタイムログ監視コマンド:"
echo "   Backend:  tail -f logs/backend.log"
echo "   Frontend: tail -f logs/frontend.log"
echo "   両方:     tail -f logs/*.log"
echo ""
echo "🛑 サービス停止コマンド:"
echo "   ./stop-services.sh"
echo ""
echo "⏰ 起動完了まで約30-60秒お待ちください..."

# 起動状況ファイルのクリーンアップ
rm -f logs/.backend_ready logs/.frontend_ready

# 起動状況を監視（フォアグラウンドで実行）
echo ""
echo "起動状況を監視中..."
for i in {1..180}; do
    sleep 1
    echo -n "."
    
    # Backend起動チェック (アプリのコンテキストパス配下)
    if curl -s http://localhost:3000/mipla2/actuator/health >/dev/null 2>&1; then
        if [ ! -f "logs/.backend_ready" ]; then
            echo ""
            echo "✅ Backend起動完了 (http://localhost:3000/mipla2)"
            touch logs/.backend_ready
        fi
    fi
    
    # Frontend 起動チェック
    if curl -s http://localhost:5173/ >/dev/null 2>&1; then
        if [ ! -f "logs/.frontend_ready" ]; then
            echo ""
            echo "✅ Frontend起動完了 (http://localhost:5173/)"
            touch logs/.frontend_ready
        fi
    fi
    
    # 両方起動したら終了
    if [ -f "logs/.backend_ready" ] && [ -f "logs/.frontend_ready" ]; then
        echo ""
        echo "🎉 全サービスが正常に起動しました!"
        echo "======================================"
        echo "🌐 アクセス可能:"
        echo "   Frontend: http://localhost:5173/"
        echo "   Backend API: http://localhost:3000/mipla2"
        echo ""
        echo "📊 ログ監視:"
        echo "   tail -f logs/backend.log"
        echo "   tail -f logs/frontend.log"
        echo ""
        echo "このターミナルは閉じても問題ありません。"
        echo "サービスを停止するには: ./scripts/stop-services.sh"
        exit 0
    fi
done

# タイムアウトした場合
echo ""
echo "⚠️  起動監視がタイムアウトしました（180秒）"
echo "サービスは継続実行中です。ログを確認してください:"
echo "   tail -f logs/backend.log"
echo "   tail -f logs/frontend.log"
exit 1