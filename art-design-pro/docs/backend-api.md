# 后端接口文档（Spring Boot）

## 技术栈与约定
- 框架：`Spring Boot 3`、`MyBatis-Plus`、`Sa-Token`、`Redis`
- 统一响应：`Result<T>`，成功 `code=0`，失败 `code=-1`，字段：`data`、`code`、`message`
- 分页响应：`PageResult<T>`，字段：`data`（列表）、`total`（总数）
- 鉴权：登录后携带 `Sa-Token` 生成的令牌；默认通过请求头 `satoken: <token>` 或 Cookie 传递
- 流式输出：SSE，`Content-Type: text/event-stream`

## 通用模块 `/api/common` （src/main/java/com/ldd/initialization/controller/CommonController.java）
- `POST /api/common/register`
  - 描述：用户注册
  - 请求体：`RegisterDTO`（`phone` 11位字符串、`password` 6-20位、`captcha`、`captchaId`、`roleType` 1/2/3）
  - 响应：`Result<Void>`
- `GET /api/common/captcha`
  - 描述：获取图形验证码
  - 响应：`Result<CaptchaVO>`（`captchaId`、`imageBase64`）
- `POST /api/common/login`
  - 描述：用户登录，返回令牌
  - 请求体：`LoginDTO`（`phone`、`password`、`captcha`、`captchaId`、`roleType`）
  - 响应：`Result<String>`（令牌字符串）
- `POST /api/common/logout`（需登录）
  - 描述：退出登录
  - 响应：`Result<Void>`
- `GET /api/common/user/info`（需登录）
  - 描述：获取当前登录用户信息
  - 响应：`Result<User>`
- `GET /api/common/user/info/{userId}`
  - 描述：根据用户ID获取信息
  - 响应：`Result<UserInfoVO>`
- `PUT /api/common/user/update`
  - 描述：更新当前用户信息（以登录用户ID覆盖）
  - 请求体：`User`
  - 响应：`Result<Void>`
- `POST /api/common/file/upload`
  - 描述：上传文件（`multipart/form-data` 字段名 `file`）
  - 响应：`Result<FileUpInfo>`
- `GET /api/common/users`（需 ADMIN 角色）
  - 描述：分页查询用户
  - 查询参数：`page`、`size`、`roleType?`、`keyword?`
  - 响应：`Result<PageResult<User>>`
- `GET /api/common/test/connection-pool`（需 ADMIN 角色）
  - 描述：PgVector 连接池状态检查
  - 响应：`Result<Map<String,Object>>`
- `GET /api/common/diagnose/vector-db`
  - 描述：诊断向量数据库（可选 `userId`）
  - 响应：`Result<Map<String,Object>>`

## 设置模块 `/api/settings` （src/main/java/com/ldd/initialization/controller/SettingsController.java）
- `GET /api/settings/ai`（需登录）
  - 描述：获取当前用户的 AI 设置；若不存在则初始化为默认值后返回
  - 响应：`Result<UserAISettings>`
- `PUT /api/settings/ai`（需登录）
  - 描述：更新当前用户的 AI 设置
  - 请求体：`AISettingsDTO`（与前端 Settings/index.vue 同步字段）
  - 校验：`personalKbMaxDocs/sharedKbMaxDocs` 在 1-50；`chatStyle/replyLanguage` 为枚举 1/2/3；端点最长 255
  - 响应：`Result<Void>`

## 笔记模块 `/api/notes` （src/main/java/com/ldd/initialization/controller/NoteController.java）
- 需登录：类上 `@SaCheckLogin`
- `POST /api/notes`
  - 描述：创建笔记
  - 请求体：`NoteCreateDTO`（`notebookId`、`title`、`contentMd?`、`status?`、`isPinned?`、`tags?`）
  - 响应：`Result<NoteDetailVO>`
- `GET /api/notes`
  - 描述：分页查询笔记
  - 查询参数：`page`、`size`、`notebookId?`、`keyword?`
  - 响应：`Result<PageResult<NoteBriefVO>>`
- `GET /api/notes/{noteId}`
  - 描述：获取笔记详情
  - 响应：`Result<NoteDetailVO>`
- `POST /api/notes/update`
  - 描述：更新笔记
  - 请求体：`NoteUpdateDTO`（`noteId`、`title`、`contentMd?`、`status?`、`isPinned?`、`tags?`）
  - 响应：`Result<NoteDetailVO>`
- `DELETE /api/notes/{noteId}`
  - 描述：删除笔记
  - 响应：`Result<Void>`
- `POST /api/notes/{noteId}/pin`
  - 描述：切换置顶
  - 响应：`Result<Void>`
- `GET /api/notes/tags`
  - 描述：获取用户所有标签
  - 响应：`Result<List<String>>`
- `POST /api/notes/autosave`
  - 描述：自动保存笔记内容
  - 请求体：`NoteAutoSaveDTO`（`noteId`、`contentMd`）
  - 响应：`Result<Void>`
- `POST /api/notes/{noteId}/add-to-knowledge-base`
  - 描述：将笔记加入个人知识库（向量化）
  - 响应：`Result<Map<String,Object>>`

## 笔记本模块 `/api/notebooks` （src/main/java/com/ldd/initialization/controller/NotebookController.java）
- 需登录：类上 `@SaCheckLogin`
- `POST /api/notebooks`
  - 描述：创建笔记本
  - 请求体：`NotebookCreateDTO`（`name`、`cover?`、`description?`、`sortOrder?`）
  - 响应：`Result<NotebookVO>`
- `GET /api/notebooks`
  - 描述：获取当前用户的笔记本列表
  - 响应：`Result<List<NotebookVO>>`
- `GET /api/notebooks/{notebookId}`
  - 描述：获取笔记本详情（仅当前用户的）
  - 响应：`Result<NotebookVO>`
- `PUT /api/notebooks/{notebookId}`
  - 描述：更新笔记本
  - 请求体：`NotebookCreateDTO`
  - 响应：`Result<NotebookVO>`
- `DELETE /api/notebooks/{notebookId}`
  - 描述：删除笔记本
  - 响应：`Result<Void>`

## 文件与文档模块 `/api/documents` （src/main/java/com/ldd/initialization/controller/DocumentController.java）
- `POST /api/documents/upload`（需登录）
  - 描述：上传 `.md/.txt/.doc/.pdf`，非 PDF 自动转换为 PDF 后存储与向量化
  - 表单：`file`（`multipart/form-data`）
  - 响应：`Result<FileUploadVO>`（含 `processingStatus`、`documentCount` 等）
- `POST /api/documents/files/list`（需登录）
  - 描述：获取用户上传文件列表（分页，JSON 请求体）
  - 请求体：`FileListRequestDTO`（`page`、`size`、`keyword?`、`fileType?`、`sortBy?`、`sortOrder?`）
  - 响应：`Result<PageResult<FileUpInfo>>`
- `DELETE /api/documents/files/{fileId}`（需登录）
  - 描述：删除用户上传文件（含向量数据清理）
  - 响应：`Result<Void>`
- `POST /api/documents/test-parse`
  - 描述：测试文档解析（无需登录）
  - 表单：`file`
  - 响应：`Result<Map<String,Object>>`

## 共享知识库模块 `/api/shared-knowledge-base` （src/main/java/com/ldd/initialization/controller/SharedKnowledgeBaseController.java）
- `POST /api/shared-knowledge-base`
  - 描述：创建共享知识库
  - 请求体：`SharedKnowledgeBaseCreateDTO`（`name`、`description?`、`coverUrl?`、`password?`、`isPublic?`、`initialFileIds?`）
  - 响应：`Result<SharedKnowledgeBaseVO>`
- `PUT /api/shared-knowledge-base/{knowledgeBaseId}`（需登录）
  - 描述：更新知识库
  - 请求体：`SharedKnowledgeBaseUpdateDTO`
  - 响应：`Result<SharedKnowledgeBaseVO>`
- `DELETE /api/shared-knowledge-base/{knowledgeBaseId}`（需登录）
  - 描述：删除知识库
  - 响应：`Result<Void>`
- `GET /api/shared-knowledge-base/{knowledgeBaseId}`（需登录）
  - 描述：知识库详情
  - 响应：`Result<SharedKnowledgeBaseVO>`
- `GET /api/shared-knowledge-base/my-created`（需登录）
  - 描述：我创建的知识库
  - 响应：`Result<List<SharedKnowledgeBaseVO>>`
- `GET /api/shared-knowledge-base/my-joined`（需登录）
  - 描述：我加入的知识库
  - 响应：`Result<List<SharedKnowledgeBaseVO>>`
- `GET /api/shared-knowledge-base/square`（需登录）
  - 描述：公开知识库广场
  - 查询参数：`keyword?`、`sortBy=create_time|member_count|file_count`、`sortOrder=asc|desc`、`page`、`size`
  - 响应：`Result<PageResult<KnowledgeBaseSquareVO>>`
- `POST /api/shared-knowledge-base/join`（需登录）
  - 描述：申请加入（若设置密码需提供）
  - 请求体：`KnowledgeBaseJoinDTO`（`knowledgeBaseId`、`password?`）
  - 响应：`Result<Void>`
- `POST /api/shared-knowledge-base/{knowledgeBaseId}/leave`（需登录）
  - 描述：退出知识库
  - 响应：`Result<Void>`
- `DELETE /api/shared-knowledge-base/{knowledgeBaseId}/members/{memberId}`（需登录）
  - 描述：移除成员（创建者权限）
  - 响应：`Result<Void>`
- `POST /api/shared-knowledge-base/{knowledgeBaseId}/files/upload`（需登录）
  - 描述：上传文件并向量化
  - 表单：`file`
  - 响应：`Result<KnowledgeBaseFileVO>`
- `POST /api/shared-knowledge-base/{knowledgeBaseId}/files/copy`（需登录）
  - 描述：从个人知识库复制文件
  - 请求体：`KnowledgeBaseFileUploadDTO`（`knowledgeBaseId`、`fileIds`、`sourceType=2`）
  - 响应：`Result<Map<String,Object>>`
- `DELETE /api/shared-knowledge-base/{knowledgeBaseId}/files/{fileId}`（需登录）
  - 描述：删除知识库文件（含向量清理）
  - 响应：`Result<Void>`
- `GET /api/shared-knowledge-base/{knowledgeBaseId}/files`（需登录）
  - 描述：分页获取知识库文件
  - 查询参数：`page`、`size`、`keyword?`
  - 响应：`Result<PageResult<KnowledgeBaseFileVO>>`
- `GET /api/shared-knowledge-base/personal-files`（需登录）
  - 描述：获取个人知识库文件列表
  - 响应：`Result<List<KnowledgeBaseFileVO>>`
- `GET /api/shared-knowledge-base/members/{knowledgeBaseId}`（需登录）
  - 描述：成员列表
  - 响应：`Result<List<KnowledgeBaseMemberVO>>`
- `GET /api/shared-knowledge-base/member/{userId}/{knowledgeBaseId}`
  - 描述：单个成员信息
  - 响应：`Result<KnowledgeBaseMemberVO>`
- `DELETE /api/shared-knowledge-base/members/{deleteUserId}/{knowledgeBaseId}`
  - 描述：删除指定成员（创建者权限）
  - 响应：`Result<Void>`
- `POST /api/shared-knowledge-base/chat/stream`（需登录，SSE）
  - 描述：基于共享知识库的流式 AI 对话
  - 请求体：`SharedKnowledgeBaseChatDTO`（`knowledgeBaseId`、`message`、`memoryId?`）
  - 响应：`Flux<String>`（SSE 文本片段）

## 聊天模块 `/api/chat` （src/main/java/com/ldd/initialization/controller/ChatController.java）
- `POST /api/chat/stream`（需登录，SSE）
  - 描述：流式聊天（带速率控制）
  - 请求体：`ChatRequestDTO`（`memoryId`、`message`；`userId`由后端填充）
  - 响应：`Flux<String>`（SSE 文本片段）
- `POST /api/chat/stream/local`（无需登录，SSE）
  - 描述：快速流式聊天（不做速率控制）
  - 表单参数：`modelName`、`memoryId`、`message`
  - 响应：`Flux<String>`

## 领域与模型参考
- `User`（用户）：`id`、`phone`、`roleType`、`username`、`avatarUrl`、`realName`、`idCard`、`gender`、`birthDate`、`workYears`、`education`、`status`、`lastLoginTime`、`lastLoginIp`、`createTime`、`updateTime`
- `FileUpInfo`（文件）：`id`、`originalFilename`、`fileName`、`fileExtension`、`fileSize`、`filePath`、`fileUrl`、`userId`、`fileMd5`、`createTime`、`updateTime`、`fileType`、`mimeType`、`fileHash`、`sourceType`、`sourceNoteId`、`sourceNoteTitle`
- `NoteBriefVO`、`NoteDetailVO`、`NotebookVO`、`SharedKnowledgeBaseVO`、`KnowledgeBaseFileVO`、`KnowledgeBaseMemberVO`、`FileUploadVO` 详见 VO 类定义

## 错误处理
- 业务异常：`Result.error(BizException)`，`code` 来源于业务码
- 通用异常：`Result.error(String|RuntimeException)`，`code=-1`，`message` 为错误描述

## 鉴权与角色
- 登录校验：`@SaCheckLogin`
- 角色校验：`@SaCheckRole("ADMIN")`
- 拦截器：全局登录校验，部分路径排除（如 `/file/local-plus/**`）
