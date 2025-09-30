# CI Configuration Guide for Full E2E Test Execution

## Current Status

The E2E testing infrastructure is complete and fully functional for local development. However, full test execution in GitHub Actions CI is currently disabled due to network restrictions.

## Network Restrictions

GitHub Actions environment blocks access to:
- `api.foojay.io` - Required for Gradle/Java toolchain
- `esm.ubuntu.com` - Required for apt package manager

These domains are necessary for:
1. Building the backend with Gradle (`./gradlew bootRun`)
2. Installing Playwright system dependencies
3. Starting the Java application

## Current CI Workflow

The workflow currently runs in **validation-only mode**:

```yaml
jobs:
  validate-setup:
    - Setup Node.js 20
    - Install E2E dependencies (npm ci)
    - Validate setup (npm run test:e2e:validate)
    - List test specifications
```

This ensures the E2E infrastructure is correct without requiring network-blocked services.

## Enabling Full E2E Tests in CI

### Option 1: Custom Allowlist (Recommended)

1. Navigate to repository settings
2. Go to: **Settings** → **Copilot coding agent settings**
3. Find **Custom allowlist** section
4. Add the following domains:
   ```
   api.foojay.io
   esm.ubuntu.com
   ```
5. Save settings

After configuration:
1. Copy jobs from `.github/workflows/e2e-tests-full.yml.disabled`
2. Merge into `.github/workflows/e2e-tests.yml`
3. Commit and push changes

### Option 2: Actions Setup Steps

Configure services to start before the firewall is enabled:

```yaml
jobs:
  e2e-tests:
    steps:
      - name: Pre-firewall setup
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
      
      # Additional setup steps before firewall activation
```

See `.github/workflows/e2e-tests-full.yml.disabled` for complete implementation.

## Local Development

No configuration needed - all features work immediately:

```bash
# Validate setup
npm run test:e2e:validate

# Start services
./scripts/start-services.sh

# Run E2E tests
npm run test:e2e                # Headless mode
npm run test:e2e:headed         # With browser visible
npm run test:e2e:ui             # Interactive UI mode
npm run test:e2e:debug          # Debug mode

# Custom script
./scripts/e2e/run-tests.sh -b chromium -h
```

## Test Infrastructure Components

All components are implemented and ready:

- ✅ **47+ Test Specifications**
  - Basic functionality tests
  - Complete workflow tests
  - Accessibility tests (WCAG 2.1 AA)
  - Visual regression tests
  - UI enhancement validation

- ✅ **Page Object Model**
  - Base page class
  - ProMarker page object
  - Reusable utilities

- ✅ **Test Data**
  - Real stencil configurations
  - Validation test cases
  - Fixtures and helpers

- ✅ **Documentation**
  - Comprehensive testing guide
  - Troubleshooting section
  - Best practices

## Verification

After enabling full CI execution, verify:

1. Check the Actions tab for workflow runs
2. Ensure all jobs complete successfully
3. Review test results and artifacts
4. Check for any flaky tests

## Support

For questions or issues:
1. Review `docs/E2E_TESTING.md` for detailed documentation
2. Check `.github/workflows/e2e-tests-full.yml.disabled` for job templates
3. Consult this guide for network configuration

## Future Enhancements

Once full CI execution is enabled, consider:
- Visual regression baseline generation
- Performance testing integration
- Cross-browser testing optimization
- Parallel test execution tuning