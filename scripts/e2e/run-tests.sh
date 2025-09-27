#!/bin/bash

# ProMarker E2E テスト実行スクリプト
# Usage: ./scripts/e2e/run-tests.sh [options]

set -e

# Default values
BROWSER="chromium"
HEADED=false
DEBUG=false
UI_MODE=false
UPDATE_SNAPSHOTS=false
SERVICES_RUNNING=false

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

# Function to show usage
show_usage() {
    cat << EOF
ProMarker E2E テスト実行スクリプト

使用方法:
    $0 [options]

オプション:
    -b, --browser BROWSER     ブラウザを指定 (chromium|firefox|webkit) [default: chromium]
    -h, --headed             ヘッドモードで実行 (ブラウザウィンドウを表示)
    -d, --debug              デバッグモードで実行
    -u, --ui                 UIモードで実行
    -s, --update-snapshots   スナップショットを更新
    -r, --services-running   サービスが既に起動している場合
    --help                   このヘルプを表示

例:
    $0                           # デフォルト設定で実行
    $0 -b firefox -h            # Firefoxのヘッドモードで実行
    $0 -d                       # デバッグモードで実行
    $0 -u                       # UIモードで実行
    $0 -s                       # スナップショット更新モード
    $0 -r                       # サービス起動済みの場合
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -b|--browser)
            BROWSER="$2"
            shift 2
            ;;
        -h|--headed)
            HEADED=true
            shift
            ;;
        -d|--debug)
            DEBUG=true
            shift
            ;;
        -u|--ui)
            UI_MODE=true
            shift
            ;;
        -s|--update-snapshots)
            UPDATE_SNAPSHOTS=true
            shift
            ;;
        -r|--services-running)
            SERVICES_RUNNING=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            print_color "$RED" "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate browser
if [[ ! "$BROWSER" =~ ^(chromium|firefox|webkit)$ ]]; then
    print_color "$RED" "Invalid browser: $BROWSER. Must be one of: chromium, firefox, webkit"
    exit 1
fi

print_color "$BLUE" "🚀 ProMarker E2E テスト開始"
print_color "$YELLOW" "ブラウザ: $BROWSER"
print_color "$YELLOW" "設定: headed=$HEADED, debug=$DEBUG, ui=$UI_MODE, update-snapshots=$UPDATE_SNAPSHOTS"

# Check if we're in the right directory
if [[ ! -f "playwright.config.ts" ]]; then
    print_color "$RED" "❌ playwright.config.ts が見つかりません。プロジェクトルートで実行してください。"
    exit 1
fi

# Install dependencies if needed
if [[ ! -d "node_modules" ]]; then
    print_color "$YELLOW" "📦 依存関係をインストール中..."
    npm install
fi

# Install Playwright browsers if needed
if [[ ! -d "node_modules/@playwright/test" ]]; then
    print_color "$YELLOW" "🎭 Playwright をインストール中..."
    npx playwright install --with-deps "$BROWSER"
fi

# Create test results directory
mkdir -p test-results/screenshots

# Start services if not already running
if [[ "$SERVICES_RUNNING" == false ]]; then
    print_color "$YELLOW" "🔧 サービス起動中..."
    
    # Check if services are already running
    if curl -s -f http://localhost:8080/mirel/ > /dev/null 2>&1 && \
       curl -s -f http://localhost:3000/actuator/health > /dev/null 2>&1; then
        print_color "$GREEN" "✅ サービスは既に起動しています"
        SERVICES_RUNNING=true
    else
        # Start services
        ./scripts/start-services.sh
        
        # Wait for services to be ready
        print_color "$YELLOW" "⏳ サービス起動待機中..."
        timeout 120 bash -c 'until curl -s -f http://localhost:3000/actuator/health > /dev/null 2>&1; do sleep 2; done' || {
            print_color "$RED" "❌ Backend サービスの起動に失敗しました"
            exit 1
        }
        
        timeout 120 bash -c 'until curl -s -f http://localhost:8080/mirel/ > /dev/null 2>&1; do sleep 2; done' || {
            print_color "$RED" "❌ Frontend サービスの起動に失敗しました"
            exit 1
        }
        
        print_color "$GREEN" "✅ サービス起動完了"
        SERVICES_RUNNING=true
    fi
fi

# Build test command
TEST_CMD="npx playwright test --project=$BROWSER"

if [[ "$HEADED" == true ]]; then
    TEST_CMD="$TEST_CMD --headed"
fi

if [[ "$DEBUG" == true ]]; then
    TEST_CMD="$TEST_CMD --debug"
fi

if [[ "$UI_MODE" == true ]]; then
    TEST_CMD="$TEST_CMD --ui"
fi

if [[ "$UPDATE_SNAPSHOTS" == true ]]; then
    TEST_CMD="$TEST_CMD --update-snapshots"
fi

print_color "$BLUE" "🧪 テスト実行: $TEST_CMD"

# Run tests
if eval "$TEST_CMD"; then
    print_color "$GREEN" "✅ テスト完了"
    
    # Show report location
    if [[ -d "playwright-report" ]]; then
        print_color "$BLUE" "📊 レポート: playwright-report/index.html"
        print_color "$BLUE" "    ローカルサーバで確認: npx playwright show-report"
    fi
    
    # Show screenshots location
    if [[ -d "test-results/screenshots" ]] && [[ -n "$(ls -A test-results/screenshots 2>/dev/null)" ]]; then
        print_color "$BLUE" "📸 スクリーンショット: test-results/screenshots/"
    fi
    
    exit 0
else
    print_color "$RED" "❌ テスト失敗"
    
    # Show failure artifacts
    if [[ -d "test-results" ]]; then
        print_color "$YELLOW" "🔍 失敗時の情報:"
        print_color "$YELLOW" "    スクリーンショット: test-results/screenshots/"
        print_color "$YELLOW" "    ビデオ: test-results/"
        print_color "$YELLOW" "    トレース: test-results/"
    fi
    
    exit 1
fi