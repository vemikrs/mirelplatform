/**
 * ステンシルエディタ - Zodバリデーションスキーマ
 */
import { z } from 'zod';

export const StencilConfigSchema = z.object({
  id: z.string()
    .regex(/^\/[a-z0-9\-_\/]+$/, 'IDは/で始まり、小文字英数字・ハイフン・アンダースコアのみ使用可能です'),
  name: z.string()
    .min(1, '名前は必須です')
    .max(200, '名前は200文字以内で入力してください'),
  categoryId: z.string()
    .regex(/^\/[a-z0-9\-_\/]+$/, 'カテゴリIDは/で始まり、小文字英数字のみ使用可能です'),
  categoryName: z.string()
    .min(1, 'カテゴリ名は必須です'),
  serial: z.string()
    .regex(/^\d{6}[A-Z]$/, 'シリアル番号はYYMMDD+大文字アルファベット1文字の形式です'),
  lastUpdate: z.string(),
  lastUpdateUser: z.string(),
  description: z.string().optional(),
});

export const DataElementSchema = z.object({
  id: z.string()
    .min(1, 'IDは必須です')
    .regex(/^[a-zA-Z][a-zA-Z0-9_]*$/, 'IDは英字で始まり、英数字とアンダースコアのみ使用可能です'),
  name: z.string().min(1, '名前は必須です'),
  value: z.string(),
  valueType: z.enum(['text', 'number', 'boolean', 'file', 'array']),
  placeholder: z.string().optional(),
  note: z.string().optional(),
});

export const StencilFileSchema = z.object({
  path: z.string()
    .min(1, 'パスは必須です')
    .refine(
      (path) => !path.includes('..'),
      'パストラバーサルは禁止されています'
    ),
  name: z.string().min(1, 'ファイル名は必須です'),
  content: z.string(),
  type: z.enum(['stencil-settings', 'category-settings', 'template', 'gitkeep', 'other']),
  language: z.string().optional(),
  isEditable: z.boolean(),
});

// FTLファイル名検証
export const validateFtlFileName = (filename: string): boolean => {
  return /^[a-zA-Z0-9_\-\.]+\.ftl$/.test(filename);
};

// ディレクトリ名検証
export const validateDirectoryName = (dirname: string): boolean => {
  return /^[a-zA-Z0-9_\-]+$/.test(dirname);
};

// パストラバーサル検証
export const validateSecurePath = (path: string): boolean => {
  return !path.includes('..');
};

export type StencilConfigType = z.infer<typeof StencilConfigSchema>;
export type DataElementType = z.infer<typeof DataElementSchema>;
export type StencilFileType = z.infer<typeof StencilFileSchema>;
