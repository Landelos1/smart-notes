# 数据库结构文档（PostgreSQL + pgvector）

## 概览
- 数据库：PostgreSQL
- 向量扩展：`pgvector`（`CREATE EXTENSION vector;`）
- 初始化脚本：`sql/complete_database_init.sql`

## 表：`public.t_user`（用户）
- 主键：`id bigserial`
- 列：
  - `phone varchar(11) unique not null`
  - `password varchar(100)`
  - `role_type smallint not null`（1-求职者，2-招聘者，3-管理员）
  - `username varchar(50)`
  - `avatar_url varchar(255)`
  - `real_name varchar(50)`
  - `id_card varchar(18)`
  - `gender smallint`
  - `birth_date date`
  - `work_years smallint`
  - `education smallint`
  - `status smallint default 1`
  - `last_login_time timestamp`
  - `last_login_ip varchar(50)`
  - `create_time timestamp default CURRENT_TIMESTAMP`
  - `update_time timestamp default CURRENT_TIMESTAMP`

## 表：`public.t_notebook`（笔记本）
- 主键：`id bigserial`
- 列：
  - `name varchar(128) not null`
  - `description text`
  - `color varchar(7)`
  - `user_id bigint not null`
  - `create_time timestamp default CURRENT_TIMESTAMP`
  - `update_time timestamp default CURRENT_TIMESTAMP`
  - `sort_order integer`（排序）

## 表：`public.t_note`（笔记）
- 主键：`id bigserial`
- 列：
  - `title varchar(255) not null`
  - `content_md text`
  - `content_html text`
  - `notebook_id bigint`
  - `user_id bigint not null`
  - `is_pinned boolean default false`
  - `status smallint default 1`
  - `create_time timestamp default CURRENT_TIMESTAMP`
  - `update_time timestamp default CURRENT_TIMESTAMP`
  - `view_count integer`
  - `word_count integer`

## 表：`public.t_note_tag`（笔记标签）
- 主键：`id bigserial`
- 列：
  - `note_id bigint not null`
  - `tag_name varchar(50) not null`
  - `create_time timestamp default CURRENT_TIMESTAMP`
- 约束：
  - 唯一：`unique (note_id, tag_name)`

## 表：`public.t_shared_knowledge_base`（共享知识库）
- 主键：`id bigserial`
- 列：
  - `name varchar(128) not null`
  - `description text`
  - `cover_url varchar(255)`
  - `password varchar(100)`
  - `creator_id bigint not null`
  - `is_public boolean default true`
  - `member_count integer default 1 check (member_count >= 0)`
  - `file_count integer default 0 check (file_count >= 0)`
  - `status smallint default 1 check (status = ANY (ARRAY[0,1]))`
  - `create_time timestamp default CURRENT_TIMESTAMP`
  - `update_time timestamp default CURRENT_TIMESTAMP`

## 表：`public.t_knowledge_base_member`（知识库成员）
- 主键：`id bigserial`
- 列：
  - `knowledge_base_id bigint not null`
  - `user_id bigint not null`
  - `role smallint default 2 check (role = ANY (ARRAY[1,2]))`（1-创建者 2-成员）
  - `join_time timestamp default CURRENT_TIMESTAMP`
- 约束：
  - 唯一：`unique (knowledge_base_id, user_id)`

## 表：`public.t_knowledge_base_file`（知识库文件）
- 主键：`id bigserial`
- 列：
  - `knowledge_base_id bigint not null`
  - `file_id bigint not null`
  - `uploader_id bigint not null`
  - `source_type smallint default 1 check (source_type = ANY (ARRAY[1,2]))`（1-本地上传 2-个人知识库复制）
  - `upload_time timestamp default CURRENT_TIMESTAMP`
- 约束：
  - 唯一：`unique (knowledge_base_id, file_id)`

## 表：`public.t_file_info`（文件上传信息）
- 主键：`id bigserial`
- 列：
  - `original_filename varchar(255)`（原始文件名）
  - `file_name varchar(255)`（存储文件名）
  - `file_extension varchar(50)`（扩展名）
  - `file_size bigint`
  - `file_path text`
  - `file_url text`
  - `user_id bigint`
  - `file_md5 varchar(32)`
  - `create_time timestamp default CURRENT_TIMESTAMP`
  - `update_time timestamp default CURRENT_TIMESTAMP`
  - `file_type varchar(50)`
  - `mime_type varchar(100)`
  - `file_hash varchar(64)`
  - `source_type smallint default 1`（来源类型：1-本地 2-个人知识库复制 3-笔记转换PDF）
  - `source_note_id bigint`
  - `source_note_title varchar(255)`

## 表：`public.documents`（向量存储）
- 主键：`embedding_id uuid default gen_random_uuid()`
- 列：
  - `user_id varchar(100)`
  - `text text not null`
  - `metadata jsonb`
  - `embedding vector`（pgvector 向量列）
  - `created_at timestamp(6) default CURRENT_TIMESTAMP`
  - `file_id bigint`（关联 `t_file_info.id`，用于删除文件时清理向量）
  - `knowledge_base_id bigint`（个人为用户ID，共享为知识库ID）
  - `knowledge_base_type smallint default 1`（1-个人，2-共享）
- 索引：
  - `documents_created_at_idx`（`created_at`）
  - `documents_embedding_id_idx`（`embedding_id`）
  - `documents_metadata_idx`（GIN on `metadata`）
  - `documents_user_id_created_at_idx`（`user_id, created_at`）
  - `documents_user_id_idx`（`user_id`）
  - `idx_documents_file_id`（`file_id`）
  - `idx_documents_file_user`（`file_id, user_id`）
  - `idx_documents_kb_type`（`knowledge_base_type`）
  - `idx_documents_knowledge_base`（`knowledge_base_id, knowledge_base_type`）

## 关系与约束说明
- 用户与笔记本：`t_notebook.user_id` 指向用户
- 笔记与笔记本/用户：`t_note.notebook_id`、`t_note.user_id`
- 标签与笔记：`t_note_tag.note_id` 唯一约束配合 `tag_name`
- 知识库与成员/文件：`t_knowledge_base_member.knowledge_base_id`、`t_knowledge_base_file.knowledge_base_id`
- 文件与用户：`t_file_info.user_id`
- 向量存储与文件/知识库/用户：`documents.file_id`、`documents.knowledge_base_id`、`documents.user_id`

## 运行与诊断
- 容器运行：`docker/docker-compose.yml`（包含 `postgres`、`redis`、`backend`）
- Spring 数据源配置：`src/main/resources/application.yml`、`docker/config/application-docker.yml`
- 诊断接口：
  - `GET /api/common/diagnose/vector-db`：检查 `pgvector`、`documents` 表、分组统计
  - `GET /api/common/test/connection-pool`：连接池状态
