-- 数据库验证与边界测试用例（PostgreSQL）
-- 执行建议：逐段运行，观察异常是否按预期抛出；每段均在事务中回滚

/* 1) 外键级联：删除用户应清理其会话与笔记 */
BEGIN;
  -- 前置：创建一个临时用户并数据
  INSERT INTO public.t_user (phone, password, role_type, username)
  VALUES ('13900000000', 'pwd', 1, 'TempUser');

  -- 会话与消息
  INSERT INTO public.t_chat_session (user_id, title) 
  VALUES ((SELECT id FROM public.t_user WHERE phone='13900000000'), '临时会话');
  INSERT INTO public.t_chat_message (session_id, role, content)
  SELECT id, 1, 'Hello' FROM public.t_chat_session WHERE title='临时会话';

  -- 笔记本与笔记
  INSERT INTO public.t_notebook (name, user_id)
  VALUES ('临时本', (SELECT id FROM public.t_user WHERE phone='13900000000'));
  INSERT INTO public.t_note (title, user_id, notebook_id)
  SELECT '临时笔记', (SELECT id FROM public.t_user WHERE phone='13900000000'),
         (SELECT id FROM public.t_notebook WHERE name='临时本');
  INSERT INTO public.t_note_tag (note_id, tag_name)
  SELECT id, '测试' FROM public.t_note WHERE title='临时笔记';

  -- 删除用户 → 期望会话/消息/笔记/标签均被清理
  DELETE FROM public.t_user WHERE phone='13900000000';
ROLLBACK;

/* 2) 共享KB删除：documents 清理触发器 */
BEGIN;
  -- 构造KB与文件、向量
  INSERT INTO public.t_user (phone, password, role_type, username)
  VALUES ('13900000001', 'pwd', 1, 'KBUser');
  INSERT INTO public.t_shared_knowledge_base (name, creator_id, is_public)
  VALUES ('临时KB', (SELECT id FROM public.t_user WHERE phone='13900000001'), true);
  INSERT INTO public.t_file_info (original_filename, file_name)
  VALUES ('临时文件.pdf','temp.pdf');
  INSERT INTO public.documents (text, file_id, knowledge_base_id, knowledge_base_type)
  SELECT '临时向量文本', f.id, kb.id, 2
  FROM public.t_file_info f, public.t_shared_knowledge_base kb
  WHERE f.original_filename='临时文件.pdf' AND kb.name='临时KB';

  -- 删除KB → 期望 documents 中对应记录被清理
  DELETE FROM public.t_shared_knowledge_base WHERE name='临时KB';
ROLLBACK;

/* 3) 模板评分边界：非法评分应拒绝 */
BEGIN;
  INSERT INTO public.t_template (title, rating)
  VALUES ('非法评分模板', 6.0); -- 期望违反 chk_template_rating_range
ROLLBACK;

BEGIN;
  INSERT INTO public.t_template_comment (template_id, content, rating)
  VALUES ( (SELECT id FROM public.t_template LIMIT 1), '非法评分评论', 0 ); -- 期望违反 chk_template_comment_rating_range
ROLLBACK;

/* 4) 个人KB索引状态边界：非法状态应拒绝 */
BEGIN;
  INSERT INTO public.t_kb_document (title, index_status)
  VALUES ('非法索引状态文档', 3); -- 期望违反 chk_kb_document_index_status
ROLLBACK;

/* 5) 唯一性约束：笔记本/集合命名唯一 */
BEGIN;
  -- 准备用户
  INSERT INTO public.t_user (phone, password, role_type, username)
  VALUES ('13900000002', 'pwd', 1, 'User2');

  -- 笔记本同名（同一用户）
  INSERT INTO public.t_notebook (name, user_id) VALUES ('重复本', (SELECT id FROM public.t_user WHERE phone='13900000002'));
  INSERT INTO public.t_notebook (name, user_id) VALUES ('重复本', (SELECT id FROM public.t_user WHERE phone='13900000002'));
  -- 期望违反 uk_notebook_user_name
ROLLBACK;

BEGIN;
  -- 集合同名（同一用户）
  INSERT INTO public.t_kb_collection (name, user_id) VALUES ('重复集合', (SELECT id FROM public.t_user WHERE phone='13900000002'));
  INSERT INTO public.t_kb_collection (name, user_id) VALUES ('重复集合', (SELECT id FROM public.t_user WHERE phone='13900000002'));
  -- 期望违反 uk_kb_collection_user_name
ROLLBACK;

/* 6) 文件查重：同用户+MD5 唯一 */
BEGIN;
  INSERT INTO public.t_file_info (original_filename, file_md5, user_id)
  SELECT 'dup1', 'md5same', id FROM public.t_user WHERE phone='13900000002';
  INSERT INTO public.t_file_info (original_filename, file_md5, user_id)
  SELECT 'dup2', 'md5same', id FROM public.t_user WHERE phone='13900000002';
  -- 期望违反 uk_file_info_user_md5
ROLLBACK;

/* 7) 向量 ANN 索引基本验证（仅示意）
   - 插入若干 embedding 后执行相似度查询
   - 实际需配合应用层设置 ivfflat.probes，并比较无索引/有索引的时延 */
-- 示例：
-- INSERT INTO public.documents (text, embedding) VALUES ('v1', '[0.1,0.2,0.3,...]');
-- SELECT embedding_id, text FROM public.documents ORDER BY embedding <=> '[0.1,0.2,0.3,...]' LIMIT 10;

