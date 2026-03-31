-- Add reading status support to books
-- Run in Supabase SQL editor. Safe to re-run (idempotent).
ALTER TABLE public.books
  ADD COLUMN IF NOT EXISTS current_page INTEGER;
