import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';

const admin = createClient(
  process.env.VITE_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!,
  { auth: { autoRefreshToken: false, persistSession: false } },
);

const TABLES_WITH_USER_ID = [
  'books',
  'movies',
  'movie_categories',
  'series',
  'series_categories',
  'push_subscriptions',
] as const;

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).end();
  }

  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing auth token' });
  }
  const token = authHeader.slice(7);

  const { data: { user }, error: authError } = await admin.auth.getUser(token);
  if (authError || !user) {
    return res.status(401).json({ error: 'Invalid token' });
  }
  const userId = user.id;

  for (const table of TABLES_WITH_USER_ID) {
    const { error } = await admin.from(table).delete().eq('user_id', userId);
    if (error) {
      console.warn(`delete from ${table} failed`, error.message);
    }
  }

  const { error: delErr } = await admin.auth.admin.deleteUser(userId);
  if (delErr) {
    return res.status(500).json({ error: `Failed to delete auth user: ${delErr.message}` });
  }

  return res.status(200).json({ ok: true });
}
