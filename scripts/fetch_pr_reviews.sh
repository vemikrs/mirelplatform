#!/bin/bash

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "Error: gh CLI is not installed."
    exit 1
fi

# Check if PR number is provided
if [ -z "$1" ]; then
    echo "Usage: $0 <PR_NUMBER> [OUTPUT_DIR]"
    exit 1
fi

PR_NUMBER=$1
OUTPUT_DIR=${2:-/tmp}

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

echo "Fetching reviews for PR #$PR_NUMBER..."

# Fetch PR main info including reviews summary
gh pr view "$PR_NUMBER" --json number,title,author,body,reviews,comments > "$OUTPUT_DIR/pr_${PR_NUMBER}_reviews.json"
echo "Saved PR summary to $OUTPUT_DIR/pr_${PR_NUMBER}_reviews.json"

# Fetch detailed review comments (inline comments) and process with jq
echo "Fetching review comments..."
gh api "repos/:owner/:repo/pulls/$PR_NUMBER/comments" | \
jq '[.[] | {path: .path, line: .line, user: .user.login, body: .body, created_at: .created_at, url: .html_url}]' \
> "$OUTPUT_DIR/pr_${PR_NUMBER}_review_comments.json"
echo "Saved processed review comments to $OUTPUT_DIR/pr_${PR_NUMBER}_review_comments.json"

echo "Done."
