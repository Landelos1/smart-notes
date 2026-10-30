/**
 * 笔记AI助手相关API
 */

/**
 * 调用笔记AI处理接口（流式）
 * @param {Object} params - 请求参数
 * @param {string} params.action - AI操作类型
 * @param {string} [params.selectedText] - 选中的文本
 * @param {string} [params.fullContent] - 完整笔记内容
 * @param {string} [params.customPrompt] - 自定义指令
 * @param {string} [params.targetLanguage] - 目标语言
 * @param {Function} onChunk - 数据块回调函数
 * @param {AbortSignal} [signal] - 取消信号
 */
export async function processNoteWithAI({ action, selectedText, fullContent, customPrompt, targetLanguage }, onChunk, signal) {
  const token = localStorage.getItem('satoken') || '';
  
  const response = await fetch('/api/notes/ai/process', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      'satoken': token,
    },
    body: JSON.stringify({
      action,
      selectedText,
      fullContent,
      customPrompt,
      targetLanguage
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
            // 解码后端的换行符标记 [NL] -> \n
            const decodedContent = content.replace(/\[NL\]/g, '\n');
            onChunk(decodedContent);
          }
        }
      }
    }
    
    // 处理剩余的buffer
    if (buffer.trim()) {
      if (buffer.startsWith('data:')) {
        const content = buffer.substring(5);
        if (content.length > 0) {
          const decodedContent = content.replace(/\[NL\]/g, '\n');
          onChunk(decodedContent);
        }
      }
    }
  } finally {
    reader.releaseLock();
  }
}
