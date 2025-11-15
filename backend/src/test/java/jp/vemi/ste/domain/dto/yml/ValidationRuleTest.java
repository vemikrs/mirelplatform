/*
 * Copyright(c) 2025 mirelplatform.
 */
package jp.vemi.ste.domain.dto.yml;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationRule Test
 * TDD: Test-Driven Development
 * 
 * Tests for ValidationRule model class that holds validation rules
 * from stencil YAML definitions.
 */
class ValidationRuleTest {
    
    @Test
    void testValidationRuleCreation() {
        // Arrange & Act
        ValidationRule rule = ValidationRule.builder()
            .required(true)
            .minLength(2)
            .maxLength(50)
            .pattern("^[a-zA-Z0-9_-]+$")
            .errorMessage("半角英数字、ハイフン、アンダースコアのみ使用できます")
            .build();
        
        // Assert
        assertTrue(rule.getRequired());
        assertEquals(2, rule.getMinLength());
        assertEquals(50, rule.getMaxLength());
        assertEquals("^[a-zA-Z0-9_-]+$", rule.getPattern());
        assertEquals("半角英数字、ハイフン、アンダースコアのみ使用できます", rule.getErrorMessage());
    }
    
    @Test
    void testValidationRuleDefaults() {
        // Arrange & Act
        ValidationRule rule = new ValidationRule();
        
        // Assert - all fields should be null by default
        assertNull(rule.getRequired());
        assertNull(rule.getMinLength());
        assertNull(rule.getMaxLength());
        assertNull(rule.getPattern());
        assertNull(rule.getErrorMessage());
    }
    
    @Test
    void testValidationRuleSetters() {
        // Arrange
        ValidationRule rule = new ValidationRule();
        
        // Act
        rule.setRequired(false);
        rule.setMinLength(1);
        rule.setMaxLength(100);
        rule.setPattern("^[0-9]+$");
        rule.setErrorMessage("数字のみ入力できます");
        
        // Assert
        assertFalse(rule.getRequired());
        assertEquals(1, rule.getMinLength());
        assertEquals(100, rule.getMaxLength());
        assertEquals("^[0-9]+$", rule.getPattern());
        assertEquals("数字のみ入力できます", rule.getErrorMessage());
    }
    
    @Test
    void testValidationRulePartialValues() {
        // Test with only some fields set
        ValidationRule rule = ValidationRule.builder()
            .required(true)
            .minLength(5)
            .build();
        
        // Assert
        assertTrue(rule.getRequired());
        assertEquals(5, rule.getMinLength());
        assertNull(rule.getMaxLength());
        assertNull(rule.getPattern());
        assertNull(rule.getErrorMessage());
    }
    
    @Test
    void testValidationRuleToString() {
        // Arrange
        ValidationRule rule = ValidationRule.builder()
            .required(true)
            .minLength(2)
            .build();
        
        // Act
        String toString = rule.toString();
        
        // Assert - toString should include field values
        assertNotNull(toString);
        assertTrue(toString.contains("required"));
        assertTrue(toString.contains("minLength"));
    }
}
