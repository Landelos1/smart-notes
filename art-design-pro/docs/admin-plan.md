# 后台管理系统开发计划（admin-plan）

> 目标：基于现有 `art-design-pro`（Vue3 + Vite + Pinia + Element Plus）工程，按 `docs/admin-system-design.md` 与 `docs/backend-api.md` 的接口与业务设计，逐步完成后台管理端各功能模块。

## 1. 全局约定（必须统一）

### 1.1 技术与工程约定

- **前端技术栈**：Vue 3、Vite、TypeScript、Pinia、Vue Router、Element Plus、ECharts（趋势图/统计）
- **请求封装**：`src/utils/http/index.ts`
  - 自动注入 `satoken`、`Authorization` 请求头
  - 401 统一登出（带防抖）
- **代理**：`vite.config.ts` 已配置 `/api -> VITE_API_PROXY_URL`

### 1.2 后端响应与分页约定

- **统一响应结构**：`Result<T>`
  - 成功：`code = 0`
  - 字段：`data / code / message`（前端也兼容 `msg`）
- **分页响应**：`PageResult<T>`
  - 字段：`data`（列表）、`total`（总数）
- **分页入参**：后端普遍使用 `page/size`，前端表格默认使用 `current/size`
  - 策略：在 API 层做 `current -> page` 参数映射；在 API 层将 `PageResult<T>` 适配成 `Api.Common.PaginatedResponse<T>`（`records/current/size/total`），保证 `useTable` 无需大改。

### 1.3 鉴权与权限约定

- **Token**：Sa-Token 返回 token 字符串
- **Header**：`satoken: <token>` 或 `Authorization: <token>`（项目内已同时设置）
- **角色映射**（来源：`docs/admin-system-design.md`、`docs/backend-api.md`）
  - `roleType = 1`：普通用户（前端：`R_USER`）
  - `roleType = 2`：管理员（前端：`R_ADMIN`）
  - `roleType = 3`：超级管理员（前端：`R_SUPER`）
- **前端权限控制**：
  - 粗粒度：`v-roles`（检查 `userStore.getUserInfo.roles`）
  - 细粒度：`v-auth`（检查 `userStore.info.buttons` 或 route meta）

---

## 2. 里程碑（建议顺序）

- **M0：认证与权限基础模块（已完成）**
- **M1：仪表盘 Dashboard（/api/admin/dashboard/stats）**
- **M2：用户管理增强（状态/会员/Token 配额等 /api/admin/users/*）**
- **M3：会员套餐管理（/api/admin/plans）**
- **M4：订单管理（/api/admin/orders）**
- **M5：Token 消耗管理（/api/admin/tokens/*）**
- **M6：内容管理（笔记/知识库/文件 /api/admin/*）**
- **M7：系统设置与操作日志（角色权限/系统配置/管理员日志）**

---

## 3. 模块拆分计划（按模块）

> 说明：每个模块统一包含「目标/范围」「接口清单」「前端页面」「权限」「验收标准」。

### 3.1 认证与权限基础模块（M0，已完成）

- **范围**：
  - `/api/common/captcha`、`/api/common/login`、`/api/common/logout`、`/api/common/user/info`
  - `/api/common/users`（用户列表分页）
  - `roleType -> roles/buttons` 映射
  - 登录页适配（手机号 + 验证码 + 角色类型）
- **交付说明**：见 `docs/认证与权限基础模块.md`
- **验收**：
  - 登录/获取用户信息/退出登录链路闭环
  - `v-roles`、`v-auth` 可工作
  - `pnpm run build` 通过

### 3.2 仪表盘模块（M1）

#### 目标/范围

- 在后台首页展示关键业务指标：用户、内容、订单、Token 消耗等
- 提供趋势图（Token 消耗趋势）与概览卡片

#### 后端接口

- **GET** `/api/admin/dashboard/stats`
  - 返回：用户总数、今日新增、活跃用户、会员用户、笔记/知识库/文件总数、今日订单/收入、本月收入、Token 今日/月消耗等

#### 前端页面/组件

- **建议页面**：复用现有控制台页面（如 `src/views/dashboard/console`）进行改造
- **组件建议**：
  - 指标卡片组件（可复用现有卡片样式）
  - 折线图组件（ECharts）
  - 最近订单/最近注册用户（如后端无接口，先不做或后端补接口）

#### 权限

- `R_ADMIN` / `R_SUPER` 可访问

#### 验收标准

- 页面可正常加载，接口失败有兜底提示
- 统计字段全部展示（缺字段时按 0/-- 处理）
- 趋势图可切换时间范围（可选：7天/30天）

### 3.3 用户管理模块（M2）

#### 目标/范围

- 管理员分页查询用户、查看用户详情
- 管理员可禁用/启用用户
- 管理员可查看/调整用户会员权益、Token 配额

#### 后端接口（已存在/新增）

- **GET** `/api/common/users`（需 ADMIN）
  - Query：`page`、`size`、`roleType?`、`keyword?`
  - Response：`Result<PageResult<User>>`
- **GET** `/api/common/user/info/{userId}`（用户详情）
- **PUT** `/api/admin/users/{userId}/status`
- **GET** `/api/admin/users/{userId}/membership`
- **POST** `/api/admin/users/{userId}/grant-membership`
- **PUT** `/api/admin/users/{userId}/token-quota`

#### 前端页面/组件

- 现有页面：`src/views/system/user`（列表 + 搜索 + 编辑弹窗）
  - 改造点：字段对齐后端 `User`（例如 `avatarUrl/realName/gender/status`）
- 新增/扩展：
  - 用户详情 Drawer（展示会员信息、Token 配额、最近登录信息等）
  - 操作弹窗：禁用/启用原因、调整配额原因、赠送会员备注

#### 权限

- 列表/详情：`R_ADMIN`、`R_SUPER`
- 危险操作（赠送会员/调整配额/禁用用户）：可先全部给 `R_SUPER`，待后端明确权限再细分

#### 验收标准

- 列表分页、搜索（keyword）、按角色筛选（roleType）可用
- 启用/禁用有二次确认 + 成功提示 + 列表刷新
- 会员/Token 操作可回显最新状态

### 3.4 会员套餐管理模块（M3）

#### 目标/范围

- 配置会员套餐（名称、编码、价格、时长、Token/知识库/文件配额等）
- 支持启用/禁用（或删除=禁用）

#### 后端接口（设计文档新增）

- **GET** `/api/admin/plans`
- **POST** `/api/admin/plans`
- **PUT** `/api/admin/plans/{planId}`
- **DELETE** `/api/admin/plans/{planId}`（禁用）

#### 前端页面/组件

- 新建模块建议：`src/views/membership/plans`
- 组件：
  - 套餐列表表格（useTable）
  - 套餐新增/编辑弹窗（Form + 校验）

#### 权限

- 查看：`R_ADMIN` / `R_SUPER`
- 新增/编辑/删除：优先限制 `R_SUPER`

#### 验收标准

- CRUD 流程闭环，表单校验完整
- 套餐状态变更可见且可筛选

### 3.5 订单管理模块（M4）

#### 目标/范围

- 管理员分页查询订单
- 查看订单详情
- 手动下发权益、取消订单、退款（以业务规则为准）

#### 后端接口（设计文档新增）

- **GET** `/api/admin/orders`（分页 + 筛选）
- **GET** `/api/admin/orders/{orderId}`
- **POST** `/api/admin/orders/{orderId}/deliver`
- **POST** `/api/admin/orders/{orderId}/cancel`
- **POST** `/api/admin/orders/{orderId}/refund`

#### 前端页面/组件

- 新建模块建议：`src/views/membership/orders`
- 组件：
  - 订单列表（筛选：状态、用户、时间范围）
  - 订单详情 Drawer
  - 操作确认弹窗（下发/取消/退款）

#### 权限

- 查看：`R_ADMIN` / `R_SUPER`
- 退款/下发：可先限制 `R_SUPER`

#### 验收标准

- 列表筛选、分页正确
- 订单状态流转符合接口返回
- 操作后刷新并展示 message

### 3.6 Token 消耗管理模块（M5）

#### 目标/范围

- Token 全局消耗统计（按日期、模型、用户等维度）
- Token 消耗日志查询

#### 后端接口（设计文档新增）

- **GET** `/api/admin/tokens/stats`
- **GET** `/api/admin/tokens/logs`
- **PUT** `/api/admin/users/{userId}/token-quota`（与用户管理联动）

#### 前端页面/组件

- 新建模块建议：`src/views/token`
  - `stats`：趋势图 + 维度汇总
  - `logs`：分页日志表格（筛选：userId、usageType、时间范围）

#### 权限

- `R_ADMIN` / `R_SUPER`

#### 验收标准

- 统计图表展示正确，筛选条件生效
- 日志分页正确，字段展示完整

### 3.7 内容管理模块（M6）

#### 目标/范围

- 管理员视角：笔记管理、知识库管理、文件管理
- 支持管理员删除违规内容

#### 后端接口（设计文档新增）

- **GET** `/api/admin/notes`
- **DELETE** `/api/admin/notes/{noteId}`
- **GET** `/api/admin/knowledge-bases`
- **DELETE** `/api/admin/knowledge-bases/{kbId}`
- **GET** `/api/admin/files`
- **DELETE** `/api/admin/files/{fileId}`

#### 前端页面/组件

- 新建模块建议：`src/views/content`
  - notes：笔记列表 + 预览 + 删除
  - knowledge-bases：知识库列表 + 删除
  - files：文件列表 + 删除

#### 权限

- 查看：`R_ADMIN` / `R_SUPER`
- 删除：建议 `R_ADMIN` 也可（待后端规则确认）

#### 验收标准

- 列表分页、筛选可用
- 删除操作二次确认；删除后列表刷新

### 3.8 系统设置与操作日志模块（M7）

#### 目标/范围

- 系统参数配置（若后端提供）
- 管理员操作日志查询（t_admin_log）
- 角色/权限策略管理（若后端提供）

#### 后端接口（可能需要后端补充）

- 建议新增：
  - **GET** `/api/admin/logs`
  - **GET** `/api/admin/config`
  - **PUT** `/api/admin/config`

#### 前端页面/组件

- `src/views/system` 下扩展：
  - 操作日志列表
  - 系统配置表单

#### 权限

- `R_SUPER` 优先

#### 验收标准

- 日志可按时间/模块/管理员筛选
- 配置项变更可追溯（写入操作日志）

---

## 4. 统一实现清单（所有模块共用）

- **API 分层**：
  - `src/api/auth.ts`、`src/api/system-manage.ts`（已存在）
  - 建议新增：`src/api/admin/dashboard.ts`、`src/api/admin/membership.ts`、`src/api/admin/orders.ts`、`src/api/admin/tokens.ts`、`src/api/admin/content.ts`
- **类型沉淀**：在 `src/types/api/api.d.ts` 内按命名空间追加（例如 `Api.AdminDashboard.*`、`Api.Membership.*`、`Api.Orders.*`、`Api.Token.*`、`Api.Content.*`）
- **表格统一**：所有分页接口统一适配为 `Api.Common.PaginatedResponse<T>`，用 `useTable` 管理分页/搜索
- **权限落点**：菜单/路由与按钮权限统一以 `roles/buttons` 驱动，并在 UI 上使用 `v-roles` / `v-auth`

---

## 5. 验收与交付

- **功能验收**：按每模块“验收标准”逐条检查
- **工程验收**：
  - `pnpm run build` 必须通过
  - 关键流程回归：登录 -> 用户信息 -> 权限生效 -> 退出登录
- **联调验收**：
  - 后端接口变更时，优先在 API wrapper 层做兼容/适配，减少页面级改动
