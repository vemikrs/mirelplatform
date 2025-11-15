#!/usr/bin/env node

/**
 * Validation script for Playwright E2E testing setup
 * This script validates the setup without requiring browsers
 */

const fs = require('fs');
const path = require('path');

const RED = '\033[0;31m';
const GREEN = '\033[0;32m';
const YELLOW = '\033[1;33m';
const BLUE = '\033[0;34m';
const NC = '\033[0m'; // No Color

function log(color, message) {
  console.log(`${color}${message}${NC}`);
}

function checkFile(filePath, description) {
  if (fs.existsSync(filePath)) {
    log(GREEN, `✅ ${description}: ${filePath}`);
    return true;
  } else {
    log(RED, `❌ ${description}: ${filePath} (not found)`);
    return false;
  }
}

function validatePackageJson() {
  log(BLUE, '\n📦 Validating package.json...');
  
  const packageJsonPath = path.join(__dirname, '../../package.json');
  if (!checkFile(packageJsonPath, 'Root package.json')) {
    return false;
  }
  
  try {
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    
    // Check for required dependencies
    const requiredDeps = ['@playwright/test', '@axe-core/playwright'];
    let allDepsPresent = true;
    
    requiredDeps.forEach(dep => {
      if (packageJson.devDependencies && packageJson.devDependencies[dep]) {
        log(GREEN, `✅ Dependency: ${dep} (${packageJson.devDependencies[dep]})`);
      } else {
        log(RED, `❌ Missing dependency: ${dep}`);
        allDepsPresent = false;
      }
    });
    
    // Check for required scripts
    const requiredScripts = ['test:e2e', 'test:e2e:ui', 'test:e2e:headed'];
    requiredScripts.forEach(script => {
      if (packageJson.scripts && packageJson.scripts[script]) {
        log(GREEN, `✅ Script: ${script}`);
      } else {
        log(YELLOW, `⚠️  Missing script: ${script}`);
      }
    });
    
    return allDepsPresent;
  } catch (error) {
    log(RED, `❌ Error reading package.json: ${error.message}`);
    return false;
  }
}

function validatePlaywrightConfig() {
  log(BLUE, '\n🎭 Validating Playwright configuration...');
  
  const configPath = path.join(__dirname, '../../playwright.config.ts');
  return checkFile(configPath, 'Playwright config');
}

function validateTestStructure() {
  log(BLUE, '\n🧪 Validating test structure...');
  
  const testDir = path.join(__dirname, '..');
  const requiredDirs = [
    'e2e/pages',
    'e2e/utils', 
    'e2e/fixtures',
    'e2e/specs'
  ];
  
  let allDirsPresent = true;
  requiredDirs.forEach(dir => {
    const dirPath = path.join(__dirname, '../..', 'tests', dir);
    if (!checkFile(dirPath, `Test directory: ${dir}`)) {
      allDirsPresent = false;
    }
  });
  
  // Check for key test files
  const testFiles = [
    'e2e/specs/promarker-basic.spec.ts',
    'e2e/specs/promarker-workflow.spec.ts',
    'e2e/specs/promarker-accessibility.spec.ts',
    'e2e/pages/promarker.page.ts',
    'e2e/utils/accessibility.ts'
  ];
  
  testFiles.forEach(file => {
    checkFile(path.join(__dirname, '../..', 'tests', file), `Test file: ${file}`);
  });
  
  return allDirsPresent;
}

function validateGitHubWorkflow() {
  log(BLUE, '\n🔄 Validating GitHub Actions workflow...');
  
  const workflowPath = path.join(__dirname, '../../.github/workflows/e2e-tests.yml');
  return checkFile(workflowPath, 'GitHub Actions workflow');
}

function validateUIEnhancements() {
  log(BLUE, '\n🎨 Validating UI enhancements...');
  
  const vuePath = path.join(__dirname, '../../frontend/pages/mste/index.vue');
  if (!checkFile(vuePath, 'ProMarker Vue component')) {
    return false;
  }
  
  try {
    const vueContent = fs.readFileSync(vuePath, 'utf8');
    
    // Check for data-test-id attributes
    const testIds = [
      'data-test-id="category-select"',
      'data-test-id="stencil-select"',
      'data-test-id="generate-btn"',
      'data-test-id="clear-all-btn"'
    ];
    
    let enhancementsPresent = true;
    testIds.forEach(testId => {
      if (vueContent.includes(testId)) {
        log(GREEN, `✅ UI Enhancement: ${testId}`);
      } else {
        log(YELLOW, `⚠️  Missing UI Enhancement: ${testId}`);
        enhancementsPresent = false;
      }
    });
    
    return enhancementsPresent;
  } catch (error) {
    log(RED, `❌ Error reading Vue file: ${error.message}`);
    return false;
  }
}

function validateStencilData() {
  log(BLUE, '\n📄 Validating stencil test data...');
  
  const stencilPath = path.join(__dirname, '../../backend/src/main/resources/promarker/stencil/samples/samples/hello-world/250913A/stencil-settings.yml');
  return checkFile(stencilPath, 'Hello World stencil configuration');
}

function main() {
  log(BLUE, '🚀 ProMarker E2E Testing Setup Validation');
  log(BLUE, '=========================================\n');
  
  const validations = [
    validatePackageJson,
    validatePlaywrightConfig,
    validateTestStructure,
    validateGitHubWorkflow,
    validateUIEnhancements,
    validateStencilData
  ];
  
  let allValid = true;
  const results = validations.map(validation => {
    const result = validation();
    allValid = allValid && result;
    return result;
  });
  
  log(BLUE, '\n📊 Validation Summary');
  log(BLUE, '====================');
  
  if (allValid) {
    log(GREEN, '✅ All validations passed! E2E testing setup is ready.');
    log(BLUE, '\n🎯 Next Steps:');
    log(YELLOW, '1. Install Playwright browsers: npx playwright install');
    log(YELLOW, '2. Start services: ./scripts/start-services.sh');
    log(YELLOW, '3. Run tests: npm run test:e2e');
  } else {
    log(YELLOW, '⚠️  Some validations failed. Please review the issues above.');
    log(BLUE, '\n🔧 Common fixes:');
    log(YELLOW, '1. Run: npm install');
    log(YELLOW, '2. Check file paths and permissions');
    log(YELLOW, '3. Ensure all files are committed');
  }
  
  process.exit(allValid ? 0 : 1);
}

if (require.main === module) {
  main();
}