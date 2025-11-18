/**
 * ステンシルエディタAPI
 */
import axios from 'axios';
import type {
  LoadStencilResponse,
  SaveStencilRequest,
  SaveStencilResponse,
  VersionInfo,
  ListStencilsResponse,
} from '../types';

const API_BASE = '/mapi/apps/mste/editor';

/**
 * ステンシル情報を取得
 */
export const loadStencil = async (
  stencilId: string,
  serial: string
): Promise<LoadStencilResponse> => {
  // stencilIdは既に/で始まっている（例: /springboot/service）
  const url = `${API_BASE}${stencilId}/${serial}`;
  console.log('🌐 loadStencil API呼び出し:', {
    API_BASE,
    stencilId,
    serial,
    url,
  });
  
  const response = await axios.get(url);
  
  if (response.data.errors && response.data.errors.length > 0) {
    throw new Error(response.data.errors.join(', '));
  }

  return response.data.data;
};

/**
 * ステンシルを保存
 */
export const saveStencil = async (
  request: SaveStencilRequest
): Promise<SaveStencilResponse> => {
  const response = await axios.post(`${API_BASE}/save`, {
    content: request,
  });

  if (response.data.errors && response.data.errors.length > 0) {
    throw new Error(response.data.errors.join(', '));
  }

  return response.data.data;
};

/**
 * カテゴリ共通設定を保存
 */
export const saveCommonSettings = async (
  categoryId: string,
  content: string
): Promise<void> => {
  // categoryIdは既に/で始まっている可能性がある
  const normalizedCategoryId = categoryId.startsWith('/') ? categoryId.slice(1) : categoryId;
  const response = await axios.post(`${API_BASE}/common/${normalizedCategoryId}`, {
    content: { yamlContent: content },
  });

  if (response.data.errors && response.data.errors.length > 0) {
    throw new Error(response.data.errors.join(', '));
  }
};

/**
 * バージョン履歴を取得
 */
export const getVersionHistory = async (
  stencilId: string
): Promise<VersionInfo[]> => {
  // stencilIdは既に/で始まっている（例: /springboot/service）
  const response = await axios.get(`${API_BASE}${stencilId}/versions`);

  if (response.data.errors && response.data.errors.length > 0) {
    throw new Error(response.data.errors.join(', '));
  }

  return response.data.data;
};

/**
 * ステンシル一覧を取得
 */
export const listStencils = async (
  categoryId?: string
): Promise<ListStencilsResponse> => {
  const params = categoryId ? { categoryId } : {};
  const response = await axios.get(`${API_BASE}/list`, { params });

  if (response.data.errors && response.data.errors.length > 0) {
    throw new Error(response.data.errors.join(', '));
  }

  return response.data.data;
};
