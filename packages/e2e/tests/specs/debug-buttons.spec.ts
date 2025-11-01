import { test } from '@playwright/test';

test('debug button texts', async ({ page }) => {
  await page.goto('/mirel/mste');
  await page.waitForTimeout(3000);
  
  const buttons = await page.locator('button').all();
  console.log(`\n=== Found ${buttons.length} buttons ===`);
  
  for (let i = 0; i < buttons.length; i++) {
    const text = await buttons[i].textContent();
    const testid = await buttons[i].getAttribute('data-testid');
    console.log(`Button ${i}: "${text}" (data-testid="${testid}")`);
  }
});
