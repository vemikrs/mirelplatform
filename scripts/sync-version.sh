#!/bin/bash
# scripts/sync-version.sh
# VERSION ファイルから全 package.json を同期
set -euo pipefail

VERSION=$(cat VERSION)
echo "🔄 Syncing version: $VERSION"

# Root package.json
jq --arg v "$VERSION" '.version = $v' package.json > tmp.json && mv tmp.json package.json
echo "  ✅ package.json"

# Apps/Packages
for dir in apps/frontend-v3 packages/ui packages/e2e; do
  if [ -f "$dir/package.json" ]; then
    cd "$dir"
    npm version "$VERSION" --no-git-tag-version --allow-same-version 2>/dev/null || true
    cd - > /dev/null
    echo "  ✅ $dir"
  fi
done

echo "✨ All versions synced to $VERSION"
