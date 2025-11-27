import { test, expect } from '@playwright/test';

/**
 * OTP Login Flow E2E Test
 * 
 * Purpose: Validate OTP (One-Time Password) authentication flow
 * - OTP request
 * - OTP verification
 * - JWT token storage
 * - Tenant/License data loading
 * 
 * Prerequisites:
 * - Backend running on http://localhost:3000
 * - Frontend running on http://localhost:5173
 * - MailHog running on http://localhost:8025
 * - Test user: user@example.com
 */

test.describe('OTP Login Flow', () => {
  
  test('should complete full OTP login flow and load user data', async ({ page, request }) => {
    // ============================================
    // Step 1: Navigate to login page
    // ============================================
    await page.goto('http://localhost:5173/login');
    await expect(page).toHaveURL(/\/login/);

    // ============================================
    // Step 2: Click on OTP tab (if needed)
    // ============================================
    // Assuming UnifiedLoginPage shows OTP by default
    // If tab switching is needed, add: await page.click('button:has-text("Email Code")');

    // ============================================
    // Step 3: Request OTP
    // ============================================
    const otpRequestPromise = page.waitForResponse(
      (resp) => resp.url().includes('/mapi/auth/otp/request') && resp.status() === 200,
      { timeout: 10000 }
    );

    await page.fill('input[type="email"]', 'user@example.com');
    await page.click('button:has-text("Send")');

    const otpRequestResponse = await otpRequestPromise;
    const otpRequestData = await otpRequestResponse.json();
    
    console.log('[E2E] OTP Request Response:', otpRequestData);
    expect(otpRequestData).toHaveProperty('data');
    expect(otpRequestData.data).toHaveProperty('requestId');

    // ============================================
    // Step 4: Fetch OTP code from MailHog
    // ============================================
    await page.waitForTimeout(2000); // Wait for email delivery

    const mailhogResponse = await request.get('http://localhost:8025/api/v2/messages');
    const mailhogData = await mailhogResponse.json();
    
    console.log('[E2E] MailHog messages count:', mailhogData.items?.length);
    
    const latestEmail = mailhogData.items?.[0];
    expect(latestEmail).toBeDefined();
    
    const emailBody = latestEmail.Content.Body;
    const otpCodeMatch = emailBody.match(/\d{6}/);
    expect(otpCodeMatch).not.toBeNull();
    
    const otpCode = otpCodeMatch![0];
    console.log('[E2E] Extracted OTP Code:', otpCode);

    // ============================================
    // Step 5: Navigate to OTP verify page
    // ============================================
    await page.waitForURL(/\/auth\/otp-verify/);

    // ============================================
    // Step 6: Enter OTP code
    // ============================================
    // Assuming 6 separate input fields or single input
    const otpInputs = page.locator('input[type="text"]');
    const inputCount = await otpInputs.count();
    
    if (inputCount === 6) {
      // Separate inputs for each digit
      for (let i = 0; i < 6; i++) {
        await otpInputs.nth(i).fill(otpCode[i]);
      }
    } else {
      // Single input field
      await otpInputs.first().fill(otpCode);
    }

    // ============================================
    // Step 7: Submit OTP and monitor API calls
    // ============================================
    const otpVerifyPromise = page.waitForResponse(
      (resp) => resp.url().includes('/mapi/auth/otp/verify') && resp.status() === 200,
      { timeout: 10000 }
    );

    const tenantsPromise = page.waitForResponse(
      (resp) => resp.url().includes('/mapi/users/me/tenants'),
      { timeout: 10000 }
    );

    const licensesPromise = page.waitForResponse(
      (resp) => resp.url().includes('/mapi/users/me/licenses'),
      { timeout: 10000 }
    );

    await page.click('button[type="submit"]');

    // ============================================
    // Step 8: Verify OTP verification response
    // ============================================
    const otpVerifyResponse = await otpVerifyPromise;
    const otpVerifyData = await otpVerifyResponse.json();
    
    console.log('[E2E] OTP Verify Response:', otpVerifyData);
    expect(otpVerifyData).toHaveProperty('data');
    expect(otpVerifyData.data).toHaveProperty('accessToken');
    expect(otpVerifyData.data).toHaveProperty('user');

    // ============================================
    // Step 9: Verify tenants/licenses API calls
    // ============================================
    try {
      const tenantsResponse = await tenantsPromise;
      const licensesResponse = await licensesPromise;

      console.log('[E2E] Tenants Response Status:', tenantsResponse.status());
      console.log('[E2E] Licenses Response Status:', licensesResponse.status());

      // EXPECTED: 200 OK with JWT in Authorization header
      // ACTUAL (BUG): 302 redirect to GitHub OAuth
      
      if (tenantsResponse.status() === 302) {
        const location = tenantsResponse.headers()['location'];
        console.error('[E2E] BUG DETECTED: Tenants endpoint returned 302 redirect to:', location);
        expect(tenantsResponse.status()).toBe(200); // This will fail and highlight the bug
      }

      expect(tenantsResponse.status()).toBe(200);
      expect(licensesResponse.status()).toBe(200);

      const tenantsData = await tenantsResponse.json();
      const licensesData = await licensesResponse.json();

      console.log('[E2E] Tenants Data:', tenantsData);
      console.log('[E2E] Licenses Data:', licensesData);

      expect(Array.isArray(tenantsData)).toBe(true);
      expect(Array.isArray(licensesData)).toBe(true);

    } catch (error) {
      console.error('[E2E] Failed to fetch tenants/licenses:', error);
      throw error;
    }

    // ============================================
    // Step 10: Verify redirect to home page
    // ============================================
    await page.waitForURL('http://localhost:5173/home', { timeout: 10000 });
    await expect(page).toHaveURL(/\/home/);

    // ============================================
    // Step 11: Verify user is authenticated
    // ============================================
    await expect(page.locator('text=Regular User')).toBeVisible();
  });

  test('should show JWT token in Authorization header after OTP login', async ({ page, request }) => {
    // Navigate and complete OTP login (abbreviated)
    await page.goto('http://localhost:5173/login');
    await page.fill('input[type="email"]', 'user@example.com');
    await page.click('button:has-text("Send")');

    await page.waitForTimeout(2000);

    const mailhogResponse = await request.get('http://localhost:8025/api/v2/messages');
    const mailhogData = await mailhogResponse.json();
    const otpCode = mailhogData.items[0].Content.Body.match(/\d{6}/)[0];

    await page.waitForURL(/\/auth\/otp-verify/);
    const otpInputs = page.locator('input[type="text"]');
    const inputCount = await otpInputs.count();
    
    if (inputCount === 6) {
      for (let i = 0; i < 6; i++) {
        await otpInputs.nth(i).fill(otpCode[i]);
      }
    } else {
      await otpInputs.first().fill(otpCode);
    }

    // Monitor network request headers
    page.on('request', request => {
      if (request.url().includes('/mapi/users/me/tenants')) {
        const headers = request.headers();
        console.log('[E2E] Tenants Request Headers:', headers);
        
        // EXPECTED: Authorization: Bearer <JWT>
        // ACTUAL (BUG): Authorization header missing or invalid
        expect(headers['authorization']).toMatch(/^Bearer\s+[\w-]+\.[\w-]+\.[\w-]+$/);
      }
    });

    await page.click('button[type="submit"]');
    
    await page.waitForURL('http://localhost:5173/home', { timeout: 10000 });
  });
});
