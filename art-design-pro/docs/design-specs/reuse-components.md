# 复用组件清单与改造说明（v0.1）

## 可直接复用组件
- 统一响应与分页
  - `Result<T>`：`initialization-note-side/src/main/java/com/ldd/initialization/result/Result.java`
  - `PageResult<T>`：`initialization-note-side/src/main/java/com/ldd/initialization/result/PageResult.java`
- 鉴权与拦截
  - `SaTokenConfiguration`：全局登录拦截与角色获取
- 文档与文件处理
  - `DocumentController` 上传/列表/删除
  - `DocumentService` 文档解析/分割/向量化
  - `FileInfoService` 文件持久化
- 笔记与笔记本
  - `NoteController` 与 `NotebookController` + 对应 Service/VO/DTO
- 共享知识库
  - `SharedKnowledgeBaseController` + Service 层成员/文件管理与权限校验
- 聊天与流式
  - `ChatController` 与 `SharedKnowledgeBaseController.chat/stream`（SSE 输出）
  - `ConsultantService`/`UserConsultantService`/`DynamicChatDispatcher`
- 存储与基础设施
  - MyBatis-Plus Mapper/配置、Redis、线程池、异常处理

## 需要适配改造的接口/模块
- 笔记列表筛选增强
  - Controller：扩展查询参数（`tags/status/isPinned`）
  - Service/Mapper：按参数构造条件，避免破坏原逻辑
- 文件下载与预览
  - 新增 `GET /api/documents/files/{fileId}/download`，校验 `userId`，返回文件流或重定向 `fileUrl`
  - 预览接口可选（PDF 优先）
- 个人知识库聊天
  - 新增端点 `POST /api/chat/stream/personal`，在服务层强制 `knowledge_base_type=1` 且 `knowledge_base_id=当前用户ID`
- 带引用的聊天输出
  - 新增端点 `POST /api/chat/stream-with-citations`，在流结束时发送 `citations` 事件，内容来自 `documents.metadata`（`fileId/originalFilename/snippetId`）
- 共享广场统计与标签
  - 统计接口：`GET /api/shared-knowledge-base/{knowledgeBaseId}/stats`（成员/文件/向量条目/热度）
  - 标签可先计算生成，无需表结构；稳定后再引入 `t_shared_knowledge_base_tag`
- 向量索引
  - 迁移脚本增加 HNSW 索引，提升检索性能

## 扩展点设计
- DTO 与 VO
  - 原有 DTO/VO 保持不变；新增端点使用独立 DTO，减少对既有接口的影响
- 统一错误返回
  - 所有新接口继续使用 `Result.error(...)` 与 `BizException`，避免裸异常
- 性能与限流
  - 聊天端点沿用现有速率控制配置；统计与列表接口需索引保障

