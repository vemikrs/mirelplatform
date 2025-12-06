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
 * - MailHog API URL: process.env.MAILHOG_API_URL || 'http://localhost:8026'
 * - Test user: user@example.com
 * 
 * Note: Retries disabled due to rate limiting (6 requests per 60 seconds)
 */

test.describe('OTP Login Flow', () => {
  
  test('should complete full OTP login flow and load user data', async ({ page, request }) => {
    test.setTimeout(60000); // Increase timeout for rate limit handling
    
    // Use existing test user (required for OTP verify)
    const testEmail = 'user01@example.com';
    
    // Get MailHog API URL from environment variable (CI uses 8026, local may use 8025)
    const mailhogApiUrl = process.env.MAILHOG_API_URL || 'http://localhost:8025';
    console.log('[E2E] Using MailHog API URL:', mailhogApiUrl);
    
    // ============================================
    // Step 0: Clear MailHog messages
    // ============================================
    await request.delete(`${mailhogApiUrl}/api/v1/messages`);
    console.log('[E2E] MailHog messages cleared');
    
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

    await page.fill('input[type="email"]', testEmail);
    await page.click('button:has-text("認証コードを送信")');

    const otpRequestResponse = await otpRequestPromise;
    const otpRequestData = await otpRequestResponse.json();
    
    console.log('[E2E] OTP Request Response:', otpRequestData);
    expect(otpRequestData).toHaveProperty('data');
    expect(otpRequestData.data).toHaveProperty('requestId');

    // ============================================
    // Step 4: Fetch OTP code from MailHog
    // ============================================
    await page.waitForTimeout(2000); // Wait for email delivery

    const mailhogResponse = await request.get(`${mailhogApiUrl}/api/v2/messages`);
    const mailhogData = await mailhogResponse.json();
    
    console.log('[E2E] MailHog messages count:', mailhogData.items?.length);
    
    // Since we cleared MailHog before the test, the first (and only) email is our OTP
    const latestEmail = mailhogData.items?.[0];
    expect(latestEmail).toBeDefined();
    
    const emailBody = latestEmail.Content.Body;
    console.log('[E2E] Email Body (first 500 chars):', emailBody.substring(0, 500));
    
    // Extract OTP from HTML: <div class="code">XXXXXX</div>
    // The email body is quoted-printable encoded, so we need to match within HTML
    const otpCodeMatch = emailBody.match(/<div class=3D"code">(\d{6})<\/div>/);
    expect(otpCodeMatch).not.toBeNull();
    
    const otpCode = otpCodeMatch![1];
    console.log('[E2E] Extracted OTP Code from HTML:', otpCode);
    console.log('[E2E] Full regex match:', otpCodeMatch);

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
    
    console.log('[E2E] OTP input fields count:', inputCount);
    console.log('[E2E] OTP Code to enter:', otpCode);
    
    if (inputCount === 6) {
      // Separate inputs for each digit
      for (let i = 0; i < 6; i++) {
        await otpInputs.nth(i).fill(otpCode[i]);
      }
    } else {
      // Single input field
      await otpInputs.first().fill(otpCode);
    }

    // Wait for input to be filled
    await page.waitForTimeout(500);

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

    // Click submit button
    await page.click('button[type="submit"]');

    // ============================================
    // Step 8: Verify OTP verification response
    // ============================================
    const otpVerifyResponse = await otpVerifyPromise;
    const otpVerifyData = await otpVerifyResponse.json();
    
    console.log('[E2E] OTP Verify Response:', otpVerifyData);
    expect(otpVerifyData).toHaveProperty('data');
    expect(otpVerifyData.data).toHaveProperty('tokens');
    expect(otpVerifyData.data.tokens).toHaveProperty('accessToken');
    expect(otpVerifyData.data).toHaveProperty('user');
    expect(otpVerifyData.data).toHaveProperty('currentTenant');

    // ============================================
    // Step 9: Verify tenants/licenses API calls with Authorization header
    // ============================================
    try {
      const tenantsResponse = await tenantsPromise;
      const licensesResponse = await licensesPromise;

      console.log('[E2E] Tenants Response Status:', tenantsResponse.status());
      console.log('[E2E] Licenses Response Status:', licensesResponse.status());

      // Check for Authorization header (main fix verification)
      const tenantsRequest = tenantsResponse.request();
      const headers = tenantsRequest.headers();
      console.log('[E2E] Tenants Request Headers:', headers);
      
      expect(headers['authorization']).toBeDefined();
      expect(headers['authorization']).toMatch(/^Bearer\s+[\w-]+\.[\w-]+\.[\w-]+$/);
      
      // EXPECTED: 200 OK with JWT in Authorization header
      // ACTUAL (BUG BEFORE FIX): 302 redirect to GitHub OAuth
      
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
    // Step 11: Verify user is authenticated and redirected to /home
    // ============================================
    await page.waitForURL(/\/home/);
    
    // Verify user display name is visible (using first match)
    await expect(page.locator('text=Regular User').first()).toBeVisible();
  });
});
