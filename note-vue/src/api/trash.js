/**
 * 回收站相关API
 */
import { apiFetch } from './apiClient'

/**
 * @typedef {Object} TrashItem
 * @property {number} id - 回收站记录ID
 * @property {number} trashType - 类型：1=笔记，2=文件
 * @property {string} typeName - 类型名称
 * @property {number} sourceId - 源对象ID
 * @property {string} title - 标题（笔记标题或文件名）
 * @property {string} deleteTime - 删除时间
 * @property {string} restoreDeadline - 恢复截止时间
 * @property {number} daysLeft - 剩余天数
 * @property {string} originalCreateTime - 原创建时间
 * @property {number} [notebookId] - 笔记本ID
 * @property {string} [notebookName] - 笔记本名称
 * @property {string} [summary] - 笔记摘要
 * @property {string} [fileExtension] - 文件扩展名
 * @property {number} [fileSize] - 文件大小
 * @property {string} [fileSizeFormatted] - 格式化的文件大小
 * @property {string} [fileType] - 文件类型
 */

/**
 * @typedef {Object} TrashListParams
 * @property {number} [page=1] - 页码
 * @property {number} [size=10] - 每页大小
 * @property {number} [trashType] - 类型筛选：1=笔记，2=文件
 * @property {string} [keyword] - 搜索关键词
 */

/**
 * 获取回收站列表
 * @param {TrashListParams} params - 查询参数
 * @returns {Promise<{code: number, data: {total: number, data: TrashItem[]}, message: string}>}
 */
export async function getTrashList(params = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page || 1))
  query.set('size', String(params.size || 10))
  if (params.trashType) query.set('trashType', String(params.trashType))
  if (params.keyword) query.set('keyword', params.keyword)
  
  return apiFetch(`/api/trash?${query.toString()}`, { method: 'GET' })
}

/**
 * 获取回收站统计信息
 * @returns {Promise<{code: number, data: {total: number, noteCount: number, fileCount: number}, message: string}>}
 */
export async function getTrashStats() {
  return apiFetch('/api/trash/stats', { method: 'GET' })
}

/**
 * 恢复回收站项目
 * @param {number} trashId - 回收站记录ID
 * @returns {Promise<{code: number, data: {restoredId: number, message: string}, message: string}>}
 */
export async function restoreTrashItem(trashId) {
  return apiFetch(`/api/trash/${trashId}/restore`, { method: 'POST' })
}

/**
 * 彻底删除回收站项目
 * @param {number} trashId - 回收站记录ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function permanentDeleteTrashItem(trashId) {
  return apiFetch(`/api/trash/${trashId}`, { method: 'DELETE' })
}

/**
 * 批量彻底删除
 * @param {number[]} trashIds - 回收站记录ID列表
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function batchPermanentDelete(trashIds) {
  return apiFetch('/api/trash/batch', {
    method: 'DELETE',
    body: JSON.stringify(trashIds)
  })
}

/**
 * 清空回收站
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function emptyTrash() {
  return apiFetch('/api/trash/empty', { method: 'DELETE' })
}
