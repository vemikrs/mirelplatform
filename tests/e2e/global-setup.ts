import { FullConfig } from '@playwright/test';

/**
 * Global setup for E2E tests
 * This runs once before all tests across all workers
 */
async function globalSetup(config: FullConfig) {
  console.log('🚀 Starting E2E test environment setup...');
  
  // Set timezone
  process.env.TZ = 'Asia/Tokyo';
  
  // Additional setup if needed
  console.log('✅ E2E test environment setup completed');
}

export default globalSetup;