# CZB AI 项目后端

这是 CZB AI 项目的后端仓库，当前 GitHub 仓库不包含两个前端目录。项目后端基于 Java 17、Spring Boot 3.5、PostgreSQL/pgvector、Redis 与 LangChain4j。

## 内容

- `initialization-note-side/`：Spring Boot REST API 服务。
- `initialization-note-side/deploy/`：PostgreSQL 本地初始化和检查 SQL。
- `docs/接口目录.md`：根据当前控制器整理的接口清单，共 170 个路由。
- `docs/数据字段字典.md`：请求、响应及直接返回实体的字段说明。
- `前端重构交接与接口说明.md`：本机联调约定、SSE、上传和已知接口差异。原项目的前端源码未包含在本仓库中。

## 本地启动

1. 准备 JDK 17、Maven、PostgreSQL 16（安装 `pgvector`）和 Redis 6/7。
2. 在 `initialization-note-side/src/main/resources/` 下复制 `application-example.yml` 为 `application.yml`，填写自己的数据库、Redis、AI、对象存储和支付配置。真实配置文件已由 Git 忽略，不要提交密钥。
3. 对全新数据库执行 `initialization-note-side/deploy/init-local.sql`。已有业务库请先备份并核对迁移要求，初始化脚本不会自动修复旧表。
4. 在 IDEA 中将项目 SDK 和 Maven JDK 设为 17，运行 `com.ldd.initialization.InitializationApplication` 主类。

服务默认监听 `http://localhost:8889`。数据库连接地址和 Redis 地址以 `application.yml` 为准；PostgreSQL 默认端口是 5432，若本机端口映射不同请修改 JDBC URL。

## 外部服务

- 普通用户、笔记和聊天历史需要 PostgreSQL 与 Redis。
- AI 聊天和知识库向量化需要有效的聊天模型及 Embedding API Key。向量维数与 PostgreSQL 中的 `documents` 表定义必须一致；当前服务使用 1024 维。
- 文件上传需要正确配置本地存储或 OSS。不要把访问密钥写入代码、文档或提交记录。
- 支付接口按当前沙箱配置开发；支付宝异步通知需要公网可访问的回调地址。

## 文档与接口约定

接口目录记录了当前 Java 控制器定义，不代表 170 个接口都经过自动化集成测试。普通响应使用 `{code,message,data}`；分页数据常见结构为 `data.data` 和 `data.total`。AI 接口包含 SSE 文本流，具体读取方式与已知前后端差异见交接说明。

## 构建

```powershell
mvn -B -f initialization-note-side/pom.xml -DskipTests package
```

该命令编译并打包后端，但跳过需要外部服务的集成测试。GitHub Actions 会在推送和 Pull Request 时执行同一编译检查。
