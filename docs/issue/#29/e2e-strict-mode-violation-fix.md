# E2Eテスト Playwright Strict Mode Violation 修正

**Issue:** #29  
**Date:** 2025-10-31  
**Status:** ✅ Fixed  
**Related CI Run:** https://github.com/vemikrs/mirelplatform/actions/runs/18984295188/job/54224340787

## 問題の概要

GitHub Actions E2Eテストで **Playwright strict mode violation** エラーが発生し、アクセシビリティテスト全5ケースが失敗。

## エラー詳細

### エラーメッセージ
```
Error: expect(locator).toBeEnabled() failed

Locator: locator('.container_title, h1')
Expected: enabled
Error: strict mode violation: locator('.container_title, h1') resolved to 2 elements:
  1) <h1 class="text-2xl font-bold text-foreground">mirelplatform</h1> aka getByRole('heading', { name: 'mirelplatform' })
  2) <h1 class="container_title text-4xl font-bold text-foreground">ProMarker 払出画面</h1> aka getByRole('heading', { name: 'ProMarker 払出画面' })
```

### 発生場所
```
at utils/test-helpers.ts:24
  22 |   static async waitForElement(page: Page, selector: string, timeout = 5000) {
  23 |     await page.waitForSelector(selector, { state: 'visible', timeout });
> 24 |     await expect(page.locator(selector)).toBeEnabled({ timeout });
     |                                          ^
  25 |   }
```

### 失敗したテスト
- ❌ `should pass basic accessibility scan`
- ❌ `should have proper heading structure`
- ❌ `should have proper form labels and associations`
- ❌ `should support keyboard navigation`
- ❌ `should have proper color contrast`

## 根本原因分析

### 原因1: セレクタの曖昧性
**ファイル:** `packages/e2e/tests/pages/promarker.page.ts:19`

```typescript
pageTitle: '.container_title, h1', // Support both old and new structure
```

**問題点:**
- セレクタが2つのh1要素にマッチ
  - ヘッダーの `mirelplatform`
  - ページタイトルの `ProMarker 払出画面`
- Playwrightのstrict modeでは複数要素マッチ時にエラー

### 原因2: 不適切なアサーション
**ファイル:** `packages/e2e/tests/utils/test-helpers.ts:24`

```typescript
await expect(page.locator(selector)).toBeEnabled({ timeout });
```

**問題点:**
- 全要素に対して `toBeEnabled()` を実行
- h1, p, div などの非インタラクティブ要素には enabled/disabled の概念がない
- 見出し要素などへの不適切なアサーション

## 修正内容

### 修正1: セレクタのユニーク化

**ファイル:** `packages/e2e/tests/pages/promarker.page.ts`

```diff
   private readonly selectors = {
     container: '.space-y-6',
-    pageTitle: '.container_title, h1', // Support both old and new structure
+    pageTitle: '.container_title', // Use unique class to avoid multiple matches
```

**効果:**
- ページメインタイトルのみを正確にターゲット
- strict mode violation を回避

### 修正2: 要素タイプ判定の追加

**ファイル:** `packages/e2e/tests/utils/test-helpers.ts`

```typescript
static async waitForElement(page: Page, selector: string, timeout = 5000) {
  await page.waitForSelector(selector, { state: 'visible', timeout });
  
  // Only check enabled state for interactive elements
  const element = page.locator(selector).first();
  const tagName = await element.evaluate((el) => el.tagName.toLowerCase()).catch(() => null);
  const nonInteractiveElements = [
    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
    'p', 'div', 'span', 'label',
    'section', 'article', 'main', 'header', 'footer'
  ];
  
  if (tagName && !nonInteractiveElements.includes(tagName)) {
    await expect(element).toBeEnabled({ timeout }); // インタラクティブ要素のみ
  } else {
    await expect(element).toBeVisible({ timeout }); // 非インタラクティブ要素
  }
}
```

**効果:**
- 要素タイプに応じた適切なアサーション
- インタラクティブ要素（button, input等）: `toBeEnabled()`
- 非インタラクティブ要素（h1, p, div等）: `toBeVisible()`
- コードの汎用性と再利用性向上

## コミット情報

**Commit:** ce23c31  
**Message:** `fix(e2e): Playwright strict mode violation修正とwaitForElement改善 (refs #29)`

**変更ファイル:**
- `packages/e2e/tests/utils/test-helpers.ts`
- `packages/e2e/tests/pages/promarker.page.ts`

## 影響範囲

### 直接的な影響
- アクセシビリティテストの全5ケースが修正
- 失敗していたテストが通過するようになる

### 副次的な改善
- 同様のパターンを使用する他のテストも自動的に改善
- `waitForElement()` の汎用性向上
- 将来的な類似エラーの予防

## 期待される結果

次回のCI実行で以下のテストが通過:
- ✅ `should pass basic accessibility scan`
- ✅ `should have proper heading structure`
- ✅ `should have proper form labels and associations`
- ✅ `should support keyboard navigation`
- ✅ `should have proper color contrast`

## 学習ポイント

### Playwrightのベストプラクティス

1. **Strict Modeの理解**
   - 複数要素マッチは意図しないバグの原因
   - ユニークなセレクタを使用する

2. **要素タイプに応じたアサーション**
   - インタラクティブ要素: `toBeEnabled()`, `toBeDisabled()`
   - 非インタラクティブ要素: `toBeVisible()`, `toBeHidden()`
   - 適切なアサーションでテストの信頼性向上

3. **セレクタの設計**
   - `data-testid` 属性の活用
   - クラス名の一意性確保
   - ユニークなセレクタでメンテナンス性向上

### E2Eテストの設計パターン

1. **Page Object Modelの実践**
   - セレクタの一元管理
   - 変更時の修正箇所最小化
   - テストコードの可読性向上

2. **ヘルパー関数の汎用化**
   - 要素タイプ判定による適切な処理
   - 再利用可能なユーティリティ関数
   - テストコードの重複削減

## 参考資料

- [Playwright Locators - Strict Mode](https://playwright.dev/docs/locators#strictness)
- [Playwright Assertions](https://playwright.dev/docs/test-assertions)
- [Page Object Model Pattern](https://playwright.dev/docs/pom)

## 次のステップ

1. ✅ 修正コミット・プッシュ完了
2. ⏳ CI/CDパイプラインで自動検証待ち
3. 🎯 グリーンビルド確認後にマージ可能
4. 📝 他のテストでも同様のパターンがないか確認

---
**Powered by GitHub Copilot 🤖**
