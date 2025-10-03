#!/bin/bash

# Mirel Platform 一括起動スクリプト
# Backend (Spring Boot) と Frontend (Nuxt.js) を同時起動

echo "🚀 Mirel Platform サービス起動中..."
echo "======================================"

# プロジェクトルートに移動（scriptsフォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/.. 
cd "$PROJECT_ROOT"

# ログディレクトリの作成
mkdir -p logs

# 既存のプロセスを停止
echo "📋 既存プロセスの停止中..."
pkill -f "gradlew.*bootRun" 2>/dev/null || true
pkill -f "npm run dev" 2>/dev/null || true
pkill -f "nuxt" 2>/dev/null || true
sleep 2

# Backend起動
echo "🔧 Backend (Spring Boot) 起動中..."
echo "   ポート: 3000"
echo "   プロファイル: dev"
echo "   ログ: logs/backend.log"

# nohupを使ってプロセスをシェルから完全に切り離す
nohup bash -c "cd '$PROJECT_ROOT' && SPRING_PROFILES_ACTIVE=dev SERVER_PORT=3000 ./gradlew :backend:bootRun" > logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "   Backend PID: $BACKEND_PID"

# Frontend起動
echo "🎨 Frontend (Nuxt.js) 起動中..."
echo "   ポート: 8080"
echo "   ホスト: 0.0.0.0"
echo "   ログ: logs/frontend.log"

# npm依存関係の確認・インストール
if [ ! -d "frontend/node_modules" ]; then
    echo "   📦 npm依存関係をインストール中..."
    cd frontend
    npm install --legacy-peer-deps --no-audit
    cd ..
fi

# nohupを使ってプロセスをシェルから完全に切り離す
nohup bash -c "cd '$PROJECT_ROOT/frontend' && HOST=0.0.0.0 PORT=8080 NODE_OPTIONS='--no-deprecation' npm run dev" > logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "   Frontend PID: $FRONTEND_PID"

# プロセス情報を保存
echo "$BACKEND_PID" > logs/backend.pid
echo "$FRONTEND_PID" > logs/frontend.pid

echo ""
echo "✅ サービス起動完了!"
echo "======================================"
echo "🌐 アクセスURL:"
echo "   Frontend: http://localhost:8080/mirel/"
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

# バックグラウンドで起動状況を監視
{
    echo "起動状況を監視中..."
    for i in {1..180}; do
        sleep 1
        echo -n "."
        
        # Backend起動チェック (アプリのコンテキストパス配下)
        if curl -s http://localhost:3000/mipla2/actuator/health >/dev/null 2>&1; then
            if [ ! -f "logs/.backend_ready" ]; then
                echo "✅ Backend起動完了 (http://localhost:3000/mipla2)"
                touch logs/.backend_ready
            fi
        fi
        
        # Frontend起動チェック
        if curl -s http://localhost:8080/mirel/ >/dev/null 2>&1; then
            if [ ! -f "logs/.frontend_ready" ]; then
                echo "✅ Frontend起動完了 (http://localhost:8080/mirel/)"
                touch logs/.frontend_ready
            fi
        fi
        
        # 両方起動したら終了
        if [ -f "logs/.backend_ready" ] && [ -f "logs/.frontend_ready" ]; then
            echo ""
            echo "🎉 全サービスが正常に起動しました!"
            echo "ブラウザでアクセスできます。"
            exit 0
        fi
    done
    
    # タイムアウトした場合
    echo ""
    echo "⚠️  起動監視がタイムアウトしました（180秒）"
    echo "サービスは継続実行中です。ログを確認してください。"
} > logs/startup-monitor.log 2>&1 &

# バックグラウンドプロセスをシェルから切り離す
disown

echo "起動スクリプト実行完了。バックグラウンドで監視中..."
echo "監視状況: tail -f logs/startup-monitor.log"
echo ""
echo "このスクリプトは終了します。サービスは継続実行されます。"