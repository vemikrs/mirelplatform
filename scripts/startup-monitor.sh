#!/bin/bash

# モノレポ起動状況監視スクリプト

# プロジェクトルートに移動（scriptsフォルダから実行されることを想定）
PROJECT_ROOT="$(dirname "$0")"/.. 
cd "$PROJECT_ROOT"

echo "=========================================="
echo "🚀 mirelplatform モノレポ起動状況監視"
echo "=========================================="
echo "📅 $(date)"
echo ""

# 起動状況チェック関数
check_service() {
    local service_name=$1
    local port=$2
    local process_pattern=$3
    
    echo "🔍 ${service_name} (Port: ${port}) をチェック中..."
    
    # プロセス確認
    if pgrep -f "${process_pattern}" > /dev/null; then
        echo "✅ プロセス: 実行中"
    else
        echo "❌ プロセス: 停止中"
    fi
    
    # ポート確認
    if netstat -tuln 2>/dev/null | grep ":${port}" > /dev/null; then
        echo "✅ ポート ${port}: リスニング中"
    else
        echo "❌ ポート ${port}: 停止中"
    fi
    
    # HTTP確認（可能な場合）
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:${port}" | grep -q "200\|302\|404"; then
        echo "✅ HTTP: 応答あり"
    else
        echo "❌ HTTP: 応答なし"
    fi
    
    echo ""
}

# Backend チェック
check_service "Backend (Spring Boot)" "3000" "bootRun"

# Frontend チェック
check_service "Frontend v3 (Vite)" "5173" "vite"

# ログファイル確認
echo "📄 ログファイル状況:"
if [ -f "logs/backend.log" ]; then
    echo "✅ logs/backend.log: $(wc -l < logs/backend.log) 行"
    echo "   最新: $(tail -n 1 logs/backend.log 2>/dev/null | cut -c1-80)..."
else
    echo "❌ logs/backend.log: 見つかりません"
fi

if [ -f "logs/frontend.log" ]; then
    echo "✅ logs/frontend.log: $(wc -l < logs/frontend.log) 行"
    echo "   最新: $(tail -n 1 logs/frontend.log 2>/dev/null | cut -c1-80)..."
else
    echo "❌ logs/frontend.log: 見つかりません"
fi

echo ""

# アクセス先表示
echo "🌐 アクセス先:"
echo "   Frontend: http://localhost:5173/"
echo "   ProMarker (v3 UI 予定地): http://localhost:5173/"
echo "   Backend API: http://localhost:3000/mipla2"
echo ""

# 再起動コマンド表示
echo "🔧 手動起動コマンド:"
echo "   Backend:  SPRING_PROFILES_ACTIVE=dev SERVER_PORT=3000 ./gradlew :backend:bootRun > logs/backend.log 2>&1 &"
echo "   Frontend: pnpm --filter frontend-v3 dev > logs/frontend.log 2>&1 &  # または: (cd apps/frontend-v3 && npm run dev)"
echo "=========================================="