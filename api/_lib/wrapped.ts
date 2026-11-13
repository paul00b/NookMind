import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import { initializeApp, getApps, cert, type App } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';

/**
 * "Ton année est prête": once a year (vercel.json cron, 1 December in the evening), a push to every
 * app user with notifications on and a few titles added this year. The tap opens the review
 * (route /wrapped). Native apps only: the web app has no review.
 *
 * Served by /api/push/send-daily?job=wrapped: the Hobby plan allows 12 functions a deployment, so
 * this one lives in _lib (not a route of its own). Manual runs, with the cron secret: `?dry=1` counts without sending, `?user=<uuid>` sends to that
 * account alone (to try it), `?year=2026` picks the year.
 */

const MIN_TITLES = 3;

function getFirebaseApp(): App {
  if (getApps().length > 0) return getApps()[0]!;
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON ?? '{}');
  if (serviceAccount.private_key) {
    serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, '\n');
  }
  return initializeApp({ credential: cert(serviceAccount) });
}

const supabase = createClient(process.env.VITE_SUPABASE_URL!, process.env.SUPABASE_SERVICE_ROLE_KEY!);

/** How many titles each of [userIds] added in [year], across the three libraries. */
async function titlesByUser(userIds: string[], year: number): Promise<Map<string, number>> {
  const counts = new Map<string, number>();
  const from = `${year}-01-01`;
  const to = `${year + 1}-01-01`;
  for (const table of ['movies', 'series', 'books']) {
    for (let page = 0; page < 100; page++) {
      const { data, error } = await supabase
        .from(table)
        .select('user_id')
        .in('user_id', userIds)
        .gte('created_at', from)
        .lt('created_at', to)
        .range(page * 1000, page * 1000 + 999);
      if (error) throw new Error(`${table}: ${error.message}`);
      for (const row of data ?? []) counts.set(row.user_id, (counts.get(row.user_id) ?? 0) + 1);
      if (!data || data.length < 1000) break;
    }
  }
  return counts;
}

export async function sendWrapped(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST' && req.method !== 'GET') return res.status(405).end();

  const secret = process.env.CRON_SECRET;
  if (secret && req.headers.authorization !== `Bearer ${secret}`) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  const now = new Date();
  // In December the year that ends; in January (a late run) the one just over.
  const defaultYear = now.getUTCMonth() === 11 ? now.getUTCFullYear() : now.getUTCFullYear() - 1;
  const year = Number(req.query.year) || defaultYear;
  const dry = req.query.dry === '1';
  const onlyUser = typeof req.query.user === 'string' ? req.query.user : null;

  let query = supabase.from('push_subscriptions').select('user_id, fcm_token').eq('transport', 'fcm').not('fcm_token', 'is', null);
  if (onlyUser) query = query.eq('user_id', onlyUser);
  const { data: subs, error: subError } = await query;
  if (subError) return res.status(500).json({ error: subError.message });
  if (!subs?.length) return res.status(200).json({ year, eligible: 0, sent: 0 });

  const userIds = [...new Set(subs.map((s) => s.user_id as string))];
  let counts: Map<string, number>;
  try {
    counts = await titlesByUser(userIds, year);
  } catch (err) {
    return res.status(500).json({ error: err instanceof Error ? err.message : String(err) });
  }

  // A test on one account goes through whatever its library holds.
  const eligible = subs.filter((s) => onlyUser || (counts.get(s.user_id) ?? 0) >= MIN_TITLES);
  if (dry) {
    return res.status(200).json({ year, users: userIds.length, eligibleDevices: eligible.length, eligibleUsers: new Set(eligible.map((s) => s.user_id)).size });
  }

  const title = `Ton année ${year} est prête`;
  const body = 'Ce que tu as lu et regardé cette année, en quelques écrans. Et une carte à partager.';
  const messaging = getMessaging(getFirebaseApp());
  let sent = 0;
  let failed = 0;
  await Promise.all(
    eligible.map(async (s) => {
      try {
        await messaging.send({
          token: s.fcm_token as string,
          notification: { title, body },
          data: { route: '/wrapped' },
          apns: { payload: { aps: { sound: 'default' } } },
          android: { priority: 'high', notification: { sound: 'default', channelId: 'default' } },
        });
        sent++;
      } catch (err) {
        failed++;
        console.error('[wrapped] send failed', { error: err instanceof Error ? err.message : String(err) });
      }
    }),
  );

  console.info('[wrapped] summary', { year, eligible: eligible.length, sent, failed });
  return res.status(200).json({ year, eligible: eligible.length, sent, failed });
}
