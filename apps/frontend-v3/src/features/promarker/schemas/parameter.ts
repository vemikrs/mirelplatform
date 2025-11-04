import { z } from 'zod';
import type { DataElement } from '../types/api';

/**
 * Dynamic parameter validation schema builder
 * Creates Zod schema based on parameter definitions from API
 * 
 * **重要**: バリデーションルールはステンシル定義（YAML）から取得
 * ハードコードされたルールは使用しない
 * 
 * @see validation-improvement-plan.md
 */

/**
 * Create validation schema for a single parameter
 * @param param - Parameter definition from API (includes validation rules)
 */
function createParameterSchema(param: DataElement): z.ZodTypeAny {
  // Get validation rules from API (defaults to empty object)
  const validation = param.validation || {};
  
  // Apply type-specific validation
  switch (param.valueType?.toLowerCase()) {
    case 'text':
    case 'string': {
      let schema: z.ZodString = z.string();
      
      // Apply minLength if specified in YAML
      if (validation.minLength !== undefined && validation.minLength > 0) {
        schema = schema.min(validation.minLength, `${validation.minLength}文字以上入力してください`);
      }
      
      // Apply maxLength if specified in YAML
      if (validation.maxLength !== undefined && validation.maxLength > 0) {
        schema = schema.max(validation.maxLength, `${validation.maxLength}文字以内で入力してください`);
      }
      
      // Apply pattern if specified in YAML
      if (validation.pattern) {
        const message = validation.errorMessage || '入力形式が正しくありません';
        schema = schema.regex(new RegExp(validation.pattern), message);
      }
      
      // Handle required flag
      if (validation.required) {
        // Required field - ensure at least 1 character (only if minLength not specified)
        if (!validation.minLength || validation.minLength < 1) {
          schema = schema.min(1, '必須項目です');
        }
        return schema;
      } else {
        // Optional field - allow empty string
        return schema.optional().or(z.literal(''));
      }
    }
    
    case 'number': {
      let schema = z.coerce.number();
      
      // Apply min/max if specified
      if (validation.minLength !== undefined) {
        schema = schema.min(validation.minLength, 
          validation.errorMessage || `${validation.minLength}以上の値を入力してください`);
      }
      
      if (validation.maxLength !== undefined) {
        schema = schema.max(validation.maxLength, 
          `${validation.maxLength}以下の値を入力してください`);
      }
      
      return validation.required ? schema : schema.optional();
    }
    
    case 'file': {
      // File IDs are strings (uploaded file references)
      let schema = z.string();
      
      if (validation.required) {
        return schema.min(1, validation.errorMessage || 'ファイルをアップロードしてください');
      } else {
        return schema.optional().or(z.literal(''));
      }
    }
    
    default: {
      // Unknown type - treat as text with minimal validation
      let schema = z.string();
      
      if (validation.required) {
        return schema.min(1, validation.errorMessage || '必須項目です');
      } else {
        return schema.optional().or(z.literal(''));
      }
    }
  }
}

/**
 * Create validation schema for all parameters
 * @param parameters - Parameter definitions from API
 * @returns Zod object schema
 */
export function createParameterValidationSchema(parameters: DataElement[]) {
  const schemaShape: Record<string, z.ZodTypeAny> = {};

  parameters.forEach((param) => {
    schemaShape[param.id] = createParameterSchema(param);
  });

  return z.object(schemaShape);
}

/**
 * Parameter form values type
 * Dynamically generated based on parameters
 */
export type ParameterFormValues = Record<string, string>;

/**
 * Validation error type
 */
export interface ValidationError {
  field: string;
  message: string;
}

/**
 * Extract validation errors from Zod error
 */
export function extractValidationErrors(error: z.ZodError): ValidationError[] {
  return error.issues.map((err: z.ZodIssue) => ({
    field: err.path.join('.'),
    message: err.message,
  }));
}

/**
 * Validate parameter values against schema
 * @param values - Form values
 * @param parameters - Parameter definitions
 * @returns Validation result
 */
export function validateParameters(
  values: Record<string, string>,
  parameters: DataElement[]
): { success: boolean; errors?: ValidationError[] } {
  const schema = createParameterValidationSchema(parameters);

  try {
    schema.parse(values);
    return { success: true };
  } catch (error) {
    if (error instanceof z.ZodError) {
      return {
        success: false,
        errors: extractValidationErrors(error),
      };
    }
    return {
      success: false,
      errors: [{ field: '_global', message: 'バリデーションエラーが発生しました' }],
    };
  }
}

/**
 * Get default values for parameters
 * @param parameters - Parameter definitions
 * @returns Default values object
 */
export function getDefaultValues(parameters: DataElement[]): ParameterFormValues {
  const defaults: ParameterFormValues = {};

  parameters.forEach((param) => {
    defaults[param.id] = param.value || '';
  });

  return defaults;
}
