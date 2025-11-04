/*
 * Copyright(c) 2025 mirelplatform.
 */
package jp.vemi.ste.domain.dto.yml;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StencilSettings Validation Test
 * TDD: Test-Driven Development
 * 
 * Tests for YAML parsing with validation field support
 */
class StencilSettingsValidationTest {
    
    @Test
    void testYAMLParsingWithValidation() {
        // Arrange
        String yamlContent = """
            stencil:
              dataDomain:
                - id: "userName"
                  name: "ユーザー名"
                  type: "text"
                  value: "Developer"
                  validation:
                    required: true
                    minLength: 2
                    maxLength: 50
                    pattern: "^[a-zA-Z0-9_-]+$"
                    errorMessage: "半角英数字、ハイフン、アンダースコアのみ使用できます"
            """;
        
        // Act
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);
        
        // Assert
        assertNotNull(data);
        Map<String, Object> stencil = (Map<String, Object>) data.get("stencil");
        assertNotNull(stencil);
        
        var dataDomain = (java.util.List<Map<String, Object>>) stencil.get("dataDomain");
        assertNotNull(dataDomain);
        assertEquals(1, dataDomain.size());
        
        Map<String, Object> userNameField = dataDomain.get(0);
        assertEquals("userName", userNameField.get("id"));
        
        // Validate validation field
        Map<String, Object> validation = (Map<String, Object>) userNameField.get("validation");
        assertNotNull(validation, "validation field should be present");
        assertEquals(true, validation.get("required"));
        assertEquals(2, validation.get("minLength"));
        assertEquals(50, validation.get("maxLength"));
        assertEquals("^[a-zA-Z0-9_-]+$", validation.get("pattern"));
        assertEquals("半角英数字、ハイフン、アンダースコアのみ使用できます", validation.get("errorMessage"));
    }
    
    @Test
    void testYAMLParsingWithoutValidation() {
        // Test backward compatibility - YAML without validation should still parse
        String yamlContent = """
            stencil:
              dataDomain:
                - id: "message"
                  name: "メッセージ"
                  type: "text"
                  value: "Hello"
            """;
        
        // Act
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);
        
        // Assert
        assertNotNull(data);
        Map<String, Object> stencil = (Map<String, Object>) data.get("stencil");
        assertNotNull(stencil);
        
        var dataDomain = (java.util.List<Map<String, Object>>) stencil.get("dataDomain");
        assertNotNull(dataDomain);
        
        Map<String, Object> messageField = dataDomain.get(0);
        assertEquals("message", messageField.get("id"));
        
        // validation field should not be present (backward compatibility)
        assertNull(messageField.get("validation"));
    }
    
    @Test
    void testActualStencilYAMLParsing() {
        // Test actual hello-world stencil settings file
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("promarker/stencil/samples/samples/hello-world/250913A/stencil-settings.yml")) {
            
            assertNotNull(is, "Stencil settings file should exist");
            
            // Act
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(is);
            
            // Assert
            assertNotNull(data);
            Map<String, Object> stencil = (Map<String, Object>) data.get("stencil");
            assertNotNull(stencil);
            
            var dataDomain = (java.util.List<Map<String, Object>>) stencil.get("dataDomain");
            assertNotNull(dataDomain);
            assertTrue(dataDomain.size() >= 3, "Should have at least 3 parameters");
            
            // Find userName field
            Map<String, Object> userNameField = dataDomain.stream()
                .filter(field -> "userName".equals(field.get("id")))
                .findFirst()
                .orElse(null);
            
            assertNotNull(userNameField, "userName field should exist");
            
            // Validate validation field exists
            Map<String, Object> validation = (Map<String, Object>) userNameField.get("validation");
            assertNotNull(validation, "userName should have validation rules");
            assertEquals(true, validation.get("required"));
            assertTrue((Integer) validation.get("minLength") >= 2);
            
        } catch (Exception e) {
            fail("Failed to parse stencil settings: " + e.getMessage());
        }
    }
    
    @Test
    void testPartialValidationFields() {
        // Test with only some validation fields set
        String yamlContent = """
            stencil:
              dataDomain:
                - id: "version"
                  name: "バージョン"
                  type: "text"
                  value: "1.0"
                  validation:
                    required: false
                    minLength: 1
            """;
        
        // Act
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(yamlContent);
        
        // Assert
        Map<String, Object> stencil = (Map<String, Object>) data.get("stencil");
        var dataDomain = (java.util.List<Map<String, Object>>) stencil.get("dataDomain");
        Map<String, Object> versionField = dataDomain.get(0);
        Map<String, Object> validation = (Map<String, Object>) versionField.get("validation");
        
        assertNotNull(validation);
        assertEquals(false, validation.get("required"));
        assertEquals(1, validation.get("minLength"));
        assertNull(validation.get("maxLength"), "maxLength not specified should be null");
        assertNull(validation.get("pattern"), "pattern not specified should be null");
    }
}
