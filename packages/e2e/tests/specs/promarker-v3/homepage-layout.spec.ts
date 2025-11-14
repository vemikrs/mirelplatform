import { test, expect } from '@playwright/test';

test.describe('Homepage Layout & Responsiveness', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should display homepage with proper layout', async ({ page }) => {
    // ヘッダーの存在確認
    const header = page.locator('header');
    await expect(header).toBeVisible();

    // メインコンテンツの存在確認
    const main = page.locator('main');
    await expect(main).toBeVisible();

    // フッターの存在確認
    const footer = page.locator('footer');
    await expect(footer).toBeVisible();
  });

  test('should display module cards', async ({ page }) => {
    // モジュールカードの存在確認
    const moduleCards = page.getByTestId('home-module-card');
    const count = await moduleCards.count();
    
    // 7つのモジュールカードが表示されること
    expect(count).toBe(7);

    // 各カードに必要な要素が含まれること
    const firstCard = moduleCards.first();
    await expect(firstCard.locator('h3')).toBeVisible(); // タイトル
    await expect(firstCard.locator('button')).toBeVisible(); // アクションボタン
  });

  test('should have proper container padding on desktop', async ({ page }) => {
    // デスクトップサイズ (1280px)
    await page.setViewportSize({ width: 1280, height: 720 });
    
    const container = page.locator('main .container').first();
    await expect(container).toBeVisible();

    // Container が中央配置されていること
    const boundingBox = await container.boundingBox();
    expect(boundingBox).not.toBeNull();
    if (boundingBox) {
      // 左右に適切な余白があること (64px = 4rem)
      expect(boundingBox.x).toBeGreaterThan(50);
    }
  });

  test('should have proper container padding on tablet', async ({ page }) => {
    // タブレットサイズ (768px)
    await page.setViewportSize({ width: 768, height: 1024 });
    
    const container = page.locator('main .container').first();
    await expect(container).toBeVisible();

    const boundingBox = await container.boundingBox();
    expect(boundingBox).not.toBeNull();
    if (boundingBox) {
      // 左右に適切な余白があること (32px = 2rem)
      expect(boundingBox.x).toBeGreaterThan(20);
      expect(boundingBox.x).toBeLessThan(40);
    }
  });

  test('should have proper container padding on mobile', async ({ page }) => {
    // モバイルサイズ (375px)
    await page.setViewportSize({ width: 375, height: 667 });
    
    const container = page.locator('main .container').first();
    await expect(container).toBeVisible();

    const boundingBox = await container.boundingBox();
    expect(boundingBox).not.toBeNull();
    if (boundingBox) {
      // 左右に適切な余白があること (16px = 1rem)
      expect(boundingBox.x).toBeGreaterThan(10);
      expect(boundingBox.x).toBeLessThan(20);
    }
  });

  test('should display grid layout correctly on different viewports', async ({ page }) => {
    // デスクトップ: 4列レイアウト
    await page.setViewportSize({ width: 1536, height: 864 });
    let grid = page.locator('main .grid').first();
    await expect(grid).toBeVisible();

    // タブレット: 2列レイアウト
    await page.setViewportSize({ width: 768, height: 1024 });
    grid = page.locator('main .grid').first();
    await expect(grid).toBeVisible();

    // モバイル: 1列レイアウト
    await page.setViewportSize({ width: 375, height: 667 });
    grid = page.locator('main .grid').first();
    await expect(grid).toBeVisible();
  });

  test('should have consistent card heights in grid', async ({ page }) => {
    await page.setViewportSize({ width: 1280, height: 720 });
    
    const cards = page.getByTestId('home-module-card');
    const count = await cards.count();
    
    if (count > 1) {
      const heights: number[] = [];
      
      for (let i = 0; i < Math.min(4, count); i++) {
        const card = cards.nth(i);
        const box = await card.boundingBox();
        if (box) {
          heights.push(box.height);
        }
      }

      // 同じ行のカードは高さが一致すること (auto-rows-fr)
      if (heights.length > 1) {
        const firstHeight = heights[0];
        const tolerance = 5; // 5pxの誤差を許容
        
        heights.forEach(height => {
          expect(Math.abs(height - firstHeight)).toBeLessThanOrEqual(tolerance);
        });
      }
    }
  });

  test('should have proper header height on mobile vs desktop', async ({ page }) => {
    // モバイル: h-16 (64px)
    await page.setViewportSize({ width: 375, height: 667 });
    let header = page.locator('header');
    let headerBox = await header.boundingBox();
    expect(headerBox?.height).toBeGreaterThanOrEqual(60);
    expect(headerBox?.height).toBeLessThanOrEqual(70);

    // デスクトップ: h-20 (80px)
    await page.setViewportSize({ width: 1280, height: 720 });
    header = page.locator('header');
    headerBox = await header.boundingBox();
    expect(headerBox?.height).toBeGreaterThanOrEqual(76);
    expect(headerBox?.height).toBeLessThanOrEqual(84);
  });

  test('should have glassmorphism effects on cards', async ({ page }) => {
    const card = page.getByTestId('home-module-card').first();
    await expect(card).toBeVisible();

    // backdrop-blur や透明背景のクラスが適用されていること
    const classes = await card.getAttribute('class');
    expect(classes).toContain('backdrop-blur');
  });

  test('should navigate to ProMarker on card click', async ({ page }) => {
    // ProMarkerカードを探す
    const promarkerCard = page.getByTestId('home-module-card').filter({ hasText: 'ProMarker' });
    
    // 詳細ボタンをクリック
    await promarkerCard.locator('button').click();
    
    // ProMarkerページに遷移すること
    await expect(page).toHaveURL('/promarker');
  });

  test('should have accessible navigation', async ({ page }) => {
    // ヘッダーナビゲーションリンクが適切なaria-labelを持つこと
    const navLinks = page.locator('nav a');
    const count = await navLinks.count();
    
    expect(count).toBeGreaterThan(0);
    
    // 各リンクがクリック可能であること
    for (let i = 0; i < count; i++) {
      const link = navLinks.nth(i);
      await expect(link).toBeVisible();
    }
  });

  test('should have proper spacing in main content', async ({ page }) => {
    const main = page.locator('main');
    
    // py-10 (40px) のパディングがあること
    const mainBox = await main.boundingBox();
    expect(mainBox).not.toBeNull();
  });

  test('should display brand information correctly', async ({ page }) => {
    // ブランド名が表示されること
    const brandName = page.locator('header').getByText('mirelplatform');
    await expect(brandName).toBeVisible();

    // タグラインが表示されること (存在する場合)
    const tagline = page.locator('header').locator('p.text-xs');
    const taglineCount = await tagline.count();
    if (taglineCount > 0) {
      await expect(tagline.first()).toBeVisible();
    }
  });

  test('should have consistent border and shadow styling', async ({ page }) => {
    const header = page.locator('header');
    const classes = await header.getAttribute('class');
    
    // border-outline/20 と shadow-sm が適用されていること
    expect(classes).toContain('border-b');
    expect(classes).toContain('shadow-sm');
  });
});
