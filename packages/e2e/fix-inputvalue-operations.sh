#!/bin/bash

# E2E Test Fix Script - inputValue() on Radix Select elements
# Replace inputValue() calls on select elements with textContent

SPEC_DIR="tests/specs/promarker-v3"
BACKUP_DIR="backup-before-inputvalue-fix"

echo "🔧 Fixing inputValue() operations on Radix Select elements..."

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Find and process all spec files with inputValue issues
find "$SPEC_DIR" -name "*.spec.ts" -exec grep -l "inputValue.*select" {} \; | while read -r file; do
    echo "Processing: $file"
    
    # Create backup
    cp "$file" "$BACKUP_DIR/$(basename "$file")"
    
    # Replace inputValue() on select elements with textContent
    sed -i \
        -e "s|await serialSelect\.inputValue()|await serialSelect.textContent()|g" \
        -e "s|await page\.inputValue('\[data-testid=\"serial-select\"\]')|await page.locator('[data-testid=\"serial-select\"]').textContent()|g" \
        -e "s|await page\.inputValue('\[data-testid=\"category-select\"\]')|await page.locator('[data-testid=\"category-select\"]').textContent()|g" \
        -e "s|await page\.inputValue('\[data-testid=\"stencil-select\"\]')|await page.locator('[data-testid=\"stencil-select\"]').textContent()|g" \
        -e "s|await page\.locator('\[data-testid=\"category-select\"\]')\.inputValue()|await page.locator('[data-testid=\"category-select\"]').textContent()|g" \
        -e "s|await page\.locator('\[data-testid=\"serial-select\"\]')\.inputValue()|await page.locator('[data-testid=\"serial-select\"]').textContent()|g" \
        -e "s|await page\.locator('\[data-testid=\"stencil-select\"\]')\.inputValue()|await page.locator('[data-testid=\"stencil-select\"]').textContent()|g" \
        "$file"
    
    echo "  ✓ Fixed inputValue() operations in $(basename "$file")"
done

echo ""
echo "✅ inputValue() fixes completed!"
echo "📁 Backups saved in: $BACKUP_DIR"
echo ""
echo "⚠️  Note: Only *-select elements were changed to textContent()"
echo "   Input fields and textareas still use inputValue() (correct behavior)"