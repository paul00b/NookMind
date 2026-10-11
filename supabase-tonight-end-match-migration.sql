-- ============================================================
-- NookMind: Ce soir ? online, the match is made at the end
-- Run in: Supabase Dashboard > SQL Editor > New query. Safe to run more than once.
-- Needs supabase-social-migration.sql first.
--
-- Before: the first movie both players said yes to won at once, mid-deck.
-- Now: votes are only recorded; once both went through the deck, the app reveals the
-- verdicts, ranks the shared yeses (shared favourites first) and settles the winner.
-- ============================================================

CREATE OR REPLACE FUNCTION public.tonight_vote(p_session UUID, p_card INT, p_verdict TEXT) RETURNS public.tonight_sessions
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  me UUID := auth.uid();
  s tonight_sessions;
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
  RETURN s;
END $$;

-- The winner both phones computed from the revealed verdicts (the first call wins, the second
-- gets the same result back).
CREATE OR REPLACE FUNCTION public.tonight_settle(p_session UUID, p_card INT, p_via TEXT) RETURNS public.tonight_sessions
LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE s tonight_sessions;
BEGIN
  SELECT * INTO s FROM tonight_sessions WHERE id = p_session FOR UPDATE;
  IF NOT FOUND OR auth.uid() NOT IN (s.host, s.guest) THEN RAISE EXCEPTION 'not a player'; END IF;
  IF s.winner IS NOT NULL OR s.status = 'cancelled' THEN RETURN s; END IF;
  UPDATE tonight_sessions SET winner = p_card, via = p_via, status = 'done', updated_at = NOW()
  WHERE id = p_session RETURNING * INTO s;
  RETURN s;
END $$;
