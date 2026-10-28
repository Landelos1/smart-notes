-- 仅在独立测试库执行：测试数据回滚，但自增序列仍会前进。
BEGIN;
DO $$
DECLARE
    u bigint; nb bigint; n bigint; chat bigint; team bigint; doc bigint;
    team_chat bigint; vector_id uuid;
BEGIN
    IF (SELECT count(*) FROM pg_tables WHERE schemaname = 'public') <> 33 THEN
        RAISE EXCEPTION '预期 33 张表';
    END IF;
    INSERT INTO t_user(phone, password, username)
        VALUES ('19900000001', 'schema-test-only', 'schema-test') RETURNING id INTO u;
    INSERT INTO t_notebook(user_id, name) VALUES (u, 'schema-test') RETURNING id INTO nb;
    INSERT INTO t_note(user_id, notebook_id, title, content_md)
        VALUES (u, nb, 'schema-test', '# Hello') RETURNING id INTO n;
    INSERT INTO t_note_tag(note_id, tag_name) VALUES (n, 'test');
    IF NOT EXISTS (SELECT 1 FROM t_note n1 JOIN t_notebook b ON n1.notebook_id = b.id
                   JOIN t_user usr ON n1.user_id = usr.id
                   WHERE n1.id = n AND usr.id = u AND b.id = nb) THEN
        RAISE EXCEPTION '用户、笔记本、笔记关联失败';
    END IF;
    INSERT INTO t_note_like(note_id, user_id) VALUES (n, u);
    BEGIN
        INSERT INTO t_note_like(note_id, user_id) VALUES (n, u);
        RAISE EXCEPTION '重复点赞未被唯一约束拒绝';
    EXCEPTION WHEN unique_violation THEN NULL;
    END;
    INSERT INTO t_trash(trash_type, source_id, user_id, notebook_id, note_title)
        VALUES (1, n, u, nb, 'schema-test');
    DELETE FROM t_notebook WHERE id = nb;
    IF EXISTS (SELECT 1 FROM t_note WHERE id = n)
       OR EXISTS (SELECT 1 FROM t_note_tag WHERE note_id = n)
       OR EXISTS (SELECT 1 FROM t_note_like WHERE note_id = n) THEN
        RAISE EXCEPTION '笔记本删除级联失败';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM t_trash WHERE source_id = n AND user_id = u) THEN
        RAISE EXCEPTION '回收站快照未保留';
    END IF;
    INSERT INTO t_chat_session(user_id, title) VALUES (u, 'schema-test') RETURNING id INTO chat;
    INSERT INTO t_chat_message(session_id, role, content, citations)
        VALUES (chat, 2, 'hello', '[{"fileId": 1}]'::jsonb);
    IF NOT EXISTS (SELECT 1 FROM t_chat_message
                   WHERE session_id = chat AND citations->0->>'fileId' = '1') THEN
        RAISE EXCEPTION '聊天 JSON 引用失败';
    END IF;
    INSERT INTO t_team_space(name, creator_id) VALUES ('schema-test', u) RETURNING id INTO team;
    INSERT INTO t_team_member(team_id, user_id, role) VALUES (team, u, 0);
    INSERT INTO t_team_doc(team_id, title, created_by, updated_by)
        VALUES (team, 'schema-test', u, u) RETURNING id INTO doc;
    INSERT INTO t_team_doc_event(team_id, doc_id, user_id, event_type, metadata)
        VALUES (team, doc, u, 1, '{"version": 1}'::jsonb);
    IF NOT EXISTS (SELECT 1 FROM t_team_doc_event
                   WHERE doc_id = doc AND (metadata->>'version')::integer = 1) THEN
        RAISE EXCEPTION '团队事件版本查询失败';
    END IF;
    INSERT INTO t_team_ai_session(team_id, user_id, title)
        VALUES (team, u, 'schema-test') RETURNING id INTO team_chat;
    INSERT INTO t_team_ai_message(session_id, user_id, role, content, citations)
        VALUES (team_chat, u, 2, 'hello', '[]'::jsonb);
    INSERT INTO documents(text, metadata, embedding, file_id, knowledge_base_id, knowledge_base_type)
        VALUES ('schema-test', '{"schemaTest": true}'::jsonb,
                array_fill(0.01::real, ARRAY[1024])::vector, 1, u, 1)
        RETURNING embedding_id INTO vector_id;
    IF vector_id IS NULL OR NOT EXISTS
       (SELECT 1 FROM documents WHERE embedding_id = vector_id
        AND vector_dims(embedding) = 1024 AND metadata @> '{"schemaTest": true}'::jsonb) THEN
        RAISE EXCEPTION '向量默认 UUID 或 JSONB 查询失败';
    END IF;
    RAISE NOTICE 'PASS: 33 tables, note cascade, trash retention, uniqueness, chat JSONB, team events, vector insert';
END $$;
ROLLBACK;
