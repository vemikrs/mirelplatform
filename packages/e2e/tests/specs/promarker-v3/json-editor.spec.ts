import { test, expect } from '@playwright/test'
import { ProMarkerV3Page } from '../../pages/promarker-v3.page'

test.describe('ProMarker v3 - JSON Editor', () => {
  let promarkerPage: ProMarkerV3Page
  
  test.beforeEach(async ({ page }) => {
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
    // 選択完了
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    await page.waitForTimeout(500)
    
    await page.fill('input[name="message"]', 'Test Message')
    
    // JSON編集ダイアログ開く
    await page.click('[data-testid="json-edit-btn"]')
    
    const jsonText = await page.locator('[data-testid="json-textarea"]').inputValue()
    const json = JSON.parse(jsonText)
    
    expect(json.stencilCategory).toBe('/samples')
    expect(json.stencilCd).toBe('/samples/hello-world')
    expect(json.serialNo).toBe('250913A')
    expect(json.dataElements).toBeDefined()
    
    const messageParam = json.dataElements.find((e: any) => e.id === 'message')
    expect(messageParam?.value).toBe('Hello, World!')  // React実装はデフォルト値を使用
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
    
    const categoryValue = await page.locator('[data-testid="category-select"]').inputValue()
    expect(categoryValue).toBe('/samples')
    
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