# 智能知识库系统 - 后台管理系统设计文档

## 一、系统概述

### 1.1 项目背景
本文档为智能知识库系统的后台管理系统设计方案，旨在为系统管理员提供用户管理、内容管理、会员订单管理、Token消耗统计等功能。后台管理系统与移动端/Web端共用同一套后端代码，通过角色权限区分不同用户的访问权限。

### 1.2 设计目标
1. **复用现有后端**：基于现有Spring Boot + MyBatis-Plus架构扩展
2. **统一认证体系**：复用Sa-Token认证框架
3. **渐进式开发**：优先实现核心功能，后续迭代完善

### 1.3 技术架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端层                                │
├─────────────────┬───────────────────────────────────────┤
│   管理后台       │         移动端/Web端                  │
│   Vue3+Vite     │         Vue3+Vite                     │
│   Element-Plus  │         TailwindCSS                   │
├─────────────────┴───────────────────────────────────────┤
│                    API网关层                             │
│              Nginx + CORS + 路由分发                     │
├─────────────────────────────────────────────────────────┤
│                    后端服务层                            │
│           Spring Boot 3 + Sa-Token + WebFlux            │
├─────────────────────────────────────────────────────────┤
│                    数据存储层                            │
│         PostgreSQL + pgvector + Redis                   │
└─────────────────────────────────────────────────────────┘
```

---

## 二、功能模块设计

### 2.1 系统功能架构图

```
后台管理系统
├── 首页仪表盘
│   ├── 用户统计
│   ├── 内容统计
│   ├── 订单统计
│   └── Token消耗趋势
├── 用户管理
│   ├── 用户列表
│   ├── 用户详情
│   ├── 用户编辑
│   └── 用户状态管理
├── 内容管理
│   ├── 笔记管理
│   ├── 知识库管理
│   ├── 文档管理
│   └── 模板管理
├── 会员订单管理
│   ├── 套餐配置
│   ├── 订单列表
│   └── 权益下发状态
├── Token消耗管理
│   ├── 消耗统计
│   ├── 用户配额
│   └── 消耗日志
└── 系统设置
    ├── 角色权限
    └── 系统配置
```

---

## 三、数据库设计

### 3.1 现有表（复用）

| 表名 | 说明 | 用途 |
|------|------|------|
| t_user | 用户表 | 用户管理、会员管理 |
| t_user_ai_settings | 用户AI设置 | AI配置管理 |
| t_note | 笔记表 | 内容管理 |
| t_notebook | 笔记本表 | 内容管理 |
| t_file_info | 文件信息表 | 文档管理 |
| t_shared_knowledge_base | 共享知识库 | 知识库管理 |
| t_knowledge_base_member | 知识库成员 | 成员管理 |

### 3.2 新增表设计

#### 3.2.1 会员套餐表 (t_membership_plan)

```sql
-- 会员套餐配置表
CREATE TABLE IF NOT EXISTS "public"."t_membership_plan" (
    "id" BIGSERIAL PRIMARY KEY,
    "name" VARCHAR(100) NOT NULL,              -- 套餐名称
    "code" VARCHAR(50) NOT NULL UNIQUE,        -- 套餐编码 (FREE/BASIC/PRO/ENTERPRISE)
    "description" TEXT,                         -- 套餐描述
    "price" DECIMAL(10,2) NOT NULL DEFAULT 0,  -- 价格（元）
    "duration_days" INTEGER NOT NULL DEFAULT 30, -- 有效期（天）
    "token_quota" BIGINT NOT NULL DEFAULT 0,   -- Token配额
    "kb_quota" INTEGER NOT NULL DEFAULT 1,     -- 知识库配额
    "file_quota" INTEGER NOT NULL DEFAULT 10,  -- 文件上传配额
    "file_size_limit" BIGINT DEFAULT 10485760, -- 单文件大小限制（字节）
    "features" JSONB,                          -- 功能特性（JSON格式）
    "sort_order" INTEGER DEFAULT 0,            -- 排序
    "status" SMALLINT DEFAULT 1,               -- 状态：1-启用 0-禁用
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE "public"."t_membership_plan" IS '会员套餐配置表';

-- 插入默认套餐
INSERT INTO "public"."t_membership_plan" (name, code, description, price, duration_days, token_quota, kb_quota, file_quota, sort_order) VALUES
('免费版', 'FREE', '基础功能免费使用', 0, 0, 10000, 1, 5, 1),
('基础版', 'BASIC', '适合个人用户', 19.9, 30, 100000, 3, 20, 2),
('专业版', 'PRO', '适合专业用户', 49.9, 30, 500000, 10, 100, 3),
('企业版', 'ENTERPRISE', '适合团队使用', 199.9, 30, 2000000, 50, 500, 4);
```

#### 3.2.2 用户会员表 (t_user_membership)

```sql
-- 用户会员关系表
CREATE TABLE IF NOT EXISTS "public"."t_user_membership" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,                 -- 用户ID
    "plan_id" BIGINT NOT NULL,                 -- 套餐ID
    "plan_code" VARCHAR(50),                   -- 套餐编码（冗余）
    "start_time" TIMESTAMP NOT NULL,           -- 开始时间
    "end_time" TIMESTAMP,                      -- 结束时间（NULL表示永久）
    "token_used" BIGINT DEFAULT 0,             -- 已使用Token
    "token_quota" BIGINT DEFAULT 0,            -- Token配额
    "status" SMALLINT DEFAULT 1,               -- 状态：1-生效 2-过期 0-取消
    "source_order_id" BIGINT,                  -- 来源订单ID
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "fk_user_membership_user" FOREIGN KEY ("user_id") 
        REFERENCES "public"."t_user"("id") ON DELETE CASCADE,
    CONSTRAINT "fk_user_membership_plan" FOREIGN KEY ("plan_id") 
        REFERENCES "public"."t_membership_plan"("id")
);

CREATE INDEX "idx_user_membership_user" ON "public"."t_user_membership"("user_id");
CREATE INDEX "idx_user_membership_status" ON "public"."t_user_membership"("status", "end_time");

COMMENT ON TABLE "public"."t_user_membership" IS '用户会员关系表';
```

#### 3.2.3 订单表 (t_order)

```sql
-- 订单表
CREATE TABLE IF NOT EXISTS "public"."t_order" (
    "id" BIGSERIAL PRIMARY KEY,
    "order_no" VARCHAR(64) NOT NULL UNIQUE,    -- 订单号
    "user_id" BIGINT NOT NULL,                 -- 用户ID
    "plan_id" BIGINT NOT NULL,                 -- 套餐ID
    "plan_name" VARCHAR(100),                  -- 套餐名称（冗余）
    "amount" DECIMAL(10,2) NOT NULL,           -- 订单金额
    "pay_amount" DECIMAL(10,2),                -- 实付金额
    "pay_method" SMALLINT,                     -- 支付方式：1-微信 2-支付宝 3-余额
    "pay_time" TIMESTAMP,                      -- 支付时间
    "pay_no" VARCHAR(100),                     -- 第三方支付单号
    "status" SMALLINT DEFAULT 0,               -- 状态：0-待支付 1-已支付 2-已取消 3-已退款
    "delivery_status" SMALLINT DEFAULT 0,      -- 权益下发状态：0-未下发 1-下发中 2-已下发 3-下发失败
    "delivery_time" TIMESTAMP,                 -- 权益下发时间
    "delivery_msg" TEXT,                       -- 下发消息（失败原因等）
    "expire_time" TIMESTAMP,                   -- 订单过期时间
    "remark" TEXT,                             -- 备注
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "fk_order_user" FOREIGN KEY ("user_id") 
        REFERENCES "public"."t_user"("id") ON DELETE CASCADE,
    CONSTRAINT "fk_order_plan" FOREIGN KEY ("plan_id") 
        REFERENCES "public"."t_membership_plan"("id")
);

CREATE INDEX "idx_order_user" ON "public"."t_order"("user_id");
CREATE INDEX "idx_order_status" ON "public"."t_order"("status");
CREATE INDEX "idx_order_create_time" ON "public"."t_order"("create_time" DESC);

COMMENT ON TABLE "public"."t_order" IS '订单表';
```

#### 3.2.4 Token消耗记录表 (t_token_usage)

```sql
-- Token消耗记录表
CREATE TABLE IF NOT EXISTS "public"."t_token_usage" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,                 -- 用户ID
    "session_id" BIGINT,                       -- 会话ID
    "request_tokens" INTEGER DEFAULT 0,        -- 请求Token数
    "response_tokens" INTEGER DEFAULT 0,       -- 响应Token数
    "total_tokens" INTEGER DEFAULT 0,          -- 总Token数
    "model" VARCHAR(50),                       -- 使用的模型
    "usage_type" SMALLINT DEFAULT 1,           -- 类型：1-对话 2-文档解析 3-向量化
    "cost" DECIMAL(10,6) DEFAULT 0,            -- 成本（元）
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "fk_token_usage_user" FOREIGN KEY ("user_id") 
        REFERENCES "public"."t_user"("id") ON DELETE CASCADE
);

CREATE INDEX "idx_token_usage_user" ON "public"."t_token_usage"("user_id");
CREATE INDEX "idx_token_usage_time" ON "public"."t_token_usage"("create_time" DESC);
CREATE INDEX "idx_token_usage_user_time" ON "public"."t_token_usage"("user_id", "create_time" DESC);

COMMENT ON TABLE "public"."t_token_usage" IS 'Token消耗记录表';
```

#### 3.2.5 操作日志表 (t_admin_log)

```sql
-- 管理员操作日志表
CREATE TABLE IF NOT EXISTS "public"."t_admin_log" (
    "id" BIGSERIAL PRIMARY KEY,
    "admin_id" BIGINT NOT NULL,                -- 管理员ID
    "admin_name" VARCHAR(50),                  -- 管理员名称
    "module" VARCHAR(50),                      -- 操作模块
    "action" VARCHAR(100),                     -- 操作动作
    "target_type" VARCHAR(50),                 -- 目标类型
    "target_id" BIGINT,                        -- 目标ID
    "content" TEXT,                            -- 操作内容描述
    "ip" VARCHAR(50),                          -- 操作IP
    "user_agent" VARCHAR(500),                 -- 浏览器标识
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX "idx_admin_log_admin" ON "public"."t_admin_log"("admin_id");
CREATE INDEX "idx_admin_log_time" ON "public"."t_admin_log"("create_time" DESC);

COMMENT ON TABLE "public"."t_admin_log" IS '管理员操作日志表';
```

### 3.3 用户表扩展字段

需要在 `t_user` 表中添加以下字段：

```sql
-- 扩展用户表
ALTER TABLE "public"."t_user" 
ADD COLUMN IF NOT EXISTS "membership_level" SMALLINT DEFAULT 0,      -- 会员等级：0-免费 1-基础 2-专业 3-企业
ADD COLUMN IF NOT EXISTS "membership_expire" TIMESTAMP,              -- 会员过期时间
ADD COLUMN IF NOT EXISTS "token_balance" BIGINT DEFAULT 10000,       -- Token余额
ADD COLUMN IF NOT EXISTS "total_token_used" BIGINT DEFAULT 0;        -- 累计使用Token
```

### 3.4 ER图

```
┌─────────────────┐     ┌─────────────────────┐
│   t_user        │────<│ t_user_membership   │
│   (用户表)       │     │ (用户会员关系)       │
└────────┬────────┘     └──────────┬──────────┘
         │                         │
         │                         ▼
         │              ┌─────────────────────┐
         │              │ t_membership_plan   │
         │              │ (会员套餐表)         │
         │              └─────────────────────┘
         │
         ├─────────────<┌─────────────────────┐
         │              │ t_order             │
         │              │ (订单表)             │
         │              └─────────────────────┘
         │
         └─────────────<┌─────────────────────┐
                        │ t_token_usage       │
                        │ (Token消耗记录)      │
                        └─────────────────────┘
```

---

## 四、后端API设计

### 4.1 现有接口复用

| 模块 | 接口 | 说明 | 备注 |
|------|------|------|------|
| 用户管理 | GET /api/common/users | 分页查询用户列表 | 已存在 |
| 用户管理 | GET /api/common/user/info/{userId} | 获取用户详情 | 已存在 |
| 用户管理 | PUT /api/common/user/update | 更新用户信息 | 已存在 |

### 4.2 新增管理接口

#### 4.2.1 仪表盘统计接口

```yaml
# 获取仪表盘统计数据
GET /api/admin/dashboard/stats
Authorization: satoken
Response:
  {
    "code": 0,
    "data": {
      "userCount": 1000,           # 用户总数
      "todayNewUsers": 50,         # 今日新增
      "activeUsers": 300,          # 活跃用户
      "vipUsers": 100,             # 会员用户
      "noteCount": 5000,           # 笔记总数
      "kbCount": 200,              # 知识库总数
      "fileCount": 3000,           # 文件总数
      "todayOrders": 20,           # 今日订单
      "todayIncome": 999.00,       # 今日收入
      "monthIncome": 29999.00,     # 本月收入
      "tokenUsedToday": 100000,    # 今日Token消耗
      "tokenUsedMonth": 3000000    # 本月Token消耗
    }
  }
```

#### 4.2.2 套餐管理接口

```yaml
# 获取套餐列表
GET /api/admin/plans
Authorization: satoken
Response:
  {
    "code": 0,
    "data": [
      {
        "id": 1,
        "name": "免费版",
        "code": "FREE",
        "price": 0,
        "tokenQuota": 10000,
        "status": 1
      }
    ]
  }

# 创建套餐
POST /api/admin/plans
Authorization: satoken
Request:
  {
    "name": "专业版",
    "code": "PRO",
    "description": "适合专业用户",
    "price": 49.9,
    "durationDays": 30,
    "tokenQuota": 500000,
    "kbQuota": 10,
    "fileQuota": 100
  }

# 更新套餐
PUT /api/admin/plans/{planId}
Authorization: satoken

# 删除套餐（禁用）
DELETE /api/admin/plans/{planId}
Authorization: satoken
```

#### 4.2.3 订单管理接口

```yaml
# 分页查询订单列表
GET /api/admin/orders
Authorization: satoken
Parameters:
  - page: 1
  - size: 10
  - status: 0/1/2/3    # 订单状态筛选
  - userId: 123        # 用户ID筛选
  - startDate: 2024-01-01
  - endDate: 2024-12-31
Response:
  {
    "code": 0,
    "data": {
      "total": 100,
      "data": [
        {
          "id": 1,
          "orderNo": "ORD20241217001",
          "userId": 123,
          "userName": "张三",
          "planName": "专业版",
          "amount": 49.9,
          "status": 1,
          "deliveryStatus": 2,
          "createTime": "2024-12-17 10:00:00"
        }
      ]
    }
  }

# 获取订单详情
GET /api/admin/orders/{orderId}
Authorization: satoken

# 手动下发权益
POST /api/admin/orders/{orderId}/deliver
Authorization: satoken
Response:
  {
    "code": 0,
    "message": "权益下发成功"
  }

# 取消订单
POST /api/admin/orders/{orderId}/cancel
Authorization: satoken

# 订单退款
POST /api/admin/orders/{orderId}/refund
Authorization: satoken
Request:
  {
    "reason": "用户申请退款"
  }
```

#### 4.2.4 Token消耗管理接口

```yaml
# 获取Token消耗统计
GET /api/admin/tokens/stats
Authorization: satoken
Parameters:
  - startDate: 2024-01-01
  - endDate: 2024-12-31
  - userId: 123       # 可选，按用户筛选
Response:
  {
    "code": 0,
    "data": {
      "totalTokens": 10000000,
      "totalCost": 100.00,
      "dailyStats": [
        {"date": "2024-12-17", "tokens": 50000, "cost": 5.00}
      ],
      "modelStats": [
        {"model": "qwen-max", "tokens": 5000000, "cost": 50.00}
      ]
    }
  }

# 获取Token消耗记录
GET /api/admin/tokens/logs
Authorization: satoken
Parameters:
  - page: 1
  - size: 20
  - userId: 123
  - usageType: 1      # 1-对话 2-文档解析 3-向量化
  - startDate: 2024-01-01
  - endDate: 2024-12-31

# 调整用户Token配额
PUT /api/admin/users/{userId}/token-quota
Authorization: satoken
Request:
  {
    "tokenQuota": 100000,
    "reason": "活动赠送"
  }
```

#### 4.2.5 用户管理扩展接口

```yaml
# 获取用户会员信息
GET /api/admin/users/{userId}/membership
Authorization: satoken
Response:
  {
    "code": 0,
    "data": {
      "userId": 123,
      "currentPlan": "PRO",
      "planName": "专业版",
      "startTime": "2024-12-01",
      "endTime": "2024-12-31",
      "tokenUsed": 50000,
      "tokenQuota": 500000,
      "status": 1
    }
  }

# 手动赠送会员
POST /api/admin/users/{userId}/grant-membership
Authorization: satoken
Request:
  {
    "planId": 3,
    "days": 30,
    "tokenQuota": 100000,
    "remark": "活动赠送"
  }

# 禁用/启用用户
PUT /api/admin/users/{userId}/status
Authorization: satoken
Request:
  {
    "status": 0,       # 0-禁用 1-启用
    "reason": "违规操作"
  }
```

#### 4.2.6 内容管理接口

```yaml
# 笔记列表（管理员视角）
GET /api/admin/notes
Authorization: satoken
Parameters:
  - page: 1
  - size: 10
  - userId: 123
  - keyword: 搜索关键词
  - status: 1

# 删除笔记（管理员）
DELETE /api/admin/notes/{noteId}
Authorization: satoken

# 知识库列表（管理员视角）
GET /api/admin/knowledge-bases
Authorization: satoken
Parameters:
  - page: 1
  - size: 10
  - isPublic: true

# 删除知识库（管理员）
DELETE /api/admin/knowledge-bases/{kbId}
Authorization: satoken

# 文件列表（管理员视角）
GET /api/admin/files
Authorization: satoken
Parameters:
  - page: 1
  - size: 10
  - userId: 123
  - fileType: DOCUMENT

# 删除文件（管理员）
DELETE /api/admin/files/{fileId}
Authorization: satoken
```

---

## 五、前端设计

### 5.1 技术选型

| 项目 | 技术选择 | 说明 |
|------|----------|------|
| 框架 | Vue 3 + Vite | 与现有项目保持一致 |
| UI库 | Element Plus | 适合后台管理系统 |
| 状态管理 | Pinia | 轻量级状态管理 |
| 路由 | Vue Router 4 | 官方路由方案 |
| 图表 | ECharts | 数据可视化 |
| 请求 | Axios | HTTP请求库 |

### 5.2 页面结构

```
admin-vue/
├── src/
│   ├── api/                    # API接口
│   │   ├── admin.js           # 管理接口
│   │   ├── order.js           # 订单接口
│   │   ├── plan.js            # 套餐接口
│   │   └── token.js           # Token接口
│   ├── views/
│   │   ├── Dashboard/         # 仪表盘
│   │   │   └── index.vue
│   │   ├── User/              # 用户管理
│   │   │   ├── List.vue
│   │   │   └── Detail.vue
│   │   ├── Content/           # 内容管理
│   │   │   ├── Notes.vue
│   │   │   ├── KnowledgeBases.vue
│   │   │   └── Files.vue
│   │   ├── Membership/        # 会员管理
│   │   │   ├── Plans.vue      # 套餐配置
│   │   │   ├── Orders.vue     # 订单列表
│   │   │   └── Delivery.vue   # 权益下发
│   │   ├── Token/             # Token管理
│   │   │   ├── Stats.vue      # 消耗统计
│   │   │   └── Logs.vue       # 消耗日志
│   │   └── Settings/          # 系统设置
│   │       └── index.vue
│   ├── components/            # 公共组件
│   ├── router/                # 路由配置
│   ├── store/                 # 状态管理
│   └── utils/                 # 工具函数
```

### 5.3 主要页面设计

#### 5.3.1 仪表盘页面

```
┌────────────────────────────────────────────────────────────┐
│  📊 仪表盘                                                  │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│  用户总数    │  今日新增    │  会员用户    │  活跃用户       │
│  1,234      │  +56        │  234        │  567           │
├─────────────┴─────────────┴─────────────┴─────────────────┤
│  今日订单: 20    今日收入: ¥999.00    本月收入: ¥29,999.00  │
├───────────────────────────────────────────────────────────┤
│  Token消耗趋势图                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                    📈 折线图                         │  │
│  └─────────────────────────────────────────────────────┘  │
├───────────────────────────────────────────────────────────┤
│  最近订单列表                          最近注册用户         │
│  ┌──────────────────────────────┐    ┌─────────────────┐  │
│  │ 订单号 | 用户 | 金额 | 状态   │    │ 头像 用户名 时间 │  │
│  └──────────────────────────────┘    └─────────────────┘  │
└───────────────────────────────────────────────────────────┘
```

#### 5.3.2 订单管理页面

```
┌────────────────────────────────────────────────────────────┐
│  📋 订单管理                                                │
├────────────────────────────────────────────────────────────┤
│  筛选条件:                                                  │
│  [订单状态 ▼] [用户搜索     ] [日期范围         ] [搜索]    │
│                                                            │
│  状态标签: [全部] [待支付] [已支付] [已取消] [已退款]        │
├────────────────────────────────────────────────────────────┤
│  订单号      | 用户   | 套餐    | 金额   | 状态   | 操作     │
│  ─────────────────────────────────────────────────────────  │
│  ORD001     | 张三   | 专业版  | ¥49.9  | 待支付 | [详情]   │
│  ORD002     | 李四   | 基础版  | ¥19.9  | 已支付 | [详情]   │
├────────────────────────────────────────────────────────────┤
│  分页: < 1 2 3 4 5 >                         共 100 条     │
└────────────────────────────────────────────────────────────┘
```

#### 5.3.3 套餐配置页面

```
┌────────────────────────────────────────────────────────────┐
│  💎 套餐配置                                   [+ 新增套餐] │
├────────────────────────────────────────────────────────────┤
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  │
│  │    免费版     │  │    基础版     │  │    专业版     │  │
│  │   ¥0/永久    │  │  ¥19.9/月    │  │  ¥49.9/月    │  │
│  │ ───────────── │  │ ───────────── │  │ ───────────── │  │
│  │ Token: 10000  │  │ Token: 100000 │  │ Token: 500000 │  │
│  │ 知识库: 1个   │  │ 知识库: 3个   │  │ 知识库: 10个  │  │
│  │ 文件: 5个     │  │ 文件: 20个    │  │ 文件: 100个   │  │
│  │ ───────────── │  │ ───────────── │  │ ───────────── │  │
│  │ [编辑] [禁用] │  │ [编辑] [禁用] │  │ [编辑] [禁用] │  │
│  └───────────────┘  └───────────────┘  └───────────────┘  │
└────────────────────────────────────────────────────────────┘
```

#### 5.3.4 权益下发状态页面

```
┌────────────────────────────────────────────────────────────┐
│  🎁 权益下发状态                                            │
├────────────────────────────────────────────────────────────┤
│  筛选: [下发状态 ▼] [日期范围       ] [搜索]                 │
│                                                            │
│  状态: [全部] [未下发] [下发中] [已下发] [下发失败]          │
├────────────────────────────────────────────────────────────┤
│  订单号  | 用户 | 套餐   | 下发状态 | 下发时间    | 操作     │
│  ─────────────────────────────────────────────────────────  │
│  ORD001 | 张三 | 专业版 | ✅ 已下发 | 12-17 10:05 | [查看] │
│  ORD002 | 李四 | 基础版 | ❌ 失败   | 12-17 10:10 | [重试] │
│  ORD003 | 王五 | 专业版 | ⏳ 下发中 | -           | [查看] │
├────────────────────────────────────────────────────────────┤
│  下发成功率: 98.5%      失败订单: 3      待处理: 5          │
└────────────────────────────────────────────────────────────┘
```

#### 5.3.5 Token消耗统计页面

```
┌────────────────────────────────────────────────────────────┐
│  📊 Token消耗统计                                           │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│  今日消耗    │  本月消耗    │  总消耗      │  预估成本       │
│  100,000    │  3,000,000  │  50,000,000 │  ¥5,000.00     │
├─────────────┴─────────────┴─────────────┴─────────────────┤
│  消耗趋势图 [日] [周] [月]                                  │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                    📈 面积图                         │  │
│  └─────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────┤
│  消耗类型分布          用户消耗TOP10                        │
│  ┌─────────────────┐  ┌──────────────────────────────┐    │
│  │    🍩 饼图      │  │ 排名 | 用户 | Token | 占比   │    │
│  │ 对话: 60%       │  │  1   | 张三 | 50000 | 5%    │    │
│  │ 解析: 30%       │  │  2   | 李四 | 40000 | 4%    │    │
│  │ 向量: 10%       │  │  3   | 王五 | 30000 | 3%    │    │
│  └─────────────────┘  └──────────────────────────────┘    │
└────────────────────────────────────────────────────────────┘
```

---

## 六、权限设计

### 6.1 角色定义

| 角色 | role_type | 说明 | 权限范围 |
|------|-----------|------|----------|
| 普通用户 | 1 | App/Web用户 | 仅访问自己的数据 |
| 管理员 | 2 | 后台管理员 | 管理后台完整权限 |
| 超级管理员 | 3 | 系统管理员 | 所有权限+系统配置 |

### 6.2 权限控制实现

```java
// 管理员权限注解示例
@SaCheckRole("admin")
@GetMapping("/api/admin/users")
public Result<PageResult<User>> listUsers(...) { ... }

// 超级管理员权限
@SaCheckRole("super_admin")
@PostMapping("/api/admin/plans")
public Result<?> createPlan(...) { ... }
```

---

## 七、开发计划

### 7.1 第一阶段：基础框架（1周）

1. 创建管理后台前端项目
2. 配置路由和权限框架
3. 实现登录认证
4. 完成基础布局组件

### 7.2 第二阶段：用户与内容管理（1周）

1. 用户列表和详情页面
2. 笔记管理页面
3. 知识库管理页面
4. 文件管理页面

### 7.3 第三阶段：会员与订单管理（1周）

1. 套餐配置功能
2. 订单管理功能
3. 权益下发功能
4. 相关后端接口开发

### 7.4 第四阶段：Token管理与仪表盘（1周）

1. Token消耗统计
2. 消耗日志查询
3. 仪表盘数据展示
4. 图表可视化

---

## 八、部署说明

### 8.1 目录结构

```
note-manager/
├── initialization-note-side/   # 后端（已有）
├── note-vue/                   # 移动/Web前端（已有）
├── admin-vue/                  # 管理后台前端（新增）
└── docker/                     # Docker配置（已有）
```

### 8.2 Nginx配置示例

```nginx
# 管理后台前端
location /admin {
    alias /var/www/admin;
    try_files $uri $uri/ /admin/index.html;
}

# 管理后台API
location /api/admin {
    proxy_pass http://localhost:8889;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 九、附录

### 9.1 状态码说明

| 状态码 | 说明 |
|--------|------|
| 0 | 成功 |
| -1 | 失败 |
| 401 | 未授权 |
| 403 | 无权限 |

### 9.2 订单状态流转

```
待支付(0) ──支付成功──> 已支付(1) ──下发权益──> 权益已下发
    │                      │
    │                      └──申请退款──> 已退款(3)
    │
    └──超时/取消──> 已取消(2)
```

### 9.3 权益下发状态

| 状态码 | 说明 |
|--------|------|
| 0 | 未下发 |
| 1 | 下发中 |
| 2 | 已下发 |
| 3 | 下发失败 |

---

*文档版本: v1.0*  
*创建日期: 2024-12-17*  
*适用范围: 智能知识库系统后台管理模块*
