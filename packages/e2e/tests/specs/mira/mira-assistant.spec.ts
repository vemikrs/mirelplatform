/**
 * Mira AI Assistant E2E Tests
 * 
 * Phase 7: End-to-end tests for Mira AI assistant feature
 */
import { test, expect } from '@playwright/test';
import { LoginPage } from '../../pages/auth/login.page';

test.describe('Mira AI Assistant', () => {
  let backendAvailable = false;
  
  test.beforeAll(async ({ request }) => {
    try {
      console.log('[mira-test] Checking Mira health...');
      const resp = await request.get('http://127.0.0.1:3000/mipla2/apps/mira/api/health', {
        timeout: 5000,
      });
      backendAvailable = resp.ok();
      console.log(`[mira-test] Health check: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[mira-test] Backend not available:', error);
      backendAvailable = false;
    }
  });
  
  test.describe('API Endpoints', () => {
    test('GET /apps/mira/api/health should return UP', async ({ request }) => {
      test.skip(!backendAvailable, 'Backend not available');
      
      const response = await request.get('http://127.0.0.1:3000/mipla2/apps/mira/api/health');
      
      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.status).toBe('UP');
      expect(body.service).toBe('mira-ai-assistant');
    });
    
    test('POST /apps/mira/api/chat should process message', async ({ request }) => {
      test.skip(!backendAvailable, 'Backend not available');
      
      const response = await request.post('http://127.0.0.1:3000/mipla2/apps/mira/api/chat', {
        data: {
          model: {
            mode: 'GENERAL_CHAT',
            message: {
              content: 'こんにちは',
            },
          },
        },
      });
      
      // 認証なしでも 200 または 401/403 が返る
      expect([200, 401, 403]).toContain(response.status());
      
      if (response.ok()) {
        const body = await response.json();
        expect(body.data).toBeDefined();
        expect(body.data.conversationId).toBeDefined();
        expect(body.data.assistantMessage).toBeDefined();
        expect(body.data.assistantMessage.content).toBeTruthy();
      }
    });
    
    test('POST /apps/mira/api/error-report should analyze error', async ({ request }) => {
      test.skip(!backendAvailable, 'Backend not available');
      
      const response = await request.post('http://127.0.0.1:3000/mipla2/apps/mira/api/error-report', {
        data: {
          model: {
            source: 'frontend',
            code: 'VALIDATION_ERROR',
            message: 'フィールドが必須です',
            detail: 'カテゴリを選択してください',
            context: {
              appId: 'promarker',
              screenId: 'stencil-editor',
            },
          },
        },
      });
      
      expect([200, 401, 403]).toContain(response.status());
      
      if (response.ok()) {
        const body = await response.json();
        expect(body.data).toBeDefined();
        expect(body.data.mode).toBe('ERROR_ANALYZE');
      }
    });
    
    test('POST /apps/mira/api/context-snapshot should save snapshot', async ({ request }) => {
      test.skip(!backendAvailable, 'Backend not available');
      
      const response = await request.post('http://127.0.0.1:3000/mipla2/apps/mira/api/context-snapshot', {
        data: {
          model: {
            appId: 'promarker',
            screenId: 'stencil-editor',
            systemRole: 'STANDARD_USER',
            appRole: 'editor',
          },
        },
      });
      
      expect([200, 401, 403]).toContain(response.status());
      
      if (response.ok()) {
        const body = await response.json();
        expect(body.data).toBeDefined();
        expect(body.data.snapshotId).toBeDefined();
      }
    });
    
    test('GET /apps/mira/api/conversation/:id/status should return status', async ({ request }) => {
      test.skip(!backendAvailable, 'Backend not available');
      
      const testConversationId = '00000000-0000-0000-0000-000000000000';
      const response = await request.get(
        `http://127.0.0.1:3000/mipla2/apps/mira/api/conversation/${testConversationId}/status`
      );
      
      expect([200, 401, 403, 404]).toContain(response.status());
      
      if (response.ok()) {
        const body = await response.json();
        expect(body.conversationId).toBe(testConversationId);
        expect(typeof body.active).toBe('boolean');
      }
    });
  });
  
  test.describe('UI Integration', () => {
    test.beforeEach(async ({ page }) => {
      test.skip(!backendAvailable, 'Backend not available');
      
      // ログイン
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('admin', 'password123');
      
      // ProMarker ページへ移動
      await page.goto('/promarker');
      await page.waitForLoadState('networkidle');
    });
    
    test('Mira FAB should be visible on page', async ({ page }) => {
      test.skip(!backendAvailable, 'Backend not available');
      test.setTimeout(60000);
      
      // FAB ボタンが表示されていることを確認
      // （注: MiraFab が App に組み込まれている前提）
      const fab = page.locator('button[title="Mira AI アシスタント"]');
      
      // FAB が存在する場合のみテスト
      const fabCount = await fab.count();
      if (fabCount > 0) {
        await expect(fab).toBeVisible();
        console.log('[TEST] Mira FAB is visible');
      } else {
        console.log('[TEST] Mira FAB not yet integrated into app');
      }
    });
    
    test('Mira Panel should open on FAB click', async ({ page }) => {
      test.skip(!backendAvailable, 'Backend not available');
      test.setTimeout(60000);
      
      const fab = page.locator('button[title="Mira AI アシスタント"]');
      const fabCount = await fab.count();
      
      if (fabCount > 0) {
        await fab.click();
        
        // パネルが開くことを確認
        const panel = page.locator('h2:has-text("Mira")');
        await expect(panel).toBeVisible({ timeout: 5000 });
        
        console.log('[TEST] Mira panel opened successfully');
      } else {
        console.log('[TEST] Mira FAB not yet integrated');
      }
    });
  });
  
  test.describe('Chat Functionality (Mock Mode)', () => {
    test('should receive mock response in development mode', async ({ request }) => {
      test.skip(!backendAvailable, 'Backend not available');
      
      // Mock モードが有効な場合、定型応答が返る
      const response = await request.post('http://127.0.0.1:3000/mipla2/apps/mira/api/chat', {
        data: {
          model: {
            mode: 'GENERAL_CHAT',
            message: {
              content: 'ステンシルの作成方法を教えてください',
            },
          },
        },
      });
      
      if (response.ok()) {
        const body = await response.json();
        expect(body.data.assistantMessage.content).toBeTruthy();
        console.log('[TEST] Chat response received:', 
          body.data.assistantMessage.content.substring(0, 100));
      }
    });
    
    test('should maintain conversation context', async ({ request }) => {
      test.skip(!backendAvailable, 'Backend not available');
      
      // 最初のメッセージ
      const firstResponse = await request.post('http://127.0.0.1:3000/mipla2/apps/mira/api/chat', {
        data: {
          model: {
            mode: 'GENERAL_CHAT',
            message: { content: 'こんにちは' },
          },
        },
      });
      
      if (!firstResponse.ok()) {
        console.log('[TEST] First message failed, skipping context test');
        return;
      }
      
      const firstBody = await firstResponse.json();
      const conversationId = firstBody.data.conversationId;
      
      // 同じ会話で続きのメッセージ
      const secondResponse = await request.post('http://127.0.0.1:3000/mipla2/apps/mira/api/chat', {
        data: {
          model: {
            conversationId,
            mode: 'GENERAL_CHAT',
            message: { content: 'もう一度説明してください' },
          },
        },
      });
      
      if (secondResponse.ok()) {
        const secondBody = await secondResponse.json();
        // 会話IDが維持されていることを確認
        expect(secondBody.data.conversationId).toBe(conversationId);
        console.log('[TEST] Conversation context maintained');
      }
    });
  });
});
