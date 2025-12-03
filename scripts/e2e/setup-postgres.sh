#!/bin/bash

# E2E PostgreSQL セットアップスクリプト
# E2Eテスト用にPostgreSQL、Redis、MailHogを起動
# Usage: ./scripts/e2e/setup-postgres.sh [start|stop|restart|status]

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Get script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.e2e.yml"

# Function to check if services are running
check_services_status() {
    if docker-compose -f "$COMPOSE_FILE" ps | grep -q "Up"; then
        return 0
    else
        return 1
    fi
}

# Function to wait for PostgreSQL to be ready
wait_for_postgres() {
    print_color "$YELLOW" "⏳ PostgreSQL起動待機中..."
    for i in {1..30}; do
        if docker-compose -f "$COMPOSE_FILE" exec -T postgres-e2e pg_isready -U mirel_e2e -d mirelplatform_e2e &>/dev/null; then
            print_color "$GREEN" "✅ PostgreSQL準備完了!"
            return 0
        fi
        echo -n "."
        sleep 2
    done
    print_color "$RED" "❌ PostgreSQL起動タイムアウト"
    docker-compose -f "$COMPOSE_FILE" logs postgres-e2e
    return 1
}

# Function to wait for Redis to be ready
wait_for_redis() {
    print_color "$YELLOW" "⏳ Redis起動待機中..."
    for i in {1..15}; do
        if docker-compose -f "$COMPOSE_FILE" exec -T redis-e2e redis-cli ping 2>/dev/null | grep -q PONG; then
            print_color "$GREEN" "✅ Redis準備完了!"
            return 0
        fi
        echo -n "."
        sleep 1
    done
    print_color "$RED" "❌ Redis起動タイムアウト"
    docker-compose -f "$COMPOSE_FILE" logs redis-e2e
    return 1
}

# Function to start services
start_services() {
    print_color "$BLUE" "🚀 E2Eサービス起動中..."
    
    # Check if already running
    if check_services_status; then
        print_color "$YELLOW" "⚠️  サービスは既に起動しています"
        show_status
        return 0
    fi
    
    # Start services
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose.e2e.yml up -d
    
    # Wait for services to be ready
    if ! wait_for_postgres; then
        print_color "$RED" "❌ PostgreSQL起動失敗"
        return 1
    fi
    
    if ! wait_for_redis; then
        print_color "$RED" "❌ Redis起動失敗"
        return 1
    fi
    
    print_color "$GREEN" "✅ すべてのE2Eサービスが起動しました"
    show_status
}

# Function to stop services
stop_services() {
    print_color "$BLUE" "🛑 E2Eサービス停止中..."
    
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose.e2e.yml down -v
    
    print_color "$GREEN" "✅ E2Eサービスを停止しました"
}

# Function to restart services
restart_services() {
    print_color "$BLUE" "🔄 E2Eサービス再起動中..."
    stop_services
    sleep 2
    start_services
}

# Function to show service status
show_status() {
    print_color "$BLUE" "📊 E2Eサービス状態:"
    cd "$PROJECT_ROOT"
    docker-compose -f docker-compose.e2e.yml ps
    
    echo ""
    print_color "$BLUE" "📝 接続情報:"
    print_color "$YELLOW" "  PostgreSQL:"
    print_color "$NC" "    Host: localhost"
    print_color "$NC" "    Port: 5433"
    print_color "$NC" "    Database: mirelplatform_e2e"
    print_color "$NC" "    User: mirel_e2e"
    print_color "$NC" "    Password: mirel_e2e_password"
    print_color "$NC" "    JDBC URL: jdbc:postgresql://localhost:5433/mirelplatform_e2e"
    
    print_color "$YELLOW" "  Redis:"
    print_color "$NC" "    Host: localhost"
    print_color "$NC" "    Port: 6380"
    
    print_color "$YELLOW" "  MailHog:"
    print_color "$NC" "    SMTP: localhost:1026"
    print_color "$NC" "    Web UI: http://localhost:8026"
}

# Main script logic
case "${1:-start}" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        restart_services
        ;;
    status)
        show_status
        ;;
    *)
        print_color "$RED" "使用方法: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
