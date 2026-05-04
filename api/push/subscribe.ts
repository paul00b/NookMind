import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST' && req.method !== 'PATCH') {
    return res.status(405).end();
  }

  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing auth token' });
  }
  const token = authHeader.slice(7);
  const { data: { user }, error: authError } = await supabase.auth.getUser(token);
  if (authError || !user) {
    return res.status(401).json({ error: 'Invalid token' });
  }

  const { transport = 'webpush', subscription, fcm_token, notify_episodes, notify_seasons, notify_movies } = req.body as {
    transport?: 'webpush' | 'fcm';
    subscription?: PushSubscriptionJSON;
    fcm_token?: string;
    notify_episodes?: boolean;
    notify_seasons?: boolean;
    notify_movies?: boolean;
  };

  const prefs = {
    notify_episodes: notify_episodes ?? true,
    notify_seasons: notify_seasons ?? true,
    notify_movies: notify_movies ?? true,
    updated_at: new Date().toISOString(),
  };

  if (transport === 'fcm') {
    if (!fcm_token) {
      return res.status(400).json({ error: 'Missing fcm_token for transport=fcm' });
    }
    const { error } = await supabase
      .from('push_subscriptions')
      .upsert(
        { user_id: user.id, transport: 'fcm', fcm_token, ...prefs },
        { onConflict: 'user_id,fcm_token' }
      );
    if (error) {
      console.error('FCM subscribe upsert error', error);
      return res.status(500).json({ error: error.message });
    }
    return res.status(200).json({ ok: true });
  }

  // webpush (existing path)
  if (!subscription?.endpoint) {
    return res.status(400).json({ error: 'Missing subscription for transport=webpush' });
  }
  const { error } = await supabase
    .from('push_subscriptions')
    .upsert(
      { user_id: user.id, transport: 'webpush', subscription, ...prefs },
      { onConflict: 'user_id,endpoint' }
    );
  if (error) {
    console.error('webpush subscribe upsert error', error);
    return res.status(500).json({ error: error.message });
  }
  return res.status(200).json({ ok: true });
}
