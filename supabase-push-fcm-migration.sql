-- FCM push notifications support
-- Run in Supabase SQL editor (Dashboard → SQL Editor → New query)

-- 1. Make subscription nullable so FCM rows don't need a web-push blob
ALTER TABLE push_subscriptions
  ALTER COLUMN subscription DROP NOT NULL;

-- 2. Add transport discriminator (defaults to 'webpush' — no existing rows affected)
ALTER TABLE push_subscriptions
  ADD COLUMN IF NOT EXISTS transport text NOT NULL DEFAULT 'webpush'
    CHECK (transport IN ('webpush', 'fcm'));

-- 3. Add FCM token column (nullable; only set for transport = 'fcm')
ALTER TABLE push_subscriptions
  ADD COLUMN IF NOT EXISTS fcm_token text;

-- 4. Partial unique index: one FCM row per user per device token
--    (webpush uniqueness already covered by existing UNIQUE(user_id, endpoint))
CREATE UNIQUE INDEX IF NOT EXISTS push_subscriptions_fcm_unique
  ON push_subscriptions (user_id, fcm_token)
  WHERE fcm_token IS NOT NULL;
