-- Add explicit recurrence settings for weekly and custom review plans.
BEGIN;
SET LOCAL lock_timeout = '10s';

ALTER TABLE public.t_review_plan
    ADD COLUMN IF NOT EXISTS repeat_day smallint;
ALTER TABLE public.t_review_plan
    ADD COLUMN IF NOT EXISTS interval_days integer NOT NULL DEFAULT 7;

UPDATE public.t_review_plan
SET interval_days = 1
WHERE frequency = 'daily' AND interval_days <> 1;

UPDATE public.t_review_plan
SET repeat_day = EXTRACT(ISODOW FROM start_date)::smallint,
    interval_days = 7
WHERE frequency = 'weekly' AND repeat_day IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.t_review_plan'::regclass
          AND conname = 't_review_plan_repeat_day_range'
    ) THEN
        ALTER TABLE public.t_review_plan
            ADD CONSTRAINT t_review_plan_repeat_day_range
            CHECK (repeat_day IS NULL OR repeat_day BETWEEN 1 AND 7);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'public.t_review_plan'::regclass
          AND conname = 't_review_plan_interval_days_range'
    ) THEN
        ALTER TABLE public.t_review_plan
            ADD CONSTRAINT t_review_plan_interval_days_range
            CHECK (interval_days BETWEEN 1 AND 365);
    END IF;
END $$;

COMMIT;
