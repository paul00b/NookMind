-- NookMind — features turned on for one account (Android app only).
-- Each user reads their own row and cannot write it: rows are added from the dashboard.
-- Run once in Supabase → SQL Editor.

CREATE TABLE IF NOT EXISTS public.user_features (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  features TEXT[] NOT NULL DEFAULT '{}'
);

ALTER TABLE public.user_features ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "User features: read own" ON public.user_features;
CREATE POLICY "User features: read own" ON public.user_features FOR SELECT
  USING (auth.uid() = user_id);
-- No INSERT / UPDATE / DELETE policy on purpose: only the dashboard (service role) grants features.

-- To turn Jellyseerr on for an account, put its email below and run:
-- INSERT INTO public.user_features (user_id, features)
--   SELECT id, ARRAY['jellyseerr'] FROM auth.users WHERE email = 'ton.adresse@exemple.fr'
--   ON CONFLICT (user_id) DO UPDATE SET features = (
--     SELECT ARRAY(SELECT DISTINCT unnest(user_features.features || EXCLUDED.features)));
