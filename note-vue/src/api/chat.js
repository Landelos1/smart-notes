import { apiFetch } from './apiClient'

/**
 * 发送流式聊天消息
 * @param {Object} params - 聊天参数
 * @param {string} params.memoryId - 会话ID
 * @param {string} params.message - 消息内容
 * @param {string} [params.displayMessage] - 历史记录中显示的原始消息
 * @param {string} [params.agentId] - 智能体标识
 * @param {boolean} params.useKnowledge - 是否使用知识库增强回答（默认true）
 * @param {Function} onChunk - 接收到数据块的回调函数
 * @param {AbortSignal} [signal] - 取消请求信号
 * @returns {Promise<void>}
 */
export async function sendChatMessageStream({ memoryId, message, displayMessage, agentId, useKnowledge = true }, onChunk, signal) {
  const token = localStorage.getItem('satoken') || '';
  
  const response = await fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'satoken': token,
    },
    body: JSON.stringify({
      memoryId,
      message,
      displayMessage,
      agentId,
      useKnowledge
    }),
    signal
  });

  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem('satoken');
      throw new Error('未授权，请重新登录');
    }
    throw new Error(`HTTP错误: ${response.status}`);
  }

  // 使用 ReadableStream 读取 SSE 格式的流式响应
  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';

  try {
    while (true) {
      const { done, value } = await reader.read();
      
      if (done) {
        break;
      }

      // 解码数据块
      buffer += decoder.decode(value, { stream: true });
      
      // 按行分割
      const lines = buffer.split('\n');
      
      // 保留最后一个不完整的行
      buffer = lines.pop() || '';
      
      // 处理每一行
      for (const line of lines) {
        // SSE 格式: "data: 内容"
        if (line.startsWith('data:')) {
          // 只移除 "data:" 前缀，保留内容中的所有空格
          const content = line.substring(5);
          
          // 只跳过完全空的行（连空格都没有）
          if (content.length > 0) {
            onChunk(content);
          }
        }
      }
    }
    
    // 处理剩余的缓冲区内容
    if (buffer.length > 0) {
      const lines = buffer.split('\n');
      for (const line of lines) {
        if (line.startsWith('data:')) {
          // 只移除 "data:" 前缀，保留内容中的所有空格
          const content = line.substring(5);
          if (content.length > 0) {
            // 最后一块不需要添加换行符
            onChunk(content);
          }
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}

/**
 * 获取聊天会话列表
 * @returns {Promise<any>}
 */
export async function getChatSessions() {
  return apiFetch('/api/chat/sessions', {
    method: 'GET'
  });
}

/**
 * 创建新的聊天会话
 * @param {string} title - 会话标题
 * @returns {Promise<any>}
 */
export async function createChatSession(title) {
  return apiFetch('/api/chat/sessions', {
    method: 'POST',
    body: JSON.stringify({ title })
  });
}

/**
 * 删除聊天会话
 * @param {string} sessionId - 会话ID
 * @returns {Promise<any>}
 */
export async function deleteChatSession(sessionId) {
  return apiFetch(`/api/chat/sessions/${sessionId}`, {
    method: 'DELETE'
  });
}

/**
 * 获取会话的消息历史
 * @param {string} sessionId - 会话ID
 * @returns {Promise<any>}
 */
export async function getChatHistory(sessionId) {
  return apiFetch(`/api/chat/sessions/${sessionId}/messages`, {
    method: 'GET'
  });
}
