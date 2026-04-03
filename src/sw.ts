/// <reference lib="webworker" />
import { clientsClaim } from 'workbox-core';
import { precacheAndRoute } from 'workbox-precaching';
import { registerRoute } from 'workbox-routing';
import { NetworkOnly } from 'workbox-strategies';

declare const self: ServiceWorkerGlobalScope;

precacheAndRoute(self.__WB_MANIFEST);
clientsClaim();
self.skipWaiting();

// External APIs → réseau uniquement, jamais mis en cache
registerRoute(
  ({ url }) => url.hostname.includes('supabase.co'),
  new NetworkOnly()
);
registerRoute(
  ({ url }) => url.hostname.includes('googleapis.com'),
  new NetworkOnly()
);
registerRoute(
  ({ url }) => url.hostname.includes('api.themoviedb.org'),
  new NetworkOnly()
);

// ── Push notification handler ──────────────────────────────────────────────

interface PushPayload {
  title: string;
  body: string;
  url?: string;
  tag?: string;
}

self.addEventListener('push', (event) => {
  const data = event.data?.json() as PushPayload | undefined;
  if (!data?.title) return;

  event.waitUntil(
    self.registration.showNotification(data.title, {
      body: data.body,
      icon: '/pwa-icon.svg',
      badge: '/pwa-icon.svg',
      data: { url: data.url ?? '/' },
      tag: data.tag,
    })
  );
});

// ── Notification click handler ─────────────────────────────────────────────

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = (event.notification.data as { url: string })?.url ?? '/';

  event.waitUntil(
    self.clients
      .matchAll({ type: 'window', includeUncontrolled: true })
      .then((clientList) => {
        const existing = clientList.find((c) => 'focus' in c);
        if (existing) {
          void existing.navigate(url);
          return existing.focus();
        }
        return self.clients.openWindow(url);
      })
  );
});
