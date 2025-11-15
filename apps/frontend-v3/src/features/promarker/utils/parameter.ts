import type { DataElement } from '../types/api'

/**
 * パラメータの値を全てクリア
 */
export function clearParameters(params: DataElement[]): DataElement[] {
  return params.map(p => ({ ...p, value: '' }))
}

/**
 * APIリクエストボディを生成
 */
export function createRequestBody(
  category: string,
  stencil: string,
  serial: string,
  params: DataElement[]
): Record<string, string> {
  const body: Record<string, string> = {
    stencilCategoy: category || '*',
    stencilCanonicalName: stencil || '*',
    serialNo: serial || '*'
  }
  
  params.forEach(param => {
    if (param) {
      body[param.id] = param.value
    }
  })
  
  return body
}

/**
 * 現在の状態をJSON形式に変換
 * Vue.js index.vue の paramToJsonValue() に相当
 */
export function parametersToJson(
  category: string,
  stencil: string,
  serial: string,
  params: DataElement[]
): string {
  const dataElements = params.map(p => ({
    id: p.id,
    value: p.value
  }))
  
  return JSON.stringify({
    stencilCategory: category,
    stencilCd: stencil,
    serialNo: serial,
    dataElements
  }, null, 2)
}

/**
 * JSON文字列をパラメータ構造に変換
 * Vue.js index.vue の jsonValueToParam() に相当
 */
export function jsonToParameters(json: string): {
  stencilCategory: string
  stencilCd: string
  serialNo: string
  dataElements: Array<{id: string; value: string}>
} | null {
  try {
    const parsed = JSON.parse(json)
    
    // 必須フィールドの検証
    if (!parsed.stencilCategory || !parsed.stencilCd || !parsed.serialNo) {
      return null
    }
    
    if (!Array.isArray(parsed.dataElements)) {
      return null
    }
    
    return parsed
  } catch (error) {
    console.error('JSON parse error:', error)
    return null
  }
}

/**
 * ファイル名マップを更新
 * Vue.js index.vue の fileNames 管理に相当
 */
export function updateFileNames(
  current: Record<string, string>,
  fileId: string,
  fileName: string
): Record<string, string> {
  return {
    ...current,
    [fileId]: fileName
  }
}

/**
 * 複数ファイルIDをカンマ区切りで結合
 */
export function joinFileIds(fileIds: string[]): string {
  return fileIds.join(',')
}

/**
 * カンマ区切りのファイルIDを配列に分割
 */
export function splitFileIds(fileIdsStr: string): string[] {
  return fileIdsStr.split(',').filter(id => id.trim() !== '')
}
