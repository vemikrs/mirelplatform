# TDD実践ガイド - ProMarker Phase 1

**作成日**: 2025-10-14  
**関連Issue**: #28  
**関連ドキュメント**: phase1-plan.md, step7.1-recovery-plan.md

---

## 📋 TDD原則の再確立

### 背景: TDD計画からの逸脱

**当初計画** (phase1-plan.md L146-164):
```markdown
### 🧪 E2Eテスト戦略 - Test-First Approach

方針: 段階的テストファースト実装
- 各機能実装前に失敗するE2Eテストを作成 (Red)
- 機能を実装してテストをパス (Green)
- 必要に応じてリファクタリング (Refactor)
```

**実際の状況**:
- ❌ **Step 4**: hooks.spec.ts未作成のまま`useGenerate()`実装完了
- ❌ **Step 7**: FileUploadButton実装後にfile-upload.spec.ts作成（逆順）
- ❌ **Step 10延期**: complete-workflow.spec.tsを後回しにし、コア機能が未検証

**結果**: Generate/Downloadのような**コア機能が0件のE2Eテストで実装された**

---

## 🎯 TDD原則（厳格版）

### ルール 1: **実装前にテスト作成必須**

#### 対象
- ✅ 全てのコア機能（Generate, Download, Upload等）
- ✅ ユーザーが直接使用する機能
- ✅ API統合が必要な機能

#### 禁止事項
- ❌ 「動くから」という理由でテストなし実装
- ❌ 「後でテストを書く」という先延ばし
- ❌ 「Step 10で書く」という後回し

#### 実施手順
```bash
# 🔴 Step 1: Red - テスト作成
cd packages/e2e
touch tests/specs/promarker-v3/my-feature.spec.ts

# テストを書く（失敗することを確認）
pnpm test my-feature.spec.ts
# Expected: All tests fail

# 🟢 Step 2: Green - 実装
cd apps/frontend-v3
# コードを書く

# テストを実行（パスすることを確認）
cd ../../packages/e2e
pnpm test my-feature.spec.ts
# Expected: All tests pass

# 🔵 Step 3: Refactor - リファクタリング
# コードを整理（テストは常にパスし続ける）
```

---

### ルール 2: **コア機能のE2Eテスト優先**

#### 優先度 🔴 Critical
1. **complete-workflow.spec.ts** - 最優先
   - Generate → Auto Download
   - エラーハンドリング
   - 複数回実行
   - UI状態管理

2. **hooks.spec.ts** - API統合の基盤
   - useSuggest
   - useGenerate  
   - useReloadStencilMaster
   - useFileUpload

3. **file-upload.spec.ts** - ファイル操作
   - ファイル選択
   - アップロード
   - fileId設定

#### 実装順序の原則
```
❌ 間違い:
  実装 → 小さいテスト → 大きいテスト

✅ 正しい:
  大きいテスト（E2E） → 実装 → 小さいテスト（ユニット）
```

**理由**: ユーザーが実際に使うワークフロー（E2E）こそが品質保証の要

---

### ルール 3: **テストがパスして初めて完了**

#### 完了基準
```markdown
## Step X: 機能実装

### 完了条件（Definition of Done）
- [ ] E2Eテスト作成済み（テストファイルが存在）
- [ ] E2Eテスト全件パス（失敗0件）
- [ ] 手動動作確認実施済み（スクリーンショット添付）
- [ ] TypeScript型エラー0件
- [ ] `pnpm run build` 成功
- [ ] チェックリスト更新済み
- [ ] テスト結果表更新済み

上記すべて完了 = Step完了 ✅
```

#### 禁止される完了判断
- ❌ 「コンソールエラーなし」だけで完了
- ❌ 「APIが呼ばれる」だけで完了
- ❌ 「ボタンが動く」だけで完了
- ❌ 「手動で確認した（記録なし）」で完了

---

## 📝 TDDワークフロー詳細

### Phase 1: Red - テスト作成

#### Step 1.1: テストケース設計
```typescript
// packages/e2e/tests/specs/promarker-v3/my-feature.spec.ts

/**
 * テストケース設計
 * 
 * 1. Happy Path（正常系）
 *    - ユーザーが正しく操作した場合の動作
 * 
 * 2. Validation Error（バリデーションエラー）
 *    - 必須項目が空
 *    - 不正な形式
 * 
 * 3. API Error（APIエラー）
 *    - サーバーエラー
 *    - ネットワークエラー
 * 
 * 4. Edge Cases（境界値・特殊ケース）
 *    - 空配列
 *    - null/undefined
 *    - 複数回実行
 */

test.describe('My Feature', () => {
  test('Happy Path: 正常系動作', async ({ page }) => {
    // ユーザー操作をシミュレート
    // 期待結果を検証
  })
  
  test('Validation Error: 必須項目が空', async ({ page }) => {
    // エラーメッセージが表示されることを検証
  })
  
  test('API Error: サーバーエラー時の動作', async ({ page }) => {
    // エラートーストが表示されることを検証
  })
  
  test('Edge Case: 空配列が返される', async ({ page }) => {
    // 警告が表示されることを検証
  })
})
```

#### Step 1.2: テスト実行（失敗確認）
```bash
cd packages/e2e
pnpm test my-feature.spec.ts

# 期待される出力:
# ❌ Happy Path: 正常系動作 - FAILED
# ❌ Validation Error: 必須項目が空 - FAILED
# ❌ API Error: サーバーエラー時の動作 - FAILED
# ❌ Edge Case: 空配列が返される - FAILED
```

#### Step 1.3: 失敗理由を記録
```markdown
## テスト失敗理由

1. Happy Path: コンポーネントが存在しない
2. Validation Error: バリデーションロジック未実装
3. API Error: エラーハンドリング未実装
4. Edge Case: 空配列チェック未実装
```

---

### Phase 2: Green - 実装

#### Step 2.1: 最小限の実装
```typescript
// apps/frontend-v3/src/features/promarker/components/MyFeature.tsx

// テストを通すための最小限のコード
export function MyFeature() {
  return (
    <div data-testid="my-feature">
      {/* 必要最小限のUI */}
    </div>
  )
}
```

#### Step 2.2: テスト再実行（パス確認）
```bash
cd packages/e2e
pnpm test my-feature.spec.ts

# 期待される出力:
# ✅ Happy Path: 正常系動作 - PASSED
# ✅ Validation Error: 必須項目が空 - PASSED
# ✅ API Error: サーバーエラー時の動作 - PASSED
# ✅ Edge Case: 空配列が返される - PASSED
```

#### Step 2.3: 手動動作確認
```bash
# 開発サーバー起動
cd apps/frontend-v3
pnpm dev

# ブラウザで確認
# 1. 正常系の動作確認
# 2. エラー発生時の動作確認
# 3. スクリーンショットを撮影して docs/issue/#28/ に保存
```

---

### Phase 3: Refactor - リファクタリング

#### Step 3.1: コード整理
- 重複コードの削除
- 関数の分割
- 型安全性の向上
- コメントの追加

#### Step 3.2: テスト再実行（継続的確認）
```bash
# リファクタリング後も必ずテストを実行
pnpm test my-feature.spec.ts

# すべてパスし続けることを確認
```

#### Step 3.3: 型チェック・ビルド
```bash
cd apps/frontend-v3
pnpm run type-check
pnpm run build
```

---

## 📊 TDD実践チェックリスト

### Stepタスク開始時
- [ ] Issue #28 にコメント投稿（「Step X 開始」）
- [ ] TDD方式を明記（Red → Green → Refactor）
- [ ] テストケース設計を記載

### Red Phase
- [ ] E2Eテストファイル作成
- [ ] テストケース実装（4種類以上推奨）
- [ ] テスト実行（全て失敗）
- [ ] 失敗理由を記録

### Green Phase
- [ ] 最小限の実装でテストパス
- [ ] テスト実行（全てパス）
- [ ] 手動動作確認実施
- [ ] スクリーンショット撮影

### Refactor Phase
- [ ] コード整理
- [ ] テスト再実行（全てパス維持）
- [ ] 型チェック実行（エラー0件）
- [ ] ビルド実行（成功）

### Stepタスク完了時
- [ ] チェックリスト更新（`[x]`マーク）
- [ ] テスト結果表更新
- [ ] Issue #28 にコメント投稿（「Step X 完了」+ テスト結果）
- [ ] コミット（適切なcommit message）

---

## 🚨 TDD違反事例と修正方法

### 事例 1: 「動くから完了」

**❌ 間違った完了判断**:
```
✅ useGenerate() 実装完了
✅ APIが呼ばれることを確認
✅ コンソールエラーなし

→ Step 7完了とマーク
```

**問題点**:
- E2Eテストが0件
- 自動ダウンロードが未検証
- エラーハンドリングが未検証
- 複数回実行が未検証

**✅ 正しい完了判断**:
```
🔴 complete-workflow.spec.ts 作成（6テスト失敗）
🟢 useGenerate() 改善（6テストパス）
🔵 エラーハンドリング強化（6テストパス維持）
✅ 手動動作確認（スクリーンショット添付）

→ Step 7完了とマーク
```

---

### 事例 2: 「後でテストを書く」

**❌ 間違ったアプローチ**:
```
Day 5: useGenerate() 実装
Day 6: ParameterFields 実装
Day 10: complete-workflow.spec.ts 作成（予定）

→ Day 5-9はテストなしで実装
```

**問題点**:
- Day 5-9の間、品質保証なし
- Day 10でテスト作成時に大量のバグ発覚
- バグ修正でスケジュール遅延

**✅ 正しいアプローチ**:
```
Day 5-1: complete-workflow.spec.ts 作成（失敗）
Day 5-2: useGenerate() 実装（パス）
Day 6-1: parameter-input.spec.ts 作成（失敗）
Day 6-2: ParameterFields 実装（パス）

→ 各実装時点で品質保証済み
```

---

### 事例 3: 「小さいテストから書く」

**❌ 間違った優先度**:
```
優先度:
1. ユニットテスト（関数単体）
2. コンポーネントテスト（UI単体）
3. E2Eテスト（ワークフロー全体） ← 最後

→ ユーザーが実際に使うフローが未検証
```

**✅ 正しい優先度**:
```
優先度:
1. E2Eテスト（ワークフロー全体） ← 最初
2. コンポーネントテスト（UI単体）
3. ユニットテスト（関数単体）

→ ユーザー体験から逆算して実装
```

---

## 📈 TDD実践による効果測定

### Before TDD厳格化（Step 0-7）
- E2Eテスト数: 49件
- Generate/Downloadテスト: 0件 ❌
- 手動確認記録: なし
- バグ発見: Step 7完了後に判明

### After TDD厳格化（Step 7.1以降）
- E2Eテスト数: 68件（+19件）
- Generate/Downloadテスト: 6件 ✅
- 手動確認記録: スクリーンショット添付
- バグ発見: 実装中に早期発見

### 目標（Phase 1完了時）
- E2Eテスト数: 100+件
- テストカバレッジ: 80%+
- 全コア機能: E2Eテスト完備
- 手動確認記録: 全Step分

---

## 🎯 今後のTDD実践ルール

### Step開始時の必須タスク
1. ✅ **E2Eテスト設計書作成** (Markdown)
2. ✅ **テストファイル作成** (`*.spec.ts`)
3. ✅ **テスト実行・失敗確認** (Red)
4. ✅ **Issue #28にRed報告** (スクリーンショット添付)

### Step実装中の必須タスク
1. ✅ **最小限の実装でGreen達成**
2. ✅ **Issue #28にGreen報告** (テスト結果添付)
3. ✅ **手動動作確認** (スクリーンショット撮影)
4. ✅ **Refactor実施** (テストパス維持)

### Step完了時の必須タスク
1. ✅ **全E2Eテストパス確認**
2. ✅ **型チェック・ビルド成功確認**
3. ✅ **チェックリスト・テスト結果表更新**
4. ✅ **Issue #28に完了報告** (全確認項目のチェックリスト付き)

---

## 📚 参考資料

- **phase1-plan.md L146-164**: 当初のTDD計画
- **step7.1-recovery-plan.md**: TDD原則違反の実態と対策
- **step7-completion-gap-analysis.md**: TDD破綻の根本原因分析

---

**作成者**: GitHub Copilot  
**最終更新**: 2025-10-14  
**ステータス**: Active - 以降の全Stepで適用必須

*Powered by Copilot 🤖*
