/**
 * 共享知识库相关API
 */
import { apiFetch, uploadFile, uploadFileWithProgress } from './apiClient'

// ==================== 知识库管理 ====================

/**
 * 创建共享知识库
 * @param {Object} data - 创建参数
 * @param {string} data.name - 知识库名称
 * @param {string} data.description - 知识库描述
 * @param {boolean} data.isPublic - 是否公开
 * @param {string} [data.password] - 访问密码
 * @param {string} [data.coverUrl] - 封面URL
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function createKnowledgeBase(data) {
  return apiFetch('/api/shared-knowledge-base', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * 更新共享知识库
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {Object} data - 更新参数
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function updateKnowledgeBase(knowledgeBaseId, data) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

/**
 * 删除共享知识库
 * @param {number} knowledgeBaseId - 知识库ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function deleteKnowledgeBase(knowledgeBaseId) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}`, {
    method: 'DELETE'
  })
}

/**
 * 获取知识库详情
 * @param {number} knowledgeBaseId - 知识库ID
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function getKnowledgeBaseDetail(knowledgeBaseId) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}`, {
    method: 'GET'
  })
}

/**
 * 获取我创建的知识库列表
 * @returns {Promise<{code: number, data: Array, message: string}>}
 */
export async function getMyCreatedKnowledgeBases() {
  return apiFetch('/api/shared-knowledge-base/my-created', {
    method: 'GET'
  })
}

/**
 * 获取我加入的知识库列表
 * @returns {Promise<{code: number, data: Array, message: string}>}
 */
export async function getMyJoinedKnowledgeBases() {
  return apiFetch('/api/shared-knowledge-base/my-joined', {
    method: 'GET'
  })
}

/**
 * 知识库广场搜索
 * @param {Object} params - 搜索参数
 * @param {string} [params.keyword] - 关键词
 * @param {string} [params.sortBy='create_time'] - 排序字段
 * @param {string} [params.sortOrder='desc'] - 排序方向
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=10] - 每页大小
 * @returns {Promise<{code: number, data: {total: number, data: Array}, message: string}>}
 */
export async function getKnowledgeBaseSquare(params = {}) {
  const queryParams = new URLSearchParams({
    keyword: params.keyword || '',
    tag: params.tag || '',
    sortBy: params.sortBy || 'create_time',
    sortOrder: params.sortOrder || 'desc',
    page: String(params.page || 1),
    size: String(params.size || 10)
  })
  return apiFetch(`/api/shared-knowledge-base/square?${queryParams}`, {
    method: 'GET'
  })
}
export async function getKnowledgeBaseTags(params = {}) {
  const queryParams = new URLSearchParams({
    onlyPublic: String(params.onlyPublic ?? true)
  })
  return apiFetch(`/api/shared-knowledge-base/tags?${queryParams}`, {
    method: 'GET'
  })
}

// ==================== 成员管理 ====================

/**
 * 加入知识库
 * @param {Object} data - 加入参数
 * @param {number} data.knowledgeBaseId - 知识库ID
 * @param {string} [data.password] - 访问密码
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function joinKnowledgeBase(data) {
  return apiFetch('/api/shared-knowledge-base/join', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * 退出知识库
 * @param {number} knowledgeBaseId - 知识库ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function leaveKnowledgeBase(knowledgeBaseId) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}/leave`, {
    method: 'POST'
  })
}

/**
 * 获取知识库成员列表
 * @param {number} knowledgeBaseId - 知识库ID
 * @returns {Promise<{code: number, data: Array, message: string}>}
 */
export async function getKnowledgeBaseMembers(knowledgeBaseId) {
  return apiFetch(`/api/shared-knowledge-base/members/${knowledgeBaseId}`, {
    method: 'GET'
  })
}

/**
 * 移除知识库成员
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {number} userId - 要移除的用户ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function removeMember(knowledgeBaseId, userId) {
  return apiFetch(`/api/shared-knowledge-base/members/${userId}/${knowledgeBaseId}`, {
    method: 'DELETE'
  })
}

/**
 * 邀请成员加入知识库
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {Object} data - 邀请参数
 * @param {string} data.email - 被邀请人邮箱或用户名
 * @param {'admin'|'editor'|'viewer'} data.role - 分配的角色
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function inviteMember(knowledgeBaseId, data) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}/invite`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * 更新成员角色
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {number} userId - 成员用户ID
 * @param {'admin'|'editor'|'viewer'} role - 新角色
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function updateMemberRole(knowledgeBaseId, userId, role) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}/members/${userId}/role`, {
    method: 'PUT',
    body: JSON.stringify({ role })
  })
}

/**
 * 更新知识库公开状态
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {boolean} isPublic - 是否公开
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function updateKnowledgeBasePublicStatus(knowledgeBaseId, isPublic) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}/public`, {
    method: 'PUT',
    body: JSON.stringify({ isPublic })
  })
}

// ==================== 文件管理 ====================

/**
 * 上传文件到知识库
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {File} file - 文件对象
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function uploadKnowledgeBaseFile(knowledgeBaseId, file) {
  return uploadFile(`/api/shared-knowledge-base/${knowledgeBaseId}/files/upload`, file, 'file')
}

/**
 * 上传文件到知识库（支持进度回调）
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {File} file - 文件对象
 * @param {(percent: number) => void} onProgress - 进度回调 0~100
 * @param {() => void} onUploadComplete - 文件字节上传完成后的回调
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export function uploadKnowledgeBaseFileWithProgress(knowledgeBaseId, file, onProgress, onUploadComplete) {
  return uploadFileWithProgress(`/api/shared-knowledge-base/${knowledgeBaseId}/files/upload`, file, 'file', onProgress, onUploadComplete)
}

/**
 * 获取知识库文件列表
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {Object} params - 查询参数
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=10] - 每页大小
 * @param {string} [params.keyword] - 搜索关键词
 * @returns {Promise<{code: number, data: {total: number, data: Array}, message: string}>}
 */
export async function getKnowledgeBaseFiles(knowledgeBaseId, params = {}) {
  const queryParams = new URLSearchParams({
    page: String(params.page || 1),
    size: String(params.size || 10),
    keyword: params.keyword || ''
  })
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}/files?${queryParams}`, {
    method: 'GET'
  })
}

/**
 * 删除知识库文件
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {number} fileId - 文件ID
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function deleteKnowledgeBaseFile(knowledgeBaseId, fileId) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}/files/${fileId}`, {
    method: 'DELETE'
  })
}

/**
 * 复制文件到知识库
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {number[]} fileIds - 要复制的文件ID列表
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function copyFilesToKnowledgeBase(knowledgeBaseId, fileIds) {
  return apiFetch(`/api/shared-knowledge-base/${knowledgeBaseId}/files/copy`, {
    method: 'POST',
    body: JSON.stringify({ fileIds })
  })
}

/**
 * 获取个人文件列表（用于复制到共享知识库）
 * @returns {Promise<{code: number, data: Array, message: string}>}
 */
export async function getPersonalFilesForCopy() {
  return apiFetch('/api/shared-knowledge-base/personal-files', {
    method: 'GET'
  })
}

/**
 * 重命名知识库文件
 * @param {number} fileId - 文件ID（file_up_info表的ID）
 * @param {string} newName - 新文件名
 * @returns {Promise<{code: number, data: Object, message: string}>}
 */
export async function renameKnowledgeBaseFile(fileId, newName) {
  return apiFetch(`/api/documents/files/${fileId}/rename`, {
    method: 'PUT',
    body: JSON.stringify({ newName })
  })
}

/**
 * 获取文件下载链接
 * @param {number} fileId - 文件ID
 * @returns {string} 下载链接
 */
export function getKnowledgeBaseFileDownloadUrl(fileId) {
  return `/api/documents/files/${fileId}/download`
}

/**
 * 下载知识库文件
 * @param {number} fileId - 文件ID
 * @param {string} filename - 保存的文件名
 */
export async function downloadKnowledgeBaseFile(fileId, filename) {
  const token = localStorage.getItem('satoken') || ''
  
  try {
    const response = await fetch(getKnowledgeBaseFileDownloadUrl(fileId), {
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
    console.error('下载文件失败:', error)
    throw error
  }
}

/**
 * 获取知识库文件预览数据（以 ArrayBuffer 形式）
 * @param {number} fileId - 文件ID
 * @returns {Promise<{data: ArrayBuffer, contentType: string}>}
 */
export async function getKnowledgeBaseFilePreviewData(fileId) {
  const token = localStorage.getItem('satoken') || ''
  
  const response = await fetch(getKnowledgeBaseFileDownloadUrl(fileId), {
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

// ==================== 邀请管理 ====================

/**
 * 获取我的邀请列表
 * @param {Object} params - 查询参数
 * @param {number} [params.status=0] - 邀请状态（0-待处理，1-已接受，2-已拒绝，3-已过期）
 * @param {number} [params.page=1] - 页码
 * @param {number} [params.size=10] - 每页大小
 * @returns {Promise<{code: number, data: {total: number, data: Array}, message: string}>}
 */
export async function getMyInvitations(params = {}) {
  const queryParams = new URLSearchParams({
    status: String(params.status ?? 0),
    page: String(params.page || 1),
    size: String(params.size || 10)
  })
  return apiFetch(`/api/shared-knowledge-base/my-invitations?${queryParams}`, {
    method: 'GET'
  })
}

/**
 * 获取待处理邀请数量
 * @returns {Promise<{code: number, data: number, message: string}>}
 */
export async function getMyInvitationCount() {
  return apiFetch('/api/shared-knowledge-base/my-invitations/count', {
    method: 'GET'
  })
}

/**
 * 响应邀请（接受或拒绝）
 * @param {number} invitationId - 邀请ID
 * @param {boolean} accept - true=接受，false=拒绝
 * @returns {Promise<{code: number, data: null, message: string}>}
 */
export async function respondInvitation(invitationId, accept) {
  return apiFetch(`/api/shared-knowledge-base/invitations/${invitationId}/respond`, {
    method: 'POST',
    body: JSON.stringify({ accept })
  })
}

// ==================== AI 对话 ====================

/**
 * 共享知识库AI对话（流式）
 * @param {number} knowledgeBaseId - 知识库ID
 * @param {string} message - 用户消息
 * @param {string} [conversationId] - 会话ID
 * @returns {ReadableStreamDefaultReader} 流式响应读取器
 */
export async function chatWithKnowledgeBaseStream(knowledgeBaseId, message, conversationId, options = {}) {
  const token = localStorage.getItem('satoken') || ''
  
  const response = await fetch('/api/shared-knowledge-base/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'satoken': token
    },
    signal: options.signal,
    body: JSON.stringify({
      knowledgeBaseId,
      message,
      conversationId
    })
  })
  
  if (!response.ok) {
    throw new Error('AI对话请求失败')
  }
  
  return response.body.getReader()
}
