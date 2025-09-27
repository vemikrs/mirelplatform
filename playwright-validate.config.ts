import { defineConfig } from '@playwright/test';

/**
 * Validation configuration for Playwright setup
 * This config can be used to validate the setup without requiring browsers
 */
export default defineConfig({
  testDir: './tests/e2e',
  
  /* Run only validation tests */
  testMatch: '**/promarker-ui-enhancement.spec.ts',
  
  /* Run tests in files serially */
  fullyParallel: false,
  
  /* Don't retry on failure for validation */
  retries: 0,
  
  /* Single worker for validation */
  workers: 1,
  
  /* Simple reporter for validation */
  reporter: [['list']],
  
  /* Shared settings */
  use: {
    baseURL: 'http://localhost:8080',
    locale: 'ja-JP',
    timezoneId: 'Asia/Tokyo',
    viewport: { width: 1280, height: 720 },
    ignoreHTTPSErrors: true,
  },

  /* Configure for validation only */
  projects: [
    {
      name: 'validation',
      use: {
        // Use any available browser or skip if none
      },
    },
  ],

  /* Timeout settings */
  timeout: 10 * 1000,
  expect: {
    timeout: 2000,
  },
});