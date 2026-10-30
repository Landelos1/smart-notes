-- 支持同一手机号分别创建普通用户和管理员；每个角色内仍然唯一。
-- 已有数据库先备份，再执行本文件；全新数据库直接使用 ../init-local.sql。
-- 如果已有同一手机号、同一角色的重复记录，创建约束会失败并回滚，请先人工核查。
BEGIN;
SET LOCAL lock_timeout = '10s';
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.t_user'::regclass
          AND conname = 't_user_phone_role_key'
    ) THEN
        ALTER TABLE public.t_user
            ADD CONSTRAINT t_user_phone_role_key UNIQUE (phone, role_type);
    END IF;
END $$;
ALTER TABLE public.t_user DROP CONSTRAINT IF EXISTS t_user_phone_key;
COMMIT;
