-- ============================================================
-- NookMind — Supabase SQL Migration
-- Run this in: Supabase Dashboard > SQL Editor
-- ============================================================

-- Create the book status enum
CREATE TYPE book_status AS ENUM ('read', 'want_to_read');

-- Create the books table
CREATE TABLE IF NOT EXISTS public.books (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  google_books_id TEXT,
  title           TEXT NOT NULL,
  author          TEXT NOT NULL,
  description     TEXT,
  cover_url       TEXT,
  published_date  TEXT,
  page_count      INTEGER CHECK (page_count > 0),
  genre           TEXT,
  status          book_status NOT NULL DEFAULT 'want_to_read',
  rating          INTEGER CHECK (rating BETWEEN 1 AND 5),
  personal_note   TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS books_user_id_idx       ON public.books (user_id);
CREATE INDEX IF NOT EXISTS books_status_idx        ON public.books (user_id, status);
CREATE INDEX IF NOT EXISTS books_created_at_idx    ON public.books (user_id, created_at DESC);

-- Enable Row Level Security
ALTER TABLE public.books ENABLE ROW LEVEL SECURITY;

-- RLS Policies: users can only access their own rows
CREATE POLICY "Users can view own books"
  ON public.books FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own books"
  ON public.books FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own books"
  ON public.books FOR UPDATE
  USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete own books"
  ON public.books FOR DELETE
  USING (auth.uid() = user_id);

-- Optional: add a constraint so rating is only set when status is 'read'
-- (enforced in the app but you can add a DB check too)
ALTER TABLE public.books
  ADD CONSTRAINT rating_only_for_read
  CHECK (
    (status = 'read') OR (rating IS NULL)
  );
