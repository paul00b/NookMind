import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import webpush from 'web-push';
import * as admin from 'firebase-admin';

function getFirebaseApp(): admin.app.App {
  if (admin.apps.length > 0) return admin.apps[0]!;
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON ?? '{}') as admin.ServiceAccount;
  return admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}

const supabase = createClient(
  process.env.VITE_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

interface PushSubscriptionRow {
  user_id: string;
  transport: 'webpush' | 'fcm';
  subscription: PushSubscriptionJSON | null;
  fcm_token: string | null;
  notify_episodes: boolean;
  notify_seasons: boolean;
  notify_movies: boolean;
}

interface SeriesRow {
  id: string;
  user_id: string;
  title: string;
  status: string;
  next_air_date: string | null;
  next_season_number: number | null;
}

interface MovieRow {
  id: string;
  user_id: string;
  title: string;
  release_date: string | null;
}

interface PushSendResult {
  ok: boolean;
  statusCode: number | null;
  endpoint: string;
  error?: string;
  details?: string;
}

function cleanEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing environment variable: ${name}`);
  }

  return value.trim();
}

function configureWebPush() {
  webpush.setVapidDetails(
    cleanEnv('VAPID_CONTACT'),
    cleanEnv('VITE_VAPID_PUBLIC_KEY'),
    cleanEnv('VAPID_PRIVATE_KEY')
  );
}

async function sendFcm(fcmToken: string, payload: { title: string; body: string }): Promise<PushSendResult> {
  try {
    const app = getFirebaseApp();
    await admin.messaging(app).send({
      token: fcmToken,
      notification: { title: payload.title, body: payload.body },
      data: { route: '/library' },
      apns: { payload: { aps: { sound: 'default' } } },
      android: { priority: 'high' },
    });
    return { ok: true, statusCode: 200, endpoint: fcmToken.slice(0, 20) + '…' };
  } catch (err) {
    return { ok: false, statusCode: null, endpoint: fcmToken.slice(0, 20) + '…', error: err instanceof Error ? err.message : String(err) };
  }
}

async function sendPush(subscription: PushSubscriptionJSON, payload: object): Promise<PushSendResult> {
  const endpoint = (subscription as { endpoint?: string }).endpoint ?? 'unknown-endpoint';

  try {
    await webpush.sendNotification(subscription as Parameters<typeof webpush.sendNotification>[0], JSON.stringify(payload));
    return { ok: true, statusCode: 201, endpoint };
  } catch (err: unknown) {
    const errorWithMeta = err as { statusCode?: number; body?: string };
    const status = errorWithMeta.statusCode;
    const message = err instanceof Error ? err.message : String(err);
    const details = errorWithMeta.body;

    console.error('[push] send failed', {
      endpoint,
      statusCode: status ?? null,
      error: message,
      details: details ?? null,
    });

    // 404/410 = subscription expired → clean up
    if (status === 404 || status === 410) {
      await supabase
        .from('push_subscriptions')
        .delete()
        .eq('endpoint', (subscription as { endpoint: string }).endpoint);
    }

    return {
      ok: false,
      statusCode: status ?? null,
      endpoint,
      error: message,
      details,
    };
  }
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  try {
    configureWebPush();
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    console.error('[push] invalid VAPID config', { error: message });
    return res.status(500).json({ error: message });
  }

  if (req.method !== 'POST' && req.method !== 'GET') return res.status(405).end();

  // Vercel cron sends Authorization header with CRON_SECRET
  const secret = process.env.CRON_SECRET;
  const authHeader = req.headers.authorization;
  if (secret && authHeader !== `Bearer ${secret}`) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD

  // Fetch all push subscriptions
  const { data: subscriptions, error: subError } = await supabase
    .from('push_subscriptions')
    .select('user_id, transport, subscription, fcm_token, notify_episodes, notify_seasons, notify_movies');

  if (subError || !subscriptions?.length) {
    return res.status(200).json({ sent: 0 });
  }

  const subsByUser = new Map<string, PushSubscriptionRow[]>();
  for (const sub of subscriptions as PushSubscriptionRow[]) {
    const list = subsByUser.get(sub.user_id) ?? [];
    list.push(sub);
    subsByUser.set(sub.user_id, list);
  }

  const userIds = [...subsByUser.keys()];

  // Fetch series releasing today (watching + watched)
  const { data: seriesRows } = await supabase
    .from('series')
    .select('id, user_id, title, status, next_air_date, next_season_number')
    .in('user_id', userIds)
    .eq('next_air_date', today)
    .in('status', ['watching', 'watched']);

  // Fetch movies releasing today (want_to_watch)
  const { data: movieRows } = await supabase
    .from('movies')
    .select('id, user_id, title, release_date')
    .in('user_id', userIds)
    .eq('release_date', today)
    .eq('status', 'want_to_watch');

  const sends: Promise<void>[] = [];
  const results: PushSendResult[] = [];

  for (const series of (seriesRows ?? []) as SeriesRow[]) {
    const userSubs = subsByUser.get(series.user_id) ?? [];
    for (const sub of userSubs) {
      const isNewSeason = series.status === 'watched';
      if (isNewSeason && !sub.notify_seasons) continue;
      if (!isNewSeason && !sub.notify_episodes) continue;

      const title = isNewSeason
        ? `📺 ${series.title} · Saison ${series.next_season_number ?? ''}`
        : `📺 ${series.title}`;
      const body = isNewSeason
        ? 'Une nouvelle saison est disponible !'
        : 'Un nouvel épisode est disponible maintenant.';

      const seriesPayload = { title, body, url: '/', tag: `series-${series.id}` };
      sends.push(
        (sub.transport === 'fcm' && sub.fcm_token
          ? sendFcm(sub.fcm_token, { title, body })
          : sendPush(sub.subscription!, seriesPayload)
        ).then((result) => { results.push(result); })
      );
    }
  }

  for (const movie of (movieRows ?? []) as MovieRow[]) {
    const userSubs = subsByUser.get(movie.user_id) ?? [];
    for (const sub of userSubs) {
      if (!sub.notify_movies) continue;
      const movieTitle = `🎬 ${movie.title}`;
      const movieBody = "Le film sort en salles aujourd'hui !";
      sends.push(
        (sub.transport === 'fcm' && sub.fcm_token
          ? sendFcm(sub.fcm_token, { title: movieTitle, body: movieBody })
          : sendPush(sub.subscription!, { title: movieTitle, body: movieBody, url: '/', tag: `movie-${movie.id}` })
        ).then((result) => { results.push(result); })
      );
    }
  }

  await Promise.all(sends);

  const sent = results.filter((result) => result.ok).length;
  const failed = results.length - sent;

  console.info('[push] daily summary', {
    date: today,
    attempted: results.length,
    sent,
    failed,
  });

  return res.status(200).json({ ok: true, date: today, attempted: results.length, sent, failed });
}
