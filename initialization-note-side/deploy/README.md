# 本地数据库初始化

`init-local.sql` 按当前后端实体和查询整理，创建 34 张业务表，并补齐 `documents` 向量表字段，共 35 张表。
包括用户、笔记本、笔记、标签、文件、知识库、聊天、AI 设置、模板、团队、回收站和会员订单结构。

## 适用范围

- PostgreSQL 16，已安装 pgvector。
- 空数据库，或仅有 LangChain4j 自动创建的 `documents` 表。
- 已有业务表的旧数据库需要专门迁移，本脚本的 `CREATE TABLE IF NOT EXISTS` 不会修复旧业务表的缺失字段。
- 向量维度对应后端 `PgVectorConfig` 的 1024 维。已有不同维度数据时脚本报错并回滚。
- 原有 `documents` 数据会保留，`metadata` 从 json 转为 jsonb；已有表还会补上业务字段、UUID 默认值及查询索引。
- 不导入原项目的私人账号、文件、向量或 API 密钥，不预设管理员账号和会员套餐价格。

以实际 `application.yml` 的 datasource 配置为准。原生 PostgreSQL 默认端口为 `5432`，已有 Docker 容器也可能映射为 `15432`。本项目使用 PostgreSQL，不需要选择 MySQL 数据源。

## 在 IDEA 执行

1. 打开 `deploy/init-local.sql`。
2. 在右侧 Database 窗口确认数据源连接的是目标 PostgreSQL，主机、端口、库名、用户名和密码与后端配置一致。
3. 给 SQL 文件选择这个数据源，并选择 `public` schema。不要选择 MySQL 数据源。
4. `Ctrl+A` 全选文件内容，点击执行按钮或按 `Ctrl+Enter`；如果出现语句选择窗口，选择执行全部语句。必须包含开头的 `BEGIN` 和末尾的 `COMMIT`。
5. 等待执行结束。末尾的查询应显示 35 张表。如果中途报错，先在同一控制台执行 `ROLLBACK;`，处理错误后重新执行整个文件。
6. 刷新 Database 数据源，展开 `目标数据库 → schemas → public → tables`。

## 也可在 PowerShell 导入

使用本机原生客户端时，在仓库根目录执行；用户名、端口、数据库名按实际环境调整：

```powershell
psql -h 127.0.0.1 -p 5432 -U postgres -d smart_notes -W -v ON_ERROR_STOP=1 -f initialization-note-side/deploy/init-local.sql
```

如果客户端是容器包装命令，它不能直接读取 Windows 路径，需要先复制 SQL。下面以已运行的 `note-postgres` 容器和 `postgres` 数据库为例，在仓库根目录依次执行：

```powershell
docker --context desktop-linux cp ./initialization-note-side/deploy/init-local.sql note-postgres:/tmp/note-init-local.sql
docker --context desktop-linux exec note-postgres psql -U postgres -d postgres -v ON_ERROR_STOP=1 -f /tmp/note-init-local.sql
```

第一条执行成功后再执行第二条。第二条应以 `COMMIT` 和 35 行表名结束。
Docker context、容器名和数据库名都以自己的部署为准，使用原生服务无需执行 Docker 命令。

## 导入后的检查

在 IDEA SQL 控制台执行：

```sql
SELECT count(*) AS table_count
FROM pg_tables WHERE schemaname = 'public';

SELECT id, phone, username, role_type, status
FROM public.t_user ORDER BY id;

SELECT id, name, user_id FROM public.t_notebook ORDER BY id;
SELECT id, title, notebook_id, user_id FROM public.t_note ORDER BY id;
```

第一条结果应为 35；尚未注册时，后三条为空属于正常情况。

重启 IDEA 中的后端，保留前端 dev 服务。浏览器刷新后：

1. 注册普通用户，再登录；同一手机号在同一角色内唯一，不需要手工插入用户或密码。普通用户和管理员可共用手机号。
2. 新建一个笔记本，在里面新建笔记，编辑内容并保存。
3. 刷新页面确认笔记仍然存在，检查上面三张业务表是否出现对应数据。
4. 测试标签和删除/回收站；遇到错误时查看 IDEA 后端控制台的异常及 SQL。

建表完成不等于所有外部功能均已可用。文件上传需要有效的本地存储或 OSS 配置，AI 功能依赖模型服务与密钥；会员套餐表初始为空，支付还依赖商户配置。先完成用户和笔记流程，再逐项配置。首次管理员启用步骤见[项目 README](../../README.md#6-注册与首个管理员启用)。

## 已有数据库迁移

已有数据库先备份。旧库手机号全局唯一时执行 [20261030_account_role_uniqueness.sql](migrations/20261030_account_role_uniqueness.sql)，将约束调整为 `(phone, role_type)`，不同角色可共用手机号，同角色重复仍被拒绝。新增复习计划频率配置时执行 [20261115_review_plan_recurrence.sql](migrations/20261115_review_plan_recurrence.sql)，它会为周计划补全复习日并为每天、自定义计划补齐间隔字段。两份迁移均支持重复执行；若账号迁移遇到同角色重复数据，先核查数据再迁移，不要直接删除业务账号。

## 已完成的验证

在独立临时数据库验证，未向实际 `postgres` 库写入业务表或测试账号：

- 空库导入、重复导入成功，共 35 张表。
- 32 个 Java 实体的 320 个持久化字段与数据库列及基础类型匹配。
- 模拟已有的四字段 `documents` 表，原 UUID、文本、1024 维向量和 JSON 内容在升级后保留。
- 用户/笔记关联、笔记本级联删除、回收站快照保留、重复点赞唯一约束通过。
- 聊天引用 JSONB、团队事件版本查询、向量默认 UUID 插入通过。

`check-local.sql` 是供独立测试库使用的数据库验证脚本，数据会回滚，但自增序列会前进；日常部署只需执行 `init-local.sql`。以上是结构与 SQL 验证，业务功能的实测范围见项目根目录 `docs/` 中的联调报告。
