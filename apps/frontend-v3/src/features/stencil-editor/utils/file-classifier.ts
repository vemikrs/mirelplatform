/**
 * ファイル種別分類ユーティリティ
 */
import type { FileType } from '../types';

/**
 * ファイルパスから種別を判定
 */
export const classifyFile = (filePath: string): FileType => {
  const fileName = filePath.split('/').pop() || '';

  if (fileName === 'stencil-settings.yml') {
    return 'stencil-settings';
  }

  if (fileName.endsWith('_stencil-settings.yml')) {
    return 'category-settings';
  }

  if (fileName === '.gitkeep') {
    return 'gitkeep';
  }

  if (fileName.endsWith('.ftl')) {
    return 'template';
  }

  return 'other';
};

/**
 * ファイル名から言語モードを判定
 */
export const getLanguageMode = (fileName: string): string => {
  const ext = fileName.split('.').pop()?.toLowerCase();

  const languageMap: Record<string, string> = {
    'yml': 'yaml',
    'yaml': 'yaml',
    'ftl': 'freemarker',
    'java': 'java',
    'js': 'javascript',
    'ts': 'typescript',
    'jsx': 'jsx',
    'tsx': 'tsx',
    'json': 'json',
    'xml': 'xml',
    'html': 'html',
    'css': 'css',
    'sql': 'sql',
    'md': 'markdown',
    'sh': 'shell',
    'txt': 'text',
  };

  return languageMap[ext || ''] || 'text';
};

/**
 * ファイルが編集可能か判定
 */
export const isEditable = (fileType: FileType): boolean => {
  return fileType !== 'gitkeep';
};

/**
 * ファイルタイプに応じた表示名を取得
 */
export const getFileTypeLabel = (fileType: FileType): string => {
  const labelMap: Record<FileType, string> = {
    'stencil-settings': 'メイン設定',
    'category-settings': 'カテゴリ共通設定',
    'template': 'テンプレート',
    'gitkeep': 'ディレクトリ保持',
    'other': 'その他',
  };

  return labelMap[fileType];
};

/**
 * ファイルタイプに応じたアイコン名を取得
 */
export const getFileTypeIcon = (fileType: FileType): string => {
  const iconMap: Record<FileType, string> = {
    'stencil-settings': 'settings',
    'category-settings': 'folder-cog',
    'template': 'file-code',
    'gitkeep': 'lock',
    'other': 'file',
  };

  return iconMap[fileType];
};
