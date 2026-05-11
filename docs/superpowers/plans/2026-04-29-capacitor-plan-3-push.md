# Capacitor Push Notifications (FCM) — Plan 3 of 5

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Firebase Cloud Messaging (FCM) push notifications to the native apps while keeping the existing Web Push flow unchanged for the browser PWA.

**Architecture:** `@capacitor/push-notifications` handles FCM token registration on-device. A new `useNativePushSubscription` hook mirrors `useNotificationSubscription` but sends an FCM token instead of a Web Push subscription blob. The `push_subscriptions` table gains `transport` and `fcm_token` columns. The existing `send-daily` dispatcher gains a Firebase Admin SDK branch that fires FCM messages alongside the current `web-push` path. `NotificationPromptSheet` uses the native hook when running inside Capacitor. The web path is untouched.

**Tech Stack:** `@capacitor/push-notifications`, `firebase-admin` (Vercel function), React 19, Supabase, existing `send-daily.ts` cron.

**Spec reference:** `docs/superpowers/specs/2026-04-27-capacitor-native-app-design.md` — Section 7.

---

## File map

**Created:**
- `supabase-push-fcm-migration.sql` — adds `transport` + `fcm_token` columns, makes `subscription` nullable, adds partial unique index
- `src/lib/nativePush.ts` — FCM token request + register helpers (safe to call on web: no-op)
- `src/lib/nativePush.test.ts` — unit tests

**Modified:**
- `package.json` — add `@capacitor/push-notifications`
- `api/push/subscribe.ts` — accept `transport: 'fcm'` body variant
- `api/push/send-daily.ts` — add Firebase Admin SDK FCM dispatch branch
- `src/components/NotificationPromptSheet.tsx` — use native hook on native, web hook on web
- `src/lib/nativeBoot.ts` — register FCM listeners on native boot
- `ios/App/App/Info.plist` — `NSUserNotificationsUsageDescription` key
- `android/app/google-services.json` — placed by user (manual prereq)
- `ios/App/App/GoogleService-Info.plist` — placed by user (manual prereq)

**Untouched (intentionally):**
- `useNotificationSubscription.ts` — web push hook unchanged
- `api/push/unsubscribe.ts` — endpoint unchanged (webpush only; FCM unsubscribe is handled by DELETE on the row)
- `api/push/test.ts` — left as webpush-only for now

---

## Prerequisites — manual (do before Task 5)

These cannot be automated. Tasks 1–4 can run before any of this is done.

### A. Firebase project + app registration

1. Go to https://console.firebase.google.com → **Add project** → name: `nookmind`.
2. **Android app**: package `fr.paulbr.nookmind` → download `google-services.json` → place at `android/app/google-services.json`.
3. **iOS app**: bundle ID `fr.paulbr.nookmind` → download `GoogleService-Info.plist` → place at `ios/App/App/GoogleService-Info.plist`.
4. In Firebase Console → Project Settings → Cloud Messaging → generate or confirm a **Server key** (legacy) is present (you won't use it directly, but it confirms FCM is active).

### B. APNs Authentication Key → Firebase (iOS only)

1. Apple Developer → **Keys** → create new key with **Apple Push Notifications service (APNs)** enabled → download `.p8`.
2. Firebase Console → Project Settings → Cloud Messaging → **Apple app configuration** → upload the `.p8` key. Provide Team ID and Key ID.

This allows Firebase to relay push messages through APNs to iOS devices.

### C. Firebase service account → Vercel env

1. Firebase Console → Project Settings → **Service accounts** → **Generate new private key** → download JSON.
2. Stringify the entire JSON (one line): `jq -c . < service-account.json | pbcopy` on Mac.
3. Add to Vercel: `FIREBASE_SERVICE_ACCOUNT_JSON=<stringified json>`.
4. Add to local `.env` as well for local testing.

---

## Task 1: Install `@capacitor/push-notifications`

**Files:**
- Modify: `package.json`

- [ ] **Step 1: Install**

```bash
npm install @capacitor/push-notifications
```

Expected: added to `dependencies`. No peer dep errors (it's a first-party Capacitor plugin).

- [ ] **Step 2: Sync to native**

```bash
npm run cap:sync
```

Expected: `@capacitor/push-notifications` listed under both iOS and Android plugins. `[info] All plugins have a Package.swift file` on iOS.

- [ ] **Step 3: Commit**

```bash
git add package.json package-lock.json android/ ios/
git commit -m "feat(push): install @capacitor/push-notifications

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Install `firebase-admin` (server-side only)

**Files:**
- Modify: `package.json`

- [ ] **Step 1: Install**

```bash
npm install firebase-admin
```

Expected: added to `dependencies`.

- [ ] **Step 2: Verify build still works**

```bash
npm run build
```

Expected: build succeeds. `firebase-admin` is a Node package; Vite won't bundle it into the frontend because it's only imported in `api/` files.

- [ ] **Step 3: Commit**

```bash
git add package.json package-lock.json
git commit -m "feat(push): install firebase-admin for FCM server dispatch

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Database migration

**Files:**
- Create: `supabase-push-fcm-migration.sql`

- [ ] **Step 1: Create the migration file**

Create `supabase-push-fcm-migration.sql`:

```sql
-- FCM push notifications support
-- Run in Supabase SQL editor (Dashboard → SQL Editor → New query)

-- 1. Make subscription nullable so FCM rows don't need a web-push blob
ALTER TABLE push_subscriptions
  ALTER COLUMN subscription DROP NOT NULL;

-- 2. Add transport discriminator (defaults to 'webpush' — no existing rows affected)
ALTER TABLE push_subscriptions
  ADD COLUMN IF NOT EXISTS transport text NOT NULL DEFAULT 'webpush'
    CHECK (transport IN ('webpush', 'fcm'));

-- 3. Add FCM token column (nullable; only set for transport = 'fcm')
ALTER TABLE push_subscriptions
  ADD COLUMN IF NOT EXISTS fcm_token text;

-- 4. Partial unique index: one FCM row per user per device token
--    (webpush uniqueness already covered by existing UNIQUE(user_id, endpoint))
CREATE UNIQUE INDEX IF NOT EXISTS push_subscriptions_fcm_unique
  ON push_subscriptions (user_id, fcm_token)
  WHERE fcm_token IS NOT NULL;
```

- [ ] **Step 2: Run in Supabase**

Open Supabase Dashboard → SQL Editor → paste and run the migration. Verify: no errors. Existing webpush rows are unaffected (`transport` defaults to `'webpush'`, `fcm_token` is `NULL`).

- [ ] **Step 3: Commit the migration file**

```bash
git add supabase-push-fcm-migration.sql
git commit -m "feat(push): add transport + fcm_token columns to push_subscriptions

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: `nativePush.ts` helper (TDD)

**Files:**
- Create: `src/lib/nativePush.ts`
- Create: `src/lib/nativePush.test.ts`

- [ ] **Step 1: Write failing tests**

Create `src/lib/nativePush.test.ts`:

```ts
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest';

vi.mock('@capacitor/push-notifications', () => ({
  PushNotifications: {
    requestPermissions: vi.fn(),
    register: vi.fn(),
    addListener: vi.fn(),
    removeAllListeners: vi.fn(),
  },
}));

vi.mock('./platform', () => ({
  isNative: vi.fn(),
}));

import { PushNotifications } from '@capacitor/push-notifications';
import { isNative } from './platform';
import { requestNativePushPermission, getNativeFcmToken } from './nativePush';

describe('requestNativePushPermission', () => {
  beforeEach(() => vi.clearAllMocks());

  it('returns false on web without calling plugin', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const result = await requestNativePushPermission();
    expect(result).toBe(false);
    expect(PushNotifications.requestPermissions).not.toHaveBeenCalled();
  });

  it('returns true when permission is granted on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(PushNotifications.requestPermissions).mockResolvedValue({ receive: 'granted' });
    const result = await requestNativePushPermission();
    expect(result).toBe(true);
  });

  it('returns false when permission is denied on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(PushNotifications.requestPermissions).mockResolvedValue({ receive: 'denied' });
    const result = await requestNativePushPermission();
    expect(result).toBe(false);
  });
});

describe('getNativeFcmToken', () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(() => vi.clearAllMocks());

  it('returns null on web', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const token = await getNativeFcmToken();
    expect(token).toBeNull();
  });

  it('returns FCM token on native when registration succeeds', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(PushNotifications.register).mockResolvedValue(undefined);
    vi.mocked(PushNotifications.addListener).mockImplementation((event, cb) => {
      if (event === 'registration') {
        // Simulate async token delivery
        setTimeout(() => (cb as (data: { value: string }) => void)({ value: 'fake-fcm-token' }), 0);
      }
      return Promise.resolve({ remove: vi.fn() });
    });

    const token = await getNativeFcmToken();
    expect(token).toBe('fake-fcm-token');
    expect(PushNotifications.removeAllListeners).toHaveBeenCalled();
  });

  it('returns null when registration throws', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(PushNotifications.register).mockRejectedValue(new Error('Registration failed'));
    vi.mocked(PushNotifications.addListener).mockResolvedValue({ remove: vi.fn() });

    const token = await getNativeFcmToken();
    expect(token).toBeNull();
  });
});
```

- [ ] **Step 2: Run — verify failing**

```bash
npm run test -- nativePush
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement `src/lib/nativePush.ts`**

```ts
import { PushNotifications } from '@capacitor/push-notifications';
import { isNative } from './platform';

export async function requestNativePushPermission(): Promise<boolean> {
  if (!isNative()) return false;
  const { receive } = await PushNotifications.requestPermissions();
  return receive === 'granted';
}

export async function getNativeFcmToken(): Promise<string | null> {
  if (!isNative()) return null;
  return new Promise<string | null>((resolve) => {
    let settled = false;

    PushNotifications.addListener('registration', ({ value }) => {
      if (!settled) {
        settled = true;
        PushNotifications.removeAllListeners();
        resolve(value);
      }
    });

    PushNotifications.addListener('registrationError', () => {
      if (!settled) {
        settled = true;
        PushNotifications.removeAllListeners();
        resolve(null);
      }
    });

    PushNotifications.register().catch(() => {
      if (!settled) {
        settled = true;
        PushNotifications.removeAllListeners();
        resolve(null);
      }
    });
  });
}
```

- [ ] **Step 4: Run — verify passing**

```bash
npm run test -- nativePush
```

Expected: PASS — 5 tests passing.

- [ ] **Step 5: Commit**

```bash
git add src/lib/nativePush.ts src/lib/nativePush.test.ts
git commit -m "feat(push): add nativePush helpers for FCM token registration

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Update `api/push/subscribe.ts` — accept FCM

**Files:**
- Modify: `api/push/subscribe.ts`

The existing handler accepts `{ subscription: PushSubscriptionJSON, notify_* }`. We need to also accept `{ transport: 'fcm', fcm_token: string, notify_* }`.

- [ ] **Step 1: Replace the handler**

Rewrite `api/push/subscribe.ts`:

```ts
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

  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Missing auth token' });
  }
  const token = authHeader.slice(7);
  const { data: { user }, error: authError } = await supabase.auth.getUser(token);
  if (authError || !user) {
    return res.status(401).json({ error: 'Invalid token' });
  }

  const { transport = 'webpush', subscription, fcm_token, notify_episodes, notify_movies, notify_seasons } = req.body as {
    transport?: 'webpush' | 'fcm';
    subscription?: PushSubscriptionJSON;
    fcm_token?: string;
    notify_episodes?: boolean;
    notify_movies?: boolean;
    notify_seasons?: boolean;
  };

  const prefs = {
    notify_episodes: notify_episodes ?? true,
    notify_seasons: notify_seasons ?? true,
    notify_movies: notify_movies ?? true,
    updated_at: new Date().toISOString(),
  };

  if (transport === 'fcm') {
    if (!fcm_token) {
      return res.status(400).json({ error: 'Missing fcm_token for transport=fcm' });
    }
    const { error } = await supabase
      .from('push_subscriptions')
      .upsert(
        { user_id: user.id, transport: 'fcm', fcm_token, ...prefs },
        { onConflict: 'user_id,fcm_token' }
      );
    if (error) {
      console.error('FCM subscribe upsert error', error);
      return res.status(500).json({ error: error.message });
    }
    return res.status(200).json({ ok: true });
  }

  // webpush (existing path)
  if (!subscription?.endpoint) {
    return res.status(400).json({ error: 'Missing subscription for transport=webpush' });
  }
  const { error } = await supabase
    .from('push_subscriptions')
    .upsert(
      { user_id: user.id, transport: 'webpush', subscription, ...prefs },
      { onConflict: 'user_id,endpoint' }
    );
  if (error) {
    console.error('webpush subscribe upsert error', error);
    return res.status(500).json({ error: error.message });
  }
  return res.status(200).json({ ok: true });
}
```

- [ ] **Step 2: Verify build**

```bash
npm run build
```

Expected: succeeds, no TypeScript errors.

- [ ] **Step 3: Commit**

```bash
git add api/push/subscribe.ts
git commit -m "feat(push): subscribe endpoint accepts transport=fcm with fcm_token

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Update `api/push/send-daily.ts` — FCM dispatch branch

**Files:**
- Modify: `api/push/send-daily.ts`

This is the cron that sends daily notifications. It currently only handles webpush rows. We add a Firebase Admin SDK branch for FCM rows.

- [ ] **Step 1: Read the current bottom section of `send-daily.ts`**

```bash
cat api/push/send-daily.ts
```

Identify:
- The `PushSubscriptionRow` interface (we'll extend it)
- The main dispatch loop that calls `webpush.sendNotification()`

- [ ] **Step 2: Add Firebase Admin initialization at the top of the file**

At the very top of `api/push/send-daily.ts`, after the existing imports, add:

```ts
import * as admin from 'firebase-admin';

// Lazily initialize Firebase Admin (cold-start safe)
function getFirebaseApp(): admin.app.App {
  if (admin.apps.length > 0) return admin.apps[0]!;
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_JSON ?? '{}') as admin.ServiceAccount;
  return admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
}
```

- [ ] **Step 3: Extend `PushSubscriptionRow` to include FCM fields**

Find the existing `PushSubscriptionRow` interface and replace it with:

```ts
interface PushSubscriptionRow {
  user_id: string;
  transport: 'webpush' | 'fcm';
  subscription: PushSubscriptionJSON | null;
  fcm_token: string | null;
  notify_episodes: boolean;
  notify_seasons: boolean;
  notify_movies: boolean;
}
```

- [ ] **Step 4: Add the FCM send helper alongside the existing webpush helper**

Find the existing `sendWebPush` (or equivalent named) function and add right after it:

```ts
async function sendFcm(
  fcmToken: string,
  payload: { title: string; body: string }
): Promise<{ ok: boolean; error?: string }> {
  try {
    const app = getFirebaseApp();
    await admin.messaging(app).send({
      token: fcmToken,
      notification: { title: payload.title, body: payload.body },
      apns: { payload: { aps: { sound: 'default' } } },
      android: { priority: 'high' },
    });
    return { ok: true };
  } catch (err) {
    return { ok: false, error: err instanceof Error ? err.message : String(err) };
  }
}
```

- [ ] **Step 5: Update the dispatch loop to branch on transport**

Find the loop where `webpush.sendNotification()` is called. It looks approximately like:

```ts
for (const sub of subscriptions) {
  // ...builds payload...
  await sendWebPush(sub.subscription, payload);
}
```

Replace the inner send call with a transport branch:

```ts
if (sub.transport === 'fcm' && sub.fcm_token) {
  await sendFcm(sub.fcm_token, payload);
} else if (sub.subscription) {
  await sendWebPush(sub.subscription, payload);
}
```

Adapt to match the exact variable names in the existing file.

- [ ] **Step 6: Verify build**

```bash
npm run build
```

Expected: succeeds.

- [ ] **Step 7: Commit**

```bash
git add api/push/send-daily.ts
git commit -m "feat(push): dispatch FCM notifications via Firebase Admin in send-daily

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Wire FCM into `nativeBoot.ts`

**Files:**
- Modify: `src/lib/nativeBoot.ts`

Register a persistent `pushNotificationReceived` listener so notifications received while the app is foregrounded are handled. Also register the `registration` listener once on boot so token refreshes are caught.

- [ ] **Step 1: Add the import and listener registration**

In `src/lib/nativeBoot.ts`, add after the existing imports:

```ts
import { PushNotifications } from '@capacitor/push-notifications';
```

Inside `nativeBoot()`, after the back-button registration block but before `requestAnimationFrame(SplashScreen.hide)`, add:

```ts
  // Push — handle foreground notifications and token refresh silently
  if (isNative()) {
    PushNotifications.addListener('pushNotificationReceived', (notification) => {
      console.log('[push] foreground notification:', notification.title);
    });
  }
```

Note: We do NOT call `register()` or `requestPermissions()` here — that's triggered by user interaction in the settings sheet (Task 8). Boot-time registration causes App Store rejection.

- [ ] **Step 2: Run tests**

```bash
npm run test
```

Expected: all passing (no new tests needed — this is a listener registration, not logic).

- [ ] **Step 3: Commit**

```bash
git add src/lib/nativeBoot.ts
git commit -m "feat(push): register foreground push listener in nativeBoot

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Update `NotificationPromptSheet.tsx` — native FCM flow

**Files:**
- Modify: `src/components/NotificationPromptSheet.tsx`

On native, clicking "Enable" should request FCM permission + get token + POST to `/api/push/subscribe` with `transport: 'fcm'`. On web, the existing web-push flow is unchanged.

- [ ] **Step 1: Read the current file**

```bash
cat src/components/NotificationPromptSheet.tsx
```

Confirm where `subscribe` from `useNotificationSubscription` is called (around line 14–17).

- [ ] **Step 2: Replace the file**

```tsx
import { useState } from 'react';
import { Bell, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import SheetModal, { SheetCloseButton } from './SheetModal';
import { useNotificationSubscription } from '../hooks/useNotificationSubscription';
import { isNative } from '../lib/platform';
import { requestNativePushPermission, getNativeFcmToken } from '../lib/nativePush';
import { supabase } from '../lib/supabase';
import toast from 'react-hot-toast';

interface Props {
  onDismiss: () => void;
}

export default function NotificationPromptSheet({ onDismiss }: Props) {
  const { i18n } = useTranslation();
  const isFr = i18n.language.startsWith('fr');
  const { subscribe, loading: webLoading } = useNotificationSubscription();
  const [nativeLoading, setNativeLoading] = useState(false);
  const loading = isNative() ? nativeLoading : webLoading;

  const handleEnable = async () => {
    if (isNative()) {
      setNativeLoading(true);
      try {
        const granted = await requestNativePushPermission();
        if (!granted) { onDismiss(); return; }

        const fcmToken = await getNativeFcmToken();
        if (!fcmToken) {
          toast.error(isFr ? 'Impossible d\'obtenir le token FCM.' : 'Could not get push token.');
          return;
        }

        const { data } = await supabase.auth.getSession();
        const accessToken = data.session?.access_token;
        if (!accessToken) return;

        const res = await fetch('/api/push/subscribe', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${accessToken}`,
          },
          body: JSON.stringify({ transport: 'fcm', fcm_token: fcmToken }),
        });

        if (res.ok) {
          toast.success(isFr ? 'Notifications activées !' : 'Notifications enabled!');
          onDismiss();
        } else {
          toast.error(isFr ? 'Erreur lors de l\'activation.' : 'Failed to enable notifications.');
        }
      } finally {
        setNativeLoading(false);
      }
      return;
    }

    // Web push path — unchanged
    const ok = await subscribe();
    if (ok) {
      toast.success(isFr ? 'Notifications activées !' : 'Notifications enabled!');
      onDismiss();
    } else {
      onDismiss();
    }
  };

  return (
    <SheetModal
      onClose={onDismiss}
      panelClassName="bg-[#f8f6f1] dark:bg-[#1a1f2e] rounded-t-3xl md:rounded-3xl shadow-2xl animate-slide-up px-6 pt-5 pb-8 md:max-w-sm"
    >
      <SheetCloseButton className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors">
        <X size={18} />
      </SheetCloseButton>

      <div className="flex items-center gap-4 mb-4">
        <div className="w-14 h-14 rounded-2xl bg-teal-500/10 flex items-center justify-center shrink-0">
          <Bell size={26} className="text-teal-500" />
        </div>
        <div>
          <h2 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100">
            {isFr ? 'Restez informé' : 'Stay up to date'}
          </h2>
          <p className="text-xs text-teal-600 dark:text-teal-400 font-medium mt-0.5">
            {isFr ? 'Notifications NookMind' : 'NookMind notifications'}
          </p>
        </div>
      </div>

      <p className="text-sm text-gray-600 dark:text-gray-400 leading-relaxed mb-6">
        {isFr
          ? 'Activez les notifications pour être prévenu le jour de la sortie de vos épisodes et films préférés — sans avoir à ouvrir l\'app.'
          : 'Enable notifications to be notified on the release day of your favourite episodes and movies — without opening the app.'}
      </p>

      <div className="flex gap-3">
        <button onClick={onDismiss} className="btn-ghost flex-1 text-sm">
          {isFr ? 'Plus tard' : 'Maybe later'}
        </button>
        <button
          onClick={handleEnable}
          disabled={loading}
          className="flex-1 flex items-center justify-center gap-2 bg-teal-500 hover:bg-teal-600 text-white font-medium rounded-full px-5 py-2.5 transition-all duration-150 active:scale-95 disabled:opacity-60 text-sm"
        >
          <Bell size={14} />
          {loading
            ? (isFr ? 'Activation…' : 'Enabling…')
            : (isFr ? 'Activer' : 'Enable')}
        </button>
      </div>
    </SheetModal>
  );
}
```

- [ ] **Step 3: Build + test**

```bash
npm run build && npm run test
```

Expected: build succeeds, all tests pass.

- [ ] **Step 4: Verify on web**

```bash
npm run dev
```

Open the browser, trigger the notification prompt (via Settings). The web-push flow should work exactly as before (`isNative()` returns false).

- [ ] **Step 5: Commit**

```bash
git add src/components/NotificationPromptSheet.tsx
git commit -m "feat(push): use FCM on native, keep webpush on web in NotificationPromptSheet

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: iOS native config

**Files:**
- Modify: `ios/App/App/Info.plist`
- Add: `ios/App/App/GoogleService-Info.plist` (user places manually — prereq B)

- [ ] **Step 1: Add notification usage description to `Info.plist`**

In `ios/App/App/Info.plist`, inside `<dict>`, add after the existing `UIViewControllerBasedStatusBarAppearance` key:

```xml
<key>NSUserNotificationsUsageDescription</key>
<string>NookMind uses notifications to alert you when your favourite series and movies release.</string>
```

- [ ] **Step 2: Verify `GoogleService-Info.plist` is in place (once prereq B is done)**

```bash
ls ios/App/App/GoogleService-Info.plist
```

If not there yet, skip this step — it's a manual prereq. The build will succeed without it but push won't work until it's added.

- [ ] **Step 3: Add `GoogleService-Info.plist` to `.gitignore` if it contains secrets**

The plist contains your Firebase project's API key — commit it or gitignore based on your security policy. Since this is a private repo and the key is client-side (readable by anyone who installs the app), committing is fine:

```bash
# It's already in the repo at ios/App/App/ once you place it
# No .gitignore change needed
```

- [ ] **Step 4: Sync**

```bash
npm run cap:sync
```

- [ ] **Step 5: Commit**

```bash
git add ios/App/App/Info.plist
git commit -m "feat(push): add NSUserNotificationsUsageDescription to iOS Info.plist

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Android native config

**Files:**
- Add: `android/app/google-services.json` (user places manually — prereq A)
- Modify: `android/build.gradle` — add Google services classpath
- Modify: `android/app/build.gradle` — apply `google-services` plugin

- [ ] **Step 1: Add Google services classpath to `android/build.gradle`**

Open `android/build.gradle`. In the `buildscript > dependencies` block, add:

```groovy
classpath 'com.google.gms:google-services:4.4.2'
```

The block already contains something like `classpath 'com.android.tools.build:gradle:...'`. Add the Google services line right after it.

- [ ] **Step 2: Apply the plugin in `android/app/build.gradle`**

Open `android/app/build.gradle`. At the very bottom of the file (after all existing content), add:

```groovy
apply plugin: 'com.google.gms.google-services'
```

- [ ] **Step 3: Verify `google-services.json` is in place (once prereq A is done)**

```bash
ls android/app/google-services.json
```

The Gradle build will fail if this file is missing and the plugin is applied. If you haven't done prereq A yet, skip Gradle sync for now — commit the Gradle changes and sync once the file is placed.

- [ ] **Step 4: Commit Gradle changes**

```bash
git add android/build.gradle android/app/build.gradle
git commit -m "feat(push): add google-services Gradle plugin for Firebase/FCM

Co-Authored-By: Claude Sonnet 4.6 (1M context) <noreply@anthropic.com>"
```

- [ ] **Step 5: Once `google-services.json` is placed, sync**

```bash
# Place android/app/google-services.json (from Firebase Console)
npm run cap:sync
git add android/app/google-services.json
git commit -m "feat(push): add google-services.json for Android FCM"
```

---

## Task 11: Verification

**Files:** none modified — verification only.

Prereqs required: Firebase project created, `google-services.json` in `android/app/`, `GoogleService-Info.plist` in `ios/App/App/`, `FIREBASE_SERVICE_ACCOUNT_JSON` in Vercel env.

### Android

- [ ] **Step 1: Sync + build + run**

```bash
npm run cap:android
```

In Android Studio: Run ▶.

- [ ] **Step 2: Trigger the notification prompt**

Open the app → Settings → tap the notification enable toggle. The system permission dialog should appear.

Expected: OS dialog "Allow NookMind to send notifications?" → tap Allow.

- [ ] **Step 3: Verify FCM token was stored**

Check Supabase Dashboard → Table Editor → `push_subscriptions`. A row with `transport = 'fcm'` and a non-null `fcm_token` should appear for your user.

- [ ] **Step 4: Trigger a test notification from Firebase Console**

Firebase Console → Cloud Messaging → **Send your first message** → notification title: "Test" → body: "Works!" → target: **Single device** → paste the FCM token from Supabase → Send.

Expected: notification appears on the emulator.

### iOS Simulator (once APNs key is uploaded to Firebase)

- [ ] **Step 5: Sync + open**

```bash
npm run cap:ios
```

- [ ] **Step 6: Enable notifications, verify FCM row in Supabase**

Same flow as Android. iOS Simulator can receive FCM messages when the APNs key is correctly uploaded to Firebase.

- [ ] **Step 7: Test Firebase Console → Send to device**

Same as Step 4. The simulator should receive the notification.

---

## Verification & Definition of Done

Plan 3 is complete when:

- [ ] `npm run test` passes (including new `nativePush.test.ts`)
- [ ] `npm run build` succeeds
- [ ] `npm run cap:sync` succeeds
- [ ] `push_subscriptions` table has `transport` + `fcm_token` columns
- [ ] Tapping "Enable notifications" on Android shows the OS permission dialog
- [ ] After enabling: a row with `transport = 'fcm'` appears in Supabase
- [ ] Firebase Console test message arrives on Android emulator
- [ ] Firebase Console test message arrives on iOS Simulator
- [ ] Web push flow on browser is unchanged
- [ ] `git status` is clean

## What's NOT in this plan (deferred)

- Per-notification-type FCM preference handling (episodes/seasons/movies toggles on native — same DB columns, just the subscribe call doesn't send them yet; `send-daily` already filters by them)
- Notification tap deep-link routing → **Plan 4**
- In-app notification badge count → out of scope for v1
- Firebase Console admin push broadcast → manual for v1; cron handles scheduled sends
