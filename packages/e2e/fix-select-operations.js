#!/usr/bin/env node

/**
 * E2E Test Fix Script - Radix Select Operations
 * 
 * Purpose: Replace direct page.selectOption() calls with POM selectByIndex() methods
 * Scope: All spec files in packages/e2e/tests/specs/promarker-v3/
 */

const fs = require('fs');
const path = require('path');
const glob = require('glob');

const SPEC_DIR = './tests/specs/promarker-v3';
const BACKUP_DIR = './backup-before-select-fix';

// Ensure backup directory exists
if (!fs.existsSync(BACKUP_DIR)) {
  fs.mkdirSync(BACKUP_DIR, { recursive: true });
}

// Replacement patterns
const replacements = [
  // Category selection patterns
  {
    pattern: /await page\.selectOption\('\[data-testid="category-select"\]', '\/samples'\)/g,
    replacement: 'await promarkerPage.selectCategoryByIndex(0)'
  },
  {
    pattern: /await page\.selectOption\('\[data-testid="category-select"\]', { index: (\d+) }\)/g,
    replacement: 'await promarkerPage.selectCategoryByIndex($1)'
  },
  
  // Stencil selection patterns
  {
    pattern: /await page\.selectOption\('\[data-testid="stencil-select"\]', '\/samples\/hello-world'\)/g,
    replacement: 'await promarkerPage.selectStencilByIndex(0)'
  },
  {
    pattern: /await page\.selectOption\('\[data-testid="stencil-select"\]', { index: (\d+) }\)/g,
    replacement: 'await promarkerPage.selectStencilByIndex($1)'
  },
  
  // Serial selection patterns - more complex due to fallback logic
  {
    pattern: /await page\.selectOption\('\[data-testid="serial-select"\]', '250913A'\)/g,
    replacement: 'await promarkerPage.selectSerialByIndex(0)'
  },
  {
    pattern: /await page\.selectOption\('\[data-testid="serial-select"\]', { index: (\d+) }\)/g,
    replacement: 'await promarkerPage.selectSerialByIndex($1)'
  }
];

// Find all spec files
const specFiles = glob.sync(`${SPEC_DIR}/**/*.spec.ts`);

console.log(`Found ${specFiles.length} spec files to process`);

specFiles.forEach(filePath => {
  console.log(`Processing: ${filePath}`);
  
  // Create backup
  const backupPath = path.join(BACKUP_DIR, path.basename(filePath));
  const originalContent = fs.readFileSync(filePath, 'utf8');
  fs.writeFileSync(backupPath, originalContent);
  
  // Apply replacements
  let modifiedContent = originalContent;
  let changeCount = 0;
  
  replacements.forEach(({ pattern, replacement }) => {
    const matches = modifiedContent.match(pattern);
    if (matches) {
      changeCount += matches.length;
      modifiedContent = modifiedContent.replace(pattern, replacement);
    }
  });
  
  // Write back if changes were made
  if (changeCount > 0) {
    fs.writeFileSync(filePath, modifiedContent);
    console.log(`  ✓ Applied ${changeCount} replacements`);
  } else {
    console.log(`  - No changes needed`);
  }
});

console.log('\n✅ Select operation fix completed');
console.log(`📁 Backups saved in: ${BACKUP_DIR}`);
console.log('\nNext: Run tests to verify fixes');