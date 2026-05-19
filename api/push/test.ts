import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';
import crypto from 'node:crypto';
import webpush from 'web-push';
import * as admin from 'firebase-admin';
import { applyCors } from '../_lib/cors.js';

// Initialisation de Supabase
const supabase = createClient(
    process.env.VITE_SUPABASE_URL!,
    process.env.SUPABASE_SERVICE_ROLE_KEY!
);

// Initialisation de Firebase Admin (Singleton)
function getFirebaseApp(): admin.app.App {
    if (admin.apps.length > 0) return admin.apps[0]!;
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON ?? '{}') as admin.ServiceAccount;
    return admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
    });
}

interface PushAttemptResult {
    ok: boolean;
    statusCode: number | null;
    endpoint: string;
    error?: string;
    details?: string;
}

// --- Utilitaires ---

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
    if (!value) throw new Error(`Missing environment variable: ${name}`);
    return value.trim();
}

function getVapidConfig() {
    return {
        subject: cleanEnv('VAPID_CONTACT'),
        publicKey: cleanEnv('VITE_VAPID_PUBLIC_KEY'),
        privateKey: cleanEnv('VAPID_PRIVATE_KEY'),
    };
}

// --- Handler Principal ---

export default async function handler(req: VercelRequest, res: VercelResponse) {
    try {
        if (applyCors(req, res, ['POST'])) return;

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

        const { title, body, url, transport, token: fcmToken } = (req.body ?? {}) as {
            title?: string;
            body?: string;
            url?: string;
            transport?: 'webpush' | 'fcm';
            token?: string;
        };

        const payload = {
            title: title?.trim() || 'NookMind Test 🚀',
            body: body?.trim() || 'Si tu vois ceci, tes notifications fonctionnent !',
            url: url?.trim() || '/',
            tag: `test-${Date.now()}`,
        };

        if (transport === 'fcm' && fcmToken) {
            try {
                const app = getFirebaseApp();
                await admin.messaging(app).send({
                    token: fcmToken,
                    notification: {
                        title: payload.title,
                        body: payload.body,
                    },
                    data: {
                        url: payload.url,
                    },
                    android: {
                        priority: 'high',
                        notification: { sound: 'default' },
                    },
                    apns: {
                        payload: {
                            aps: { sound: 'default', badge: 1 },
                        },
                    },
                });

                return res.status(200).json({
                    ok: true,
                    sent: 1,
                    failed: 0,
                    results: [{ ok: true, endpoint: 'fcm-device', statusCode: 200 }],
                });
            } catch (err) {
                console.error('[push] FCM test failed', err);
                return res.status(500).json({
                    ok: false,
                    error: err instanceof Error ? err.message : 'FCM delivery failed'
                });
            }
        }

        let vapid: ReturnType<typeof getVapidConfig>;
        try {
            vapid = getVapidConfig();
            webpush.setVapidDetails(vapid.subject, vapid.publicKey, vapid.privateKey);
        } catch (err: unknown) {
            console.error('[push] invalid VAPID config', err);
            return res.status(500).json({ error: 'Invalid VAPID configuration' });
        }

        const { data: subscriptions, error: subError } = await supabase
            .from('push_subscriptions')
            .select('user_id, subscription, transport')
            .eq('user_id', user.id)
            .eq('transport', 'webpush');

        if (subError) return res.status(500).json({ error: subError.message });
        if (!subscriptions?.length) {
            return res.status(404).json({ error: 'No web push subscription found for this user' });
        }

        const results = await Promise.all(
            subscriptions.map(async (row) => {
                const sub = row.subscription as unknown as webpush.PushSubscription;
                const endpoint = sub.endpoint || 'unknown';

                try {
                    await webpush.sendNotification(sub, JSON.stringify(payload));
                    return { ok: true, statusCode: 201, endpoint } satisfies PushAttemptResult;
                } catch (err: unknown) {
                    const errorWithMeta = err as { statusCode?: number; body?: string };
                    const statusCode = errorWithMeta.statusCode ?? null;

                    if (statusCode === 404 || statusCode === 410) {
                        await supabase.from('push_subscriptions').delete().eq('endpoint', endpoint);
                    }

                    return {
                        ok: false,
                        statusCode,
                        endpoint,
                        error: err instanceof Error ? err.message : String(err),
                        details: errorWithMeta.body,
                    } satisfies PushAttemptResult;
                }
            })
        );

        const sent = results.filter((r) => r.ok).length;

        return res.status(200).json({
            ok: sent > 0,
            attempted: results.length,
            sent,
            failed: results.length - sent,
            diagnostics: {
                serverPublicKey: vapid.publicKey,
                derivedPublicKey: derivePublicKey(vapid.privateKey),
                endpointHosts: results.map(r => { try { return new URL(r.endpoint).host; } catch { return 'fcm'; } })
            },
            results,
        });
    } catch (err) {
        console.error('[push] unhandled test route failure', err);
        return res.status(500).json({
            ok: false,
            error: err instanceof Error ? err.message : 'Unhandled push test failure',
        });
    }
}
