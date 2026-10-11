import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import { initializeApp, getApps, cert } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';
import { applyCors } from '../_lib/cors.js';

// Social notifications: an invitation to « Ce soir ? », a friend request, an accepted request, a
// reaction. The app calls this route right after the matching write; every type is checked against
// the database first, so a caller can only notify about something that really happened.

const supabase = createClient(
  process.env.VITE_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

function getFirebaseApp() {
  if (getApps().length > 0) return getApps()[0]!;
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON ?? '{}');
  if (serviceAccount.private_key) {
    serviceAccount.private_key = serviceAccount.private_key.replace(/\\n/g, '\n');
  }
  return initializeApp({ credential: cert(serviceAccount) });
}

interface Message {
  to: string;
  title: string;
  body: string;
  route: string;
}

const RECENT_MS = 10 * 60 * 1000;

function firstName(name: string | null | undefined): string {
  const trimmed = (name ?? '').trim();
  return trimmed ? trimmed.split(/\s+/)[0]! : 'Un ami';
}

async function displayName(userId: string): Promise<string> {
  const { data } = await supabase.from('profiles').select('display_name').eq('id', userId).maybeSingle();
  return firstName(data?.display_name as string | undefined);
}

async function areFriends(a: string, b: string): Promise<boolean> {
  const { data } = await supabase.rpc('are_friends', { a, b });
  return data === true;
}

function reactionText(name: string, reaction: string, kind: string, title: string): string | null {
  switch (reaction) {
    case 'culte': return `${name} trouve « ${title} » culte`;
    case 'envie': return kind === 'book' ? `${name} a envie de lire « ${title} »` : `${name} a envie de voir « ${title} »`;
    case 'merci': return `${name} te dit merci pour « ${title} »`;
    default: return null;
  }
}

/** What to send for this request, or a reason to refuse it. */
async function buildMessage(callerId: string, body: Record<string, unknown>): Promise<Message | { error: string; status: number }> {
  const type = body.type;
  const name = await displayName(callerId);

  if (type === 'tonight_invite') {
    const { data: session } = await supabase
      .from('tonight_sessions')
      .select('id, host, guest, status, created_at')
      .eq('id', String(body.session_id ?? ''))
      .maybeSingle();
    if (!session || session.host !== callerId || session.status !== 'invited') return { error: 'No such invitation', status: 404 };
    return {
      to: session.guest as string,
      title: `${name} t'invite à « Ce soir ? »`,
      body: 'Chacun swipe sur son téléphone, le match tombe à la fin.',
      route: `/tonight/${session.id}`,
    };
  }

  if (type === 'friend_request') {
    const { data: target } = await supabase
      .from('profiles')
      .select('id')
      .eq('friend_code', String(body.code ?? '').trim().toUpperCase())
      .maybeSingle();
    if (!target) return { error: 'Unknown code', status: 404 };
    const { data: request } = await supabase
      .from('friendships')
      .select('id, created_at')
      .eq('requester', callerId)
      .eq('addressee', target.id)
      .eq('status', 'pending')
      .maybeSingle();
    if (!request || Date.now() - new Date(request.created_at as string).getTime() > RECENT_MS) {
      return { error: 'No recent request', status: 404 };
    }
    return { to: target.id as string, title: `${name} veut t'ajouter en ami`, body: 'Accepte pour voir ce que vous regardez et lisez.', route: '/social' };
  }

  if (type === 'friend_accepted') {
    let to = String(body.user_id ?? '');
    if (!to && body.code) {
      const { data: owner } = await supabase
        .from('profiles')
        .select('id')
        .eq('friend_code', String(body.code).trim().toUpperCase())
        .maybeSingle();
      to = (owner?.id as string | undefined) ?? '';
    }
    if (!to || !(await areFriends(callerId, to))) return { error: 'Not friends', status: 403 };
    return { to, title: `${name} a accepté ta demande`, body: 'Vous pouvez maintenant voir vos bibliothèques et jouer à « Ce soir ? ».', route: '/social' };
  }

  if (type === 'reaction') {
    const to = String(body.to_user ?? '');
    const { data: reaction } = await supabase
      .from('reactions')
      .select('kind, item_title, reaction, created_at')
      .eq('from_user', callerId)
      .eq('to_user', to)
      .eq('kind', String(body.kind ?? ''))
      .eq('item_key', String(body.item_key ?? ''))
      .maybeSingle();
    if (!reaction || Date.now() - new Date(reaction.created_at as string).getTime() > RECENT_MS) {
      return { error: 'No recent reaction', status: 404 };
    }
    const title = reactionText(name, reaction.reaction as string, reaction.kind as string, reaction.item_title as string);
    if (!title) return { error: 'Unknown reaction', status: 400 };
    return { to, title, body: 'Ouvre NookMind pour lui répondre.', route: '/social' };
  }

  return { error: 'Unknown type', status: 400 };
}

async function sendTo(message: Message): Promise<number> {
  const { data: rows } = await supabase
    .from('push_subscriptions')
    .select('fcm_token, notify_social')
    .eq('user_id', message.to)
    .eq('transport', 'fcm');
  const tokens = (rows ?? [])
    .filter((row) => row.fcm_token && row.notify_social !== false)
    .map((row) => row.fcm_token as string);
  if (!tokens.length) return 0;

  const app = getFirebaseApp();
  const results = await Promise.all(tokens.map(async (token) => {
    try {
      await getMessaging(app).send({
        token,
        notification: { title: message.title, body: message.body },
        data: { route: message.route },
        android: { priority: 'high', notification: { sound: 'default', channelId: 'default' } },
        apns: { payload: { aps: { sound: 'default' } } },
      });
      return true;
    } catch (err) {
      const error = err instanceof Error ? err.message : String(err);
      console.error('[social] FCM send failed', { error });
      if (error.includes('Requested entity was not found') || error.includes('registration-token-not-registered') || error.includes('invalid-registration-token')) {
        await supabase.from('push_subscriptions').delete().eq('fcm_token', token);
      }
      return false;
    }
  }));
  return results.filter(Boolean).length;
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  try {
    if (applyCors(req, res, ['POST'])) return;
    if (req.method !== 'POST') return res.status(405).end();

    const authHeader = req.headers.authorization;
    if (!authHeader?.startsWith('Bearer ')) return res.status(401).json({ error: 'Missing auth token' });
    const { data: { user }, error: authError } = await supabase.auth.getUser(authHeader.slice(7));
    if (authError || !user) return res.status(401).json({ error: 'Invalid token' });

    const message = await buildMessage(user.id, (req.body ?? {}) as Record<string, unknown>);
    if ('error' in message) return res.status(message.status).json({ error: message.error });

    const sent = await sendTo(message);
    return res.status(200).json({ ok: true, sent });
  } catch (err) {
    console.error('[social] notify failed', err);
    return res.status(500).json({ error: err instanceof Error ? err.message : 'Notify failed' });
  }
}
