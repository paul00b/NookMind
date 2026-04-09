import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import webpush from 'web-push';

webpush.setVapidDetails(
  process.env.VAPID_CONTACT!,
  process.env.VITE_VAPID_PUBLIC_KEY!,
  process.env.VAPID_PRIVATE_KEY!
);

const supabase = createClient(
  process.env.VITE_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

interface PushSubscriptionRow {
  user_id: string;
  subscription: PushSubscriptionJSON;
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
}

async function sendPush(subscription: PushSubscriptionJSON, payload: object): Promise<PushSendResult> {
  const endpoint = (subscription as { endpoint?: string }).endpoint ?? 'unknown-endpoint';

  try {
    await webpush.sendNotification(subscription as Parameters<typeof webpush.sendNotification>[0], JSON.stringify(payload));
    return { ok: true, statusCode: 201, endpoint };
  } catch (err: unknown) {
    const status = (err as { statusCode?: number }).statusCode;
    const message = err instanceof Error ? err.message : String(err);

    console.error('[push] send failed', {
      endpoint,
      statusCode: status ?? null,
      error: message,
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
    };
  }
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
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
    .select('user_id, subscription, notify_episodes, notify_seasons, notify_movies');

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

      sends.push(
        sendPush(sub.subscription, {
          title,
          body,
          url: '/',
          tag: `series-${series.id}`,
        }).then((result) => {
          results.push(result);
        })
      );
    }
  }

  for (const movie of (movieRows ?? []) as MovieRow[]) {
    const userSubs = subsByUser.get(movie.user_id) ?? [];
    for (const sub of userSubs) {
      if (!sub.notify_movies) continue;
      sends.push(
        sendPush(sub.subscription, {
          title: `🎬 ${movie.title}`,
          body: 'Le film sort en salles aujourd\'hui !',
          url: '/',
          tag: `movie-${movie.id}`,
        }).then((result) => {
          results.push(result);
        })
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
