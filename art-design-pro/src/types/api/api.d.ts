/**
 * API 接口类型定义模块
 *
 * 提供所有后端接口的类型定义
 *
 * ## 主要功能
 *
 * - 通用类型（分页参数、响应结构等）
 * - 认证类型（登录、用户信息等）
 * - 系统管理类型（用户、角色等）
 * - 全局命名空间声明
 *
 * ## 使用场景
 *
 * - API 请求参数类型约束
 * - API 响应数据类型定义
 * - 接口文档类型同步
 *
 * ## 注意事项
 *
 * - 在 .vue 文件使用需要在 eslint.config.mjs 中配置 globals: { Api: 'readonly' }
 * - 使用全局命名空间，无需导入即可使用
 *
 * ## 使用方式
 *
 * ```typescript
 * const params: Api.Auth.LoginParams = { userName: 'admin', password: '123456' }
 * const response: Api.Auth.UserInfo = await fetchUserInfo()
 * ```
 *
 * @module types/api/api
 * @author Art Design Pro Team
 */

declare namespace Api {
  /** 通用类型 */
  namespace Common {
    /** 分页参数 */
    interface PaginationParams {
      /** 当前页码 */
      current: number
      /** 每页条数 */
      size: number
      /** 总条数 */
      total: number
    }

    /** 通用搜索参数 */
    type CommonSearchParams = Pick<PaginationParams, 'current' | 'size'>

    /** 分页响应基础结构 */
    interface PaginatedResponse<T = any> {
      records: T[]
      current: number
      size: number
      total: number
    }

    interface PageResult<T = any> {
      data: T[]
      total: number
    }

    /** 启用状态 */
    type EnableStatus = '1' | '2'
  }

  /** 聊天 Token 统计 */
  namespace Chat {
    interface TokenTrendItem {
      period: string
      totalTokens: number
    }

    interface TokenStatsVO {
      todayTokens: number
      monthTokens: number
      totalTokens: number
      monthlyTrend: TokenTrendItem[]
    }
  }

  namespace Dashboard {
    interface DashboardOverviewVO {
      activeUsers: number
      newNotes: number
      paidOrders: number
      requestCount: number
      activeUsersChange: string
      newNotesChange: string
      paidOrdersChange: string
      requestCountChange: string
    }

    interface UserGrowth {
      periods: string[]
      counts: number[]
      monthNew: number
      weekNew: number
    }

    interface NoteSquareStats {
      periods: string[]
      publishCounts: number[]
      interactionCounts: number[]
    }

    interface OrderStats {
      periods: string[]
      monthlyCounts: number[]
      quarterlyCounts: number[]
      yearlyCounts: number[]
      amounts: number[]
      monthAmount: number
    }

    interface DynamicsItem {
      username: string
      type: string
      target: string
    }

    interface Dynamics {
      newCount: number
      list: DynamicsItem[]
    }
  }

  /** 认证类型 */
  namespace Auth {
    /** 图形验证码 */
    interface CaptchaVO {
      captchaId: string
      imageBase64: string
    }

    /** 登录参数 */
    interface LoginParams {
      phone: string
      password: string
      captcha: string
      captchaId: string
      roleType: 1 | 2
    }

    /** 注册参数（Swagger RegisterDTO） */
    interface RegisterParams {
      phone: string
      password: string
      captcha: string
      captchaId: string
      roleType: 1 | 2
      username?: string
      avatarUrl?: string
    }

    /** 登录响应 */
    interface LoginResponse {
      token: string
    }

    /** 后端用户信息（Swagger User） */
    interface BackendUser {
      id: number
      phone: string
      roleType: number
      username: string
      avatarUrl?: string
      realName?: string
      gender?: number
      status?: number
      createTime?: string
      updateTime?: string
      isMember?: boolean
      memberExpireTime?: string
      memberLevelName?: string
    }

    /** 用户信息 */
    interface UserInfo {
      buttons: string[]
      roles: string[]
      userId: number
      userName: string
      phone?: string
      roleType?: number
      email?: string
      avatar?: string
    }
  }

  /** 系统管理类型 */
  namespace SystemManage {
    /** 用户列表 */
    type UserList = Api.Common.PaginatedResponse<UserListItem>

    /** 用户列表项 */
    interface UserListItem {
      id: number
      avatar: string
      status: string
      userName: string
      userGender: string
      nickName: string
      userPhone: string
      userEmail: string
      userRoles: string[]
      createBy: string
      createTime: string
      updateBy: string
      updateTime: string

      /** 下面为后端字段的可选透传（用于编辑回填） */
      roleType?: number
      gender?: number
      statusCode?: number
      avatarUrl?: string
      realName?: string
      memberActive?: boolean
      memberExpireTime?: string
      memberLevelName?: string
    }

    /** 用户搜索参数 */
    type UserSearchParams = Partial<
      Pick<UserListItem, 'id' | 'userName' | 'userGender' | 'userPhone' | 'userEmail' | 'status'> &
      Api.Common.CommonSearchParams
    >

    /** 管理员创建用户参数 */
    interface AdminUserCreateParams {
      phone: string
      password: string
      roleType: 1 | 2
      username?: string
      avatarUrl?: string
      realName?: string
      gender?: 0 | 1 | 2
      status?: 0 | 1 | 2
    }

    /** 管理员更新用户参数（字段可选） */
    interface AdminUserUpdateParams {
      username?: string
      avatarUrl?: string
      realName?: string
      gender?: 0 | 1 | 2
      password?: string
      roleType?: 1 | 2
      status?: 0 | 1 | 2
      memberActive?: boolean
      memberExpireTime?: string
    }

    /** 角色列表 */
    type RoleList = Api.Common.PaginatedResponse<RoleListItem>

    /** 角色列表项 */
    interface RoleListItem {
      roleId: number
      roleName: string
      roleCode: string
      description: string
      enabled: boolean
      createTime: string
    }

    /** 角色搜索参数 */
    type RoleSearchParams = Partial<
      Pick<RoleListItem, 'roleId' | 'roleName' | 'roleCode' | 'description' | 'enabled'> &
      Api.Common.CommonSearchParams
    >
  }

  namespace Note {
    interface NoteListParams {
      page?: number
      size?: number
      notebookId?: number
      keyword?: string
    }

    interface NoteBriefVO {
      id: number
      title: string
      status: number
      isPinned: boolean
      viewCount: number
      wordCount: number
      creatorName?: string
      notebookName: string
      tags: string[]
      createTime: string
      updateTime: string
    }

    interface NoteDetailVO {
      id: number
      notebookId: number
      title: string
      contentMd: string
      contentHtml: string
      status: number
      isPinned: boolean
      viewCount: number
      wordCount: number
      notebookName: string
      tags: string[]
      createTime: string
      updateTime: string
    }

    interface NoteCreateDTO {
      notebookId: number
      title: string
      contentMd?: string
      status?: number
      isPinned?: boolean
      tags?: string[]
    }

    interface NoteUpdateDTO {
      noteId: number
      title: string
      contentMd?: string
      status?: number
      isPinned?: boolean
      tags?: string[]
    }

    interface NoteAutoSaveDTO {
      noteId: number
      contentMd?: string
    }
  }

  namespace NoteSquare {
    interface NoteSquareListParams {
      keyword?: string
      sortBy?: string
      sortOrder?: string
      page?: number
      size?: number
    }

    interface NoteSquareVO {
      id: number
      squareId: number
      title: string
      content?: string
      summary?: string
      coverUrl?: string
      authorId?: number
      authorName?: string
      authorAvatar?: string
      likeCount?: number
      favoriteCount?: number
      commentCount?: number
      viewCount?: number
      isLiked?: boolean
      isFavorited?: boolean
      squareStatus?: number
      publishTime?: string
      updateTime?: string
      createTime?: string
      tags?: string[]
    }

    interface NoteSquareDetailVO {
      id: number
      squareId: number
      title: string
      content?: string
      summary?: string
      coverUrl?: string
      authorId?: number
      authorName?: string
      authorAvatar?: string
      likeCount?: number
      favoriteCount?: number
      commentCount?: number
      viewCount?: number
      isLiked?: boolean
      isFavorited?: boolean
      squareStatus?: number
      publishTime?: string
      updateTime?: string
      createTime?: string
      tags?: string[]
    }

    interface NoteSquarePublishDTO {
      noteId: number
      summary?: string
      coverUrl?: string
    }
  }

  namespace Notebook {
    interface NotebookVO {
      id: number
      name: string
      cover: string
      description: string
      sortOrder: number
      noteCount: number
      createTime: string
      updateTime: string
    }

    interface NotebookCreateDTO {
      name: string
      cover?: string
      description?: string
      sortOrder?: number
    }
  }

  namespace SharedKnowledgeBase {
    interface SharedKnowledgeBaseVO {
      id: number
      name: string
      description?: string
      tagName?: string
      coverUrl?: string
      hasPassword?: boolean
      creatorId?: number
      creatorName?: string
      creatorAvatar?: string
      isPublic?: boolean
      memberCount?: number
      fileCount?: number
      status?: number
      createTime?: string
      updateTime?: string
      isMember?: boolean
      userRole?: number
    }

    interface SharedKnowledgeBaseCreateDTO {
      name: string
      description?: string
      tagName?: string
      coverUrl?: string
      password?: string
      isPublic?: boolean
      initialFileIds?: number[]
    }

    interface SharedKnowledgeBaseUpdateDTO {
      name: string
      description?: string
      tagName?: string
      coverUrl?: string
      password?: string
      isPublic?: boolean
    }

    interface KnowledgeBaseFileVO {
      id: number
      knowledgeBaseId?: number
      fileId?: number
      originalFilename?: string
      fileName?: string
      extension?: string
      fileSize?: number
      fileUrl?: string
      fileType?: string
      uploaderId?: number
      uploaderName?: string
      uploaderAvatar?: string
      sourceType?: number
      sourceTypeName?: string
      uploadTime?: string
      processingStatus?: string
      documentCount?: number
    }
  }

  namespace Template {
    interface TemplateSearchParams {
      category?: string
      keyword?: string
      page?: number
      pageSize?: number
    }

    interface TemplateVO {
      id: number
      title: string
      description: string
      category: string
      thumbnail: string
      content?: string
      downloads: number
      rating: number
      views: number
      author: string
      authorId: number
      updatedAt: string
      createTime?: string
      tags: string[]
      commentCount: number
      isPublic?: boolean
      status?: number
    }

    interface TemplateCommentVO {
      id: number
      sourceType?: 'TEMPLATE' | 'NOTE'
      targetId?: number
      targetTitle?: string
      author: string
      avatar: string
      content: string
      rating: number
      createdAt: string
      likes: number
    }

    interface AdminCommentListParams extends Partial<Api.Common.CommonSearchParams> {
      sourceType?: 'ALL' | 'TEMPLATE' | 'NOTE'
      keyword?: string
    }

    interface TemplateCreateDTO {
      id?: number
      title: string
      description?: string
      category: string
      thumbnail?: string
      content?: string
      tags?: string[]
      isPublic?: boolean
    }
  }

  /** 支付订单相关 */
  namespace Payment {
    interface OrderVO {
      id: number
      orderNo: string
      userId?: number
      planName?: string
      amount: number
      payMethod?: number
      payMethodName?: string
      payTime?: string
      status?: number
      statusName?: string
      createTime?: string
    }

    interface OrderListParams extends Partial<Api.Common.CommonSearchParams> {
      orderNo?: string
      planName?: string
      status?: number
      payMethod?: number
      startTime?: string
      endTime?: string
    }
  }
}
