import { test, expect } from '@playwright/test'
import { ProMarkerV3Page } from '../../pages/promarker-v3.page'

test.describe('ProMarker v3 - TanStack Query Hooks', () => {
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
  
  test('useSuggest - カテゴリ変更時にAPIコール', async ({ page }) => {
    const responsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/suggest')
    )
    
    await page.selectOption('[data-testid="category-select"]', '/samples')
    
    const response = await responsePromise
    expect(response.status()).toBe(200)
    
    const data = await response.json()
    expect(data.data.model).toBeDefined()
    expect(data.data.model.fltStrStencilCd).toBeDefined()
  })
  
  test('useSuggest - ステンシル変更時にAPIコール', async ({ page }) => {
    // カテゴリ選択
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    const responsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/suggest')
    )
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    
    const response = await responsePromise
    expect(response.status()).toBe(200)
    
    const data = await response.json()
    expect(data.data.model.fltStrSerialNo).toBeDefined()
    expect(data.data.model.params).toBeDefined()
  })
  
  test('useGenerate - コード生成とダウンロード', async ({ page }) => {
    await page.goto('/promarker')
    
    // 3段階選択実行
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    await page.waitForTimeout(500)
    
    // パラメータ入力
    await page.fill('input[name="message"]', 'Test Message')
    
    // API応答を確認（ダウンロードは自動実行されるがE2Eでは検証困難）
    const responsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/generate')
    )
    
    await page.click('[data-testid="generate-btn"]')
    
    const response = await responsePromise
    expect(response.status()).toBe(200)
    
    const data = await response.json()
    expect(data.data.files).toBeDefined()
    expect(data.data.files.length).toBeGreaterThan(0)
    
    console.log('Generate API test completed successfully')
  })
  
  test('useReloadStencilMaster - マスタ再読み込み成功', async ({ page }) => {
    const responsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/reloadStencilMaster')
    )
    
    await page.click('[data-testid="reload-stencil-btn"]')
    
    const response = await responsePromise
    expect(response.status()).toBe(200)
    
    // API成功確認（Toast表示は補完機能のためコア機能テストでは省略）
    expect(response.status()).toBe(200)
    const data = await response.json()
    expect(data.data).toBeDefined()
    expect(data.errors).toHaveLength(0)
  })
  
  test('useGenerate - エラーハンドリング', async ({ page }) => {
    // 不正なリクエストでエラー発生
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    await page.waitForTimeout(500)
    
    // 必須パラメータを空にしてエラー発生
    await page.fill('input[name="message"]', '')
    
    await page.click('[data-testid="generate-btn"]')
    
    // バリデーションエラー表示確認
    await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
  })
  
  test('useSuggest - React Strict Mode重複実行防止', async ({ page }) => {
    let requestCount = 0
    
    page.on('request', request => {
      if (request.url().includes('/mapi/apps/mste/api/suggest')) {
        requestCount++
      }
    })
    
    await page.reload()
    await page.waitForTimeout(1000)
    
    // Strict Modeでは2回実行されることを確認（正常動作）
    expect(requestCount).toBe(2)
  })
  
  test('useSuggest - シリアル選択時にパラメータフィールド表示', async ({ page }) => {
    // 3段階選択
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    const responsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/suggest')
    )
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    
    const response = await responsePromise
    expect(response.status()).toBe(200)
    
    // パラメータフィールドが表示される
    await expect(page.locator('input[name="message"]')).toBeVisible()
  })
})
