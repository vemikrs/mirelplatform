# GEMINI

日本語で回答する。

[copilot-instructions.md](.github/copilot-instructions.md) の指示に従う。

## PR Review Response Standards

PRレビュー対応時は以下を**必ず**実行:

1. **PRのmerge refを明示的に指定してアラート取得**
   - ❌ 誤: `gh api "repos/:owner/:repo/code-scanning/alerts"` (全体を取得)
   - ✅ 正: `gh api "repos/:owner/:repo/code-scanning/alerts?ref=refs/pull/{PR}/merge&state=open"`
   - **理由**: refなしだとmasterのアラートが返り、PRで新規導入されたアラートを見落とす

2. **アラート種類の網羅確認**
   - `log-injection`, `polynomial-redos`, `sql-injection`, `path-injection` 等の全ルール
   - severityがwarning以上のものは全て対応

3. **PRチェック全件確認**
   - `gh pr checks {PR}` で**全チェックがpass**するまで完了とみなさない
   - CodeQL/Analyzeジョブ成功 ≠ Code Scanningチェック成功（別物）
