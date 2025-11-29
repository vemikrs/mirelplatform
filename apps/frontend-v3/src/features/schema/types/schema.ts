export interface SchemaApiRequest {
  content: Record<string, any>;
}

export interface SchemaApiResponse {
  data: Record<string, any>;
  messages: string[];
  errors: string[];
  errorCode?: string;
}

export interface SchRecord {
  id: string;
  schema: string;
  recordData: Record<string, any>;
  text?: string;
  tenantId: string;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface SchDicModel {
  modelId: string;
  fieldId: string;
  isKey?: boolean;
  fieldName?: string;
  widgetType?: string;
  dataType?: string;
  description?: string;
  sort?: number;
  isHeader?: boolean;
  displayWidth?: number;
  format?: string;
  constraintId?: string;
  maxDigits?: number;
  isRequired?: boolean;
  defaultValue?: string;
  relationCodeGroup?: string;
  function?: string;
  regexPattern?: string;
  minLength?: number;
  maxLength?: number;
  minValue?: number;
  maxValue?: number;
  tenantId: string;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}

export interface SchDicCode {
  groupId: string;
  code: string;
  groupText?: string;
  text?: string;
  sort?: number;
  deleteFlag: boolean;
  tenantId: string;
  version: number;
  createdAt: string;
  createdBy: string;
  updatedAt: string;
  updatedBy: string;
}
