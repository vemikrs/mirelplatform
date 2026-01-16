---
description: PRの全フィードバック（CodeQL、Copilotレビュー）を取得して対応状況を確認
---

# PR Feedback Check Workflow

このワークフローは、PRに関連する全フィードバックを取得し、対応漏れがないか確認します。

## 使用方法

```
/pr-feedback-check
```

---

## ステップ

### 1. PR番号の特定

現在のブランチからPR番号を特定するか、ユーザーに確認します。

```bash
gh pr view --json number -q .number
```

### 2. Code Scanningアラート取得

// turbo

```bash
gh api "repos/:owner/:repo/code-scanning/alerts?ref=refs/pull/{PR}/merge&state=open" \
  --jq '.[] | {number, rule: .rule.id, severity: .rule.severity, file: .most_recent_instance.location.path, line: .most_recent_instance.location.start_line}'
```

### 3. Copilot/セキュリティレビューコメント取得

// turbo

```bash
gh api "repos/:owner/:repo/pulls/{PR}/comments" \
  --jq '.[] | select(.user.login | contains("copilot") or contains("security") or contains("github-advanced-security")) | {author: .user.login, path, line, body: (.body | split("\n")[0:2] | join(" "))}'
```

### 4. PRチェック状態確認

// turbo

```bash
gh pr checks {PR}
```

### 5. 対応状況レポート

取得した情報を以下の形式でレポート:

```markdown
## PR#{PR} フィードバック対応状況

### Code Scanningアラート

- [ ] アラート#{number}: {rule} in {file}:{line}

### Copilotレビュー

- [ ] {path}:{line} - {summary}

### PRチェック

- {check_name}: {status}

### 残タスク

1. ...
```
