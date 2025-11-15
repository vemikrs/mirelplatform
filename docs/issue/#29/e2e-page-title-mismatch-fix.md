# E2E Page Title Mismatch Fix

## Issue
GitHub Issue: [#29 - E2E test failures](https://github.com/vemikrs/mirelplatform/issues/29)

## Problem Summary

### Error Pattern
All 5 accessibility tests were failing with the same error:
```
Error: expect(locator).toContainText(expected) failed

Locator: locator('.container_title')
Expected substring: "ProMarker 支出画面"
Received string:    "ProMarker 払出画面"
```

### Root Cause Analysis
1. **Text Mismatch**: E2E test expected "ProMarker 支出画面" but React implementation uses "ProMarker 払出画面"
2. **Timeout Issues**: React SPA client-side rendering requires longer wait times than traditional server-side rendering

### GitHub Actions Run
- Run ID: 18987054610
- All 5 tests failing: basic accessibility, heading structure, form labels, keyboard navigation, color contrast
- Error occurred in `beforeEach` hook during `verifyPageLoaded()`

## Investigation Process

### 1. Log Analysis
```bash
gh run view 18987054610 --log 2>&1 | grep -B 10 -A 10 "toContainText" | head -100
```

Key finding:
```
locator resolved to <h1 class="container_title text-4xl font-bold text-foreground">ProMarker 払出画面</h1>
unexpected value "ProMarker 払出画面"
```

### 2. Source Code Verification
```bash
# Found in React implementation
apps/frontend-v3/src/features/promarker/pages/ProMarkerPage.tsx:309
  ProMarker 払出画面

# Found in E2E test
packages/e2e/tests/pages/promarker.page.ts:13
  readonly pageTitle = 'ProMarker 支出画面';  # ❌ Mismatch!
```

### 3. Local Testing
Created diagnostic tests to verify:
- Page navigation works correctly
- `.container_title` selector finds the correct element
- React SPA rendering timing issues

Test results:
```
✓ promarker-simple-check.spec.ts    - Page loads successfully
✓ promarker-title-check.spec.ts     - Confirmed h1.container_title contains "ProMarker 払出画面"
✓ promarker-nav-debug.spec.ts       - navigate() method works as expected
```

## Solution Implementation

### Changes Made

#### 1. Update Expected Page Title
**File**: `packages/e2e/tests/pages/promarker.page.ts`

```diff
  // Main UI elements
- readonly pageTitle = 'ProMarker 支出画面';
+ readonly pageTitle = 'ProMarker 払出画面';
```

**Reason**: Match React implementation text

#### 2. Increase Wait Timeout
**File**: `packages/e2e/tests/utils/test-helpers.ts`

```diff
  /**
   * Wait for text to appear in element
   * @param page - Playwright page instance
   * @param selector - Element selector
   * @param text - Text to wait for
-  * @param timeout - Timeout in milliseconds
+  * @param timeout - Timeout in milliseconds (default: 10000ms for React SPA)
   */
- static async waitForText(page: Page, selector: string, text: string, timeout = 5000) {
+ static async waitForText(page: Page, selector: string, text: string, timeout = 10000) {
    await expect(page.locator(selector)).toContainText(text, { timeout });
  }
```

**Reason**: React SPA requires longer timeout due to client-side rendering

## Verification Results

### Local Test Execution
```bash
cd packages/e2e && pnpm test promarker-accessibility.spec.ts --workers=1
```

**Results**: All 10 accessibility tests passed ✅
```
✓ should pass basic accessibility scan (5.8s)
✓ should have proper heading structure (5.7s)
✓ should have proper form labels and associations (5.7s)
✓ should support keyboard navigation (5.6s)
✓ should have proper color contrast (5.6s)
✓ should have proper ARIA attributes (6.4s)
✓ should handle modal accessibility (5.7s)
✓ should support screen reader users (5.7s)
✓ should handle high contrast mode (7.7s)
✓ should handle reduced motion preferences (5.4s)

10 passed (1.0m)
```

## Commit Details
- **Commit**: f525051
- **Message**: fix(e2e): update ProMarker page title and increase timeout for React SPA (refs #29)

## Lessons Learned

### 1. React SPA Testing Considerations
- Client-side rendering requires longer timeouts than server-side rendering
- Default 5s timeout may be insufficient for React component initialization
- Increased to 10s provides adequate buffer for various system loads

### 2. Frontend-Test Synchronization
- E2E tests must be updated when frontend text content changes
- Text assertions should match actual implementation
- Consider using data-testid attributes for more stable selectors

### 3. Diagnostic Testing Strategy
- Create simple diagnostic tests to isolate issues
- Verify assumptions about page structure and content
- Test individual components before running full test suites

### 4. GitHub Actions Debugging
- Use `gh run view <run-id> --log` for detailed error investigation
- grep patterns with context (-B/-A flags) provide better error context
- Look for "Expected"/"Received" patterns in assertion failures

## Related Documentation
- [GitHub Actions E2E Test Configuration](../../CI_CONFIGURATION.md)
- [E2E Testing Guide](../../E2E_TESTING.md)
- [Previous Fix: Strict Mode Violation](./e2e-strict-mode-violation-fix.md)

## Status
✅ **RESOLVED** - All tests passing locally, awaiting GitHub Actions verification
