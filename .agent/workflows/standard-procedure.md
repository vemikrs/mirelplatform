---
description: Copilot作業の標準手順テンプレート。Issue/WIからの着手から検証・報告までの流れを定義。
---

# 標準作業手順ワークフロー

## ステップ

1. GitHub Issue または ADO Work Item から TODO を切り出し、`manage_todo_list` で追跡。
2. 関連ファイルを読み込み (`read_file`, `grep_search`) → 必要なら `file_search`。
3. 変更は `replace_string_in_file` / `multi_replace_string_in_file`。複数ファイル修正時は差分を小さく保つ。
4. VS Code Task 経由でビルド/テスト。失敗時はログ抜粋を共有し、再現手順と暫定策を記録。
5. 変更後は `get_errors` やツール出力でエラー確認。必要に応じ `pnpm lint` / `gradlew check`。
6. PR（GitHub）へのコメントと、Issue（GitHub Issue / ADO WI）への結果要約を行い、`docs/issue/#<id>/` に詳細を追記。コメント末尾に **"Powered by Copilot 🤖"**。
