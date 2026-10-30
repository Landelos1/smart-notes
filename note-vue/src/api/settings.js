import { apiFetch } from './apiClient'

/**
 * 获取AI设置
 * @returns {Promise<{code: number, message: string, data: Object}>}
 */
export async function getAISettings() {
  try {
    const r = await apiFetch('/api/settings/ai', { method: 'GET' })
    if (r && r.code === 0) {
      console.log('[Settings] 获取AI设置成功')
    } else {
      console.warn('[Settings] 获取AI设置失败:', r?.message)
    }
    return r
  } catch (error) {
    console.error('[Settings] 获取AI设置请求异常:', error)
    throw error
  }
}

/**
 * 更新AI设置
 * @param {Object} settings - AI设置
 * @param {boolean} [settings.enablePersonalKb] - 启用个人知识库
 * @param {number} [settings.personalKbMaxDocs] - 个人知识库最大文档数 (1-50)
 * @param {number} [settings.chatStyle] - 回答风格 1-简洁 2-适中 3-详细
 * @param {number} [settings.replyLanguage] - 回复语言 1-跟随问题 2-只中文 3-只英文
 * @param {string} [settings.assistantName] - 助手名称
 * @param {string} [settings.chatApiKey] - 对话API Key
 * @param {string} [settings.chatApiEndpoint] - 对话API端点
 * @param {boolean} [settings.enableSharedKb] - 启用共享知识库
 * @param {number} [settings.sharedKbMaxDocs] - 共享知识库最大文档数 (1-50)
 * @param {string} [settings.customSystemPrompt] - 自定义系统提示词
 * @param {string} [settings.kbApiKey] - 知识库API Key
 * @param {string} [settings.kbApiEndpoint] - 知识库API端点
 * @returns {Promise<{code: number, message: string, data: null}>}
 */
export async function updateAISettings(settings) {
  try {
    const r = await apiFetch('/api/settings/ai', {
      method: 'PUT',
      body: JSON.stringify(settings),
    })
    if (r && r.code === 0) {
      console.log('[Settings] 更新AI设置成功')
    } else {
      console.warn('[Settings] 更新AI设置失败:', r?.message)
    }
    return r
  } catch (error) {
    console.error('[Settings] 更新AI设置请求异常:', error)
    throw error
  }
}

/**
 * 获取用户自定义 API 可用模型列表（用户需先保存 chatApiKey 和 chatApiEndpoint）
 * @returns {Promise<{code: number, message: string, data: string[]}>}
 */
export async function getAvailableModels() {
  try {
    const r = await apiFetch('/api/settings/ai/models', { method: 'GET' })
    if (r && r.code === 0) {
      console.log('[Settings] 获取模型列表成功:', r.data)
    } else {
      console.warn('[Settings] 获取模型列表失败:', r?.message)
    }
    return r
  } catch (error) {
    console.error('[Settings] 获取模型列表请求异常:', error)
    throw error
  }
}

/**
 * 测试 AI API 连接（通过后端代理，避免浏览器 CORS）
 * @param {string} apiKey
 * @param {string} apiEndpoint
 * @returns {Promise<{code: number, message: string, data: {count: number, models: string[], message: string}}>} 
 */
export async function validateAIConnection(apiKey, apiEndpoint) {
  try {
    const r = await apiFetch('/api/settings/ai/validate', {
      method: 'POST',
      body: JSON.stringify({ apiKey, apiEndpoint })
    })
    if (r && r.code === 0) {
      console.log('[Settings] 测试AI连接成功:', r.data)
    } else {
      console.warn('[Settings] 测试AI连接失败:', r?.message)
    }
    return r
  } catch (error) {
    console.error('[Settings] 测试AI连接请求异常:', error)
    throw error
  }
}

/**
 * 验证AI设置参数
 * @param {Object} settings - AI设置
 * @returns {{valid: boolean, errors: string[]}}
 */
export function validateAISettings(settings) {
  const errors = []

  // 验证个人知识库文档数
  if (settings.personalKbMaxDocs !== undefined) {
    if (settings.personalKbMaxDocs < 1 || settings.personalKbMaxDocs > 50) {
      errors.push('个人知识库参考文档数需在1-50之间')
    }
  }

  // 验证共享知识库文档数
  if (settings.sharedKbMaxDocs !== undefined) {
    if (settings.sharedKbMaxDocs < 1 || settings.sharedKbMaxDocs > 50) {
      errors.push('共享知识库参考文档数需在1-50之间')
    }
  }

  // 验证回答风格
  if (settings.chatStyle !== undefined) {
    if (![1, 2, 3].includes(settings.chatStyle)) {
      errors.push('回答风格参数非法')
    }
  }

  // 验证回复语言
  if (settings.replyLanguage !== undefined) {
    if (![1, 2, 3].includes(settings.replyLanguage)) {
      errors.push('默认回答语言参数非法')
    }
  }

  // 验证API端点长度
  if (settings.chatApiEndpoint && settings.chatApiEndpoint.length > 255) {
    errors.push('对话API端点过长（最大255字符）')
  }

  if (settings.kbApiEndpoint && settings.kbApiEndpoint.length > 255) {
    errors.push('知识库API端点过长（最大255字符）')
  }

  return {
    valid: errors.length === 0,
    errors,
  }
}
