import { apiFetch } from './apiClient.js'

/**
 * 笔记本相关 API
 */

/**
 * 获取用户的笔记本列表
 * @returns {Promise<{code: number, data: Array, message: string}>}
 */
export async function getNotebooks() {
  return apiFetch('/api/notebooks', { method: 'GET' })
}

/**
 * 创建笔记本
 * @param {Object} data - 创建参数
 * @param {string} data.name - 笔记本名称
 * @param {string} [data.cover] - 封面图片URL
 * @param {string} [data.description] - 笔记本描述
 * @param {number} [data.sortOrder] - 排序字段
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function createNotebook(data) {
  return apiFetch('/api/notebooks', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * 更新笔记本
 * @param {string|number} notebookId - 笔记本ID
 * @param {Object} data - 更新参数
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function updateNotebook(notebookId, data) {
  return apiFetch(`/api/notebooks/${notebookId}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

/**
 * 删除笔记本
 * @param {string|number} notebookId - 笔记本ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function deleteNotebook(notebookId) {
  return apiFetch(`/api/notebooks/${notebookId}`, { method: 'DELETE' })
}

/**
 * 笔记相关 API
 */

/**
 * 分页获取笔记列表
 * @param {Object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=20] - 每页大小
 * @param {string|number} [params.notebookId] - 笔记本ID
 * @param {string} [params.keyword] - 搜索关键词
 * @returns {Promise<{code: number, data: {data: Array, records?: Array, total: number}, message: string}>}
 */
export async function getNotes(params = {}) {
  const query = new URLSearchParams()
  query.set('page', String(params.page || 1))
  query.set('size', String(params.size || 20))
  if (params.notebookId) query.set('notebookId', String(params.notebookId))
  if (params.keyword) query.set('keyword', params.keyword)
  
  return apiFetch(`/api/notes?${query.toString()}`, { method: 'GET' })
}

/**
 * 获取用户笔记统计信息
 * @returns {Promise<{code: number, data: {total: number, mostProductiveDate: string, mostProductiveCount: number}, message: string}>}
 */
export async function getNoteStats() {
  return apiFetch('/api/notes/stats', { method: 'GET' })
}

/**
 * 获取笔记详情
 * @param {string|number} noteId - 笔记ID
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function getNoteDetail(noteId) {
  return apiFetch(`/api/notes/${noteId}`, { method: 'GET' })
}

/**
 * 创建笔记
 * @param {Object} data - 创建参数
 * @param {string|number} data.notebookId - 笔记本ID
 * @param {string} data.title - 笔记标题
 * @param {string} [data.contentMd] - Markdown内容
 * @param {number} [data.status] - 状态（1-已发布 0-草稿）
 * @param {boolean} [data.isPinned] - 是否置顶
 * @param {string[]} [data.tags] - 标签列表
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function createNote(data) {
  return apiFetch('/api/notes', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * 更新笔记
 * @param {Object} data - 更新参数
 * @param {string|number} data.noteId - 笔记ID
 * @param {string} data.title - 笔记标题
 * @param {string|number} [data.notebookId] - 目标笔记本ID
 * @param {string} [data.contentMd] - Markdown内容
 * @param {number} [data.status] - 状态
 * @param {boolean} [data.isPinned] - 是否置顶
 * @param {string[]} [data.tags] - 标签列表
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function updateNote(data) {
  return apiFetch('/api/notes/update', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * 自动保存笔记内容
 * @param {Object} data - 保存参数
 * @param {string|number} data.noteId - 笔记ID
 * @param {string} data.contentMd - Markdown内容
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function autoSaveNote(data) {
  return apiFetch('/api/notes/autosave', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * 删除笔记
 * @param {string|number} noteId - 笔记ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function deleteNote(noteId) {
  return apiFetch(`/api/notes/${noteId}`, { method: 'DELETE' })
}

/**
 * 切换笔记置顶状态
 * @param {string|number} noteId - 笔记ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function togglePinNote(noteId) {
  return apiFetch(`/api/notes/${noteId}/pin`, { method: 'POST' })
}

/**
 * 获取用户的所有标签
 * @returns {Promise<{code: number, data: string[], message: string}>}
 */
export async function getUserTags() {
  return apiFetch('/api/notes/tags', { method: 'GET' })
}

/**
 * 将笔记添加到个人知识库
 * @param {string|number} noteId - 笔记ID
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function addNoteToKnowledgeBase(noteId) {
  return apiFetch(`/api/notes/${noteId}/add-to-knowledge-base`, { method: 'POST' })
}

/**
 * 导出笔记文件
 * @param {string|number} noteId - 笔记ID
 * @param {string} format - 导出格式（pdf/docx/xlsx/md）
 * @returns {Promise<Blob>}
 */
export async function exportNote(noteId, format = 'pdf') {
  const token = localStorage.getItem('satoken') || ''
  const url = `/api/notes/${noteId}/export?format=${encodeURIComponent(format)}`
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      satoken: token
    }
  })

  if (!response.ok) {
    throw new Error(`导出失败: ${response.status}`)
  }

  return response.blob()
}
