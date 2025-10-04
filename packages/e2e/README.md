# ProMarker E2E Testing

This workspace contains end-to-end tests for the ProMarker platform using Playwright.

## Prerequisites

- Node.js 18.x (enforced by engines field)
- Java 21 (for backend)
- Backend and frontend services running

## Installation

```bash
# Install dependencies
npm install

# Install Playwright browsers
npm run install:browsers
```

## Running Tests

```bash
# Run all tests
npm test

# Run tests with UI mode
npm run test:ui

# Run tests in headed mode (see browser)
npm run test:headed

# Debug tests
npm run test:debug

# View test report
npm run test:report

# Generate new tests with Codegen
npm run test:codegen
```

## Environment Variables

- `E2E_BASE_URL`: Base URL for the application (default: http://localhost:3000)

## Test Structure

```
tests/
├── smoke.spec.ts           # Basic smoke tests
├── specs/                  # Feature-specific test specs
│   ├── promarker-basic.spec.ts
│   ├── promarker-workflow.spec.ts
│   ├── promarker-accessibility.spec.ts
│   └── ...
├── pages/                  # Page Object Model
│   ├── base.page.ts
│   └── promarker.page.ts
├── fixtures/               # Test data and fixtures
├── utils/                  # Utility functions
└── global-setup.ts         # Global test setup

## Configuration

Tests are configured via `playwright.config.ts`:
- Japanese locale (ja-JP)
- Asia/Tokyo timezone
- Chromium browser by default
- Screenshots and videos on failure
- HTML report generation

## CI Integration

Tests run automatically in CI:
1. Backend and frontend are started
2. Services health is checked
3. E2E tests execute from this workspace
4. Reports are uploaded as artifacts

## Writing Tests

Follow the Page Object Model pattern:

```typescript
import { test, expect } from '@playwright/test';
import { ProMarkerPage } from '../pages/promarker.page';

test('my test', async ({ page }) => {
  const proMarkerPage = new ProMarkerPage(page);
  await proMarkerPage.navigate();
  await proMarkerPage.verifyPageLoaded();
  // ... your test logic
});
```

## Troubleshooting

### Tests fail to start
- Ensure backend is running on port 3000
- Ensure frontend is running on port 8080
- Check E2E_BASE_URL is set correctly

### Browser not found
- Run `npm run install:browsers`

### Node version errors
- Use Node 18.x: `nvm use 18` or check `.nvmrc`
