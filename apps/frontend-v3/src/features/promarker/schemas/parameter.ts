import { z } from 'zod';
import type { DataElement } from '../types/api';

/**
 * Dynamic parameter validation schema builder
 * Creates Zod schema based on parameter definitions from API
 * 
 * Step 6: React Hook Form + Zod integration
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
      
      // Apply required validation
      const isRequired = !param.value || param.note?.includes('必須');
      if (isRequired) {
        stringSchema = stringSchema.min(1, '必須項目です');
      }
      
      // Min length: Default 3 chars for text fields
      stringSchema = stringSchema.min(3, '3文字以上入力してください');
      
      // Max length: Default 100 chars
      stringSchema = stringSchema.max(100, '100文字以内で入力してください');
      
      // Pattern validation based on parameter ID
      if (param.id === 'userName') {
        stringSchema = stringSchema.regex(/^[a-zA-Z0-9]+$/, '半角英数字のみ使用できます');
      }
      
      if (param.id === 'language') {
        stringSchema = stringSchema.regex(/^[a-z]{2}$/, '2文字の言語コードを入力してください');
      }
      
      return isRequired ? stringSchema : stringSchema.optional();
    }
    case 'number': {
      let numberSchema = z.coerce.number();
      
      // Default range for numbers
      numberSchema = numberSchema.min(0, '0以上の値を入力してください');
      numberSchema = numberSchema.max(9999, '9999以下の値を入力してください');
      
      const isRequired = !param.value || param.note?.includes('必須');
      return isRequired ? numberSchema : numberSchema.optional();
    }
    case 'file': {
      // File IDs are strings (uploaded file references)
      let fileSchema = z.string();
      const isRequired = !param.value || param.note?.includes('必須');
      
      if (isRequired) {
        fileSchema = fileSchema.min(1, 'ファイルをアップロードしてください');
      }
      
      return isRequired ? fileSchema : fileSchema.optional();
    }
    default: {
      let defaultSchema = z.string();
      const isRequired = !param.value || param.note?.includes('必須');
      
      if (isRequired) {
        defaultSchema = defaultSchema.min(1, '必須項目です');
      }
      
      return isRequired ? defaultSchema : defaultSchema.optional();
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
