import { test, expect } from '@playwright/test'
import { AccessibilityUtils } from '../../utils/accessibility'

/**
 * Accessibility Audit for ProMarker v3 (@a11y tag based)
 * - Executes AXE-based scans on the main screen and JSON editor modal
 */
test.describe('ProMarker v3 Accessibility Audit', () => {
  let backendAvailable = false
  const baseURL = process.env.E2E_BASE_URL || 'http://localhost:5173'

  test.beforeAll(async ({ request }) => {
    // Try to ensure backend is responsive for consistent UI rendering
    try {
      const resp = await request.post('http://127.0.0.1:3000/mipla2/apps/mste/api/reloadStencilMaster', {
        data: { content: {} },
        timeout: 5000,
      })
      backendAvailable = resp.ok()
      // small grace wait to let caches warm up
      await new Promise(r => setTimeout(r, 300))
    } catch {
      backendAvailable = false
    }
  })

  test('basic page should have no critical accessibility violations @a11y', async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping')
    test.setTimeout(30000)

    await page.goto(`${baseURL}/promarker`)
    // Wait for the page title heading to be visible (React SPA)
    await expect(page.getByRole('heading', { name: /ProMarker ワークスペース/ })).toBeVisible({ timeout: 15000 })

    // Run AXE scan with common ProMarker exclusions/tags
    await AccessibilityUtils.runProMarkerAccessibilityScan(page)
  })

  test('JSON editor modal should meet a11y expectations @a11y', async ({ page }) => {
    test.skip(!backendAvailable, 'Backend not available - skipping')
    test.setTimeout(30000)

    await page.goto(`${baseURL}/promarker`)
    await expect(page.getByRole('heading', { name: /ProMarker ワークスペース/ })).toBeVisible({ timeout: 15000 })

    // Open JSON editor
    await page.getByTestId('json-edit-btn').click()
    // Wait for modal (Radix/Dialog from @mirel/ui)
    await expect(page.getByRole('dialog')).toBeVisible({ timeout: 10000 })

    // Scan including dialog content
    await AccessibilityUtils.runAccessibilityScan(page, {
      // keep default tags but ensure typical SPA landmarks do not fail
      tags: ['wcag2a', 'wcag2aa', 'wcag21aa'],
      disableRules: ['landmark-one-main'],
    })
  })
})
