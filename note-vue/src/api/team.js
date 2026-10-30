import apiClient from './apiClient.js'

// ==================== 团队管理 ====================

/**
 * 获取用户所属团队列表
 */
export async function getMyTeams() {
  return apiClient.get('/api/teams')
}

export async function getDiscoverableTeams() {
  return apiClient.get('/api/teams/discover')
}

/**
 * 创建团队
 * @param {Object} data - { name, description, avatarUrl, isPublic }
 */
export async function createTeam(data) {
  return apiClient.post('/api/teams', data)
}

/**
 * 通过邀请码加入团队
 * @param {string} inviteCode - 邀请码
 */
export async function joinTeamByCode(inviteCode) {
  return apiClient.post('/api/teams/join', { inviteCode })
}

/**
 * 获取团队详情
 * @param {number|string} teamId - 团队ID
 */
export async function getTeamDetail(teamId) {
  return apiClient.get(`/api/teams/${teamId}`)
}

export async function updateTeam(teamId, data) {
  return apiClient.put(`/api/teams/${teamId}`, data)
}

/**
 * 解散团队（仅创建者）
 * @param {number|string} teamId - 团队ID
 */
export async function dissolveTeam(teamId) {
  return apiClient.delete(`/api/teams/${teamId}`)
}

/**
 * 退出团队
 * @param {number|string} teamId - 团队ID
 */
export async function leaveTeam(teamId) {
  return apiClient.post(`/api/teams/${teamId}/leave`)
}

/**
 * 重置邀请码
 * @param {number|string} teamId - 团队ID
 */
export async function resetInviteCode(teamId) {
  return apiClient.post(`/api/teams/${teamId}/invite-code/reset`)
}

// ==================== 成员管理 ====================

/**
 * 获取团队成员列表
 * @param {number|string} teamId - 团队ID
 */
export async function getTeamMembers(teamId) {
  return apiClient.get(`/api/teams/${teamId}/members`)
}

/**
 * 修改成员角色
 * @param {number|string} teamId - 团队ID
 * @param {number|string} userId - 目标用户ID
 * @param {number} role - 新角色（0=创建者 1=管理员 2=编辑者 3=查看者）
 */
export async function updateMemberRole(teamId, userId, role) {
  return apiClient.put(`/api/teams/${teamId}/members/${userId}/role`, { role })
}

/**
 * 移除成员
 * @param {number|string} teamId - 团队ID
 * @param {number|string} userId - 目标用户ID
 */
export async function removeMember(teamId, userId) {
  return apiClient.delete(`/api/teams/${teamId}/members/${userId}`)
}

// ==================== 文档管理 ====================

/**
 * 获取团队文档列表（分页）
 * @param {number|string} teamId - 团队ID
 * @param {Object} params - { page, size, keyword }
 */
export async function getTeamDocs(teamId, params = {}) {
  return apiClient.get(`/api/teams/${teamId}/docs`, { params })
}

/**
 * 创建文档
 * @param {number|string} teamId - 团队ID
 * @param {Object} data - { title, contentMd, contentHtml, isPinned }
 */
export async function createDoc(teamId, data) {
  return apiClient.post(`/api/teams/${teamId}/docs`, data)
}

/**
 * 获取文档详情
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 */
export async function getDocDetail(teamId, docId) {
  return apiClient.get(`/api/teams/${teamId}/docs/${docId}`)
}

/**
 * 更新文档
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 * @param {Object} data - { title, contentMd, contentHtml, isPinned, message }
 */
export async function updateDoc(teamId, docId, data) {
  return apiClient.put(`/api/teams/${teamId}/docs/${docId}`, data)
}

/**
 * 删除文档
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 */
export async function deleteDoc(teamId, docId) {
  return apiClient.delete(`/api/teams/${teamId}/docs/${docId}`)
}

/**
 * 获取编辑锁
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 */
export async function acquireLock(teamId, docId) {
  return apiClient.post(`/api/teams/${teamId}/docs/${docId}/lock`)
}

/**
 * 释放编辑锁
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 * @param {string} lockToken - 锁令牌（可选）
 */
export async function releaseLock(teamId, docId, lockToken = '') {
  const url = lockToken 
    ? `/api/teams/${teamId}/docs/${docId}/lock?lockToken=${encodeURIComponent(lockToken)}`
    : `/api/teams/${teamId}/docs/${docId}/lock`
  return apiClient.delete(url)
}

/**
 * 获取版本历史
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 */
export async function getDocVersions(teamId, docId) {
  return apiClient.get(`/api/teams/${teamId}/docs/${docId}/versions`)
}

export async function getDocActivities(teamId, docId) {
  return apiClient.get(`/api/teams/${teamId}/docs/${docId}/activities`)
}

/**
 * 恢复到指定版本
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 * @param {number|string} eventId - 版本事件ID
 */
export async function restoreDocVersion(teamId, docId, eventId) {
  return apiClient.post(`/api/teams/${teamId}/docs/${docId}/restore`, { eventId })
}

// ==================== 评论管理 ====================

/**
 * 获取文档评论列表
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 * @param {number} type - 类型（1=评论 2=批注，可选）
 */
export async function getDocComments(teamId, docId, type = null) {
  return apiClient.get(`/api/teams/${teamId}/docs/${docId}/comments`, { params: { type } })
}

/**
 * 添加评论
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 * @param {Object} data - { content, parentId, quote, type }
 */
export async function addDocComment(teamId, docId, data) {
  return apiClient.post(`/api/teams/${teamId}/docs/${docId}/comments`, data)
}

/**
 * 标记评论已解决
 * @param {number|string} teamId - 团队ID
 * @param {number|string} docId - 文档ID
 * @param {number|string} commentId - 评论ID
 */
export async function resolveComment(teamId, docId, commentId) {
  return apiClient.put(`/api/teams/${teamId}/docs/${docId}/comments/${commentId}/resolve`)
}

// ==================== AI知识库 ====================

/**
 * 获取AI会话列表
 * @param {number|string} teamId - 团队ID
 */
export async function getAISessions(teamId) {
  return apiClient.get(`/api/teams/${teamId}/ai/sessions`)
}

/**
 * 创建AI会话
 * @param {number|string} teamId - 团队ID
 * @param {string} title - 会话标题（可选）
 */
export async function createAISession(teamId, title = '') {
  return apiClient.post(`/api/teams/${teamId}/ai/sessions`, { title })
}

/**
 * 删除AI会话
 * @param {number|string} teamId - 团队ID
 * @param {number|string} sessionId - 会话ID
 */
export async function deleteAISession(teamId, sessionId) {
  return apiClient.delete(`/api/teams/${teamId}/ai/sessions/${sessionId}`)
}

/**
 * 置顶/取消置顶AI会话
 * @param {number|string} teamId - 团队ID
 * @param {number|string} sessionId - 会话ID
 */
export async function togglePinSession(teamId, sessionId) {
  return apiClient.put(`/api/teams/${teamId}/ai/sessions/${sessionId}/pin`)
}

/**
 * 获取AI消息列表
 * @param {number|string} teamId - 团队ID
 * @param {number|string} sessionId - 会话ID
 */
export async function getAIMessages(teamId, sessionId) {
  return apiClient.get(`/api/teams/${teamId}/ai/sessions/${sessionId}/messages`)
}

/**
 * 发送AI消息
 * @param {number|string} teamId - 团队ID
 * @param {number|string} sessionId - 会话ID
 * @param {string} content - 消息内容
 */
export async function sendAIMessage(teamId, sessionId, content) {
  return apiClient.post(`/api/teams/${teamId}/ai/sessions/${sessionId}/messages`, { content })
}

/**
 * 发送AI消息（流式）
 * @param {number|string} teamId - 团队ID
 * @param {number|string} sessionId - 会话ID
 * @param {string} content - 消息内容
 * @param {Function} onChunk - 接收到数据块的回调函数
 * @param {AbortSignal} [signal] - 取消请求信号
 */
export async function sendAIMessageStream(teamId, sessionId, content, onChunk, signal) {
  const token = localStorage.getItem('satoken') || ''

  const response = await fetch(`/api/teams/${teamId}/ai/sessions/${sessionId}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'satoken': token,
    },
    body: JSON.stringify({ content }),
    signal
  })

  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem('satoken')
      throw new Error('未授权，请重新登录')
    }
    throw new Error(`HTTP错误: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5)
          if (data && data !== '[DONE]') {
            onChunk(data)
          }
        }
      }
    }

    if (buffer.length > 0) {
      const lines = buffer.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5)
          if (data && data !== '[DONE]') {
            onChunk(data)
          }
        }
      }
    }
  } finally {
    reader.releaseLock()
  }
}
