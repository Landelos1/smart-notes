import apiClient from './apiClient'

/**
 * 搜索模板列表
 * @param {Object} params - 搜索参数
 * @param {string} params.category - 分类（all表示全部）
 * @param {string} params.keyword - 搜索关键词
 * @param {number} params.page - 页码
 * @param {number} params.pageSize - 每页数量
 * @returns {Promise<Object>} 分页结果
 */
export const getTemplates = (params = {}) => {
  return apiClient.get('/api/templates', { params })
}

/**
 * 获取模板详情
 * @param {number|string} id - 模板ID
 * @returns {Promise<Object>} 模板详情
 */
export const getTemplateById = (id) => {
  return apiClient.get(`/api/templates/${id}`)
}

/**
 * 获取相关模板
 * @param {number|string} id - 当前模板ID
 * @param {number} limit - 限制数量
 * @returns {Promise<Array>} 相关模板列表
 */
export const getRelatedTemplates = (id, limit = 3) => {
  return apiClient.get(`/api/templates/${id}/related`, { params: { limit } })
}

/**
 * 获取模板评论
 * @param {number|string} id - 模板ID
 * @returns {Promise<Array>} 评论列表
 */
export const getTemplateComments = (id) => {
  return apiClient.get(`/api/templates/${id}/comments`)
}

/**
 * 使用模板（增加下载次数）
 * @param {number|string} id - 模板ID
 * @returns {Promise<void>}
 */
export const useTemplate = (id) => {
  return apiClient.post(`/api/templates/${id}/use`)
}

/**
 * 点赞评论
 * @param {number|string} commentId - 评论ID
 * @returns {Promise<void>}
 */
export const likeTemplateComment = (commentId) => {
  return apiClient.post(`/api/templates/comments/${commentId}/like`)
}

/**
 * 创建模板
 * @param {Object} data - 模板数据
 * @param {string} data.title - 标题
 * @param {string} data.description - 描述
 * @param {string} data.category - 分类
 * @param {string} data.thumbnail - 缩略图
 * @param {string} data.content - 内容
 * @param {Array<string>} data.tags - 标签
 * @param {boolean} data.isPublic - 是否公开
 * @returns {Promise<Object>} 创建的模板
 */
export const createTemplate = (data) => {
  return apiClient.post('/api/templates', data)
}

/**
 * 更新模板
 * @param {number|string} id - 模板ID
 * @param {Object} data - 模板数据
 * @returns {Promise<Object>} 更新后的模板
 */
export const updateTemplate = (id, data) => {
  return apiClient.put(`/api/templates/${id}`, data)
}

/**
 * 删除模板
 * @param {number|string} id - 模板ID
 * @returns {Promise<void>}
 */
export const deleteTemplate = (id) => {
  return apiClient.delete(`/api/templates/${id}`)
}

/**
 * 获取我的模板列表
 * @returns {Promise<Array>} 我的模板列表
 */
export const getMyTemplates = () => {
  return apiClient.get('/api/templates/my')
}

/**
 * 获取模板分享信息
 * @param {number|string} id - 模板ID
 * @returns {Promise<Object>} 分享信息
 */
export const shareTemplate = (id) => {
  return apiClient.post(`/api/templates/${id}/share`)
}

/**
 * 收藏模板
 * @param {number|string} id - 模板ID
 * @returns {Promise<void>}
 */
export const favoriteTemplate = (id) => {
  return apiClient.post(`/api/templates/${id}/favorite`)
}

/**
 * 取消收藏模板
 * @param {number|string} id - 模板ID
 * @returns {Promise<void>}
 */
export const unfavoriteTemplate = (id) => {
  return apiClient.delete(`/api/templates/${id}/favorite`)
}

/**
 * 查询模板收藏状态
 * @param {number|string} id - 模板ID
 * @returns {Promise<Boolean>} 是否已收藏
 */
export const getFavoriteStatus = (id) => {
  return apiClient.get(`/api/templates/${id}/favorite/status`)
}

/**
 * 获取收藏的模板列表
 * @param {Object} params - 分页参数
 * @returns {Promise<Object>} 分页结果
 */
export const getFavoriteTemplates = (params = {}) => {
  return apiClient.get('/api/templates/favorites', { params })
}

/**
 * 举报模板
 * @param {number|string} id - 模板ID
 * @param {Object} data - 举报信息
 * @returns {Promise<void>}
 */
export const reportTemplate = (id, data) => {
  return apiClient.post(`/api/templates/${id}/report`, data)
}

/**
 * 发表模板评论
 * @param {number|string} id - 模板ID
 * @param {Object} data - 评论数据
 * @returns {Promise<Object>} 评论详情
 */
export const createTemplateComment = (id, data) => {
  return apiClient.post(`/api/templates/${id}/comments`, data)
}

/**
 * 取消点赞评论
 * @param {number|string} commentId - 评论ID
 * @returns {Promise<void>}
 */
export const unlikeTemplateComment = (commentId) => {
  return apiClient.delete(`/api/templates/comments/${commentId}/like`)
}
