-- 数据库迁移脚本：从现有结构升级到 v2（幂等执行）
-- 目标：不破坏现有接口；仅新增列/表/索引/约束/触发器；必要时清理孤儿数据

BEGIN;

-- 扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

SET search_path TO public;

/* 新增列：笔记来源模板（与前端模板中心集成），不影响现有接口 */
ALTER TABLE public.t_note
  ADD COLUMN IF NOT EXISTS origin_template_id bigint;
CREATE INDEX IF NOT EXISTS idx_note_origin_template ON public.t_note(origin_template_id);

/* 新增实体：模板中心 */
CREATE TABLE IF NOT EXISTS public.t_template
(
    id          bigserial PRIMARY KEY,
    title       varchar(255) NOT NULL,
    description text,
    category    varchar(100),
    thumbnail   text,
    downloads   integer      DEFAULT 0,
    rating      numeric(3,2) DEFAULT 0,
    views       integer      DEFAULT 0,
    author_id   bigint,
    updated_at  timestamp    DEFAULT CURRENT_TIMESTAMP,
    create_time timestamp    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_template_category ON public.t_template(category);
CREATE INDEX IF NOT EXISTS idx_template_rating ON public.t_template(rating);
CREATE INDEX IF NOT EXISTS idx_template_downloads ON public.t_template(downloads);
CREATE INDEX IF NOT EXISTS idx_template_updated_at ON public.t_template(updated_at);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_template_author') THEN
    ALTER TABLE public.t_template
      ADD CONSTRAINT fk_template_author
      FOREIGN KEY (author_id) REFERENCES public.t_user(id)
      ON DELETE SET NULL ON UPDATE NO ACTION;
  END IF;
END $$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_template_rating_range') THEN
    ALTER TABLE public.t_template
      ADD CONSTRAINT chk_template_rating_range CHECK (rating >= 0 AND rating <= 5);
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.t_template_tag
(
    id          bigserial PRIMARY KEY,
    template_id bigint      NOT NULL,
    tag_name    varchar(50) NOT NULL,
    create_time timestamp   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_template_tag UNIQUE (template_id, tag_name)
);
CREATE INDEX IF NOT EXISTS idx_template_tag_template ON public.t_template_tag(template_id);
CREATE INDEX IF NOT EXISTS idx_template_tag_name ON public.t_template_tag(tag_name);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_template_tag_template') THEN
    ALTER TABLE public.t_template_tag
      ADD CONSTRAINT fk_template_tag_template
      FOREIGN KEY (template_id) REFERENCES public.t_template(id)
      ON DELETE CASCADE ON UPDATE NO ACTION;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.t_template_comment
(
    id          bigserial PRIMARY KEY,
    template_id bigint      NOT NULL,
    user_id     bigint,
    content     text        NOT NULL,
    rating      smallint,
    created_at  timestamp   DEFAULT CURRENT_TIMESTAMP,
    likes       integer     DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_template_comment_template ON public.t_template_comment(template_id);
CREATE INDEX IF NOT EXISTS idx_template_comment_user ON public.t_template_comment(user_id);
CREATE INDEX IF NOT EXISTS idx_template_comment_template_time
  ON public.t_template_comment(template_id, created_at DESC)
  INCLUDE (rating, likes);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_template_comment_rating_range') THEN
    ALTER TABLE public.t_template_comment
      ADD CONSTRAINT chk_template_comment_rating_range CHECK (rating >= 1 AND rating <= 5);
  END IF;
END $$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_template_comment_template') THEN
    ALTER TABLE public.t_template_comment
      ADD CONSTRAINT fk_template_comment_template
      FOREIGN KEY (template_id) REFERENCES public.t_template(id)
      ON DELETE CASCADE ON UPDATE NO ACTION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_template_comment_user') THEN
    ALTER TABLE public.t_template_comment
      ADD CONSTRAINT fk_template_comment_user
      FOREIGN KEY (user_id) REFERENCES public.t_user(id)
      ON DELETE SET NULL ON UPDATE NO ACTION;
  END IF;
END $$;

/* 新增实体：聊天系统（用户会话与消息） */
CREATE TABLE IF NOT EXISTS public.t_chat_session
(
    id          bigserial PRIMARY KEY,
    user_id     bigint       NOT NULL,
    title       varchar(255),
    last_message text,
    create_time timestamp    DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_session_user ON public.t_chat_session(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_session_update ON public.t_chat_session(update_time DESC);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_chat_session_user') THEN
    ALTER TABLE public.t_chat_session
      ADD CONSTRAINT fk_chat_session_user
      FOREIGN KEY (user_id) REFERENCES public.t_user(id)
      ON DELETE CASCADE ON UPDATE NO ACTION;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.t_chat_message
(
    id          bigserial PRIMARY KEY,
    session_id  bigint       NOT NULL,
    role        smallint     NOT NULL, -- 1-用户 2-助手
    content     text         NOT NULL,
    timestamp   timestamp    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_chat_message_session ON public.t_chat_message(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_time ON public.t_chat_message(timestamp);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_chat_message_role') THEN
    ALTER TABLE public.t_chat_message
      ADD CONSTRAINT chk_chat_message_role CHECK (role IN (1,2));
  END IF;
END $$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_chat_message_session') THEN
    ALTER TABLE public.t_chat_message
      ADD CONSTRAINT fk_chat_message_session
      FOREIGN KEY (session_id) REFERENCES public.t_chat_session(id)
      ON DELETE CASCADE ON UPDATE NO ACTION;
  END IF;
END $$;

/* 新增实体：共享KB内置聊天 */
CREATE TABLE IF NOT EXISTS public.t_kb_chat_message
(
    id                bigserial PRIMARY KEY,
    knowledge_base_id bigint       NOT NULL,
    user_id           bigint,
    role              smallint     NOT NULL, -- 1-用户 2-助手
    content           text         NOT NULL,
    timestamp         timestamp    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_kb_chat_kb ON public.t_kb_chat_message(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_kb_chat_time ON public.t_kb_chat_message(timestamp);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_kb_chat_message_role') THEN
    ALTER TABLE public.t_kb_chat_message
      ADD CONSTRAINT chk_kb_chat_message_role CHECK (role IN (1,2));
  END IF;
END $$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_kb_chat_message_kb') THEN
    ALTER TABLE public.t_kb_chat_message
      ADD CONSTRAINT fk_kb_chat_message_kb
      FOREIGN KEY (knowledge_base_id) REFERENCES public.t_shared_knowledge_base(id)
      ON DELETE CASCADE ON UPDATE NO ACTION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_kb_chat_message_user') THEN
    ALTER TABLE public.t_kb_chat_message
      ADD CONSTRAINT fk_kb_chat_message_user
      FOREIGN KEY (user_id) REFERENCES public.t_user(id)
      ON DELETE SET NULL ON UPDATE NO ACTION;
  END IF;
END $$;

/* 个人知识库：集合与文档 */
CREATE TABLE IF NOT EXISTS public.t_kb_collection
(
    id          bigserial PRIMARY KEY,
    user_id     bigint       NOT NULL,
    name        varchar(128) NOT NULL,
    description text,
    color       varchar(7),
    doc_count   integer      DEFAULT 0,
    create_time timestamp    DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_kb_collection_user ON public.t_kb_collection(user_id);
CREATE INDEX IF NOT EXISTS idx_kb_collection_update ON public.t_kb_collection(update_time DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_collection_user_name ON public.t_kb_collection(user_id, name);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_kb_collection_user') THEN
    ALTER TABLE public.t_kb_collection
      ADD CONSTRAINT fk_kb_collection_user
      FOREIGN KEY (user_id) REFERENCES public.t_user(id)
      ON DELETE CASCADE ON UPDATE NO ACTION;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.t_kb_document
(
    id           bigserial PRIMARY KEY,
    title        varchar(255) NOT NULL,
    collection_id bigint,
    file_id      bigint,
    source       varchar(50),
    index_status smallint      DEFAULT 0,
    chunk_count  integer       DEFAULT 0,
    tags_json    jsonb,
    file_type    varchar(50),
    file_size    bigint,
    created_at   timestamp     DEFAULT CURRENT_TIMESTAMP,
    updated_at   timestamp     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_kb_document_collection ON public.t_kb_document(collection_id);
CREATE INDEX IF NOT EXISTS idx_kb_document_file ON public.t_kb_document(file_id);
CREATE INDEX IF NOT EXISTS idx_kb_document_index_status ON public.t_kb_document(index_status);
CREATE INDEX IF NOT EXISTS idx_kb_document_updated_at ON public.t_kb_document(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_kb_document_collection_update ON public.t_kb_document(collection_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_kb_document_tags_gin ON public.t_kb_document USING gin (tags_json);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='chk_kb_document_index_status') THEN
    ALTER TABLE public.t_kb_document
      ADD CONSTRAINT chk_kb_document_index_status CHECK (index_status = ANY (ARRAY[0,1,2]));
  END IF;
END $$;
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_kb_document_collection') THEN
    ALTER TABLE public.t_kb_document
      ADD CONSTRAINT fk_kb_document_collection
      FOREIGN KEY (collection_id) REFERENCES public.t_kb_collection(id)
      ON DELETE SET NULL ON UPDATE NO ACTION;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_kb_document_file') THEN
    ALTER TABLE public.t_kb_document
      ADD CONSTRAINT fk_kb_document_file
      FOREIGN KEY (file_id) REFERENCES public.t_file_info(id)
      ON DELETE SET NULL ON UPDATE NO ACTION;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS public.t_kb_document_tag
(
    id          bigserial PRIMARY KEY,
    document_id bigint      NOT NULL,
    tag_name    varchar(50) NOT NULL,
    create_time timestamp   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_kb_document_tag UNIQUE (document_id, tag_name)
);
CREATE INDEX IF NOT EXISTS idx_kb_document_tag_doc ON public.t_kb_document_tag(document_id);
CREATE INDEX IF NOT EXISTS idx_kb_document_tag_name ON public.t_kb_document_tag(tag_name);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_kb_document_tag_doc') THEN
    ALTER TABLE public.t_kb_document_tag
      ADD CONSTRAINT fk_kb_document_tag_doc
      FOREIGN KEY (document_id) REFERENCES public.t_kb_document(id)
      ON DELETE CASCADE ON UPDATE NO ACTION;
  END IF;
END $$;

/* 性能索引与一致性增强 */
CREATE INDEX IF NOT EXISTS idx_note_user_status_update
  ON public.t_note(user_id, status, update_time DESC)
  INCLUDE (title, notebook_id, is_pinned);
CREATE INDEX IF NOT EXISTS idx_note_pinned_update_partial
  ON public.t_note(update_time DESC) WHERE is_pinned = true;
CREATE INDEX IF NOT EXISTS idx_note_title_lower ON public.t_note (lower(title));

CREATE INDEX IF NOT EXISTS idx_file_user_type_time
  ON public.t_file_info(user_id, file_type, create_time DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_file_info_user_md5 ON public.t_file_info(user_id, file_md5);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kb_creator_name ON public.t_shared_knowledge_base(creator_id, name);
CREATE INDEX IF NOT EXISTS idx_kb_file_kb_time ON public.t_knowledge_base_file(knowledge_base_id, upload_time DESC);

/* 向量 ANN 索引与组合过滤 */
-- 修复：为 pgvector 的 embedding 列设置固定维度（Qwen text-embedding-v3 通常为 1536）
DO $$
DECLARE v_dims integer;
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_attribute a
    JOIN pg_class c ON a.attrelid = c.oid
    JOIN pg_type t ON a.atttypid = t.oid
    WHERE c.relname = 'documents'
      AND c.relnamespace = 'public'::regnamespace
      AND a.attname = 'embedding'
      AND t.typname = 'vector'
      AND a.atttypmod = -1
  ) THEN
    BEGIN
      SELECT vector_dims(embedding) INTO v_dims
      FROM public.documents
      WHERE embedding IS NOT NULL
      LIMIT 1;
    EXCEPTION WHEN undefined_function THEN
      SELECT length(regexp_replace(embedding::text, '[^,]', '', 'g')) + 1
      INTO v_dims
      FROM public.documents
      WHERE embedding IS NOT NULL
      LIMIT 1;
    END;
    IF v_dims IS NULL THEN
      v_dims := 1536;
    END IF;
    EXECUTE format(
      'ALTER TABLE public.documents ALTER COLUMN embedding TYPE vector(%s) USING embedding',
      v_dims
    );
  END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_documents_embedding_ivfflat
  ON public.documents USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX IF NOT EXISTS idx_documents_kb_type_time
  ON public.documents (knowledge_base_type, knowledge_base_id, created_at DESC);
CREATE INDEX IF NOT EXISTS documents_metadata_idx ON public.documents USING gin (metadata);

/* 触发器：统一维护时间戳与共享KB删除清理 */
CREATE OR REPLACE FUNCTION public.fn_set_update_time() RETURNS trigger AS $$
BEGIN
  IF TG_ARGV[0] = 'update_time' THEN
    NEW.update_time := CURRENT_TIMESTAMP;
  ELSIF TG_ARGV[0] = 'updated_at' THEN
    NEW.updated_at := CURRENT_TIMESTAMP;
  END IF;
  RETURN NEW;
END $$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_t_notebook_update_time') THEN
    CREATE TRIGGER trg_t_notebook_update_time BEFORE UPDATE ON public.t_notebook
    FOR EACH ROW EXECUTE FUNCTION public.fn_set_update_time('update_time');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_t_note_update_time') THEN
    CREATE TRIGGER trg_t_note_update_time BEFORE UPDATE ON public.t_note
    FOR EACH ROW EXECUTE FUNCTION public.fn_set_update_time('update_time');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_t_kb_collection_update_time') THEN
    CREATE TRIGGER trg_t_kb_collection_update_time BEFORE UPDATE ON public.t_kb_collection
    FOR EACH ROW EXECUTE FUNCTION public.fn_set_update_time('update_time');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_t_chat_session_update_time') THEN
    CREATE TRIGGER trg_t_chat_session_update_time BEFORE UPDATE ON public.t_chat_session
    FOR EACH ROW EXECUTE FUNCTION public.fn_set_update_time('update_time');
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_t_template_update_time') THEN
    CREATE TRIGGER trg_t_template_update_time BEFORE UPDATE ON public.t_template
    FOR EACH ROW EXECUTE FUNCTION public.fn_set_update_time('updated_at');
  END IF;
END $$;

CREATE OR REPLACE FUNCTION public.fn_cleanup_documents_on_kb_delete() RETURNS trigger AS $$
BEGIN
  DELETE FROM public.documents
    WHERE knowledge_base_type = 2
      AND knowledge_base_id = OLD.id;
  RETURN OLD;
END $$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_kb_delete_cleanup_documents') THEN
    CREATE TRIGGER trg_kb_delete_cleanup_documents
      AFTER DELETE ON public.t_shared_knowledge_base
      FOR EACH ROW EXECUTE FUNCTION public.fn_cleanup_documents_on_kb_delete();
  END IF;
END $$;

COMMIT;

-- =========================
-- 用户AI设置：新增表与回滚
-- =========================
BEGIN;

SET search_path TO public;

-- 用户AI设置表（与前端 Settings/index.vue 中 AISettings 同步）
CREATE TABLE IF NOT EXISTS public.t_user_ai_settings
(
    id                     bigserial PRIMARY KEY,
    user_id                bigint       NOT NULL,
    enable_personal_kb     boolean      NOT NULL DEFAULT true,
    personal_kb_max_docs   integer      NOT NULL DEFAULT 5
        CONSTRAINT chk_personal_kb_max_docs CHECK (personal_kb_max_docs BETWEEN 1 AND 50),
    chat_style             smallint     NOT NULL DEFAULT 2
        CONSTRAINT chk_chat_style CHECK (chat_style = ANY (ARRAY[1,2,3])),
    reply_language         smallint     NOT NULL DEFAULT 1
        CONSTRAINT chk_reply_language CHECK (reply_language = ANY (ARRAY[1,2,3])),
    assistant_name         varchar(100),
    chat_api_key           text,
    chat_api_endpoint      varchar(255),
    enable_shared_kb       boolean      NOT NULL DEFAULT true,
    shared_kb_max_docs     integer      NOT NULL DEFAULT 5
        CONSTRAINT chk_shared_kb_max_docs CHECK (shared_kb_max_docs BETWEEN 1 AND 50),
    custom_system_prompt   text,
    kb_api_key             text,
    kb_api_endpoint        varchar(255),
    create_time            timestamp    DEFAULT CURRENT_TIMESTAMP,
    update_time            timestamp    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_ai_settings UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_ai_settings_user ON public.t_user_ai_settings(user_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_user_ai_settings_user') THEN
    ALTER TABLE public.t_user_ai_settings
      ADD CONSTRAINT fk_user_ai_settings_user
      FOREIGN KEY (user_id) REFERENCES public.t_user(id)
      ON DELETE CASCADE ON UPDATE NO ACTION;
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_user_ai_settings_update_time') THEN
    CREATE TRIGGER trg_user_ai_settings_update_time BEFORE UPDATE ON public.t_user_ai_settings
    FOR EACH ROW EXECUTE FUNCTION public.fn_set_update_time('update_time');
  END IF;
END $$;

COMMIT;

-- 版本回退（DOWN）过程：仅移除本节新增对象
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.routines WHERE routine_name = 'fn_migration_v2_ai_settings_down') THEN
    CREATE OR REPLACE FUNCTION public.fn_migration_v2_ai_settings_down() RETURNS void AS $DOWN$
    BEGIN
      -- 删除触发器
      IF EXISTS (SELECT 1 FROM pg_trigger WHERE tgname='trg_user_ai_settings_update_time') THEN
        DROP TRIGGER trg_user_ai_settings_update_time ON public.t_user_ai_settings;
      END IF;
      -- 删除外键与索引
      IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_user_ai_settings_user') THEN
        ALTER TABLE public.t_user_ai_settings DROP CONSTRAINT fk_user_ai_settings_user;
      END IF;
      IF EXISTS (SELECT 1 FROM pg_class WHERE relname='idx_user_ai_settings_user') THEN
        DROP INDEX IF EXISTS idx_user_ai_settings_user;
      END IF;
      -- 删除唯一约束（随表一同删除，这里容错）
      -- DROP INDEX IF EXISTS uk_user_ai_settings;
      -- 删除表
      DROP TABLE IF EXISTS public.t_user_ai_settings;
    END
    $DOWN$ LANGUAGE plpgsql;
  END IF;
END $$;
