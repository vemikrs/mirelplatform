import { test, expect } from '@playwright/test'
import { ProMarkerV3Page } from '../../pages/promarker-v3.page'

test.describe.skip('ProMarker v3 - JSON Editor', () => {
  let promarkerPage: ProMarkerV3Page
  let backendAvailable = false

  test.beforeAll(async ({ request }) => {
    try {
      console.log('[json-editor] Reloading stencil master...');
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      });
      backendAvailable = resp.ok();
      console.log(`[json-editor] Reload result: ${resp.status()}, available: ${backendAvailable}`);
    } catch (error) {
      console.error('[json-editor] Backend not available:', error);
      backendAvailable = false;
    }
  });
  
  test.beforeEach(async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping');
    promarkerPage = new ProMarkerV3Page(page)
    
    // API待機設定
    const responsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/suggest'),
      { timeout: 60000 }
    )
    
    await promarkerPage.navigate()
    await responsePromise
  })
  
  test('JSON編集ダイアログが開く', async ({ page }) => {
    await page.click('[data-testid="json-edit-btn"]')
    
    await expect(page.locator('[role="dialog"]')).toBeVisible()
    await expect(page.locator('[data-testid="json-textarea"]')).toBeVisible()
  })
  
  test('現在のパラメータがJSON形式で表示される', async ({ page }) => {
    // Use reliable hello-world stencil setup directly
    await promarkerPage.setupHelloWorldStencil()
    
    // Fill message parameter
    await expect(page.locator('input[name="message"]')).toBeVisible({ timeout: 15000 });
    await page.fill('input[name="message"]', 'Test Message')
    
    // JSON編集ダイアログ開く
    await page.click('[data-testid="json-edit-btn"]')
    
    const jsonText = await page.locator('[data-testid="json-textarea"]').inputValue()
    const json = JSON.parse(jsonText)
    
    expect(json.stencilCategory).toBe('/samples')
    expect(json.stencilCd).toBe('/samples/hello-world')
    expect(json.dataElements).toBeDefined()
    
    const messageParam = json.dataElements.find((e: any) => e.id === 'message')
    expect(messageParam).toBeDefined()
    expect(messageParam?.value).toBeTruthy()
  })
  
  test('JSONを編集して適用', async ({ page }) => {
    await page.click('[data-testid="json-edit-btn"]')
    
    const json = {
      stencilCategory: '/samples',
      stencilCd: '/samples/hello-world',
      serialNo: '250913A',
      dataElements: [
        { id: 'message', value: 'Modified via JSON' }
      ]
    }
    
    await page.fill('[data-testid="json-textarea"]', JSON.stringify(json, null, 2))
    await page.click('[data-testid="json-apply-btn"]')
    
    // 適用結果確認（時間をおく）
    await page.waitForTimeout(3000)
    
    // Radix Select: Check if category was set to expected value
    // Instead of checking the display text, verify that the expected option is selectable
    await expect(page.locator('[data-testid="category-select"]')).toContainText('Sample')
    
    const messageValue = await page.locator('input[name="message"]').inputValue()
    expect(messageValue).toBe('Modified via JSON')
  })
  
  test('不正なJSONはエラー表示', async ({ page }) => {
    await page.click('[data-testid="json-edit-btn"]')
    
    await page.fill('[data-testid="json-textarea"]', '{invalid json}')
    await page.click('[data-testid="json-apply-btn"]')
    
    await expect(page.locator('[data-testid="json-error-message"]')).toBeVisible()
    await expect(page.locator('[data-testid="json-error-message"]')).toContainText(/不正/i)
  })
  
  test('JSON編集ダイアログをキャンセル', async ({ page }) => {
    await page.click('[data-testid="json-edit-btn"]')
    
    // ダイアログが表示される
    await expect(page.locator('[role="dialog"]')).toBeVisible()
    
    // キャンセルボタンをクリック
    await page.click('[data-testid="json-cancel-btn"]')
    
    // ダイアログが閉じる
    await expect(page.locator('[role="dialog"]')).not.toBeVisible()
  })
})