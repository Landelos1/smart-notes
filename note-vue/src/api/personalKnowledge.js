/**
 * 个人知识库相关API
 */
import { apiFetch, uploadFile } from './apiClient'

/**
 * @typedef {Object} DocumentFile
 * @property {number} id - 文件ID
 * @property {string} originalFilename - 原始文件名
 * @property {string} fileName - 存储文件名
 * @property {string} fileExtension - 文件扩展名
 * @property {number} fileSize - 文件大小（字节）
 * @property {string} fileType - 文件类型
 * @property {string} mimeType - MIME类型
 * @property {string} createTime - 创建时间
 * @property {string} updateTime - 更新时间
 * @property {string} processingStatus - 处理状态
 */

/**
 * @typedef {Object} FileListParams
 * @property {number} [page=1] - 页码
 * @property {number} [size=10] - 每页大小
 * @property {string} [keyword] - 关键字搜索
 * @property {string} [fileType] - 文件类型筛选
 * @property {string} [sortBy='create_time'] - 排序字段
 * @property {string} [sortOrder='desc'] - 排序方向
 */

/**
 * 获取用户文档列表
 * @param {FileListParams} params - 查询参数
 * @returns {Promise<{code: number, data: {total: number, data: DocumentFile[]}, message: string}>}
 */
export async function getDocumentList(params = {}) {
  const requestBody = {
    page: params.page || 1,
    size: params.size || 50,
    keyword: params.keyword || '',
    fileType: params.fileType || '',
    sortBy: params.sortBy || 'create_time',
    sortOrder: params.sortOrder || 'desc'
  }
  
  return apiFetch('/api/documents/files/list', {
    method: 'POST',
    body: JSON.stringify(requestBody)
  })
}

/**
 * 上传文档到个人知识库
 * @param {File} file - 文件对象
 * @returns {Promise<{code: number, data: DocumentFile, message: string}>}
 */
export async function uploadDocument(file) {
  return uploadFile('/api/documents/upload', file, 'file')
}

/**
 * 删除文档
 * @param {number} fileId - 文件ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function deleteDocument(fileId) {
  return apiFetch(`/api/documents/files/${fileId}`, {
    method: 'DELETE'
  })
}

/**
 * 重命名文档
 * @param {number} fileId - 文件ID
 * @param {string} newName - 新文件名
 * @returns {Promise<{code: number, data: DocumentFile, message: string}>}
 */
export async function renameDocument(fileId, newName) {
  return apiFetch(`/api/documents/files/${fileId}/rename`, {
    method: 'PUT',
    body: JSON.stringify({ newName })
  })
}

/**
 * 获取文档下载链接
 * @param {number} fileId - 文件ID
 * @returns {string} 下载链接
 */
export function getDocumentDownloadUrl(fileId) {
  return `/api/documents/files/${fileId}/download`
}

/**
 * 下载文档
 * @param {number} fileId - 文件ID
 * @param {string} filename - 保存的文件名
 */
export async function downloadDocument(fileId, filename) {
  const token = localStorage.getItem('satoken') || ''
  
  try {
    const response = await fetch(getDocumentDownloadUrl(fileId), {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
        'satoken': token
      }
    })
    
    if (!response.ok) {
      throw new Error('下载失败')
    }
    
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename || 'document'
    document.body.appendChild(a)
    a.click()
    window.URL.revokeObjectURL(url)
    document.body.removeChild(a)
  } catch (error) {
    console.error('下载文档失败:', error)
    throw error
  }
}

/**
 * 重建文档索引
 * @param {number[]} fileIds - 文件ID列表
 * @returns {Promise<{code: number, data: any, message: string}>}
 */
export async function rebuildDocumentIndex(fileIds) {
  return apiFetch('/api/documents/rebuild-index', {
    method: 'POST',
    body: JSON.stringify({ fileIds })
  })
}

/**
 * 批量删除文档
 * @param {number[]} fileIds - 文件ID列表
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function batchDeleteDocuments(fileIds) {
  return apiFetch('/api/documents/files/batch-delete', {
    method: 'DELETE',
    body: JSON.stringify({ fileIds })
  })
}

/**
 * 获取文件预览数据（以 ArrayBuffer 形式）
 * @param {number} fileId - 文件ID
 * @returns {Promise<{data: ArrayBuffer, contentType: string}>}
 */
export async function getFilePreviewData(fileId) {
  const token = localStorage.getItem('satoken') || ''
  
  const response = await fetch(getDocumentDownloadUrl(fileId), {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'satoken': token
    }
  })
  
  if (!response.ok) {
    throw new Error('获取文件预览失败')
  }
  
  const contentType = response.headers.get('content-type') || 'application/octet-stream'
  const data = await response.arrayBuffer()
  
  return { data, contentType }
}
