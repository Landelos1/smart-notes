/**
 * 笔记类型
 * @typedef {Object} Note
 * @property {string} id - 笔记ID
 * @property {string} title - 标题
 * @property {string} content - 内容
 * @property {string[]} tags - 标签
 * @property {string} notebookId - 笔记本ID
 * @property {boolean} isPinned - 是否置顶
 * @property {Date} createdAt - 创建时间
 * @property {Date} updatedAt - 更新时间
 */

/**
 * 笔记本类型
 * @typedef {Object} Notebook
 * @property {string} id - 笔记本ID
 * @property {string} name - 名称
 * @property {number} noteCount - 笔记数量
 */

/**
 * 过滤类型
 * @typedef {'全部'|'最近7天'|string} FilterType
 */

/**
 * 用户个人信息
 * @typedef {Object} UserProfile
 * @property {string} [id] - 用户ID
 * @property {string} username - 用户名
 * @property {string} phone - 手机号
 * @property {string} avatarUrl - 头像URL
 * @property {0|1|2} gender - 性别 0未设置/1男/2女
 * @property {string} birthDate - 生日
 */

/**
 * AI助手设置
 * @typedef {Object} AISettings
 * @property {boolean} enablePersonalKb - 启用个人知识库
 * @property {number} personalKbMaxDocs - 个人知识库最大文档数
 * @property {1|2|3} chatStyle - 对话风格 1简洁/2适中/3详细
 * @property {1|2|3} replyLanguage - 回复语言 1跟随问题/2只中文/3只英文
 * @property {string} assistantName - 助手名称
 * @property {string} chatApiKey - 对话API Key
 * @property {string} chatApiEndpoint - 对话API端点
 * @property {boolean} enableSharedKb - 启用共享知识库
 * @property {number} sharedKbMaxDocs - 共享知识库最大文档数
 * @property {string} customSystemPrompt - 自定义系统提示词
 * @property {string} kbApiKey - 知识库API Key
 * @property {string} kbApiEndpoint - 知识库API端点
 * @property {boolean} useCustomKey - 是否使用自定义 API Key 对话
 * @property {string} chatModel - 用户选择的模型名称（useCustomKey=true 时生效）
 * @property {boolean} kbUseCustomKey - 是否对知识库 AI 使用自定义 API Key
 * @property {string} kbChatModel - 知识库 AI 使用的模型名称（kbUseCustomKey=true 时生效）
 */

/**
 * 共享知识库
 * @typedef {Object} SharedKnowledgeBase
 * @property {string} id - 知识库ID
 * @property {string} name - 名称
 * @property {string} description - 描述
 * @property {string} coverUrl - 封面URL
 * @property {boolean} isPublic - 是否公开
 * @property {boolean} hasPassword - 是否有密码
 * @property {number} memberCount - 成员数量
 * @property {number} fileCount - 文件数量
 * @property {string} creatorName - 创建者名称
 * @property {string} creatorId - 创建者ID
 * @property {boolean} isJoined - 是否已加入
 * @property {string} createTime - 创建时间
 */

/**
 * 知识库文件
 * @typedef {Object} KnowledgeBaseFile
 * @property {string} id - 文件ID
 * @property {string} originalFilename - 原始文件名
 * @property {number} fileSize - 文件大小
 * @property {string} fileType - 文件类型
 * @property {string} fileUrl - 文件URL
 * @property {string} uploaderName - 上传者名称
 * @property {string} uploadTime - 上传时间
 * @property {'upload'|'note'} sourceType - 来源类型
 */

/**
 * 知识库成员角色
 * @typedef {'admin'|'editor'|'viewer'} KbRole
 */

/**
 * 知识库成员
 * @typedef {Object} KnowledgeBaseMember
 * @property {string} id - 成员ID
 * @property {string} userId - 用户ID
 * @property {string} username - 用户名
 * @property {string} avatarUrl - 头像URL
 * @property {KbRole} role - 成员角色
 * @property {string} roleName - 角色名称（显示用）
 * @property {string} status - 成员状态（显示用）
 * @property {boolean} online - 是否在线
 * @property {string} joinTime - 加入时间
 */

/**
 * 聊天消息
 * @typedef {Object} ChatMessage
 * @property {'user'|'ai'} type - 消息类型
 * @property {string} content - 内容
 * @property {string} time - 时间
 */

/**
 * 知识库邀请
 * @typedef {Object} KbInvitation
 * @property {number} id - 邀请ID
 * @property {number} knowledgeBaseId - 知识库ID
 * @property {string} knowledgeBaseName - 知识库名称
 * @property {string} knowledgeBaseCover - 知识库封面URL
 * @property {string} knowledgeBaseDescription - 知识库描述
 * @property {number} inviterId - 邀请人用户ID
 * @property {string} inviterName - 邀请人用户名
 * @property {string} inviterAvatar - 邀请人头像URL
 * @property {number} role - 分配的角色(1:创建者/2:管理员/3:成员)
 * @property {string} roleLabel - 角色显示名称
 * @property {number} status - 邀请状态(0:待处理/1:已接受/2:已拒绝/3:已过期)
 * @property {string} inviteTime - 邀请时间
 * @property {string} expireTime - 过期时间
 * @property {number} daysLeft - 剩余天数
 * @property {string} inviterMessage - 邀请附言
 */

// 导出空对象以保持模块结构
export default {}
