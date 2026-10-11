-- ============================================================
-- NookMind — Social: profiles, friends, Ce soir ? online, reactions
-- Run in: Supabase Dashboard > SQL Editor > New query. Safe to run more than once.
--
-- Privacy: friends never read the library tables directly. Everything social goes through the
-- security definer functions below, which return only what a profile chose to share (titles,
-- statuses, ratings), never personal notes.
-- ============================================================

-- 1. Profiles ---------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.profiles (
  id            UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  display_name  TEXT NOT NULL DEFAULT '',
  avatar_url    TEXT,
  friend_code   TEXT NOT NULL UNIQUE,
  share_movies  BOOLEAN NOT NULL DEFAULT TRUE,
  share_series  BOOLEAN NOT NULL DEFAULT TRUE,
  share_books   BOOLEAN NOT NULL DEFAULT TRUE,
  share_ratings BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- 2. Friendships: a request, then both sides --------------------------------

CREATE TABLE IF NOT EXISTS public.friendships (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  requester  UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  addressee  UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  status     TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (requester, addressee),
  CHECK (requester <> addressee)
);
CREATE INDEX IF NOT EXISTS friendships_addressee_idx ON public.friendships (addressee);
ALTER TABLE public.friendships ENABLE ROW LEVEL SECURITY;

CREATE OR REPLACE FUNCTION public.are_friends(a UUID, b UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM friendships
    WHERE status = 'accepted'
      AND ((requester = a AND addressee = b) OR (requester = b AND addressee = a))
  );
$$;

DROP POLICY IF EXISTS "Profiles: self and friends" ON public.profiles;
CREATE POLICY "Profiles: self and friends" ON public.profiles FOR SELECT
  USING (
    id = auth.uid() OR EXISTS (
      SELECT 1 FROM friendships f
      WHERE (f.requester = auth.uid() AND f.addressee = profiles.id)
         OR (f.addressee = auth.uid() AND f.requester = profiles.id)
    )
  );
DROP POLICY IF EXISTS "Profiles: update self" ON public.profiles;
CREATE POLICY "Profiles: update self" ON public.profiles FOR UPDATE
  USING (id = auth.uid()) WITH CHECK (id = auth.uid());

DROP POLICY IF EXISTS "Friendships: own" ON public.friendships;
CREATE POLICY "Friendships: own" ON public.friendships FOR SELECT
  USING (auth.uid() IN (requester, addressee));
DROP POLICY IF EXISTS "Friendships: remove" ON public.friendships;
CREATE POLICY "Friendships: remove" ON public.friendships FOR DELETE
  USING (auth.uid() IN (requester, addressee));
-- Requests and acceptations go through request_friend() and accept_friend().

-- The profile of the signed-in user, created with a friend code on first use.
CREATE OR REPLACE FUNCTION public.ensure_profile(p_name TEXT DEFAULT NULL) RETURNS public.profiles
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  me UUID := auth.uid();
  result profiles;
  code TEXT;
  alphabet TEXT := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
BEGIN
  IF me IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  SELECT * INTO result FROM profiles WHERE id = me;
  IF FOUND THEN
    IF COALESCE(p_name, '') <> '' AND result.display_name = '' THEN
      UPDATE profiles SET display_name = p_name WHERE id = me RETURNING * INTO result;
    END IF;
    RETURN result;
  END IF;
  LOOP
    code := '';
    FOR i IN 1..6 LOOP
      code := code || substr(alphabet, 1 + floor(random() * length(alphabet))::INT, 1);
    END LOOP;
    EXIT WHEN NOT EXISTS (SELECT 1 FROM profiles WHERE friend_code = code);
  END LOOP;
  INSERT INTO profiles (id, display_name, friend_code) VALUES (me, COALESCE(p_name, ''), code)
  RETURNING * INTO result;
  RETURN result;
END $$;

-- Sends a request to the owner of a friend code, or accepts theirs. Returns
-- 'requested', 'accepted', 'already', 'self' or 'not_found'.
CREATE OR REPLACE FUNCTION public.request_friend(p_code TEXT) RETURNS TEXT
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  me UUID := auth.uid();
  target UUID;
BEGIN
  IF me IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  SELECT id INTO target FROM profiles WHERE friend_code = upper(trim(p_code));
  IF target IS NULL THEN RETURN 'not_found'; END IF;
  IF target = me THEN RETURN 'self'; END IF;
  IF are_friends(me, target) THEN RETURN 'already'; END IF;
  IF EXISTS (SELECT 1 FROM friendships WHERE requester = target AND addressee = me) THEN
    UPDATE friendships SET status = 'accepted' WHERE requester = target AND addressee = me;
    RETURN 'accepted';
  END IF;
  INSERT INTO friendships (requester, addressee) VALUES (me, target)
  ON CONFLICT (requester, addressee) DO NOTHING;
  RETURN 'requested';
END $$;

CREATE OR REPLACE FUNCTION public.accept_friend(p_friendship UUID) RETURNS VOID
LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
  UPDATE friendships SET status = 'accepted' WHERE id = p_friendship AND addressee = auth.uid();
$$;

-- Friends and pending requests, with the other person's profile.
CREATE OR REPLACE FUNCTION public.my_friends() RETURNS TABLE (
  friendship_id UUID, user_id UUID, display_name TEXT, avatar_url TEXT, status TEXT, incoming BOOLEAN
)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT f.id, p.id, p.display_name, p.avatar_url, f.status, f.addressee = auth.uid()
  FROM friendships f
  JOIN profiles p ON p.id = CASE WHEN f.requester = auth.uid() THEN f.addressee ELSE f.requester END
  WHERE auth.uid() IN (f.requester, f.addressee)
  ORDER BY p.display_name;
$$;

-- What friends share of their libraries: never personal notes, ratings only when shared.
CREATE OR REPLACE FUNCTION public.friends_library() RETURNS TABLE (
  owner UUID, kind TEXT, item_key TEXT, title TEXT, subtitle TEXT, image_url TEXT,
  status TEXT, rating NUMERIC, created_at TIMESTAMPTZ, finished_at TEXT
)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  WITH friends AS (
    SELECT CASE WHEN f.requester = auth.uid() THEN f.addressee ELSE f.requester END AS id
    FROM friendships f
    WHERE f.status = 'accepted' AND auth.uid() IN (f.requester, f.addressee)
  )
  SELECT m.user_id, 'movie', m.tmdb_id::TEXT, m.title, m.director, m.poster_url, m.status::TEXT,
         CASE WHEN p.share_ratings THEN m.rating::NUMERIC END, m.created_at, m.watched_date::TEXT
  FROM movies m JOIN friends fr ON fr.id = m.user_id JOIN profiles p ON p.id = m.user_id
  WHERE p.share_movies AND m.tmdb_id IS NOT NULL
  UNION ALL
  SELECT s.user_id, 'series', s.tmdb_id::TEXT, s.title, s.creator, s.poster_url, s.status::TEXT,
         CASE WHEN p.share_ratings THEN s.rating::NUMERIC END, s.created_at, NULL
  FROM series s JOIN friends fr ON fr.id = s.user_id JOIN profiles p ON p.id = s.user_id
  WHERE p.share_series AND s.tmdb_id IS NOT NULL
  UNION ALL
  SELECT b.user_id, 'book', COALESCE(b.google_books_id, b.id::TEXT), b.title, b.author, b.cover_url, b.status::TEXT,
         CASE WHEN p.share_ratings THEN b.rating::NUMERIC END, b.created_at, NULL
  FROM books b JOIN friends fr ON fr.id = b.user_id JOIN profiles p ON p.id = b.user_id
  WHERE p.share_books;
$$;

-- 3. Ce soir ? online -------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.tonight_sessions (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  host           UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  guest          UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  deck           JSONB NOT NULL,
  round          INT NOT NULL DEFAULT 1,
  status         TEXT NOT NULL DEFAULT 'invited' CHECK (status IN ('invited', 'playing', 'done', 'cancelled')),
  host_progress  INT NOT NULL DEFAULT 0,
  guest_progress INT NOT NULL DEFAULT 0,
  host_pick      INT,
  guest_pick     INT,
  winner         INT,
  via            TEXT,
  cancelled_by   UUID,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS tonight_sessions_guest_idx ON public.tonight_sessions (guest, created_at DESC);
CREATE INDEX IF NOT EXISTS tonight_sessions_host_idx ON public.tonight_sessions (host, created_at DESC);
ALTER TABLE public.tonight_sessions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Sessions: players read" ON public.tonight_sessions;
CREATE POLICY "Sessions: players read" ON public.tonight_sessions FOR SELECT
  USING (auth.uid() IN (host, guest));
DROP POLICY IF EXISTS "Sessions: host invites a friend" ON public.tonight_sessions;
CREATE POLICY "Sessions: host invites a friend" ON public.tonight_sessions FOR INSERT
  WITH CHECK (host = auth.uid() AND public.are_friends(host, guest));
-- Everything else goes through the tonight_* functions, which keep the game consistent.

-- Each player's verdicts. Only their author reads them: the other one never sees them.
CREATE TABLE IF NOT EXISTS public.tonight_votes (
  session_id UUID NOT NULL REFERENCES public.tonight_sessions(id) ON DELETE CASCADE,
  user_id    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  card       INT NOT NULL,
  verdict    TEXT NOT NULL CHECK (verdict IN ('yes', 'no', 'love', 'seen')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (session_id, user_id, card)
);
ALTER TABLE public.tonight_votes ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Votes: own" ON public.tonight_votes;
CREATE POLICY "Votes: own" ON public.tonight_votes FOR SELECT USING (user_id = auth.uid());

CREATE OR REPLACE FUNCTION public.tonight_join(p_session UUID) RETURNS public.tonight_sessions
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE s tonight_sessions;
BEGIN
  UPDATE tonight_sessions SET status = 'playing', updated_at = NOW()
  WHERE id = p_session AND guest = auth.uid() AND status = 'invited'
  RETURNING * INTO s;
  IF NOT FOUND THEN
    SELECT * INTO s FROM tonight_sessions WHERE id = p_session AND auth.uid() IN (host, guest);
  END IF;
  RETURN s;
END $$;

-- Records a verdict; the first card both said yes to ends the game.
CREATE OR REPLACE FUNCTION public.tonight_vote(p_session UUID, p_card INT, p_verdict TEXT) RETURNS public.tonight_sessions
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  me UUID := auth.uid();
  s tonight_sessions;
  theirs TEXT;
BEGIN
  SELECT * INTO s FROM tonight_sessions WHERE id = p_session FOR UPDATE;
  IF NOT FOUND OR me NOT IN (s.host, s.guest) THEN RAISE EXCEPTION 'not a player'; END IF;
  IF s.status IN ('done', 'cancelled') THEN RETURN s; END IF;
  INSERT INTO tonight_votes (session_id, user_id, card, verdict) VALUES (p_session, me, p_card, p_verdict)
  ON CONFLICT (session_id, user_id, card) DO UPDATE SET verdict = EXCLUDED.verdict, created_at = NOW();
  IF me = s.host THEN
    UPDATE tonight_sessions SET host_progress = GREATEST(host_progress, p_card + 1), status = 'playing', updated_at = NOW()
    WHERE id = p_session RETURNING * INTO s;
  ELSE
    UPDATE tonight_sessions SET guest_progress = GREATEST(guest_progress, p_card + 1), status = 'playing', updated_at = NOW()
    WHERE id = p_session RETURNING * INTO s;
  END IF;
  IF p_verdict IN ('yes', 'love') THEN
    SELECT verdict INTO theirs FROM tonight_votes
    WHERE session_id = p_session AND card = p_card AND user_id = CASE WHEN me = s.host THEN s.guest ELSE s.host END;
    IF theirs IN ('yes', 'love') THEN
      UPDATE tonight_sessions
      SET status = 'done', winner = p_card,
          via = CASE WHEN p_verdict = 'love' AND theirs = 'love' THEN 'shared_love' ELSE 'match' END,
          updated_at = NOW()
      WHERE id = p_session RETURNING * INTO s;
    END IF;
  END IF;
  RETURN s;
END $$;

CREATE OR REPLACE FUNCTION public.tonight_undo(p_session UUID, p_card INT) RETURNS public.tonight_sessions
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  me UUID := auth.uid();
  s tonight_sessions;
BEGIN
  SELECT * INTO s FROM tonight_sessions WHERE id = p_session FOR UPDATE;
  IF NOT FOUND OR me NOT IN (s.host, s.guest) THEN RAISE EXCEPTION 'not a player'; END IF;
  IF s.status IN ('done', 'cancelled') THEN RETURN s; END IF;
  DELETE FROM tonight_votes WHERE session_id = p_session AND user_id = me AND card >= p_card;
  IF me = s.host THEN
    UPDATE tonight_sessions SET host_progress = LEAST(host_progress, p_card), updated_at = NOW() WHERE id = p_session RETURNING * INTO s;
  ELSE
    UPDATE tonight_sessions SET guest_progress = LEAST(guest_progress, p_card), updated_at = NOW() WHERE id = p_session RETURNING * INTO s;
  END IF;
  RETURN s;
END $$;

-- Once both players went through the deck, everyone's verdicts, for extra time.
CREATE OR REPLACE FUNCTION public.tonight_reveal(p_session UUID) RETURNS TABLE (user_id UUID, card INT, verdict TEXT)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT v.user_id, v.card, v.verdict
  FROM tonight_votes v JOIN tonight_sessions s ON s.id = v.session_id
  WHERE s.id = p_session AND auth.uid() IN (s.host, s.guest)
    AND s.host_progress >= jsonb_array_length(s.deck)
    AND s.guest_progress >= jsonb_array_length(s.deck);
$$;

-- Extra time: each player taps a poster. Same pick wins; otherwise chance decides between the two.
CREATE OR REPLACE FUNCTION public.tonight_pick(p_session UUID, p_card INT, p_a INT, p_b INT) RETURNS public.tonight_sessions
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  me UUID := auth.uid();
  s tonight_sessions;
BEGIN
  SELECT * INTO s FROM tonight_sessions WHERE id = p_session FOR UPDATE;
  IF NOT FOUND OR me NOT IN (s.host, s.guest) THEN RAISE EXCEPTION 'not a player'; END IF;
  IF s.winner IS NOT NULL OR s.status = 'cancelled' THEN RETURN s; END IF;
  IF me = s.host THEN
    UPDATE tonight_sessions SET host_pick = p_card, updated_at = NOW() WHERE id = p_session RETURNING * INTO s;
  ELSE
    UPDATE tonight_sessions SET guest_pick = p_card, updated_at = NOW() WHERE id = p_session RETURNING * INTO s;
  END IF;
  IF s.host_pick IS NOT NULL AND s.guest_pick IS NOT NULL THEN
    IF s.host_pick = s.guest_pick THEN
      UPDATE tonight_sessions SET winner = s.host_pick, via = 'extra_time', status = 'done', updated_at = NOW()
      WHERE id = p_session RETURNING * INTO s;
    ELSE
      UPDATE tonight_sessions
      SET winner = (ARRAY[p_a, p_b])[1 + floor(random() * 2)::INT], via = 'chance', status = 'done', updated_at = NOW()
      WHERE id = p_session RETURNING * INTO s;
    END IF;
  END IF;
  RETURN s;
END $$;

-- Chance decides at once among the given cards (the first call wins, the second gets the same result).
CREATE OR REPLACE FUNCTION public.tonight_chance(p_session UUID, p_cards INT[]) RETURNS public.tonight_sessions
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE s tonight_sessions;
BEGIN
  SELECT * INTO s FROM tonight_sessions WHERE id = p_session FOR UPDATE;
  IF NOT FOUND OR auth.uid() NOT IN (s.host, s.guest) THEN RAISE EXCEPTION 'not a player'; END IF;
  IF s.winner IS NOT NULL OR s.status = 'cancelled' OR array_length(p_cards, 1) IS NULL THEN RETURN s; END IF;
  UPDATE tonight_sessions
  SET winner = p_cards[1 + floor(random() * array_length(p_cards, 1))::INT], via = 'chance', status = 'done', updated_at = NOW()
  WHERE id = p_session RETURNING * INTO s;
  RETURN s;
END $$;

-- A new deck for the same evening: verdicts and picks start over.
CREATE OR REPLACE FUNCTION public.tonight_redeal(p_session UUID, p_deck JSONB) RETURNS public.tonight_sessions
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE s tonight_sessions;
BEGIN
  SELECT * INTO s FROM tonight_sessions WHERE id = p_session FOR UPDATE;
  IF NOT FOUND OR auth.uid() NOT IN (s.host, s.guest) THEN RAISE EXCEPTION 'not a player'; END IF;
  IF s.winner IS NOT NULL OR s.status = 'cancelled' THEN RETURN s; END IF;
  DELETE FROM tonight_votes WHERE session_id = p_session;
  UPDATE tonight_sessions
  SET deck = p_deck, round = round + 1, host_progress = 0, guest_progress = 0, host_pick = NULL, guest_pick = NULL,
      status = 'playing', updated_at = NOW()
  WHERE id = p_session RETURNING * INTO s;
  RETURN s;
END $$;

CREATE OR REPLACE FUNCTION public.tonight_cancel(p_session UUID) RETURNS VOID
LANGUAGE sql SECURITY DEFINER SET search_path = public AS $$
  UPDATE tonight_sessions SET status = 'cancelled', cancelled_by = auth.uid(), updated_at = NOW()
  WHERE id = p_session AND auth.uid() IN (host, guest) AND status IN ('invited', 'playing');
$$;

-- 4. Reactions to a friend's movie, series or book ---------------------------

CREATE TABLE IF NOT EXISTS public.reactions (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  from_user  UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  to_user    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  kind       TEXT NOT NULL CHECK (kind IN ('movie', 'series', 'book')),
  item_key   TEXT NOT NULL,
  item_title TEXT NOT NULL,
  reaction   TEXT NOT NULL CHECK (reaction IN ('culte', 'envie', 'merci')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (from_user, to_user, kind, item_key)
);
CREATE INDEX IF NOT EXISTS reactions_to_user_idx ON public.reactions (to_user, created_at DESC);
ALTER TABLE public.reactions ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Reactions: sender and receiver read" ON public.reactions;
CREATE POLICY "Reactions: sender and receiver read" ON public.reactions FOR SELECT
  USING (auth.uid() IN (from_user, to_user));
DROP POLICY IF EXISTS "Reactions: friends react" ON public.reactions;
CREATE POLICY "Reactions: friends react" ON public.reactions FOR INSERT
  WITH CHECK (from_user = auth.uid() AND public.are_friends(from_user, to_user));
DROP POLICY IF EXISTS "Reactions: change own" ON public.reactions;
CREATE POLICY "Reactions: change own" ON public.reactions FOR UPDATE
  USING (from_user = auth.uid()) WITH CHECK (from_user = auth.uid());
DROP POLICY IF EXISTS "Reactions: remove own" ON public.reactions;
CREATE POLICY "Reactions: remove own" ON public.reactions FOR DELETE USING (from_user = auth.uid());

-- Received reactions, with the name of who sent them.
CREATE OR REPLACE FUNCTION public.my_reactions() RETURNS TABLE (
  from_user UUID, display_name TEXT, kind TEXT, item_key TEXT, item_title TEXT, reaction TEXT, created_at TIMESTAMPTZ
)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public AS $$
  SELECT r.from_user, p.display_name, r.kind, r.item_key, r.item_title, r.reaction, r.created_at
  FROM reactions r JOIN profiles p ON p.id = r.from_user
  WHERE r.to_user = auth.uid() AND r.created_at > NOW() - INTERVAL '30 days'
  ORDER BY r.created_at DESC
  LIMIT 30;
$$;

-- 5. Social notifications can be turned off like the others ----------------------

ALTER TABLE public.push_subscriptions ADD COLUMN IF NOT EXISTS notify_social BOOLEAN NOT NULL DEFAULT TRUE;
