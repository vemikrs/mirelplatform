import { test, expect } from '@playwright/test'

test.describe('Preview Smoke', () => {
  test('should load /promarker without JS crash', async ({ page }) => {
    const consoleErrors: string[] = []
    const pageErrors: string[] = []

    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text())
      }
    })

    page.on('pageerror', (error) => {
      pageErrors.push(error.stack || error.message)
    })

    await page.goto('/promarker', { waitUntil: 'domcontentloaded', timeout: 60_000 })
    await page.locator('#root').waitFor({ state: 'attached', timeout: 20_000 })

    // 初期評価の例外を拾うため少し待つ
    await page.waitForTimeout(2000)

    // preview では未ログイン等により 401 が出ることがあるため、既知のノイズを除外
    // （pageerror=未捕捉例外は引き続き厳密にfailさせる）
    const criticalConsoleErrors = consoleErrors.filter((err) => {
      const lowered = err.toLowerCase()
      if (lowered.includes('favicon.ico')) return false
      if (lowered.includes('failed to load resource') && lowered.includes('401')) return false
      if (lowered.includes('failed to rehydrate auth store from server session')) return false
      return true
    })

    if (pageErrors.length > 0) {
      console.error('=== Page Errors (with stack if available) ===')
      pageErrors.forEach((e) => console.error(e))
    }

    expect(pageErrors, `Page errors:\n${pageErrors.join('\n')}`).toEqual([])
    expect(criticalConsoleErrors, `Console errors:\n${criticalConsoleErrors.join('\n')}`).toEqual([])
  })
})
