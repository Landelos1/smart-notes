/**
 * 配额错误处理工具
 */

// 错误代码常量
export const QUOTA_ERROR_CODES = {
  AI_QUOTA_EXCEEDED: 40301,
  KB_QUOTA_EXCEEDED: 40302,
  QUOTA_GENERAL_ERROR: 403
};

/**
 * 判断是否为配额超限错误
 * @param {Object} error - 错误对象或响应对象
 * @returns {boolean}
 */
export function isQuotaExceededError(error) {
  if (!error) return false;
  
  const code = error.code || error.status;
  return code === QUOTA_ERROR_CODES.AI_QUOTA_EXCEEDED || 
         code === QUOTA_ERROR_CODES.KB_QUOTA_EXCEEDED ||
         (code === QUOTA_ERROR_CODES.QUOTA_GENERAL_ERROR && error.message && error.message.includes('配额'));
}

/**
 * 获取配额错误的友好提示信息
 * @param {Object} error - 错误对象
 * @returns {Object} 包含标题、消息和建议的对象
 */
export function getQuotaErrorMessage(error) {
  const code = error.code || error.status;
  const defaultMessage = error.message || '操作失败';
  
  if (code === QUOTA_ERROR_CODES.AI_QUOTA_EXCEEDED) {
    return {
      title: 'AI对话次数已用完',
      message: defaultMessage,
      suggestion: '您本月的AI对话次数已达到上限,升级会员可获得更多对话次数。',
      action: 'upgrade',
      actionText: '立即升级'
    };
  }
  
  if (code === QUOTA_ERROR_CODES.KB_QUOTA_EXCEEDED) {
    return {
      title: '知识库数量已达上限',
      message: defaultMessage,
      suggestion: '您已达到知识库创建上限,升级会员可创建更多知识库。',
      action: 'upgrade',
      actionText: '升级会员'
    };
  }
  
  return {
    title: '配额限制',
    message: defaultMessage,
    suggestion: '您的使用已达到配额限制,请升级会员以获取更多权益。',
    action: 'upgrade',
    actionText: '了解会员'
  };
}

/**
 * 显示配额错误提示(使用浏览器原生alert,可根据实际UI库替换)
 * @param {Object} error - 错误对象
 * @param {Function} onUpgrade - 升级回调函数
 */
export function showQuotaErrorDialog(error, onUpgrade) {
  const errorInfo = getQuotaErrorMessage(error);
  
  // 这里使用简单的 confirm,实际项目中应该使用 UI 库的 Dialog 组件
  const message = `${errorInfo.title}

${errorInfo.message}

${errorInfo.suggestion}`;
  const userConfirmed = window.confirm(`${message}\n\n点击"确定"查看会员权益`);
  
  if (userConfirmed && onUpgrade) {
    onUpgrade();
  }
}

/**
 * API 响应拦截器辅助函数
 * 用于在 axios 或其他 HTTP 客户端的响应拦截器中处理配额错误
 * @param {Object} response - API 响应对象
 * @param {Function} onUpgrade - 升级回调函数
 * @returns {Object} 响应对象
 */
export function handleQuotaError(response, onUpgrade) {
  if (response && response.data) {
    const { code, message } = response.data;
    
    if (code === QUOTA_ERROR_CODES.AI_QUOTA_EXCEEDED || 
        code === QUOTA_ERROR_CODES.KB_QUOTA_EXCEEDED) {
      // 显示错误提示
      showQuotaErrorDialog({ code, message }, onUpgrade);
    }
  }
  
  return response;
}

/**
 * 创建配额错误提示的 Vue 组件用数据
 * @param {Object} error - 错误对象
 * @returns {Object} Vue 组件可用的数据对象
 */
export function createQuotaErrorData(error) {
  const errorInfo = getQuotaErrorMessage(error);
  const quotaData = error.data || {};
  
  return {
    visible: true,
    type: 'warning',
    ...errorInfo,
    quotaUsed: quotaData.used || 0,
    quotaTotal: quotaData.total || 0,
    quotaRemaining: quotaData.remaining || 0
  };
}

/**
 * 格式化配额信息为可读文本
 * @param {Object} quotaData - 配额数据
 * @returns {string}
 */
export function formatQuotaInfo(quotaData) {
  if (!quotaData) return '';
  
  const { used = 0, total = 0, remaining = 0 } = quotaData;
  return `已使用 ${used}/${total},剩余 ${remaining}`;
}
