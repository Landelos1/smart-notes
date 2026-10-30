# 数据库脚本

项目统一使用后端目录中的数据库脚本：

- [空库初始化](../../initialization-note-side/deploy/init-local.sql)
- [已有数据库迁移](../../initialization-note-side/deploy/migrations/20261030_account_role_uniqueness.sql)
- [接口目录](../../docs/接口目录.md)

旧的 `public.sql` 是历史数据库导出，包含旧账号、密码摘要、文档正文及文件记录，且表结构已落后于当前业务，已从当前工作树移除。它不适合作为部署初始化文件。不要将本机数据库导出作为项目源码提交。
