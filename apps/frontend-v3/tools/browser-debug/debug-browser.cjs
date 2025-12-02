
const { chromium } = require('@playwright/test');

(async () => {
  const fs = require('fs');
  const path = require('path');

  // Define output directory relative to this script
  // Script is in apps/frontend-v3/tools/browser-debug/
  // Output should be in apps/frontend-v3/debug-output/
  const outputDir = path.resolve(__dirname, '../../debug-output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
    console.log(`Created output directory: ${outputDir}`);
  }

  console.log('Launching browser...');
  const browser = await chromium.launch({
    executablePath: '/usr/bin/google-chrome-stable',
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'] // Often needed in container/WSL environments
  });
  const page = await browser.newPage();
  
  console.log('Navigating to /home...');
  try {
    const response = await page.goto('http://localhost:5173/home', { waitUntil: 'networkidle' });
    console.log(`Status: ${response.status()}`);
  } catch (e) {
    console.error('Navigation failed:', e.message);
  }

  console.log('Page Title:', await page.title());
  
  // Capture screenshot
  const screenshotPath = path.join(outputDir, 'error-screenshot.png');
  await page.screenshot({ path: screenshotPath });
  console.log(`Screenshot saved to ${screenshotPath}`);

  // Get page content to look for errors
  const content = await page.content();
  console.log('Page Content Length:', content.length);
  
  // Try to find error overlay or visible text
  const bodyText = await page.evaluate(() => document.body.innerText);
  console.log('Body Text:\n', bodyText);

  await browser.close();
})();
