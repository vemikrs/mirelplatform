/**
 * ステンシルエディタ - 型定義
 */

export type EditorMode = 'view' | 'edit' | 'create';

export type FileType = 
  | 'stencil-settings'      // stencil-settings.yml
  | 'category-settings'     // {prefix}_stencil-settings.yml
  | 'template'              // *.ftl
  | 'gitkeep'               // .gitkeep (読取専用)
  | 'other';                // その他

export interface StencilFile {
  path: string;
  name: string;
  content: string;
  type: FileType;
  language?: string;
  isEditable: boolean;
}

export interface StencilConfig {
  id: string;
  name: string;
  categoryId: string;
  categoryName: string;
  serial: string;
  lastUpdate: string;
  lastUpdateUser: string;
  description: string;
}

export interface DataElement {
  id: string;
  name: string;
  value: string;
  valueType: string;
  placeholder?: string;
  note?: string;
}

export interface StencilVersion {
  serial: string;
  createdAt: string;
  createdBy: string;
  isActive: boolean;
  changes?: string;
}

// API Request/Response types
export interface LoadStencilRequest {
  stencilId: string;
  serial: string;
}

export interface LoadStencilResponse {
  config: StencilConfig;
  files: StencilFile[];
  versions: StencilVersion[];
}

export interface SaveStencilRequest {
  stencilId: string;
  serial: string;
  config: StencilConfig;
  files: StencilFile[];
  message?: string;
}

export interface SaveStencilResponse {
  newSerial: string;
  success: boolean;
}

export interface VersionInfo {
  serial: string;
  createdAt: string;
  createdBy: string;
  isActive: boolean;
  changes?: string;
}

// ステンシル一覧用の型
export interface StencilListItem {
  id: string;
  name: string;
  categoryId: string;
  categoryName: string;
  latestSerial: string;
  lastUpdate: string;
  lastUpdateUser: string;
  description: string;
  versionCount: number;
}

export interface StencilCategory {
  id: string;
  name: string;
  stencilCount: number;
}

export interface ListStencilsResponse {
  categories: StencilCategory[];
  stencils: StencilListItem[];
}
