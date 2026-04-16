import { useState, useEffect, useCallback } from 'react';
import { supabase } from '../lib/supabase';
import toast from 'react-hot-toast';

export interface NotificationPreferences {
  notify_episodes: boolean;
  notify_seasons: boolean;
  notify_movies: boolean;
}

export interface PushTestResult {
  ok: boolean;
  message: string;
}

const VAPID_PUBLIC_KEY = import.meta.env.VITE_VAPID_PUBLIC_KEY as string | undefined;

function urlBase64ToUint8Array(base64String: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = atob(base64);
  const buffer = new ArrayBuffer(rawData.length);
  const arr = new Uint8Array(buffer);
  for (let i = 0; i < rawData.length; i++) arr[i] = rawData.charCodeAt(i);
  return arr;
}

/** Résout quand le service worker est actif, ou null après timeout */
async function waitForSW(timeoutMs = 6000): Promise<ServiceWorkerRegistration | null> {
  return Promise.race([
    navigator.serviceWorker.ready,
    new Promise<null>(resolve => setTimeout(() => resolve(null), timeoutMs)),
  ]);
}

async function getAuthToken(): Promise<string | null> {
  const { data } = await supabase.auth.getSession();
  return data.session?.access_token ?? null;
}

async function callApi(
  path: string,
  method: string,
  body: object
): Promise<boolean> {
  const token = await getAuthToken();
  if (!token) return false;
  const res = await fetch(path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });
  return res.ok;
}

async function callApiJson<T>(
  path: string,
  method: string,
  body: object
): Promise<{ ok: boolean; data: T | null; status: number; rawText?: string }> {
  const token = await getAuthToken();
  if (!token) return { ok: false, data: null, status: 401, rawText: 'Missing auth token' };

  const res = await fetch(path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body),
  });

  const rawText = await res.text();
  let data: T | null = null;
  try {
    data = rawText ? (JSON.parse(rawText) as T) : null;
  } catch {
    data = null;
  }

  return { ok: res.ok, data, status: res.status, rawText };
}

export function useNotificationSubscription() {
  const supported =
    typeof window !== 'undefined' &&
    'Notification' in window &&
    'serviceWorker' in navigator &&
    'PushManager' in window;

  const [permission, setPermission] = useState<NotificationPermission>(
    supported ? Notification.permission : 'denied'
  );
  const [subscribed, setSubscribed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [preferences, setPreferences] = useState<NotificationPreferences>({
    notify_episodes: true,
    notify_seasons: true,
    notify_movies: true,
  });

  // Check if already subscribed on mount
  useEffect(() => {
    if (!supported) return;
    waitForSW(3000)
      .then((reg) => reg?.pushManager.getSubscription() ?? null)
      .then((sub) => setSubscribed(!!sub))
      .catch(() => {});
  }, [supported]);

  const subscribe = useCallback(async (): Promise<boolean> => {
    if (!supported) return false;
    if (!VAPID_PUBLIC_KEY) {
      toast.error('VAPID_PUBLIC_KEY manquante — vérifiez les variables d\'environnement.');
      console.error('[push] VITE_VAPID_PUBLIC_KEY is not set');
      return false;
    }
    setLoading(true);
    try {
      const perm = await Notification.requestPermission();
      setPermission(perm);
      if (perm !== 'granted') return false;

      const reg = await waitForSW(8000);
      if (!reg) {
        toast.error('Le service worker n\'est pas prêt. Rechargez la page et réessayez.');
        console.error('[push] serviceWorker.ready timed out');
        return false;
      }

      const sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
      });

      const ok = await callApi('/api/push/subscribe', 'POST', {
        subscription: sub.toJSON(),
        ...preferences,
      });

      if (!ok) {
        toast.error('Erreur lors de l\'enregistrement en base. Vérifiez les logs Vercel.');
        console.error('[push] /api/push/subscribe returned error');
      }

      if (ok) setSubscribed(true);
      return ok;
    } catch (err) {
      console.error('[push] subscribe error:', err);
      toast.error('Erreur : ' + (err instanceof Error ? err.message : String(err)));
      return false;
    } finally {
      setLoading(false);
    }
  }, [supported, preferences]);

  const unsubscribe = useCallback(async (): Promise<boolean> => {
    if (!supported) return false;
    setLoading(true);
    try {
      const reg = await waitForSW(6000);
      if (!reg) { setSubscribed(false); return false; }
      const sub = await reg.pushManager.getSubscription();
      if (!sub) { setSubscribed(false); return true; }

      await callApi('/api/push/unsubscribe', 'DELETE', { endpoint: sub.endpoint });
      await sub.unsubscribe();
      setSubscribed(false);
      return true;
    } catch (err) {
      console.error('[push] unsubscribe error:', err);
      return false;
    } finally {
      setLoading(false);
    }
  }, [supported]);

  const updatePreferences = useCallback(async (prefs: Partial<NotificationPreferences>): Promise<boolean> => {
    const next = { ...preferences, ...prefs };
    setPreferences(next);

    if (!subscribed) return true;

    const reg = await waitForSW(6000);
    if (!reg) return false;
    const sub = await reg.pushManager.getSubscription();
    if (!sub) return false;

    return callApi('/api/push/subscribe', 'PATCH', {
      subscription: sub.toJSON(),
      ...next,
    });
  }, [preferences, subscribed]);

  const sendTestNotification = useCallback(async (): Promise<PushTestResult> => {
    if (!supported) {
      return { ok: false, message: 'Notifications non supportees sur cet appareil.' };
    }

    const result = await callApiJson<{
      sent?: number;
      failed?: number;
      error?: string;
      results?: Array<{ statusCode: number | null; error?: string; details?: string; endpoint: string }>;
      diagnostics?: {
        serverPublicKey?: string | null;
        derivedServerPublicKey?: string | null;
        serverKeyPairMatches?: boolean;
        vapidContact?: string | null;
        endpointHosts?: string[];
      };
    }>(
      '/api/push/test',
      'POST',
      {}
    );

    if (!result.ok) {
      const message =
        result.data?.error ??
        (result.rawText ? `HTTP ${result.status} - ${result.rawText.slice(0, 220)}` : null) ??
        'Impossible d\'envoyer la notification de test.';
      toast.error(message);
      return { ok: false, message };
    }

    if ((result.data?.sent ?? 0) > 0) {
      toast.success('Notification de test envoyee.');
      return { ok: true, message: 'Notification de test envoyee.' };
    }

    const serverPublicKey = result.data?.diagnostics?.serverPublicKey ?? null;
    const serverKeyPairMatches = result.data?.diagnostics?.serverKeyPairMatches;
    if (serverKeyPairMatches === false) {
      const message = 'Mismatch VAPID sur le backend: la cle publique ne correspond pas a la cle privee lue par Vercel.';
      toast.error(message);
      return { ok: false, message };
    }

    if (serverPublicKey && VAPID_PUBLIC_KEY && serverPublicKey !== VAPID_PUBLIC_KEY) {
      const message = 'Mismatch VAPID: le backend et le front n utilisent pas la meme cle publique.';
      toast.error(message);
      return { ok: false, message };
    }

    const firstFailure = result.data?.results?.find((entry) => entry.error);
    if (firstFailure) {
      const details = [
        firstFailure.statusCode ? `HTTP ${firstFailure.statusCode}` : null,
        firstFailure.error,
        firstFailure.details,
        result.data?.diagnostics?.endpointHosts?.length
          ? `endpoint ${result.data.diagnostics.endpointHosts[0]}`
          : null,
      ].filter(Boolean).join(' - ');

      toast.error(details || 'La notification de test a ete rejetee.');
      console.error('[push] test rejection details', firstFailure);
      return { ok: false, message: details || 'La notification de test a ete rejetee.' };
    }

    const message = 'Aucun appareil abonne n\'a accepte la notification.';
    toast.error(message);
    return { ok: false, message };
  }, [supported]);

  return {
    supported,
    permission,
    subscribed,
    loading,
    preferences,
    subscribe,
    unsubscribe,
    updatePreferences,
    sendTestNotification,
  };
}
