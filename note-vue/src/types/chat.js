/**
 * 消息类型
 * @typedef {Object} Message
 * @property {string} id - 消息ID
 * @property {'user'|'assistant'} role - 角色
 * @property {string} content - 内容
 * @property {{name: string, previewUrl?: string}[]} [attachments] - 当前消息携带的附件
 * @property {{fileId?: number, title?: string, quote?: string, page?: number, chunkIndex?: number, score?: number, sourceType?: string}[]} [citations] - 参考来源文件
 * @property {Date} timestamp - 时间戳
 */

/**
 * 聊天会话类型
 * @typedef {Object} ChatSession
 * @property {string} id - 会话ID
 * @property {string} title - 标题
 * @property {string} lastMessage - 最后消息
 * @property {Date} timestamp - 时间戳
 * @property {Message[]} messages - 消息列表
 */

// 导出空对象以保持模块结构
export default {}
