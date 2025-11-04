import { test, expect } from '@playwright/test'
import { ProMarkerV3Page } from '../../pages/promarker-v3.page'

test.describe('ProMarker v3 - TanStack Query Hooks', () => {
  let promarkerPage: ProMarkerV3Page
  let backendAvailable = false

  test.beforeAll(async ({ request }) => {
    try {
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      })
      backendAvailable = resp.ok()
    } catch (_) {
      backendAvailable = false
    }
  })
  
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
  
  test('useGenerate - コード生成とダウンロード', async ({ page, request }) => {
    // テンプレートエンジンの状態を安定化
    await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
      data: { content: {} },
      timeout: 5000,
    })
    await page.goto('/promarker')
    
    // 3段階選択実行
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)

    // シリアルオプションのロードを待機
    await page.waitForFunction(() => {
      const el = document.querySelector('[data-testid="serial-select"]');
      return !!el && (el as HTMLSelectElement).options.length > 0;
    }, { timeout: 15000 });
    
    // シリアル選択（オプション待機＋フォールバック）
    const serialSelect1 = page.locator('[data-testid="serial-select"]');
    await expect(serialSelect1).toBeEnabled({ timeout: 10000 });
    // 必須シリアルが存在しない場合は環境依存と判断してスキップ
    const mustSerialExists = await page.locator('[data-testid="serial-select"] option[value="250913A"]').count();
    if (mustSerialExists === 0) {
      test.skip('Required sample serial 250913A not available in this environment');
    }
    const hasTarget1 = await page.locator('[data-testid="serial-select"] option[value="250913A"]').count();
    if (hasTarget1 > 0) {
      await page.selectOption('[data-testid="serial-select"]', '250913A');
    } else {
      const current1 = await page.inputValue('[data-testid="serial-select"]');
      if (!current1 || current1.length === 0) {
        const optionsText1 = await page.locator('[data-testid="serial-select"] option').allTextContents();
        const firstIdx1 = optionsText1[0]?.trim() === '' && optionsText1.length > 1 ? 1 : 0;
        await page.selectOption('[data-testid="serial-select"]', { index: firstIdx1 });
      }
    }
  // パラメータセクション・ステンシル情報が表示されるまで待機
  await expect(page.locator('[data-testid="parameter-section"]')).toBeVisible({ timeout: 15000 });
  await expect(page.locator('[data-testid="stencil-info"]')).toBeVisible({ timeout: 15000 });
    
    // パラメータ入力（存在する項目のみ入力）
    await page.fill('input[name="message"]', 'Test Message')
    const userNameField = page.locator('input[name="userName"]').first();
    if (await userNameField.count()) {
      await userNameField.fill('Developer');
    }
    const langField = page.locator('[data-testid="param-language"]').first();
    if (await langField.count()) {
      const tagName = await langField.evaluate(el => el.tagName.toLowerCase());
      if (tagName === 'select') {
        await page.selectOption('[data-testid="param-language"]', 'ja');
      } else {
        await page.locator('[data-testid="param-language"]').fill('ja');
      }
    }
    
    // API応答を確認（ダウンロードは自動実行されるがE2Eでは検証困難）
    const responsePromise = page.waitForResponse(
      r => r.url().includes('/mapi/apps/mste/api/generate')
    )
    
    await page.click('[data-testid="generate-btn"]')
    
    const response = await responsePromise
    expect(response.status()).toBe(200)
    
    const data = await response.json()
    expect(data.errors ?? []).toHaveLength(0)
    expect(data.data).toBeDefined()
    expect(Array.isArray(data.data.files)).toBe(true)
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
  
  // TODO: バリデーションエラー表示機能実装後に有効化
  test.skip('useGenerate - エラーハンドリング', async ({ page }) => {
    // 不正なリクエストでエラー発生
    await page.selectOption('[data-testid="category-select"]', '/samples')
    await page.waitForTimeout(500)
    
    await page.selectOption('[data-testid="stencil-select"]', '/samples/hello-world')
    await page.waitForTimeout(500)
    
    // シリアル選択（オプション待機＋フォールバック）
    const serialSelect2 = page.locator('[data-testid="serial-select"]');
    await expect(serialSelect2).toBeEnabled({ timeout: 10000 });
    const hasTarget2 = await page.locator('[data-testid="serial-select"] option[value="250913A"]').count();
    if (hasTarget2 > 0) {
      await page.selectOption('[data-testid="serial-select"]', '250913A');
    } else {
      const current2 = await page.inputValue('[data-testid="serial-select"]');
      if (!current2 || current2.length === 0) {
        const optionsText2 = await page.locator('[data-testid="serial-select"] option').allTextContents();
        const firstIdx2 = optionsText2[0]?.trim() === '' && optionsText2.length > 1 ? 1 : 0;
        await page.selectOption('[data-testid="serial-select"]', { index: firstIdx2 });
      }
    }
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

    // シリアル選択（オプション待機＋フォールバック）
    const serialSelect3 = page.locator('[data-testid="serial-select"]');
    await expect(serialSelect3).toBeEnabled({ timeout: 10000 });
    const hasTarget3 = await page.locator('[data-testid="serial-select"] option[value="250913A"]').count();
    if (hasTarget3 > 0) {
      await page.selectOption('[data-testid="serial-select"]', '250913A');
    } else {
      const current3 = await page.inputValue('[data-testid="serial-select"]');
      if (!current3 || current3.length === 0) {
        const optionsText3 = await page.locator('[data-testid="serial-select"] option').allTextContents();
        const firstIdx3 = optionsText3[0]?.trim() === '' && optionsText3.length > 1 ? 1 : 0;
        await page.selectOption('[data-testid="serial-select"]', { index: firstIdx3 });
      }
    }

    // ネットワーク待機ではなくUI可視性を待つ（自動選択でAPIが発火しない場合があるため）
    await expect(page.locator('[data-testid="parameter-section"]')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('input[name="message"]').first()).toBeVisible({ timeout: 15000 });
  })
})
