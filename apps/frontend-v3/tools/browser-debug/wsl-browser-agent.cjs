
const { chromium } = require('@playwright/test');
const fs = require('fs');
const path = require('path');

// Usage: node wsl-browser-agent.cjs <instructions.json>
// instructions.json should be an array of action objects.

const run = async () => {
  const instructionsPath = process.argv[2];
  if (!instructionsPath) {
    console.error('Please provide path to instructions.json');
    process.exit(1);
  }

  const instructions = JSON.parse(fs.readFileSync(instructionsPath, 'utf8'));
  
  // Define output directory relative to this script
  const outputDir = path.resolve(__dirname, '../../debug-output');
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }

  console.log('Launching browser...');
  const browser = await chromium.launch({
    executablePath: '/usr/bin/google-chrome-stable',
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  const page = await browser.newPage();

  // Capture console logs
  page.on('console', msg => console.log(`BROWSER CONSOLE: ${msg.text()}`));
  page.on('pageerror', err => console.error(`BROWSER ERROR: ${err.message}`));

  for (const action of instructions) {
    console.log(`Executing action: ${action.type}`);
    try {
      switch (action.type) {
        case 'goto':
          await page.goto(action.url, { waitUntil: 'networkidle' });
          break;
        case 'click':
          await page.click(action.selector);
          break;
        case 'fill':
          await page.fill(action.selector, action.value);
          break;
        case 'screenshot':
          const filename = action.path || 'screenshot.png';
          // If path is absolute, use it. If relative, join with outputDir
          const screenshotPath = path.isAbsolute(filename) 
            ? filename 
            : path.join(outputDir, path.basename(filename));
            
          await page.screenshot({ path: screenshotPath });
          console.log(`Screenshot saved to ${screenshotPath}`);
          break;
        case 'wait':
          await page.waitForTimeout(action.duration);
          break;
        case 'getText':
          const text = await page.textContent(action.selector);
          console.log(`Text of ${action.selector}: ${text}`);
          break;
        case 'evaluate':
           // Be careful with eval, but useful for debugging
           const result = await page.evaluate(action.func);
           console.log('Eval result:', result);
           break;
        default:
          console.warn(`Unknown action type: ${action.type}`);
      }
    } catch (error) {
      console.error(`Failed to execute action ${action.type}:`, error.message);
      // Continue or break based on preference? Let's continue for now but log error.
    }
  }

  await browser.close();
};

run().catch(err => {
  console.error('Script failed:', err);
  process.exit(1);
});
