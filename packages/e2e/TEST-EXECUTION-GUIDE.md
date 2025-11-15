# E2E Test Execution Guide

## Quick Start

```bash
# Navigate to project root
cd /home/runner/work/mirelplatform/mirelplatform

# Run all E2E tests
pnpm test:e2e

# Run tests in UI mode (interactive)
pnpm test:e2e:ui

# View test report
pnpm test:e2e:report
```

## Test Configuration

### Current Settings (Optimized for Stability)

- **Workers:** 2 (parallel execution)
- **Timeout:** 30 seconds per test
- **Expect Timeout:** 10 seconds for assertions
- **Retries:** 1 retry for local runs
- **MaxFailures:** Disabled (run all tests)

### Automatic Server Management

The `webServer` configuration automatically:
1. Starts backend (Spring Boot) on port 3000
2. Starts frontend (Vite/React) on port 5173
3. Waits for both services to be ready
4. Runs tests
5. Stops services after completion

**Note:** Use `reuseExistingServer: true` to skip starting servers if already running.

## Test Structure

### Active Tests (68 total)
- **Smoke Test** (1): Basic application accessibility
- **API Integration** (8): Backend API communication
- **Form Validation** (13): Input validation logic
- **Stencil Selection** (6): Dropdown workflows
- **Parameter Input** (6): Dynamic form fields
- **JSON Editor** (5): JSON editing functionality
- **File Upload** (6): File upload features
- **Routing** (8): Navigation and routing
- **Hooks** (7): React hooks behavior
- **Complete Workflow** (5): End-to-end workflows
- **Console Errors** (1): Error detection
- **Debug API** (1): Debugging utilities
- **Simple Test** (1): Basic functionality

### Archived Tests (46 total)
Located in `tests/specs/_archived-vue-frontend/`
- These target the obsolete Vue.js frontend at `/mirel/mste`
- Kept for reference but excluded from execution

## Troubleshooting

### Frontend Crashes
**Symptom:** `ERR_CONNECTION_REFUSED` errors
**Solution:** Workers reduced to 2 to prevent resource exhaustion

### API Timeouts
**Symptom:** API integration tests fail with timeout
**Solution:** Test timeout increased to 30s, expect timeout to 10s

### Flaky Tests
**Symptom:** Tests pass/fail intermittently
**Solution:** Retry logic enabled (1 retry locally, 2 on CI)

### Backend Not Starting
**Symptom:** Tests skip with "Backend not available"
**Solution:** 
1. Check Java 21 is installed
2. Verify port 3000 is not in use
3. Check backend logs for errors

### Frontend Not Starting
**Symptom:** Cannot navigate to pages
**Solution:**
1. Verify port 5173 is not in use
2. Check `pnpm install` has been run
3. Verify frontend build succeeds

## Performance Tips

### Reduce Test Time
```bash
# Run specific test file
pnpm test:e2e tests/specs/promarker-v3/routing.spec.ts

# Run tests matching pattern
pnpm test:e2e --grep "should load"

# Skip slow tests
pnpm test:e2e --grep-invert "workflow"
```

### Debug Failing Tests
```bash
# Run in headed mode (see browser)
pnpm test:e2e:headed

# Run in debug mode (step through)
pnpm test:e2e --debug

# Run in UI mode (interactive)
pnpm test:e2e:ui
```

## CI/CD Integration

### GitHub Actions
Configuration automatically adjusts for CI:
- **Workers:** 1 (sequential execution)
- **Retries:** 2 (more resilient)
- **Server reuse:** Disabled (clean environment)

### Environment Variables
- `CI=true`: Enables CI-specific settings
- `E2E_BASE_URL`: Override base URL (default: http://localhost:5173)

## Test Reports

### HTML Report
Generated at: `packages/e2e/playwright-report/index.html`
```bash
pnpm test:e2e:report
```

### JUnit XML
Generated at: `packages/e2e/test-results/junit.xml`
- For CI integration

### JSON Results
Generated at: `packages/e2e/test-results/results.json`
- For programmatic analysis

## Best Practices

1. **Run tests locally** before pushing
2. **Check test reports** for failures
3. **Investigate flaky tests** and fix root causes
4. **Keep tests fast** - avoid unnecessary waits
5. **Use data-testid** for stable selectors
6. **Mock external services** when possible
7. **Clean up test data** after tests
8. **Document test intent** with clear descriptions

## Support

- **Documentation:** See `README-test-organization.md`
- **Changelog:** See `CHANGELOG-stability-improvements.md`
- **Issues:** Report to GitHub issue tracker
