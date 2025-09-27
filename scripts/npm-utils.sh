#!/bin/bash

# Mirel Platform npm utility functions
# npm install の共通処理とエラーハンドリング

# 色付きメッセージ関数
print_info() {
    echo -e "\033[36m$1\033[0m"  # シアン色
}

print_success() {
    echo -e "\033[32m$1\033[0m"  # 緑色
}

print_warning() {
    echo -e "\033[33m$1\033[0m"  # 黄色
}

print_error() {
    echo -e "\033[31m$1\033[0m"  # 赤色
}

# Node.js環境をチェックする関数
check_node_environment() {
    print_info "🔍 Node.js環境をチェック中..."
    
    NODE_VERSION=$(node -v | sed 's/v//')
    NPM_VERSION=$(npm -v)
    
    echo "   Node.js: $NODE_VERSION"
    echo "   npm: $NPM_VERSION"
    
    # Node.js 16以上を推奨
    if ! node -e "process.exit(process.version.split('.')[0].slice(1) >= 16 ? 0 : 1)" 2>/dev/null; then
        print_warning "⚠️  Node.js 16以上を推奨します (現在: $NODE_VERSION)"
    else
        print_success "✅ Node.js バージョン OK"
    fi
    
    # npm キャッシュをクリア（古い依存関係の問題を回避）
    print_info "   npm キャッシュをクリア中..."
    npm cache clean --force >/dev/null 2>&1 || true
    echo ""
}

# npm依存関係をインストールする強化版関数
install_npm_dependencies() {
    local log_file="$1"
    local context_name="${2:-npm install}"
    
    print_info "📦 npm依存関係をインストール中... ($context_name)"
    
    # node_modulesが既に存在する場合はスキップ
    if [ -d "node_modules" ]; then
        print_success "✅ node_modules が既に存在します (スキップ)"
        return 0
    fi
    
    # ログファイルが指定されている場合
    if [ -n "$log_file" ]; then
        echo "   出力: リアルタイム表示 + $log_file"
        echo ""
        
        # まず --legacy-peer-deps で試行
        if npm install --legacy-peer-deps --no-audit --no-fund 2>&1 | tee "$log_file"; then
            if [ -d "node_modules" ]; then
                print_success "✅ npm install 成功 (legacy-peer-deps)"
                return 0
            else
                print_error "❌ npm install 失敗 (node_modules フォルダ未作成)"
                return 1
            fi
        fi
        
        # 失敗したら --force で試行
        print_warning "⚠️  legacy-peer-deps で失敗、--force で再試行..."
        if npm install --force --no-audit --no-fund 2>&1 | tee -a "$log_file"; then
            if [ -d "node_modules" ]; then
                print_success "✅ npm install 成功 (force)"
                return 0
            else
                print_error "❌ npm install 失敗 (node_modules フォルダ未作成)"
                return 1
            fi
        fi
    else
        # ログファイルなしの場合（簡易版）
        # まず --legacy-peer-deps で試行
        if npm install --legacy-peer-deps --no-audit --no-fund; then
            print_success "✅ npm install 成功 (legacy-peer-deps)"
            return 0
        fi
        
        # 失敗したら --force で試行
        print_warning "⚠️  legacy-peer-deps で失敗、--force で再試行..."
        if npm install --force --no-audit --no-fund; then
            print_success "✅ npm install 成功 (force)"
            return 0
        fi
    fi
    
    print_error "❌ npm install 失敗"
    if [ -n "$log_file" ]; then
        print_error "   詳細ログ: $log_file"
    fi
    return 1
}

# node_modulesとビルドファイルをクリーンする関数
clean_npm_build() {
    print_info "🗑️  既存ファイルをクリーン中..."
    echo "   削除対象: node_modules, dist, .nuxt"
    
    # 削除実行
    rm -rf node_modules dist .nuxt
    
    print_success "✅ クリーン完了"
    echo ""
}

# package.jsonの存在確認
check_package_json() {
    if [ ! -f "package.json" ]; then
        print_error "❌ package.json が見つかりません"
        print_error "   現在のディレクトリ: $(pwd)"
        return 1
    fi
    
    print_success "✅ package.json 確認済み"
    return 0
}

# npm scriptsの実行（エラーハンドリング付き）
run_npm_script() {
    local script_name="$1"
    local log_file="$2"
    local description="${3:-$script_name}"
    
    print_info "🏗️  $description 実行中..."
    echo "   タスク: npm run $script_name"
    
    if [ -n "$log_file" ]; then
        echo "   出力: リアルタイム表示 + $log_file"
        echo ""
        
        if npm run "$script_name" 2>&1 | tee "$log_file"; then
            print_success "✅ $description 成功!"
            return 0
        else
            print_error "❌ $description 失敗"
            print_error "   詳細ログ: $log_file"
            return 1
        fi
    else
        if npm run "$script_name"; then
            print_success "✅ $description 成功!"
            return 0
        else
            print_error "❌ $description 失敗"
            return 1
        fi
    fi
}

# 使用例とヘルプ
show_usage() {
    echo "npm-utils.sh - Mirel Platform npm utility functions"
    echo ""
    echo "使用可能な関数:"
    echo "  check_node_environment        - Node.js環境チェック"
    echo "  install_npm_dependencies      - npm依存関係インストール"
    echo "  clean_npm_build              - node_modules等のクリーン"
    echo "  check_package_json           - package.json存在確認"
    echo "  run_npm_script              - npm scripts実行"
    echo ""
    echo "使用例:"
    echo "  source scripts/npm-utils.sh"
    echo "  check_node_environment"
    echo "  install_npm_dependencies '../logs/install.log' 'frontend build'"
}

# このファイルが直接実行された場合はヘルプを表示
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    show_usage
fi