#!/usr/bin/env bash

# OTPログインの疎通確認を自動化するスモークテスト。
# Usage: ./scripts/e2e/otp-login-smoke.sh
# 環境変数で BASE_URL / MAILHOG_URL / EMAIL / PURPOSE / COOKIE_JAR を上書き可能。

set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:3000/mipla2}
MAILHOG_URL=${MAILHOG_URL:-http://localhost:8025}
EMAIL=${EMAIL:-user@example.com}
PURPOSE=${PURPOSE:-LOGIN}
COOKIE_JAR=${COOKIE_JAR:-/tmp/otp-login-cookies.txt}
POLL_MAX=${POLL_MAX:-30}
POLL_INTERVAL=${POLL_INTERVAL:-2}
TMP_DIR=$(mktemp -d /tmp/otp-login.XXXXXX)

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

log_info()    { echo -e "\033[0;34m[INFO]\033[0m $*"; }
log_success() { echo -e "\033[0;32m[SUCCESS]\033[0m $*"; }
log_error()   { echo -e "\033[0;31m[ERROR]\033[0m $*" >&2; }

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    log_error "'$1' コマンドが見つかりません。"
    exit 1
  fi
}

require_cmd curl
require_cmd python3

clear_mailhog() {
  log_info "MailHogメッセージをクリア"
  if ! curl -sS -X DELETE "$MAILHOG_URL/api/v1/messages" >/dev/null; then
    log_error "MailHog APIへの接続に失敗しました"
    exit 1
  fi
}

request_otp() {
  log_info "OTPリクエスト送信: $EMAIL"
  local payload
  payload=$(printf '{"model":{"email":"%s","purpose":"%s"}}' "$EMAIL" "$PURPOSE")
  if ! curl -sS -H 'Content-Type: application/json' -d "$payload" \
    "$BASE_URL/auth/otp/request" -o "$TMP_DIR/otp-request.json"; then
    log_error "OTPリクエストが失敗しました"
    exit 1
  fi
  local request_id
  request_id=$(python3 - "$TMP_DIR/otp-request.json" <<'PY'
import json, sys
with open(sys.argv[1]) as fh:
    data = json.load(fh)
errors = data.get('errors') or []
if errors:
    sys.stderr.write('OTPリクエストでエラー: ' + '; '.join(errors) + '\n')
    sys.exit(1)
print(data.get('data', {}).get('requestId', ''))
PY
  )
  if [[ -z "$request_id" ]]; then
    log_error "OTPリクエストIDを取得できませんでした"
    exit 1
  fi
  echo "$request_id" > "$TMP_DIR/request-id.txt"
  log_success "OTPリクエスト成功 requestId=$request_id"
}

extract_otp_from_mailhog() {
  python3 - <<'PY'
import json, re, sys
from html import unescape
try:
    payload = json.load(sys.stdin)
except json.JSONDecodeError:
    print('', end='')
    sys.exit(0)
for item in payload.get('items', []):
    body = item.get('Content', {}).get('Body') or ''
    body = unescape(body)
    match = re.search(r'\b(\d{6})\b', body)
    if match:
        print(match.group(1))
        sys.exit(0)
print('', end='')
PY
}

wait_for_otp_email() {
  log_info "MailHogからOTPメールを待機 (最大 ${POLL_MAX} 回)"
  local attempt=1
  while (( attempt <= POLL_MAX )); do
    if curl -sS "$MAILHOG_URL/api/v2/messages" -o "$TMP_DIR/mailhog.json"; then
      local code
      code=$(extract_otp_from_mailhog <"$TMP_DIR/mailhog.json")
      if [[ -n "$code" ]]; then
        echo "$code" > "$TMP_DIR/otp-code.txt"
        log_success "OTPコード取得: $code"
        return 0
      fi
    fi
    attempt=$((attempt + 1))
    sleep "$POLL_INTERVAL"
  done
  log_error "MailHogからOTPメールを取得できませんでした"
  exit 1
}

verify_otp() {
  local otp_code
  otp_code=$(cat "$TMP_DIR/otp-code.txt")
  log_info "OTPコードで検証: $otp_code"
  local payload
  payload=$(printf '{"model":{"email":"%s","otpCode":"%s","purpose":"%s"}}' \
    "$EMAIL" "$otp_code" "$PURPOSE")
  if ! curl -sS -c "$COOKIE_JAR" -H 'Content-Type: application/json' -d "$payload" \
    "$BASE_URL/auth/otp/verify" -o "$TMP_DIR/otp-verify.json"; then
    log_error "OTP検証API呼び出しに失敗しました"
    exit 1
  fi
  python3 - "$TMP_DIR/otp-verify.json" <<'PY'
import json, sys
with open(sys.argv[1]) as fh:
    data = json.load(fh)
if not data.get('data'):
    sys.stderr.write('OTP検証が失敗しました: ' + '; '.join(data.get('errors') or []) + '\n')
    sys.exit(1)
PY
  log_success "OTP検証成功。セッションCookieを $COOKIE_JAR に保存"
}

call_protected_endpoint() {
  log_info "セッションを使って /users/me を呼び出し"
  local response_file="$TMP_DIR/users-me.json"
  local http_code
  http_code=$(curl -sS -w '%{http_code}' -o "$response_file" -b "$COOKIE_JAR" \
    "$BASE_URL/users/me")
  if [[ "$http_code" != "200" ]]; then
    log_error "認証済みエンドポイントが失敗しました (status=$http_code)"
    cat "$response_file" >&2
    exit 1
  fi
  log_success "認証済みエンドポイント応答:"
  python3 -m json.tool "$response_file"
}

main() {
  clear_mailhog
  request_otp
  wait_for_otp_email
  verify_otp
  call_protected_endpoint
  log_success "OTPスモークテスト完了"
}

main
