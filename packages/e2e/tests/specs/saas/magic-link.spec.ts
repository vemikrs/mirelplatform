
import { test, expect } from '@playwright/test';

test.describe('Magic Link Verification', () => {
  // TODO: Fix Auth Mocking for Login flow
  test.skip('should handle LOGIN purpose and redirect to home', async ({ page }) => {
    // Mock the magic verify API endpoint
    await page.route('**/mapi/auth/otp/magic-verify', async route => {
      const json = {
        data: {
          verified: true,
          purpose: 'LOGIN',
          email: 'user@example.com',
          auth: {
            user: { userId: 'u1', email: 'user@example.com' },
            currentTenant: { tenantId: 't1' },
            tokens: { accessToken: 'fake-jwt', refreshToken: 'fake-refresh' }
          }
        },
        messages: ['Verified']
      };
      await route.fulfill({ json });
    });

    // Mock profile fetch which happens after login
    await page.route('**/mapi/users/me', async route => {
        await route.fulfill({ 
            json: { 
                user: { userId: 'u1', email: 'user@example.com' },
                tenants: [],
                licenses: []
            } 
        });
    });

    await page.route('**/mapi/users/me/tenants', async route => {
        await route.fulfill({ json: [] });
    });

    await page.route('**/mapi/users/me/licenses', async route => {
        await route.fulfill({ json: [] });
    });

    // Visit the magic link URL
    await page.goto('/auth/magic-verify?token=valid-token');

    // Should redirect to home (or dashboard)
    // The implementation redirects to '/'
    await expect(page).toHaveURL('/');
    
    // Check if we are "logged in" (e.g. by checking local storage or UI element, 
    // but here we just checked the redirect occurred after API success)
  });

  test('should handle PASSWORD_RESET purpose and redirect to reset page', async ({ page }) => {
    // Mock the magic verify API endpoint
    await page.route('**/mapi/auth/otp/magic-verify', async route => {
        const json = {
          data: {
            verified: true,
            purpose: 'PASSWORD_RESET',
            email: 'user@example.com'
          },
          messages: ['Verified']
        };
        await route.fulfill({ json });
    });

    // Visit the magic link URL
    await page.goto('/auth/magic-verify?token=reset-token');

    // Should redirect to password reset verify page with state
    await expect(page).toHaveURL(/\/auth\/password-reset-verify/);
    
    // Check if the page is in "password" step (inputting new password)
    // The "verify" step asks for 6-digit code.
    // The "password" step asks for "新しいパスワード" (New Password)
    await expect(page.getByLabel('新しいパスワード')).toBeVisible();
    await expect(page.getByLabel('認証コード')).not.toBeVisible();
  });

  test('should handle invalid token and show error', async ({ page }) => {
    // Mock failure
    await page.route('**/mapi/auth/otp/magic-verify', async route => {
        await route.fulfill({ 
            status: 400,
            json: { 
                errors: ['無効または期限切れのリンクです']
            } 
        });
    });

    await page.goto('/auth/magic-verify?token=invalid-token');

    // Should show error message
    await expect(page.getByText('検証エラー')).toBeVisible();
    await expect(page.getByText('無効または期限切れのリンクです')).toBeVisible();
    
    // Should show link to login
    await expect(page.getByRole('button', { name: 'ログイン画面へ戻る' })).toBeVisible();
  });
});
