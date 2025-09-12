#!/bin/bash

# モノレポ起動状況監視スクリプト
echo "=========================================="
echo "🚀 Mirel Platform モノレポ起動状況監視"
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
check_service "Frontend (Nuxt.js)" "8080" "npm.*dev"

# ログファイル確認
echo "📄 ログファイル状況:"
if [ -f "backend.log" ]; then
    echo "✅ backend.log: $(wc -l < backend.log) 行"
    echo "   最新: $(tail -n 1 backend.log 2>/dev/null | cut -c1-80)..."
else
    echo "❌ backend.log: 見つかりません"
fi

if [ -f "frontend.log" ]; then
    echo "✅ frontend.log: $(wc -l < frontend.log) 行"
    echo "   最新: $(tail -n 1 frontend.log 2>/dev/null | cut -c1-80)..."
else
    echo "❌ frontend.log: 見つかりません"
fi

echo ""

# アクセス先表示
echo "🌐 アクセス先:"
echo "   Frontend: http://localhost:8080/mirel"
echo "   ProMarker: http://localhost:8080/mirel/mste"  
echo "   Backend API: http://localhost:3000/mipla2"
echo ""

# 再起動コマンド表示
echo "🔧 手動起動コマンド:"
echo "   Backend:  SPRING_PROFILES_ACTIVE=dev SERVER_PORT=3000 ./gradlew :backend:bootRun"
echo "   Frontend: cd frontend && PORT=8080 npm run dev"
echo "=========================================="