/*
 * Copyright(c) 2025 mirelplatform.
 */
package jp.vemi.ste.domain.dto.yml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ValidationRule
 * 
 * Validation rule definition for stencil parameters.
 * Maps to the 'validation' field in stencil YAML definitions.
 * 
 * Example YAML:
 * <pre>
 * dataDomain:
 *   - id: "userName"
 *     name: "ユーザー名"
 *     type: "text"
 *     value: "Developer"
 *     validation:
 *       required: true
 *       minLength: 2
 *       maxLength: 50
 *       pattern: "^[a-zA-Z0-9_-]+$"
 *       errorMessage: "半角英数字、ハイフン、アンダースコアのみ使用できます"
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationRule {
    
    /**
     * Whether the field is required.
     * Default: null (not specified, treated as false)
     */
    private Boolean required;
    
    /**
     * Minimum length for text fields.
     * Default: null (no minimum length restriction)
     */
    private Integer minLength;
    
    /**
     * Maximum length for text fields.
     * Default: null (no maximum length restriction)
     */
    private Integer maxLength;
    
    /**
     * Regular expression pattern for validation.
     * Default: null (no pattern validation)
     */
    private String pattern;
    
    /**
     * Custom error message to display when validation fails.
     * Default: null (use default error message)
     */
    private String errorMessage;
}
