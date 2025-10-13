import { FullConfig } from '@playwright/test';

/**
 * Global teardown for E2E tests
 * This runs once after all tests across all workers
 */
async function globalTeardown(config: FullConfig) {
  console.log('🧹 Starting E2E test environment teardown...');
  
  // Cleanup if needed
  
  console.log('✅ E2E test environment teardown completed');
}

export default globalTeardown;