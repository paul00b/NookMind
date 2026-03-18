-- Movies feature migration
-- Run this in your Supabase SQL editor after the initial supabase-migration.sql
-- Safe to re-run: all statements are idempotent

-- Movie status enum (safe create)
DO $$ BEGIN
  CREATE TYPE movie_status AS ENUM ('watched', 'want_to_watch');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Movies table
CREATE TABLE IF NOT EXISTS public.movies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  tmdb_id INTEGER,
  title TEXT NOT NULL,
  director TEXT NOT NULL DEFAULT '',
  description TEXT,
  poster_url TEXT,
  release_date TEXT,
  runtime INTEGER,
  genre TEXT,
  status movie_status NOT NULL DEFAULT 'want_to_watch',
  rating INTEGER CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
  personal_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS movies_user_id_idx ON public.movies(user_id);
CREATE INDEX IF NOT EXISTS movies_status_idx ON public.movies(status);
CREATE INDEX IF NOT EXISTS movies_created_at_idx ON public.movies(created_at DESC);

-- RLS
ALTER TABLE public.movies ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
  CREATE POLICY "Users can view own movies" ON public.movies FOR SELECT USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE POLICY "Users can insert own movies" ON public.movies FOR INSERT WITH CHECK (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE POLICY "Users can update own movies" ON public.movies FOR UPDATE USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE POLICY "Users can delete own movies" ON public.movies FOR DELETE USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Movie collections table
CREATE TABLE IF NOT EXISTS public.movie_categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.movie_categories ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
  CREATE POLICY "Users can manage own movie categories" ON public.movie_categories FOR ALL USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Movie category items join table
CREATE TABLE IF NOT EXISTS public.movie_category_items (
  category_id UUID NOT NULL REFERENCES public.movie_categories(id) ON DELETE CASCADE,
  movie_id UUID NOT NULL REFERENCES public.movies(id) ON DELETE CASCADE,
  PRIMARY KEY (category_id, movie_id)
);

ALTER TABLE public.movie_category_items ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
  CREATE POLICY "Users can manage own movie category items"
    ON public.movie_category_items FOR ALL
    USING (
      EXISTS (
        SELECT 1 FROM public.movie_categories
        WHERE id = category_id AND user_id = auth.uid()
      )
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
