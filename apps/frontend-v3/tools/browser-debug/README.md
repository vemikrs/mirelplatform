# ブラウザデバッグツール

このディレクトリには、WSL2環境などのヘッドレス環境でブラウザ操作をデバッグするためのツールが含まれています。

## 概要

Playwrightを使用してChromeブラウザを操作し、スクリーンショットの取得やコンソールログの確認を行うことができます。
主に、GUIが利用できない環境でのレンダリング確認やエラー調査に使用します。

## ツール一覧

### 1. debug-browser.cjs

単体のデバッグ用スクリプトです。
指定されたURL（デフォルトは `http://localhost:5173/home`）にアクセスし、スクリーンショットを取得してページ内容をコンソールに出力します。

**使用方法:**

```bash
node tools/browser-debug/debug-browser.cjs
```

### 2. wsl-browser-agent.cjs

JSON形式の指示書に基づいてブラウザ操作を行うエージェントスクリプトです。
複数のステップ（移動、クリック、入力、待機など）を順次実行できます。

**使用方法:**

```bash
node tools/browser-debug/wsl-browser-agent.cjs <path/to/instructions.json>
```

**instructions.json の例:**

```json
[
  { "type": "goto", "url": "http://localhost:5173/login" },
  { "type": "fill", "selector": "input[name='username']", "value": "user" },
  { "type": "click", "selector": "button[type='submit']" },
  { "type": "screenshot", "path": "login-result.png" }
]
```

## 出力先

実行結果（スクリーンショットなど）は、`apps/frontend-v3/debug-output/` ディレクトリに出力されます。
このディレクトリはGitの追跡対象外（`.gitignore`）に設定されています。
