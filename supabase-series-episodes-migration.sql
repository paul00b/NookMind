-- Episode tracking migration
-- Run this in your Supabase SQL editor
-- Safe to re-run: statement is idempotent

ALTER TABLE public.series
  ADD COLUMN IF NOT EXISTS watched_episodes JSONB NOT NULL DEFAULT '{}'::jsonb;
