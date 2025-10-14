import { test, expect } from '@playwright/test'
import { ProMarkerV3Page } from '../../pages/promarker-v3.page'

test.describe('ProMarker v3 - Complete Workflow', () => {
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
  
  test('Complete workflow: Select → Fill → Generate → Download', async ({ page }) => {
    // 1. 3段階選択
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    await page.waitForTimeout(500)
    
    // 2. 必須パラメータ入力
    await page.fill('input[name="message"]', 'E2E Test Message')
    
    // 3. Generate実行
    const downloadPromise = page.waitForEvent('download', { timeout: 30000 })
    const responsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/generate'),
      { timeout: 30000 }
    )
    
    await page.click('[data-testid="generate-btn"]')
    
    // 4. API成功確認
    const response = await responsePromise
    expect(response.status()).toBe(200)
    
    const data = await response.json()
    expect(data.data.data.files).toBeDefined()
    expect(data.data.data.files.length).toBeGreaterThan(0)
    
    // 5. 自動ダウンロード確認
    const download = await downloadPromise
    const filename = download.suggestedFilename()
    expect(filename).toMatch(/\.zip$/)
    console.log(`Downloaded: ${filename}`)
    
    // 6. Toast通知確認
    await expect(page.locator('.sonner-toast')).toContainText(
      /ダウンロード|成功/i,
      { timeout: 5000 }
    )
    
    // 7. UI状態確認（ボタン再有効化）
    await expect(page.locator('[data-testid="generate-btn"]')).toBeEnabled()
  })
  
  test('Generate with validation errors shows inline errors', async ({ page }) => {
    // 3段階選択完了
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    await page.waitForTimeout(500)
    
    // 必須パラメータを空に（バリデーションエラー発生）
    await page.fill('input[name="message"]', '')
    
    // Generate実行（ボタンは無効化されているはず）
    const generateBtn = page.locator('[data-testid="generate-btn"]')
    await expect(generateBtn).toBeDisabled()
    
    // エラーメッセージ表示確認
    const errorMsg = page.locator('text=必須項目です')
    await expect(errorMsg).toBeVisible()
  })
  
  test('Generate API error displays error toast', async ({ page }) => {
    // 3段階選択完了
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    await page.waitForTimeout(500)
    
    // APIエラーをモック
    await page.route('**/mapi/apps/mste/api/generate', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: null,
          errors: ['テンプレート生成に失敗しました'],
          messages: []
        })
      })
    })
    
    await page.fill('input[name="message"]', 'Test')
    await page.click('[data-testid="generate-btn"]')
    
    // エラートースト表示確認
    await expect(page.locator('.sonner-toast')).toContainText(
      /失敗|エラー/i,
      { timeout: 5000 }
    )
  })
  
  test('Generate returns empty files array shows warning', async ({ page }) => {
    // 3段階選択完了
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    await page.waitForTimeout(500)
    
    // 空のfiles配列をモック
    await page.route('**/mapi/apps/mste/api/generate', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          data: { data: { files: [] } },
          errors: [],
          messages: []
        })
      })
    })
    
    await page.fill('input[name="message"]', 'Test')
    await page.click('[data-testid="generate-btn"]')
    
    // 警告トースト表示確認
    await expect(page.locator('.sonner-toast')).toContainText(
      /ファイルがありません/i,
      { timeout: 5000 }
    )
  })
  
  test('Multiple generate executions work correctly', async ({ page }) => {
    // 3段階選択完了
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="serial-select"]', '250913A')
    await page.waitForTimeout(500)
    
    // 1回目の生成
    await page.fill('input[name="message"]', 'First Generation')
    
    let downloadPromise = page.waitForEvent('download', { timeout: 30000 })
    await page.click('[data-testid="generate-btn"]')
    
    let download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/\.zip$/)
    
    // 2回目の生成（パラメータ変更）
    await page.fill('input[name="message"]', 'Second Generation')
    
    downloadPromise = page.waitForEvent('download', { timeout: 30000 })
    await page.click('[data-testid="generate-btn"]')
    
    download = await downloadPromise
    expect(download.suggestedFilename()).toMatch(/\.zip$/)
    
    // UI状態が正常
    await expect(page.locator('[data-testid="generate-btn"]')).toBeEnabled()
  })
})
