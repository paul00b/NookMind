import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import crypto from 'node:crypto';
import webpush from 'web-push';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

interface PushSubscriptionRow {
  user_id: string;
  subscription: PushSubscriptionJSON;
}

interface PushAttemptResult {
  ok: boolean;
  statusCode: number | null;
  endpoint: string;
  error?: string;
  details?: string;
}

function base64UrlToBuffer(value: string): Buffer {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
  return Buffer.from(padded, 'base64');
}

function derivePublicKey(privateKey: string): string | null {
  try {
    const ecdh = crypto.createECDH('prime256v1');
    ecdh.setPrivateKey(base64UrlToBuffer(privateKey.trim()));
    return ecdh
      .getPublicKey()
      .toString('base64')
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  } catch {
    return null;
  }
}

function cleanEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing environment variable: ${name}`);
  }

  return value.trim();
}

function getVapidConfig() {
  return {
    subject: cleanEnv('VAPID_CONTACT'),
    publicKey: cleanEnv('VITE_VAPID_PUBLIC_KEY'),
    privateKey: cleanEnv('VAPID_PRIVATE_KEY'),
  };
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  try {
    const vapid = getVapidConfig();
    webpush.setVapidDetails(vapid.subject, vapid.publicKey, vapid.privateKey);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : String(err);
    console.error('[push] invalid VAPID config', { error: message });
    return res.status(500).json({ error: message });
  }

  if (req.method !== 'POST') return res.status(405).end();

  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing auth token' });
  }

  const token = authHeader.slice(7);
  const { data: { user }, error: authError } = await supabase.auth.getUser(token);
  if (authError || !user) {
    return res.status(401).json({ error: 'Invalid token' });
  }

  const { title, body, url } = (req.body ?? {}) as {
    title?: string;
    body?: string;
    url?: string;
  };

  const { data: subscriptions, error: subError } = await supabase
    .from('push_subscriptions')
    .select('user_id, subscription')
    .eq('user_id', user.id);

  if (subError) {
    console.error('[push] test fetch subscriptions failed', {
      userId: user.id,
      error: subError.message,
    });
    return res.status(500).json({ error: subError.message });
  }

  if (!subscriptions?.length) {
    return res.status(404).json({ error: 'No push subscription found for this user' });
  }

  const payload = {
    title: title?.trim() || 'NookMind test',
    body: body?.trim() || 'Si tu vois ceci, les notifications iOS fonctionnent.',
    url: url?.trim() || '/',
    tag: `test-${Date.now()}`,
  };

  const vapid = getVapidConfig();
  const serverPublicKey = vapid.publicKey;
  const serverPrivateKey = vapid.privateKey;
  const vapidContact = vapid.subject;
  const derivedServerPublicKey = serverPrivateKey ? derivePublicKey(serverPrivateKey) : null;

  const results = await Promise.all(
    (subscriptions as PushSubscriptionRow[]).map(async ({ subscription }) => {
      const endpoint = (subscription as { endpoint?: string }).endpoint ?? 'unknown-endpoint';

      try {
        await webpush.sendNotification(
          subscription as Parameters<typeof webpush.sendNotification>[0],
          JSON.stringify(payload)
        );

        return { ok: true, statusCode: 201, endpoint } satisfies PushAttemptResult;
      } catch (err: unknown) {
        const errorWithMeta = err as { statusCode?: number; body?: string };
        const statusCode = errorWithMeta.statusCode ?? null;
        const error = err instanceof Error ? err.message : String(err);
        const details = errorWithMeta.body;

        console.error('[push] test send failed', {
          userId: user.id,
          endpoint,
          statusCode,
          error,
          details: details ?? null,
        });

        if (statusCode === 404 || statusCode === 410) {
          await supabase
            .from('push_subscriptions')
            .delete()
            .eq('endpoint', endpoint);
        }

        return { ok: false, statusCode, endpoint, error, details } satisfies PushAttemptResult;
      }
    })
  );

  const sent = results.filter((result) => result.ok).length;
  const failed = results.length - sent;

  return res.status(200).json({
    ok: sent > 0,
    attempted: results.length,
    sent,
    failed,
    payload,
    diagnostics: {
      serverPublicKey,
      derivedServerPublicKey,
      serverKeyPairMatches: !!serverPublicKey && !!derivedServerPublicKey && serverPublicKey === derivedServerPublicKey,
      vapidContact,
      endpointHosts: results.map(({ endpoint }) => {
        try {
          return new URL(endpoint).host;
        } catch {
          return 'invalid-endpoint';
        }
      }),
    },
    results,
  });
}
