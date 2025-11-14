#!/bin/bash

# E2E Test Fix Script - Radix Select Operations
# Replace direct page.selectOption() calls with POM selectByIndex() methods

SPEC_DIR="tests/specs/promarker-v3"
BACKUP_DIR="backup-before-select-fix"

echo "🔧 Fixing Radix Select operations in E2E tests..."

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Find and process all spec files
find "$SPEC_DIR" -name "*.spec.ts" | while read -r file; do
    echo "Processing: $file"
    
    # Create backup
    cp "$file" "$BACKUP_DIR/$(basename "$file")"
    
    # Apply replacements using sed
    sed -i \
        -e "s|await page\.selectOption('\[data-testid=\"category-select\"\]', '/samples')|await promarkerPage.selectCategoryByIndex(0)|g" \
        -e "s|await page\.selectOption('\[data-testid=\"category-select\"\]', { index: \([0-9]\+\) })|await promarkerPage.selectCategoryByIndex(\1)|g" \
        -e "s|await page\.selectOption('\[data-testid=\"stencil-select\"\]', '/samples/hello-world')|await promarkerPage.selectStencilByIndex(0)|g" \
        -e "s|await page\.selectOption('\[data-testid=\"stencil-select\"\]', { index: \([0-9]\+\) })|await promarkerPage.selectStencilByIndex(\1)|g" \
        -e "s|await page\.selectOption('\[data-testid=\"serial-select\"\]', '250913A')|await promarkerPage.selectSerialByIndex(0)|g" \
        -e "s|await page\.selectOption('\[data-testid=\"serial-select\"\]', { index: \([0-9]\+\) })|await promarkerPage.selectSerialByIndex(\1)|g" \
        "$file"
    
    echo "  ✓ Fixed select operations in $(basename "$file")"
done

echo ""
echo "✅ Select operation fixes completed!"
echo "📁 Backups saved in: $BACKUP_DIR"
echo ""
echo "📋 Next steps:"
echo "1. Run tests to verify fixes work"
echo "2. Check for any remaining inputValue() issues"