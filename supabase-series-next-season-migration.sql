-- Series next season tracking migration
-- Run this in your Supabase SQL editor
-- Safe to re-run: all statements are idempotent

ALTER TABLE public.series ADD COLUMN IF NOT EXISTS next_air_date TEXT;
ALTER TABLE public.series ADD COLUMN IF NOT EXISTS next_season_number INTEGER;
