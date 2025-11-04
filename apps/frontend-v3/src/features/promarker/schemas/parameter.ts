import { z } from 'zod';
import type { DataElement } from '../types/api';

/**
 * Dynamic parameter validation schema builder
 * Creates Zod schema based on parameter definitions from API
 * 
 * **重要**: バリデーションルールはステンシル定義（YAML）から取得すべき
 * ハードコードされたルールは使用しない（緊急対応として削除）
 */

/**
 * Create validation schema for a single parameter
 */
function createParameterSchema(param: DataElement): z.ZodTypeAny {
  // Apply type-specific validation
  switch (param.valueType?.toLowerCase()) {
    case 'text':
    case 'string': {
      let stringSchema = z.string();
      
      // Apply required validation based on note or value presence
      const isRequired = param.note?.includes('必須');
      
      if (isRequired) {
        // Required field - must have at least 1 character
        stringSchema = stringSchema.min(1, '必須項目です');
      }
      
      // ❌ 削除: ハードコードされた min(3) - バリデーションルールはステンシル定義から取得すべき
      // ❌ 削除: ハードコードされた max(100) - 同上
      // ❌ 削除: 特定フィールド名への依存（userName, language） - 同上
      
      // Optional field - allow empty string
      return isRequired ? stringSchema : stringSchema.optional().or(z.literal(''));
    }
    case 'number': {
      let numberSchema = z.coerce.number();
      
      // ❌ 削除: ハードコードされた範囲制限
      // バリデーションルールはステンシル定義から取得すべき
      
      const isRequired = param.note?.includes('必須');
      return isRequired ? numberSchema : numberSchema.optional();
    }
    case 'file': {
      // File IDs are strings (uploaded file references)
      let fileSchema = z.string();
      const isRequired = param.note?.includes('必須');
      
      if (isRequired) {
        fileSchema = fileSchema.min(1, 'ファイルをアップロードしてください');
      }
      
      return isRequired ? fileSchema : fileSchema.optional().or(z.literal(''));
    }
    default: {
      let defaultSchema = z.string();
      const isRequired = param.note?.includes('必須');
      
      if (isRequired) {
        defaultSchema = defaultSchema.min(1, '必須項目です');
      }
      
      return isRequired ? defaultSchema : defaultSchema.optional().or(z.literal(''));
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
  return error.errors.map((err) => ({
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
