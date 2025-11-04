import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect } from 'react';
import type { DataElement } from '../types/api';
import {
  createParameterValidationSchema,
  getDefaultValues,
  type ParameterFormValues,
} from '../schemas/parameter';

/**
 * Custom hook for parameter form with validation
 * Integrates React Hook Form + Zod validation
 * 
 * Step 6: React Hook Form + Zod integration
 */
export function useParameterForm(parameters: DataElement[]) {
  // Create dynamic schema based on parameters
  const schema = createParameterValidationSchema(parameters);
  
  // Get default values
  const defaultValues = getDefaultValues(parameters);

  // Initialize form
  const form = useForm<ParameterFormValues>({
    resolver: zodResolver(schema) as any, // Schema is dynamically generated
    defaultValues,
    mode: 'onBlur', // Validate on blur
    reValidateMode: 'onChange', // Re-validate on change after first validation
  });

  // Update form when parameters change
  useEffect(() => {
    if (parameters.length > 0) {
      const newDefaults = getDefaultValues(parameters);
      form.reset(newDefaults);
    }
  }, [parameters]); // eslint-disable-line react-hooks/exhaustive-deps

  /**
   * Get all form values
   */
  const getValues = () => {
    return form.getValues();
  };

  /**
   * Get value for specific field
   */
  const getValue = (fieldName: string) => {
    return form.getValues(fieldName);
  };

  /**
   * Set value for specific field
   */
  const setValue = (fieldName: string, value: string) => {
    form.setValue(fieldName, value, {
      shouldValidate: true,
      shouldDirty: true,
    });
  };

  /**
   * Get error for specific field
   */
  const getError = (fieldName: string) => {
    return form.formState.errors[fieldName];
  };

  /**
   * Check if form is valid
   */
  const isValid = form.formState.isValid;

  /**
   * Check if form has been modified
   */
  const isDirty = form.formState.isDirty;

  /**
   * Validate all fields
   */
  const validateAll = async () => {
    return await form.trigger();
  };

  /**
   * Reset form to default values
   */
  const reset = () => {
    form.reset(getDefaultValues(parameters));
  };

  /**
   * Clear all values
   */
  const clearAll = () => {
    const emptyValues: ParameterFormValues = {};
    parameters.forEach((param) => {
      emptyValues[param.id] = '';
    });
    form.reset(emptyValues);
  };

  return {
    // Form instance (for direct access if needed)
    form,
    
    // Register field (for React Hook Form integration)
    register: form.register,
    
    // Form state
    formState: form.formState,
    errors: form.formState.errors,
    isValid,
    isDirty,
    isSubmitting: form.formState.isSubmitting,
    
    // Field operations
    getValues,
    getValue,
    setValue,
    getError,
    
    // Form operations
    validateAll,
    reset,
    clearAll,
    
    // Submit handler
    handleSubmit: form.handleSubmit,
    
    // Watch for changes
    watch: form.watch,
  };
}

/**
 * Type for useParameterForm return value
 */
export type UseParameterFormReturn = ReturnType<typeof useParameterForm>;
