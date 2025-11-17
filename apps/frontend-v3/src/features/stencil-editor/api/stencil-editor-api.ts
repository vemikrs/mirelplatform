/**
 * ステンシルエディタAPI
 */
import axios from 'axios';
import type {
  LoadStencilResponse,
  SaveStencilRequest,
  SaveStencilResponse,
  VersionInfo,
} from '../types';

const API_BASE = '/mapi/apps/mste/editor';

/**
 * ステンシル情報を取得
 */
export const loadStencil = async (
  stencilId: string,
  serial: string
): Promise<LoadStencilResponse> => {
  const response = await axios.get(`${API_BASE}/${stencilId}/${serial}`);
  
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
  const response = await axios.post(`${API_BASE}/common/${categoryId}`, {
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
  const response = await axios.get(`${API_BASE}/${stencilId}/versions`);

  if (response.data.errors && response.data.errors.length > 0) {
    throw new Error(response.data.errors.join(', '));
  }

  return response.data.data;
};
