-- 本地部署初始化：对应当前后端的 32 个实体表和 documents 向量表。
-- PostgreSQL 16 + pgvector；适用于空库或仅由后端创建 documents 的数据库。
-- 已有旧版业务库请先做专门迁移；本文件不覆盖旧表、不导入原作者账号或文件。
-- 在连接 postgres 数据库的 SQL 控制台执行整个文件；失败时执行 ROLLBACK。
BEGIN;
SET LOCAL search_path TO public;
SET LOCAL lock_timeout = '10s';

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS public.t_user (
    id bigserial PRIMARY KEY,
    phone varchar(11) NOT NULL,
    password varchar(100),
    role_type integer NOT NULL DEFAULT 1,
    username varchar(50), avatar_url text, real_name varchar(50), id_card varchar(18),
    gender integer, birth_date date,
    status integer DEFAULT 1,
    last_login_time timestamp,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT t_user_phone_role_key UNIQUE (phone, role_type)
);

CREATE TABLE IF NOT EXISTS public.t_notebook (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    name varchar(128) NOT NULL, cover text, description text,
    sort_order integer DEFAULT 0,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, name)
);

CREATE TABLE IF NOT EXISTS public.t_note (
    id bigserial PRIMARY KEY,
    notebook_id bigint REFERENCES public.t_notebook(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    title varchar(255) NOT NULL, content_md text,
    status integer DEFAULT 1, is_pinned boolean DEFAULT false,
    view_count integer DEFAULT 0, word_count integer DEFAULT 0,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_note_tag (
    id bigserial PRIMARY KEY,
    note_id bigint NOT NULL REFERENCES public.t_note(id) ON DELETE CASCADE,
    tag_name varchar(50) NOT NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (note_id, tag_name)
);

-- 学习闭环：任务与复习计划由用户或 AI Agent 创建，均严格归属当前用户。
CREATE TABLE IF NOT EXISTS public.t_task (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    note_id bigint REFERENCES public.t_note(id) ON DELETE SET NULL,
    title varchar(255) NOT NULL,
    description text,
    status integer NOT NULL DEFAULT 0,
    priority integer NOT NULL DEFAULT 2,
    due_time timestamp,
    completed_time timestamp,
    source_type varchar(30) DEFAULT 'manual',
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_review_plan (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    note_id bigint REFERENCES public.t_note(id) ON DELETE SET NULL,
    title varchar(255) NOT NULL,
    goal text,
    frequency varchar(30) NOT NULL DEFAULT 'weekly',
    repeat_day smallint,
    interval_days integer NOT NULL DEFAULT 7,
    start_date date NOT NULL,
    end_date date NOT NULL,
    status integer NOT NULL DEFAULT 0,
    source_type varchar(30) DEFAULT 'manual',
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_file_info (
    id bigserial PRIMARY KEY,
    original_filename varchar(255), file_name varchar(255), file_extension varchar(50),
    file_size bigint, file_path text, file_url text,
    user_id bigint REFERENCES public.t_user(id) ON DELETE CASCADE,
    file_md5 varchar(32), file_type varchar(50), mime_type varchar(100), file_hash varchar(64),
    source_type integer DEFAULT 1, source_note_id bigint, source_note_title varchar(255),
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_shared_knowledge_base (
    id bigserial PRIMARY KEY,
    name varchar(128) NOT NULL, description text, tag_name varchar(100), cover_url text,
    password varchar(100),
    creator_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    is_public boolean DEFAULT true,
    member_count integer DEFAULT 1, file_count integer DEFAULT 0, status integer DEFAULT 1,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_knowledge_base_member (
    id bigserial PRIMARY KEY,
    knowledge_base_id bigint NOT NULL REFERENCES public.t_shared_knowledge_base(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    role integer DEFAULT 3,
    join_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (knowledge_base_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.t_knowledge_base_file (
    id bigserial PRIMARY KEY,
    knowledge_base_id bigint NOT NULL REFERENCES public.t_shared_knowledge_base(id) ON DELETE CASCADE,
    file_id bigint NOT NULL REFERENCES public.t_file_info(id) ON DELETE CASCADE,
    uploader_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    source_type integer DEFAULT 1,
    upload_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (knowledge_base_id, file_id)
);

CREATE TABLE IF NOT EXISTS public.t_kb_invitation (
    id bigserial PRIMARY KEY,
    knowledge_base_id bigint NOT NULL REFERENCES public.t_shared_knowledge_base(id) ON DELETE CASCADE,
    inviter_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    invitee_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    role integer DEFAULT 3, status integer DEFAULT 0,
    invite_time timestamp DEFAULT CURRENT_TIMESTAMP, response_time timestamp,
    expire_time timestamp DEFAULT (CURRENT_TIMESTAMP + INTERVAL '7 days'),
    inviter_message text
);

CREATE TABLE IF NOT EXISTS public.t_chat_session (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    title varchar(255), last_message text,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_chat_message (
    id bigserial PRIMARY KEY,
    session_id bigint NOT NULL REFERENCES public.t_chat_session(id) ON DELETE CASCADE,
    role integer NOT NULL, content text NOT NULL, citations jsonb, total_tokens integer,
    timestamp timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_user_ai_settings (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL UNIQUE REFERENCES public.t_user(id) ON DELETE CASCADE,
    enable_personal_kb boolean DEFAULT true, personal_kb_max_docs integer DEFAULT 5,
    chat_style integer DEFAULT 2, reply_language integer DEFAULT 1, assistant_name varchar(100),
    chat_api_key text, chat_api_endpoint text,
    enable_shared_kb boolean DEFAULT true, shared_kb_max_docs integer DEFAULT 5,
    custom_system_prompt text, kb_api_key text, kb_api_endpoint text,
    use_custom_key boolean DEFAULT false, chat_model varchar(100),
    kb_use_custom_key boolean DEFAULT false, kb_chat_model varchar(100),
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_member_plan (
    id bigserial PRIMARY KEY,
    plan_name varchar(100) NOT NULL, duration_months integer NOT NULL,
    price numeric(12,2) NOT NULL, description text, status integer DEFAULT 1,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_order (
    id bigserial PRIMARY KEY,
    order_no varchar(100) NOT NULL UNIQUE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    plan_id bigint REFERENCES public.t_member_plan(id) ON DELETE SET NULL,
    plan_name varchar(100), amount numeric(12,2), pay_method integer,
    pay_time timestamp, status integer DEFAULT 0,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_user_membership (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL UNIQUE REFERENCES public.t_user(id) ON DELETE CASCADE,
    expire_time timestamp, is_active boolean DEFAULT false,
    ai_call_quota integer DEFAULT 0, ai_call_used integer DEFAULT 0,
    last_ai_reset_time timestamp,
    kb_quota integer DEFAULT 0, kb_used integer DEFAULT 0,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_note_square (
    id bigserial PRIMARY KEY,
    note_id bigint NOT NULL UNIQUE REFERENCES public.t_note(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    status integer DEFAULT 1, recommend_score integer DEFAULT 0, is_pinned boolean DEFAULT false,
    summary text, cover_url text,
    view_count integer DEFAULT 0, like_count integer DEFAULT 0,
    favorite_count integer DEFAULT 0, comment_count integer DEFAULT 0,
    publish_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_note_comment (
    id bigserial PRIMARY KEY,
    note_id bigint NOT NULL REFERENCES public.t_note(id) ON DELETE CASCADE,
    user_id bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    parent_id bigint REFERENCES public.t_note_comment(id) ON DELETE CASCADE,
    reply_to_user_id bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    content text NOT NULL, likes integer DEFAULT 0,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_note_like (
    id bigserial PRIMARY KEY,
    note_id bigint NOT NULL REFERENCES public.t_note(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (note_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.t_note_favorite (
    id bigserial PRIMARY KEY,
    note_id bigint NOT NULL REFERENCES public.t_note(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (note_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.t_template (
    id bigserial PRIMARY KEY,
    title varchar(255) NOT NULL, description text, category varchar(100), thumbnail text,
    content text, downloads integer DEFAULT 0, rating numeric(3,2) DEFAULT 0,
    views integer DEFAULT 0,
    author_id bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    is_public boolean DEFAULT true, status integer DEFAULT 1,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_template_tag (
    id bigserial PRIMARY KEY,
    template_id bigint NOT NULL REFERENCES public.t_template(id) ON DELETE CASCADE,
    tag_name varchar(50) NOT NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (template_id, tag_name)
);

CREATE TABLE IF NOT EXISTS public.t_template_comment (
    id bigserial PRIMARY KEY,
    template_id bigint NOT NULL REFERENCES public.t_template(id) ON DELETE CASCADE,
    user_id bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    content text NOT NULL, rating integer, likes integer DEFAULT 0,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_template_comment_like (
    id bigserial PRIMARY KEY,
    comment_id bigint NOT NULL REFERENCES public.t_template_comment(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (comment_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.t_template_favorite (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    template_id bigint NOT NULL REFERENCES public.t_template(id) ON DELETE CASCADE,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, template_id)
);

CREATE TABLE IF NOT EXISTS public.t_template_report (
    id bigserial PRIMARY KEY,
    template_id bigint NOT NULL REFERENCES public.t_template(id) ON DELETE CASCADE,
    reporter_id bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    reason text NOT NULL, description text, status integer DEFAULT 0,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    handle_time timestamp,
    handler_id bigint REFERENCES public.t_user(id) ON DELETE SET NULL
);

-- 回收站保留原始数据快照：source_id / notebook_id 不设外键，避免原对象删除后丢失快照。
CREATE TABLE IF NOT EXISTS public.t_trash (
    id bigserial PRIMARY KEY,
    trash_type integer NOT NULL, source_id bigint NOT NULL,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    delete_time timestamp DEFAULT CURRENT_TIMESTAMP,
    restore_deadline timestamp DEFAULT (CURRENT_TIMESTAMP + INTERVAL '30 days'),
    note_title text, note_content_md text, note_content_html text, notebook_id bigint,
    note_is_pinned boolean, note_status integer, note_view_count integer, note_word_count integer,
    note_create_time timestamp, note_update_time timestamp,
    file_original_filename text, file_name text, file_extension varchar(50), file_size bigint,
    file_path text, file_url text, file_md5 varchar(32), file_update_time timestamp,
    file_type varchar(50), mime_type varchar(100), file_hash varchar(64),
    source_type integer, source_note_id bigint, source_note_title text, file_create_time timestamp
);

CREATE TABLE IF NOT EXISTS public.t_team_space (
    id bigserial PRIMARY KEY,
    name varchar(128) NOT NULL, description text, avatar_url text,
    creator_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    is_public boolean DEFAULT false,
    member_count integer DEFAULT 1, doc_count integer DEFAULT 0,
    last_active_time timestamp DEFAULT CURRENT_TIMESTAMP,
    invite_code varchar(100) UNIQUE, invite_role integer DEFAULT 3,
    invite_expire_time timestamp, invite_max_uses integer DEFAULT 0,
    invite_used_count integer DEFAULT 0, status integer DEFAULT 1,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_team_member (
    id bigserial PRIMARY KEY,
    team_id bigint NOT NULL REFERENCES public.t_team_space(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    role integer DEFAULT 3, status integer DEFAULT 1,
    join_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (team_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.t_team_doc (
    id bigserial PRIMARY KEY,
    team_id bigint NOT NULL REFERENCES public.t_team_space(id) ON DELETE CASCADE,
    title varchar(255) NOT NULL, content_md text, content_html text,
    status integer DEFAULT 1, is_pinned boolean DEFAULT false, version integer DEFAULT 1,
    locked_by bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    lock_token varchar(100), lock_time timestamp, lock_expire_time timestamp,
    created_by bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    updated_by bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_team_doc_event (
    id bigserial PRIMARY KEY,
    team_id bigint NOT NULL REFERENCES public.t_team_space(id) ON DELETE CASCADE,
    doc_id bigint NOT NULL REFERENCES public.t_team_doc(id) ON DELETE CASCADE,
    user_id bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    event_type integer NOT NULL,
    parent_id bigint REFERENCES public.t_team_doc_event(id) ON DELETE CASCADE,
    content text, metadata jsonb,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_team_ai_session (
    id bigserial PRIMARY KEY,
    team_id bigint NOT NULL REFERENCES public.t_team_space(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES public.t_user(id) ON DELETE CASCADE,
    title varchar(255), is_pinned boolean DEFAULT false, status integer DEFAULT 1,
    last_message text,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS public.t_team_ai_message (
    id bigserial PRIMARY KEY,
    session_id bigint NOT NULL REFERENCES public.t_team_ai_session(id) ON DELETE CASCADE,
    user_id bigint REFERENCES public.t_user(id) ON DELETE SET NULL,
    role integer NOT NULL, content text NOT NULL, citations jsonb,
    create_time timestamp DEFAULT CURRENT_TIMESTAMP
);

-- LangChain4j 只自动创建 embedding_id / embedding / text / metadata。
-- 知识库服务还依赖以下业务字段和 jsonb 运算，必须补齐。
CREATE TABLE IF NOT EXISTS public.documents (
    embedding_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    embedding vector(1024), text text, metadata jsonb
);
ALTER TABLE public.documents
    ALTER COLUMN embedding_id SET DEFAULT gen_random_uuid(),
    ADD COLUMN IF NOT EXISTS user_id varchar(100),
    ADD COLUMN IF NOT EXISTS created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS file_id bigint,
    ADD COLUMN IF NOT EXISTS knowledge_base_id bigint,
    ADD COLUMN IF NOT EXISTS knowledge_base_type integer DEFAULT 1;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'public' AND table_name = 'documents'
                 AND column_name = 'metadata' AND data_type = 'json') THEN
        ALTER TABLE public.documents ALTER COLUMN metadata TYPE jsonb USING metadata::jsonb;
    END IF;
    IF EXISTS (SELECT 1 FROM public.documents
               WHERE embedding IS NOT NULL AND vector_dims(embedding) <> 1024) THEN
        RAISE EXCEPTION 'documents 存在非 1024 维向量，请单独迁移，初始化已回滚';
    END IF;
END $$;

-- 创建常用查询索引；唯一约束已同时创建对应索引。
CREATE INDEX IF NOT EXISTS idx_note_notebook ON public.t_note(notebook_id);
CREATE INDEX IF NOT EXISTS idx_note_user_update ON public.t_note(user_id, update_time DESC);
CREATE INDEX IF NOT EXISTS idx_task_user_status_due ON public.t_task(user_id, status, due_time);
CREATE INDEX IF NOT EXISTS idx_review_plan_user_status_date ON public.t_review_plan(user_id, status, start_date);
CREATE INDEX IF NOT EXISTS idx_file_user_time ON public.t_file_info(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_shared_kb_creator ON public.t_shared_knowledge_base(creator_id);
CREATE INDEX IF NOT EXISTS idx_kb_member_user ON public.t_knowledge_base_member(user_id);
CREATE INDEX IF NOT EXISTS idx_kb_file_file ON public.t_knowledge_base_file(file_id);
CREATE INDEX IF NOT EXISTS idx_kb_invitation_invitee ON public.t_kb_invitation(invitee_id, status);
CREATE INDEX IF NOT EXISTS idx_chat_session_user_update ON public.t_chat_session(user_id, update_time DESC);
CREATE INDEX IF NOT EXISTS idx_chat_message_session_time ON public.t_chat_message(session_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_order_user_time ON public.t_order(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_note_square_status_time ON public.t_note_square(status, publish_time DESC);
CREATE INDEX IF NOT EXISTS idx_note_comment_note_time ON public.t_note_comment(note_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_note_like_user ON public.t_note_like(user_id);
CREATE INDEX IF NOT EXISTS idx_note_favorite_user ON public.t_note_favorite(user_id);
CREATE INDEX IF NOT EXISTS idx_template_category ON public.t_template(category);
CREATE INDEX IF NOT EXISTS idx_template_author ON public.t_template(author_id);
CREATE INDEX IF NOT EXISTS idx_template_comment_template ON public.t_template_comment(template_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_template_report_status ON public.t_template_report(status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_trash_user_deadline ON public.t_trash(user_id, restore_deadline);
CREATE INDEX IF NOT EXISTS idx_team_member_user ON public.t_team_member(user_id, status);
CREATE INDEX IF NOT EXISTS idx_team_doc_team_update ON public.t_team_doc(team_id, update_time DESC);
CREATE INDEX IF NOT EXISTS idx_team_event_doc_type_time ON public.t_team_doc_event(doc_id, event_type, create_time);
CREATE INDEX IF NOT EXISTS idx_team_ai_session_team ON public.t_team_ai_session(team_id, user_id, update_time DESC);
CREATE INDEX IF NOT EXISTS idx_team_ai_message_session ON public.t_team_ai_message(session_id, create_time);
CREATE INDEX IF NOT EXISTS idx_documents_metadata ON public.documents USING gin(metadata);
CREATE INDEX IF NOT EXISTS idx_documents_file ON public.documents(file_id);
CREATE INDEX IF NOT EXISTS idx_documents_kb ON public.documents(knowledge_base_id, knowledge_base_type);
CREATE INDEX IF NOT EXISTS idx_documents_user ON public.documents(user_id);

COMMIT;

SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;
