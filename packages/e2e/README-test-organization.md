# E2E Test Organization Update

## Changes Made

### 1. Archived Obsolete Tests (46 tests)
Moved to `tests/specs/_archived-vue-frontend/`:
- `promarker-accessibility.spec.ts` - For old Vue.js `/mirel/mste` route
- `promarker-basic.spec.ts` - For old Vue.js `/mirel/mste` route
- `promarker-full-workflow.spec.ts` - For old Vue.js `/mirel/mste` route  
- `promarker-ui-enhancement.spec.ts` - Obsolete UI features
- `promarker-visual.spec.ts` - Visual regression for old UI
- `promarker-workflow.spec.ts` - For old Vue.js `/mirel/mste` route

These tests targeted the obsolete Vue.js frontend at `/mirel/mste` which has been replaced by React v3 at `/promarker`.

### 2. Active V3 Tests (67 tests)
Located in `tests/specs/promarker-v3/`:
- All tests target the current React v3 frontend at `/promarker`
- Tests include comprehensive coverage:
  - API integration
  - Form validation  
  - Stencil selection
  - Parameter input
  - JSON editor
  - File upload
  - Routing
  - Hooks
  - Complete workflows
  - Console error detection

### 3. Configuration Optimization
Updated `playwright.config.ts`:
- Increased parallel workers to 4 (from undefined/1)
- Optimized timeout to 20s (from variable 10-30s)
- Increased maxFailures to 10 (from 5)
- Set expect timeout to 8s (from variable 5-10s)

### 4. Test Count
- **Total active tests**: 68 (67 v3 + 1 smoke test)
- **Archived tests**: 46
- **Grand total**: 114 tests (not 121-123)

## Test Execution
```bash
# Run all active tests
pnpm test:e2e

# Run specific test suite
pnpm --filter e2e test tests/specs/promarker-v3/stencil-selection.spec.ts
```

## Next Steps
1. Ensure backend is properly started before running tests
2. Review and enable skipped tests that are marked as backend-dependent
3. Consider splitting test suites for parallel execution
