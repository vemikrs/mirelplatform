import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright configuration for ProMarker E2E tests
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './tests',
  testIgnore: ['**/tests/specs/_archived-vue-frontend/**'],  // Ignore archived Vue.js tests
  
  /* Run tests in files in parallel - controlled by workers */
  fullyParallel: true,
  
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 1,  // Retry once locally for flaky tests
  
  /* Reduce concurrent workers to prevent frontend crashes */
  workers: process.env.CI ? 1 : 2,  // Reduced from 4 to 2 to prevent resource exhaustion
  
  /* Disable maxFailures to run all tests */
  maxFailures: undefined,  // Run all tests to completion
  
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: 'list',
  
  /* Start dev servers automatically for local E2E runs only (CI handles server startup separately) */
  webServer: process.env.CI ? undefined : [
    {
      command: 'bash -lc "cd ../../ && SPRING_PROFILES_ACTIVE=e2e SERVER_PORT=3000 DATABASE_URL=jdbc:postgresql://localhost:5432/mirelplatform DATABASE_USER=mirel DATABASE_PASS=mirel REDIS_PORT=6379 SMTP_PORT=1025 ./gradlew --console=plain :backend:bootRun"',
      url: 'http://localhost:3000/mipla2/actuator/health',
      reuseExistingServer: true,
      timeout: 180_000
    },
    {
      command: 'bash -lc "cd ../../ && pnpm --filter frontend-v3 dev"',
      url: 'http://localhost:5173',
      reuseExistingServer: true,
      timeout: 120_000
    }
  ],
  
  /* Shared settings for all the projects below. */
  use: {
    /* Base URL - use E2E_BASE_URL env var or default to localhost:5173 (frontend-v3) */
    baseURL: process.env.E2E_BASE_URL || 'http://localhost:5173',
    
    /* Collect trace when retrying the failed test */
    trace: 'on-first-retry',
    
    /* Take screenshot on failure */
    screenshot: 'only-on-failure',
    
    /* Record video on failure */
    video: 'retain-on-failure',
    
    /* Locale and timezone settings for Japanese environment */
    locale: 'ja-JP',
    timezoneId: 'Asia/Tokyo',
    
    /* Viewport size */
    viewport: { width: 1280, height: 720 },
    
    /* Ignore HTTPS errors */
    ignoreHTTPSErrors: true,
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'chrome-stable',
      use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    },
  ],

  /* Test timeout - Extended for stability */
  timeout: 30 * 1000, // 30s - increased from 20s for more reliable API calls
  expect: {
    /* Maximum time expect() should wait for the condition to be met. */
    timeout: 10000, // 10s - increased from 8s for API integration tests
  },
});
