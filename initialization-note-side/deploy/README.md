# 本地数据库初始化

`init-local.sql` 按当前后端实体和查询整理，创建 32 张业务表，并补齐 `documents` 向量表字段。
包括用户、笔记本、笔记、标签、文件、知识库、聊天、AI 设置、模板、团队、回收站和会员订单结构。

## 适用范围

- PostgreSQL 16，已安装 pgvector。
- 空数据库，或仅有 LangChain4j 自动创建的 `documents` 表。
- 已有业务表的旧数据库需要专门迁移，本脚本的 `CREATE TABLE IF NOT EXISTS` 不会修复旧业务表的缺失字段。
- 向量维度对应后端 `PgVectorConfig` 的 1024 维。已有不同维度数据时脚本报错并回滚。
- 原有 `documents` 数据会保留，`metadata` 从 json 转为 jsonb；已有表还会补上业务字段、UUID 默认值及查询索引。
- 不导入原项目的私人账号、文件、向量或 API 密钥，不预设管理员账号和会员套餐价格。

脚本在当前连接的数据库中创建表，不负责创建数据库或配置连接。目标 PostgreSQL 需已安装 pgvector；连接地址、端口、库名和账号以本机 application.yml 为准。

## 在 IDEA 执行

1. 打开 `deploy/init-local.sql`。
2. 在右侧 Database 窗口选择与本机 application.yml 相同的 PostgreSQL 数据源，确认当前连接的是准备初始化的目标数据库。
3. 给 SQL 文件选择这个数据源，并选择 `public` schema。不要选择 MySQL 数据源。
4. `Ctrl+A` 全选文件内容，点击执行按钮或按 `Ctrl+Enter`；如果出现语句选择窗口，选择执行全部语句。必须包含开头的 `BEGIN` 和末尾的 `COMMIT`。
5. 等待执行结束。末尾的查询应显示 33 张表。如果中途报错，先在同一控制台执行 `ROLLBACK;`，处理错误后重新执行整个文件。
6. 刷新 Database 数据源，展开 `postgres → schemas → public → tables`。

## 也可在 PowerShell 导入

在仓库根目录打开 PowerShell，确保 Docker Desktop 和目标数据库容器已启动。下面命令以容器名 `note-postgres` 为例；如果你的容器名或 Docker context 不同，请替换后再执行：

```powershell
docker --context desktop-linux cp ".\initialization-note-side\deploy\init-local.sql" note-postgres:/tmp/note-init-local.sql
docker --context desktop-linux exec note-postgres psql -U postgres -d postgres -v ON_ERROR_STOP=1 -f /tmp/note-init-local.sql
```

第一条执行成功后再执行第二条。第二条应以 `COMMIT` 和 33 行表名结束。
本机的 psql 包装命令在容器里运行，因此不能直接用 `psql -f E:\...` 读取 Windows 文件；上面先将 SQL 复制进容器。

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

第一条结果应为 33；尚未注册时，后三条为空属于正常情况。

重启 IDEA 中的后端；完整应用的前端需另行运行。浏览器刷新后：

1. 注册普通用户，再登录；手机号唯一，不需要手工插入用户或密码。
2. 新建一个笔记本，在里面新建笔记，编辑内容并保存。
3. 刷新页面确认笔记仍然存在，检查上面三张业务表是否出现对应数据。
4. 测试标签和删除/回收站；遇到错误时查看 IDEA 后端控制台的异常及 SQL。

建表完成不等于所有外部功能均已可用。文件上传依赖有效的 OSS 配置，AI 功能依赖模型服务与密钥；会员套餐表初始为空，支付还依赖商户配置。建议先完成用户和笔记流程，再逐项配置这些功能。

## 已完成的验证

在独立临时数据库验证，未向实际 `postgres` 库写入业务表或测试账号：

- 空库导入、重复导入成功，共 33 张表。
- 32 个 Java 实体的 320 个持久化字段与数据库列及基础类型匹配。
- 模拟已有的四字段 `documents` 表，原 UUID、文本、1024 维向量和 JSON 内容在升级后保留。
- 用户/笔记关联、笔记本级联删除、回收站快照保留、重复点赞唯一约束通过。
- 聊天引用 JSONB、团队事件版本查询、向量默认 UUID 插入通过。

`check-local.sql` 是供独立测试库使用的数据库验证脚本，数据会回滚，但自增序列会前进；日常部署只需执行 `init-local.sql`。以上是结构与 SQL 验证，浏览器端完整业务流程仍需按上面的步骤验证。
