# Old Test Files to Skip

These test files target the obsolete Vue.js frontend at `/mirel/mste` which has been replaced by React v3 at `/promarker`.

## Files:
1. tests/specs/promarker-accessibility.spec.ts (10 tests) -> Covered by v3/routing.spec.ts
2. tests/specs/promarker-basic.spec.ts (7 tests) -> Covered by v3/stencil-selection.spec.ts
3. tests/specs/promarker-full-workflow.spec.ts (7 tests) -> Covered by v3/complete-workflow.spec.ts
4. tests/specs/promarker-ui-enhancement.spec.ts (2 tests) -> Obsolete features
5. tests/specs/promarker-visual.spec.ts (12 tests) -> Visual regression for old UI
6. tests/specs/promarker-workflow.spec.ts (8 tests) -> Covered by v3/complete-workflow.spec.ts

Total: 46 tests to skip
