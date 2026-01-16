---
name: PR Feedback Complete Check
description: PRの全フィードバック（CodeQL、Copilotレビュー、手動コメント）を取得し、漏れなく対応する
---

# PR Feedback Complete Check

このスキルは、PRに関連する**全種類のフィードバック**を取得し、漏れなく対応するための手順を提供します。

## 重要な教訓

> **CodeQL/Analyzeジョブ成功 ≠ Code Scanningチェック成功**
>
> CodeQL Analyzeジョブが成功しても、Code Scanningアラートが残っていればPRチェックは失敗します。
> 必ず `gh pr checks` で全件passを確認してください。

---

## 1. Code Scanningアラート取得

PRに関連する**全種類**のCode Scanningアラートを取得:

```bash
# PRマージコミットに対するopenアラート（全種類）
gh api "repos/:owner/:repo/code-scanning/alerts?ref=refs/pull/{PR_NUMBER}/merge&state=open" \
  --jq '.[] | {number, rule: .rule.id, severity: .rule.severity, file: .most_recent_instance.location.path, line: .most_recent_instance.location.start_line}'
```

### 確認すべきアラート種類

| ルールID                | 説明                         | 対応方法                |
| ----------------------- | ---------------------------- | ----------------------- |
| `java/log-injection`    | ログにユーザー入力を直接出力 | `SanitizeUtil.forLog()` |
| `java/polynomial-redos` | 正規表現のReDoS脆弱性        | `.*`の連続を避ける      |
| `java/sql-injection`    | SQLインジェクション          | パラメータバインド使用  |
| `java/path-injection`   | パストラバーサル             | パス検証                |

---

## 2. Copilot/セキュリティレビューコメント取得

```bash
# PRのレビューコメント（Copilot、セキュリティボット含む）
gh api "repos/:owner/:repo/pulls/{PR_NUMBER}/comments" \
  --jq '.[] | {author: .user.login, path, line, body: (.body | split("\n")[0])}'
```

---

## 3. PRチェック全件確認

```bash
# 全チェックの状態確認
gh pr checks {PR_NUMBER}
```

**重要**: 以下が**全て**successになるまで完了としない:

- `CodeQL` (Code Scanningチェック)
- `CodeQL/Analyze (java)`
- `CodeQL/Analyze (javascript)`
- `E2E Tests/End-to-End`
- その他全てのチェック

---

## 4. 対応チェックリスト

PRレビュー対応時は以下を確認:

- [ ] Code Scanningアラート全件取得済み
- [ ] 全アラート種類（log-injection, redos等）を確認
- [ ] Copilotレビューコメント全件確認
- [ ] 手動レビューコメント確認
- [ ] `gh pr checks` で全件pass確認
- [ ] 修正コードをビルド・テスト検証済み
