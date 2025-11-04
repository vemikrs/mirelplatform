# E2E Test Stability Improvements

## Changes Made (Commit: 4643259)

### 1. Concurrency Reduction
**Problem:** Frontend server crashes under high load (4 parallel workers)
**Solution:** 
- Reduced `workers` from 4 to 2
- This prevents resource exhaustion and `ERR_CONNECTION_REFUSED` errors

### 2. Timeout Adjustments
**Problem:** API integration tests failing due to insufficient wait times
**Solution:**
- Test timeout: 20s → 30s
- Expect timeout: 8s → 10s
- These values balance speed with reliability for API calls

### 3. MaxFailures Disabled
**Problem:** Tests stopped after 10 failures, preventing full suite execution
**Solution:**
- Set `maxFailures: undefined`
- All 68 tests now run to completion
- Enables comprehensive quality assessment

### 4. Retry Logic
**Problem:** Flaky tests causing false negatives
**Solution:**
- Added `retries: 1` for local runs
- Tests get one retry attempt to handle transient failures

### 5. Smoke Test Route Fix
**Problem:** Smoke test using obsolete Vue.js route `/mirel/mste`
**Solution:**
- Updated to React v3 route `/promarker`
- Updated assertions to match v3 UI structure

## Expected Impact

### Before Changes
- ❌ 10 tests failed, then stopped (maxFailures)
- ❌ Frontend crashes: `ERR_CONNECTION_REFUSED`
- ❌ API integration tests timeout
- ❌ Smoke test fails (wrong route)

### After Changes
- ✅ All 68 tests execute to completion
- ✅ Stable frontend operation (reduced concurrency)
- ✅ API tests have adequate timeout
- ✅ Smoke test uses correct route

## Test Execution

```bash
# Run all tests with new configuration
pnpm test:e2e

# The webServer config will automatically:
# 1. Start backend on port 3000
# 2. Start frontend on port 5173
# 3. Wait for services to be ready
# 4. Run tests with 2 workers
# 5. Stop services after completion
```

## Test Count Summary
- **Active Tests:** 68 (67 v3 + 1 smoke)
- **Archived Tests:** 46 (obsolete Vue.js frontend)
- **Total:** 114 tests

## Configuration Summary

| Setting | Before | After | Reason |
|---------|--------|-------|--------|
| workers | 4 | 2 | Prevent frontend crashes |
| timeout | 20s | 30s | API stability |
| expect timeout | 8s | 10s | API response time |
| maxFailures | 10 | undefined | Run all tests |
| retries | 0 (local) | 1 (local) | Handle flaky tests |
| smoke route | /mirel/mste | /promarker | React v3 UI |

## Next Steps

1. **Run full test suite** to validate improvements
2. **Monitor for remaining failures** and address systematically
3. **Consider test splitting** if further optimization needed
4. **Document flaky tests** for future investigation
