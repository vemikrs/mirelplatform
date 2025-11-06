# PR #36 E2Eテスト完全復旧計画書

## Executive Summary

プルリクエスト #36 「feat: refresh navigation and toast experience」によるE2Eテスト失敗の根本原因を分析し、1PR完全復旧の包括的計画を策定する。

**現在の状況**:
- 実行結果: 34 passed / 18 failed / 27 skipped (約65.4%通過率)
- 主要問題: ネイティブ`<select>`からRadix UI `Select`への移行による互換性問題
- 修正対象: E2Eテスト仕様とPage Object Model

## 根本原因分析

### 1. PR #36による主要変更の影響

#### 1.1 StencilSelector ComponentのRadix UI移行
**変更前** (ネイティブHTML):
```html
<select data-testid="category-select" className="...">
  <option value="">選択してください</option>
  <option value="/samples">Sample Stencils</option>
</select>
```

**変更後** (Radix UI Select):
```tsx
<Select value={categories.selected} onValueChange={onCategoryChange}>
  <SelectTrigger data-testid="category-select">
    <SelectValue placeholder="カテゴリを選択" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="/samples">Sample Stencils</SelectItem>
  </SelectContent>
</Select>
```

**DOM構造の変化**:
- Radix UI SelectはCSSによる複雑なDOM構造を生成
- `<option>`要素が`[role="option"]`のdivへ変更
- 選択値の取得方法が`inputValue()`から`textContent()`へ変更

#### 1.2 UI Design Systemの大幅刷新

**新規追加コンポーネント**:
- `@mirel/ui` - 統一されたdesign system
- `SectionHeading` - ページ見出し構造の変更
- `Card`, `Badge`, `StepIndicator` - レイアウト階層の変更
- `FormField`, `FormLabel`, `FormHelper` - フォーム構造の統一

**影響範囲**:
- ページタイトルが `h1` から `SectionHeading` 内の `h2` へ変更
- セレクタパターンの変更: `getByRole('heading', { name: /ProMarker 払出画面/ })` → `getByRole('heading', { name: /ProMarker/ })`

#### 1.3 トースト通知システムの変更

**Before**: `sonner` library
```typescript
import { toast } from 'sonner'
toast.success('成功', { description: 'メッセージ' })
```

**After**: `@mirel/ui` 統合トースト
```typescript
import { toast } from '@mirel/ui'
toast({ ...toastMessages.generateSuccess })
```

**影響**: トースト表示のテスト検証が動作しない

### 2. E2Eテスト失敗パターン分析

#### 2.1 Select操作の互換性問題（最重要）
**失敗パターン1: `page.selectOption()` 操作**
```typescript
// 動作しない（Radix UI Select非対応）
await page.selectOption('[data-testid="category-select"]', '/samples')

// 必要な修正
await promarkerPage.selectCategoryByIndex(0)
```

**失敗パターン2: `inputValue()` による値取得**
```typescript
// 動作しない
const value = await page.inputValue('[data-testid="serial-select"]')

// 必要な修正  
const value = await page.locator('[data-testid="serial-select"]').textContent()
```

**技術的根拠**: Radix UI Selectは`<input>`要素ではなく、`[role="combobox"]`の`<button>`要素として実装されている

#### 2.2 Page Object Model (POM)の部分実装
**現状**: promarker-v3.page.tsでRadix Select対応を実装済み
```typescript
async selectCategoryByIndex(index = 0) {
  await this.selectByIndex(this.selectors.categorySelect, index);
}

private async selectByIndex(triggerTestId: string, index = 0) {
  await this.page.locator(triggerTestId).click();
  await this.page.waitForSelector('[role="option"]', { timeout: 5000 });
  await this.page.locator('[role="option"]').nth(index).click();
}
```

**問題**: テストファイル内で直接`page.selectOption()`を使用している箇所が残存

#### 2.3 残存する未修正パターン
**検索結果による特定**:
- `selectOption`: 9箇所の未修正
- `inputValue`: 7箇所の値取得未修正  
- 主要影響ファイル:
  - `complete-workflow.spec.ts` - 6箇所
  - `hooks.spec.ts` - 3箇所
  - `form-validation.spec.ts` - 2箇所
  - `json-editor.spec.ts` - 1箇所

## 完全復旧計画

### Phase 1: 残存するSelect操作の完全修正

#### 1.1 自動修正スクリプトの完成・実行
```bash
# 既存の修正スクリプトを改良・実行
./fix-select-operations.sh
./fix-inputvalue-operations.sh
```

**対象ファイル**:
- `complete-workflow.spec.ts` - ワークフロー統合テスト
- `hooks.spec.ts` - React Hooksテスト  
- `form-validation.spec.ts` - バリデーションテスト
- `json-editor.spec.ts` - JSON編集機能テスト
- `file-upload.spec.ts` - ファイルアップロードテスト

#### 1.2 修正内容の詳細

**Pattern 1**: selectOption → POM method
```typescript
// Before
await page.selectOption('[data-testid="category-select"]', '/samples')
await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
await page.selectOption('[data-testid="serial-select"]', '250913A')

// After  
await promarkerPage.selectCategoryByIndex(0)
await promarkerPage.selectStencilByIndex(0)
await promarkerPage.selectSerialByIndex(0)
```

**Pattern 2**: inputValue → textContent/getAttribute
```typescript
// Before
const current = await serialSelect.inputValue()

// After
const current = await serialSelect.textContent()
```

**Pattern 3**: Serial selection fallback logic
```typescript
// Complex fallback logic → Simple POM call
const hasTarget = await page.locator('[data-testid="serial-select"] option[value="250913A"]').count()
if (hasTarget > 0) {
  await page.selectOption('[data-testid="serial-select"]', '250913A')
} else {
  // fallback logic...
}

// Simplified to:
await promarkerPage.selectSerialByIndex(0)
```

### Phase 2: トースト通知テストの修正

#### 2.1 新しいtoastMessages定数の活用
```typescript
// packages/e2e/tests/fixtures/toast-messages.ts (新規作成)
export const expectedToastMessages = {
  generateSuccess: {
    title: '生成が完了しました',
    description: '成果物をダウンロードできるようになりました。'
  },
  generateError: {
    title: '生成に失敗しました', 
    description: '入力内容を確認し、再度お試しください。'
  }
  // ... 他のメッセージ
}
```

#### 2.2 Toast検証ロジックの更新
```typescript
// Before: sonner toast検証
await expect(page.locator('.toast-success')).toContainText('成功')

// After: @mirel/ui toast検証  
await expect(page.locator('[role="status"]')).toContainText('生成が完了しました')
```

### Phase 3: UI階層変更への対応

#### 3.1 見出し要素の修正
```typescript
// Before: 
await expect(page.getByRole('heading', { name: /ProMarker 払出画面/ })).toBeVisible()

// After:
await expect(page.getByRole('heading', { name: /ProMarker/ })).toBeVisible()
```

#### 3.2 Card/Badge コンポーネント対応
新しいdesign system componentに対応したセレクタの追加:
```typescript
// StencilInfo → Card component
await expect(page.locator('[data-testid="stencil-info"]')).toBeVisible()

// Parameter count → Badge component
await expect(page.locator('.badge')).toContainText('3 項目')
```

### Phase 4: Performance & Stability改善

#### 4.1 Wait策略の最適化
```typescript
// API依存の待機からUI状態ベースへ改善
// Before
await page.waitForResponse(r => r.url().includes('/suggest'))

// After  
await expect(page.locator('[data-testid="stencil-select"]')).toBeEnabled({ timeout: 10000 })
```

#### 4.2 Flaky test対策
- タイムアウト時間の統一: 10秒
- retry logic の改良
- ネットワーク状態に依存しないアサーション

### Phase 5: 統合テスト・検証

#### 5.1 完全修正後のテスト実行
```bash
# Full test suite実行
pnpm test

# 期待結果: 79 passed / 0 failed / 0 skipped
```

#### 5.2 回帰テストの実施
- 既存の通過テストが継続して通過することを確認
- 新しく修正されたテストが安定して通過することを確認
- パフォーマンスの改善を測定

## 実装スケジュール

### タスク分解と優先度

| Priority | Task | Estimated Time | Dependencies |
|----------|------|---------------|--------------|
| P0 | Select操作の完全修正 | 2時間 | 自動修正スクリプト |
| P0 | POM methodの完全適用 | 1時間 | Phase 1完了 |
| P1 | Toast通知テスト修正 | 1時間 | toastMessages定数 |
| P1 | UI階層変更対応 | 30分 | Design system理解 |
| P2 | Performance改善 | 1時間 | 基本修正完了 |
| P2 | 統合テスト実行・検証 | 30分 | 全修正完了 |

**Total Estimated Time**: 5時間
**Target Completion**: 1 working day

## 成功指標

### Quantitative Metrics
- **Test Pass Rate**: 100% (79/79 tests passing)
- **Test Execution Time**: < 6 minutes  
- **Flaky Test Rate**: 0%
- **Coverage Maintenance**: 既存機能の回帰なし

### Qualitative Metrics  
- **Maintainability**: POM pattern完全適用
- **Stability**: Radix UI Select操作の安定性確保
- **Documentation**: 修正内容の完全文書化

## Risk Assessment & Mitigation

### High Risk
**Risk**: 大量ファイル修正による新規バグ導入
**Mitigation**: 
- 段階的修正とテスト実行
- バックアップディレクトリの活用
- Git commit per phase

### Medium Risk  
**Risk**: Radix UI特異的な動作による予期しない失敗
**Mitigation**:
- POM層での抽象化
- Retry logic強化
- Timeout調整

### Low Risk
**Risk**: パフォーマンス劣化
**Mitigation**: 
- ベンチマーク測定
- Wait策略最適化

## 長期展望

### Design System Migration Complete
- 全UIコンポーネントがRadix UI/shadcn基盤
- 一貫したアクセシビリティ対応
- テスト安定性の大幅向上

### E2E Test Framework Evolution
- Page Object Model完全適用
- Component-specificテストpatterns
- CI/CD Integration ready

## Conclusion

PR #36による「ナビゲーション・トースト改善」は大規模なUI刷新を含むが、技術的根拠に基づいた系統的修正により1PR完全復旧が可能である。

**Key Success Factors**:
1. **根本原因の正確な特定**: Radix UI Select互換性問題
2. **段階的アプローチ**: Phase-by-phase execution  
3. **自動化活用**: 修正スクリプトによる効率化
4. **POM pattern**: 将来の変更に対する耐性確保

**Expected Outcome**: 100% E2E test pass rate with improved maintainability and stability.

---
**Document Version**: 1.0  
**Created**: 2025年11月6日  
**Author**: GitHub Copilot  
**Status**: Ready for Implementation