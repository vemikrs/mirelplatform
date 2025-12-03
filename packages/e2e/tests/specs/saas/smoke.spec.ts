import { test, expect } from '@playwright/test';

/**
 * SaaS Authentication & Admin Smoke Tests
 * 
 * Purpose: Validate core SaaS functionality with minimal scenarios
 * - Authentication flow (login, /auth/me)
 * - Password reset flow
 * - Admin access control
 * 
 * Prerequisites:
 * - Backend running on http://localhost:3000
 * - Frontend running on http://localhost:5173
 * - Test data loaded from data.sql:
 *   - admin@example.com / password123 (ADMIN role)
 *   - user@example.com / password123 (MEMBER role)
 */

test.describe.skip('SaaS Smoke Tests', () => {
  
  // ====================
  // Smoke Test 1: Authentication Basic Flow
  // ====================
  
  test('should login and verify user via /auth/me API', async ({ page }) => {
    // 1. Navigate to login page
    await page.goto('http://localhost:5173/login');
    await expect(page).toHaveURL(/\/login/);

    // 2. Fill in credentials
    await page.fill('input[type="email"]', 'user@example.com');
    await page.fill('input[type="password"]', 'password123');

    // 3. Wait for /auth/me API call
    const authMePromise = page.waitForResponse(
      (resp) => resp.url().includes('/mapi/auth/me') && resp.status() === 200,
      { timeout: 10000 }
    );

    // 4. Submit login form
    await page.click('button[type="submit"]');

    // 5. Verify redirect to dashboard
    await page.waitForURL('http://localhost:5173/', { timeout: 10000 });

    // 6. Verify /auth/me response
    const authMeResponse = await authMePromise;
    const authMeData = await authMeResponse.json();
    
    expect(authMeData).toHaveProperty('user');
    expect(authMeData.user.email).toBe('user@example.com');
    expect(authMeData.user.displayName).toBe('Regular User');

    // 7. Verify user menu displays correctly
    await expect(page.locator('text=Regular User')).toBeVisible();
  });

  // ====================
  // Smoke Test 2: Password Reset Flow
  // ====================
  
  test('should display password reset request page', async ({ page }) => {
    // 1. Navigate to password reset page
    await page.goto('http://localhost:5173/password-reset');
    await expect(page).toHaveURL(/\/password-reset/);

    // 2. Fill in email
    await page.fill('input[type="email"]', 'user@example.com');

    // 3. Submit request
    await page.click('button:has-text("Send reset instructions")');

    // 4. Verify success message appears
    await expect(page.locator('text=Check your email')).toBeVisible({ timeout: 5000 });

    // Note: Actual token-based reset requires email service or manual DB query
    // This test validates UI flow only
  });

  // ====================
  // Smoke Test 3: Admin Access Control
  // ====================
  
  test('should allow admin to access /admin/users', async ({ page }) => {
    // 1. Login as admin
    await page.goto('http://localhost:5173/login');
    await page.fill('input[type="email"]', 'admin@example.com');
    await page.fill('input[type="password"]', 'password123');
    
    const authMePromise = page.waitForResponse(
      (resp) => resp.url().includes('/mapi/auth/me') && resp.status() === 200
    );
    
    await page.click('button[type="submit"]');
    await page.waitForURL('http://localhost:5173/');
    await authMePromise;

    // 2. Navigate to admin users page
    await page.goto('http://localhost:5173/admin/users');

    // 3. Wait for API call to fetch users
    const adminUsersPromise = page.waitForResponse(
      (resp) => resp.url().includes('/mapi/admin/users') && resp.status() === 200,
      { timeout: 10000 }
    );

    const adminUsersResponse = await adminUsersPromise;
    const adminUsersData = await adminUsersResponse.json();

    // 4. Verify user list is returned
    expect(adminUsersData).toHaveProperty('result');
    expect(Array.isArray(adminUsersData.result)).toBe(true);
    expect(adminUsersData.result.length).toBeGreaterThan(0);

    // 5. Verify table is visible
    await expect(page.locator('table')).toBeVisible({ timeout: 5000 });
  });

  test('should deny regular user access to /admin/users', async ({ page }) => {
    // 1. Login as regular user
    await page.goto('http://localhost:5173/login');
    await page.fill('input[type="email"]', 'user@example.com');
    await page.fill('input[type="password"]', 'password123');
    
    await page.click('button[type="submit"]');
    await page.waitForURL('http://localhost:5173/');

    // 2. Try to access admin page
    await page.goto('http://localhost:5173/admin/users');

    // 3. Wait for 403 error or redirect
    const response = await page.waitForResponse(
      (resp) => resp.url().includes('/mapi/admin/users'),
      { timeout: 10000 }
    );

    // 4. Verify 403 Forbidden
    expect(response.status()).toBe(403);

    // 5. Verify error message is displayed
    await expect(page.locator('text=/403|Forbidden|Access denied/i')).toBeVisible({ timeout: 5000 });
  });
});
