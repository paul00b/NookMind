-- Series feature migration
-- Run this in your Supabase SQL editor after the movies migration
-- Safe to re-run: all statements are idempotent

-- Series status enum (reuse movie_status: watched / want_to_watch)
-- No new enum needed — we use the same values as text

-- Series table
CREATE TABLE IF NOT EXISTS public.series (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  tmdb_id INTEGER,
  title TEXT NOT NULL,
  creator TEXT NOT NULL DEFAULT '',
  description TEXT,
  poster_url TEXT,
  first_air_date TEXT,
  seasons INTEGER,
  watched_seasons JSONB NOT NULL DEFAULT '[]',
  genre TEXT,
  status TEXT NOT NULL DEFAULT 'want_to_watch' CHECK (status IN ('watched', 'watching', 'want_to_watch')),
  rating INTEGER CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
  personal_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS series_user_id_idx ON public.series(user_id);
CREATE INDEX IF NOT EXISTS series_status_idx ON public.series(status);
CREATE INDEX IF NOT EXISTS series_created_at_idx ON public.series(created_at DESC);

-- RLS
ALTER TABLE public.series ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
  CREATE POLICY "Users can view own series" ON public.series FOR SELECT USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE POLICY "Users can insert own series" ON public.series FOR INSERT WITH CHECK (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE POLICY "Users can update own series" ON public.series FOR UPDATE USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  CREATE POLICY "Users can delete own series" ON public.series FOR DELETE USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Series collections table
CREATE TABLE IF NOT EXISTS public.series_categories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.series_categories ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
  CREATE POLICY "Users can manage own series categories" ON public.series_categories FOR ALL USING (auth.uid() = user_id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- Series category items join table
CREATE TABLE IF NOT EXISTS public.series_category_items (
  category_id UUID NOT NULL REFERENCES public.series_categories(id) ON DELETE CASCADE,
  series_id UUID NOT NULL REFERENCES public.series(id) ON DELETE CASCADE,
  PRIMARY KEY (category_id, series_id)
);

ALTER TABLE public.series_category_items ENABLE ROW LEVEL SECURITY;

DO $$ BEGIN
  CREATE POLICY "Users can manage own series category items"
    ON public.series_category_items FOR ALL
    USING (
      EXISTS (
        SELECT 1 FROM public.series_categories
        WHERE id = category_id AND user_id = auth.uid()
      )
    );
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- If you already ran this migration before season tracking was added,
-- run these ALTER statements to add the watched_seasons column and update the status constraint:
--
-- ALTER TABLE public.series ADD COLUMN IF NOT EXISTS watched_seasons JSONB NOT NULL DEFAULT '[]';
-- ALTER TABLE public.series DROP CONSTRAINT IF EXISTS series_status_check;
-- ALTER TABLE public.series ADD CONSTRAINT series_status_check CHECK (status IN ('watched', 'watching', 'want_to_watch'));
