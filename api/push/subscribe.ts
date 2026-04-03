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

  // Authenticate user via Supabase JWT
  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing auth token' });
  }
  const token = authHeader.slice(7);
  const { data: { user }, error: authError } = await supabase.auth.getUser(token);
  if (authError || !user) {
    return res.status(401).json({ error: 'Invalid token' });
  }

  const { subscription, notify_episodes, notify_seasons, notify_movies } = req.body as {
    subscription: PushSubscriptionJSON;
    notify_episodes?: boolean;
    notify_seasons?: boolean;
    notify_movies?: boolean;
  };

  if (!subscription?.endpoint) {
    return res.status(400).json({ error: 'Missing subscription' });
  }

  const { error } = await supabase
    .from('push_subscriptions')
    .upsert(
      {
        user_id: user.id,
        subscription,
        notify_episodes: notify_episodes ?? true,
        notify_seasons: notify_seasons ?? true,
        notify_movies: notify_movies ?? true,
        updated_at: new Date().toISOString(),
      },
      { onConflict: 'user_id,endpoint' }
    );

  if (error) {
    console.error('subscribe upsert error', error);
    return res.status(500).json({ error: error.message });
  }

  return res.status(200).json({ ok: true });
}
