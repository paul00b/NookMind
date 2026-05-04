# Capacitor Auth — Plan 2 of 5

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the web-only Google OAuth redirect with native Google Sign-In on iOS/Android, add Sign-in-with-Apple (iOS only — required by App Store guideline 4.8), and add in-app account deletion (required by both stores). Web OAuth flow remains unchanged.

**Architecture:** `AuthContext.signInWithGoogle` becomes platform-aware: native shows the system Google sheet via `@codetrix-studio/capacitor-google-auth`, then exchanges the returned ID token via `supabase.auth.signInWithIdToken`. A new `signInWithApple` does the same via `@capacitor-community/apple-sign-in` with a SHA-256 nonce. `signOut` additionally clears native session caches. A new Vercel API route `/api/account/delete` uses the Supabase service-role key to delete user rows + the auth user; the Settings panel calls it from a confirmation dialog. Native projects get their OAuth client IDs wired via `strings.xml` (Android) and `Info.plist` URL schemes (iOS).

**Tech Stack:** Capacitor 6+, `@codetrix-studio/capacitor-google-auth`, `@capacitor-community/apple-sign-in`, Supabase JS, Vercel Functions, Web Crypto API (nonce hashing), React 19, vitest.

**Spec reference:** `docs/superpowers/specs/2026-04-27-capacitor-native-app-design.md` — Section 6.

---

## File map

**Created:**
- `src/lib/nonce.ts` — generates a random nonce + returns SHA-256 hash for Apple sign-in
- `src/lib/nonce.test.ts` — unit tests
- `src/lib/nativeAuth.ts` — wrappers around GoogleAuth + AppleSignIn plugins (initialization + sign-in helpers)
- `src/lib/nativeAuth.test.ts` — unit tests (mocked plugins)
- `api/account/delete.ts` — Vercel function that deletes user data and the auth user using the service-role key
- `src/components/DeleteAccountDialog.tsx` — confirmation modal for destructive action

**Modified:**
- `package.json` — new dependencies
- `capacitor.config.ts` — add `GoogleAuth` plugin config block
- `src/context/AuthContext.tsx` — platform-aware `signInWithGoogle`, new `signInWithApple`, native-aware `signOut`, new `deleteAccount`
- `src/pages/Login.tsx` — show Apple button on iOS only
- `src/components/SettingsPanel.tsx` — add "Delete account" entry that opens the dialog
- `src/i18n/locales/en.json` and `src/i18n/locales/fr.json` (or wherever translation files live — confirm in Task 0) — new keys for Apple button + delete account flow
- `android/app/src/main/res/values/strings.xml` — `server_client_id` resource
- `android/app/src/main/java/.../MainActivity.java` — register `GoogleAuth` plugin
- `ios/App/App/Info.plist` — add Google reversed-client-id URL scheme; declare `NSAppleIDUsageDescription`
- `ios/App/App/AppDelegate.swift` — handle URL callback for Google sheet (if not auto-handled by plugin v3+)
- `.env.example` (create if missing) — document the new build-time vars consumed in `capacitor.config.ts`
- Vercel project env (manual) — add `SUPABASE_SERVICE_ROLE_KEY`

**Untouched (intentionally):**
- `src/pages/AuthCallback.tsx` — still required for the web OAuth redirect path
- Existing `signIn` / `signUp` (email-password)
- All push-related code (Plan 3)

---

## Prerequisites — manual / external (one-time, non-coding)

These cannot be automated by an agent. They require you (Paul) to log into external dashboards. **Do all of these before starting Task 5.** Tasks 1-4 can run before any of this is done.

### A. Google Cloud Console — 3 OAuth client IDs

Project: the same Google Cloud project that already has the **Web** client used by Supabase today.

1. **Web client** — already exists. Note the client ID (you'll reuse it as the `serverClientId` for the GoogleAuth plugin — that's intentional, not a typo).
2. **iOS client** (new):
   - Console → APIs & Services → Credentials → Create credentials → OAuth client ID → Application type: iOS
   - Bundle ID: `fr.paulbr.nookmind`
   - Note the client ID and the "iOS URL scheme" (looks like `com.googleusercontent.apps.<digits>-<hash>`).
3. **Android client** (new):
   - Application type: Android
   - Package name: `fr.paulbr.nookmind`
   - SHA-1 fingerprint: get it from your debug keystore for now:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
     Add the **debug** SHA-1 first. After enrolling in Play App Signing (Plan 5), also add the upload-key SHA-1 and the Play-managed SHA-1.

### B. Supabase — accept native ID tokens

Dashboard → Authentication → Providers → Google:
- Enable provider (already enabled)
- **Authorized Client IDs:** add the iOS client ID and the Android client ID (comma-separated). Leave the existing web client ID as the primary.

Dashboard → Authentication → Providers → Apple:
- Enable provider
- Services ID: the one you'll create in step C
- Secret key: paste the `.p8` content (Apple gives you a downloadable key)
- Team ID + Key ID: from Apple Developer

Dashboard → Authentication → URL Configuration → Redirect URLs:
- Add `nookmind://auth/callback`

### C. Apple Developer Portal — Sign in with Apple

1. Identifiers → App IDs → `fr.paulbr.nookmind` → **enable Sign in with Apple capability**.
2. Identifiers → Services IDs → create new (e.g. `fr.paulbr.nookmind.signin`):
   - Enable Sign in with Apple
   - Configure: Primary App ID = `fr.paulbr.nookmind`; add return URL `https://<supabase-project-ref>.supabase.co/auth/v1/callback`.
3. Keys → create new key with Sign in with Apple enabled. Download the `.p8` (one-shot — store securely). Note the Key ID.

### D. Vercel project env vars

Add to Vercel (production + preview):
- `SUPABASE_SERVICE_ROLE_KEY` — from Supabase → Project Settings → API → `service_role` key (NEVER expose this to the client; only used server-side in the delete function).

### E. Local `.env` (for Capacitor build only)

Add the following keys to `.env` (gitignored):
```
VITE_GOOGLE_AUTH_SERVER_CLIENT_ID=<web-client-id-from-step-A.1>
VITE_GOOGLE_AUTH_IOS_CLIENT_ID=<ios-client-id-from-step-A.2>
```

(Android does not need a build-time env — the `strings.xml` resource is enough.)

---

## Task 0: Confirm i18n location and existing Login UI structure

**Files:** none modified — read-only inspection.

- [ ] **Step 1: Locate translation files**

```bash
ls src/i18n/locales 2>/dev/null || find src -name "*.json" | grep -i locale
```

Expected: identifies the JSON file(s) containing translation keys. Note their paths — used in Tasks 7 and 9. If translations live differently (e.g. inline), adapt those tasks accordingly without changing scope.

- [ ] **Step 2: Read `src/pages/Login.tsx` end-to-end**

```bash
cat src/pages/Login.tsx
```

Identify the JSX block where `signInWithGoogle` is wired (around line 134). The Apple button will sit immediately below the Google button. Note the existing button's className so the Apple button matches visually.

- [ ] **Step 3: Read `src/components/SettingsPanel.tsx` around the sign-out button**

The sign-out is at line ~411. The "Delete account" entry will go just below it as a separate destructive section.

No commit (read-only task).

---

## Task 1: Install auth plugins

**Files:**
- Modify: `package.json` (deps)

- [ ] **Step 1: Install the two auth plugins**

```bash
npm install @codetrix-studio/capacitor-google-auth @capacitor-community/apple-sign-in
```

Expected: both packages added to `dependencies`. No errors.

- [ ] **Step 2: Sync to native**

```bash
npm run cap:sync
```

Expected: "✔ Sync finished" with both plugins listed for both platforms. (`apple-sign-in` will be listed for iOS only — that's fine.)

- [ ] **Step 3: Commit**

```bash
git add package.json package-lock.json android/ ios/
git commit -m "feat(auth): install Google and Apple sign-in capacitor plugins"
```

---

## Task 2: Add `GoogleAuth` block to `capacitor.config.ts`

**Files:**
- Modify: `capacitor.config.ts`

- [ ] **Step 1: Read current `capacitor.config.ts`**

```bash
cat capacitor.config.ts
```

- [ ] **Step 2: Replace with the augmented version**

```ts
import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'fr.paulbr.nookmind',
  appName: 'NookMind',
  webDir: 'dist',
  server: {
    androidScheme: 'https',
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 1500,
      backgroundColor: '#0f1117',
      androidScaleType: 'CENTER_CROP',
      showSpinner: false,
    },
    Keyboard: {
      resize: 'native',
    },
    GoogleAuth: {
      scopes: ['profile', 'email'],
      serverClientId: process.env.VITE_GOOGLE_AUTH_SERVER_CLIENT_ID,
      forceCodeForRefreshToken: true,
    },
  },
};

export default config;
```

Note: `serverClientId` is read from the env at config-load time (CLI runs in Node and sees `process.env`). The value becomes literal in the synced native project, so it persists into the built app.

- [ ] **Step 3: Verify the value flows through to the native projects**

```bash
npm run cap:sync
grep -r "VITE_GOOGLE_AUTH_SERVER_CLIENT_ID\|googlecontent" android/app/src/main/assets/capacitor.config.json ios/App/App/capacitor.config.json
```

Expected: the actual web-client-id value is present (NOT the literal env-var name). If the env var isn't set, you'll see `null` — that's a sign you forgot prerequisite E.

- [ ] **Step 4: Commit**

```bash
git add capacitor.config.ts
git commit -m "feat(auth): configure GoogleAuth plugin via env-driven serverClientId"
```

---

## Task 3: Nonce helper (TDD)

Apple sign-in requires you to generate a nonce, send its SHA-256 hash to Apple, and pass the **raw** (unhashed) nonce to Supabase. The shape of the helper is the same regardless of platform.

**Files:**
- Create: `src/lib/nonce.ts`
- Create: `src/lib/nonce.test.ts`

- [ ] **Step 1: Write failing tests**

Create `src/lib/nonce.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { generateNonce, sha256Hex } from './nonce';

describe('generateNonce', () => {
  it('returns a non-empty string', async () => {
    const n = generateNonce();
    expect(typeof n).toBe('string');
    expect(n.length).toBeGreaterThanOrEqual(32);
  });

  it('returns a different value each time', () => {
    const a = generateNonce();
    const b = generateNonce();
    expect(a).not.toBe(b);
  });
});

describe('sha256Hex', () => {
  it('hashes a known input to a known sha-256 hex digest', async () => {
    // sha256("abc") = ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
    const hex = await sha256Hex('abc');
    expect(hex).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad');
  });

  it('returns 64 hex chars', async () => {
    const hex = await sha256Hex('hello world');
    expect(hex).toMatch(/^[0-9a-f]{64}$/);
  });
});
```

- [ ] **Step 2: Run — verify failing**

```bash
npm run test -- nonce
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement**

Create `src/lib/nonce.ts`:

```ts
const HEX = '0123456789abcdef';

function bytesToHex(bytes: Uint8Array): string {
  let out = '';
  for (let i = 0; i < bytes.length; i++) {
    out += HEX[bytes[i] >> 4] + HEX[bytes[i] & 0xf];
  }
  return out;
}

export function generateNonce(): string {
  const bytes = new Uint8Array(32);
  crypto.getRandomValues(bytes);
  return bytesToHex(bytes);
}

export async function sha256Hex(input: string): Promise<string> {
  const data = new TextEncoder().encode(input);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return bytesToHex(new Uint8Array(digest));
}
```

- [ ] **Step 4: Run — verify passing**

```bash
npm run test -- nonce
```

Expected: PASS — 4 tests passing.

- [ ] **Step 5: Commit**

```bash
git add src/lib/nonce.ts src/lib/nonce.test.ts
git commit -m "feat(auth): add nonce helper for Apple sign-in"
```

---

## Task 4: `nativeAuth.ts` wrapper (TDD)

Wrap both plugins behind a tiny module so `AuthContext` does not import plugin SDKs directly. Easier to mock and gives one place to handle initialization.

**Files:**
- Create: `src/lib/nativeAuth.ts`
- Create: `src/lib/nativeAuth.test.ts`

- [ ] **Step 1: Write failing tests**

Create `src/lib/nativeAuth.test.ts`:

```ts
import { describe, expect, it, vi, beforeEach } from 'vitest';

vi.mock('@codetrix-studio/capacitor-google-auth', () => ({
  GoogleAuth: {
    initialize: vi.fn(),
    signIn: vi.fn(),
    signOut: vi.fn(),
  },
}));

vi.mock('@capacitor-community/apple-sign-in', () => ({
  SignInWithApple: {
    authorize: vi.fn(),
  },
}));

vi.mock('./platform', () => ({
  isNative: vi.fn(),
  isIOS: vi.fn(),
  isAndroid: vi.fn(),
  isWeb: vi.fn(),
}));

import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { SignInWithApple } from '@capacitor-community/apple-sign-in';
import { isNative, isIOS } from './platform';
import {
  initNativeAuth,
  nativeGoogleSignIn,
  nativeAppleSignIn,
  nativeGoogleSignOut,
} from './nativeAuth';

describe('nativeAuth', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('initNativeAuth: no-op on web', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    await initNativeAuth();
    expect(GoogleAuth.initialize).not.toHaveBeenCalled();
  });

  it('initNativeAuth: initializes GoogleAuth on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    await initNativeAuth();
    expect(GoogleAuth.initialize).toHaveBeenCalledTimes(1);
  });

  it('nativeGoogleSignIn: returns idToken from plugin response', async () => {
    vi.mocked(GoogleAuth.signIn).mockResolvedValue({
      authentication: { idToken: 'fake-id-token' },
    } as never);
    const token = await nativeGoogleSignIn();
    expect(token).toBe('fake-id-token');
  });

  it('nativeGoogleSignIn: throws if plugin returns no idToken', async () => {
    vi.mocked(GoogleAuth.signIn).mockResolvedValue({
      authentication: { idToken: '' },
    } as never);
    await expect(nativeGoogleSignIn()).rejects.toThrow(/idToken/);
  });

  it('nativeAppleSignIn: returns identityToken + raw nonce', async () => {
    vi.mocked(isIOS).mockReturnValue(true);
    vi.mocked(SignInWithApple.authorize).mockResolvedValue({
      response: { identityToken: 'apple-token' },
    } as never);
    const result = await nativeAppleSignIn();
    expect(result.identityToken).toBe('apple-token');
    expect(result.nonce).toMatch(/^[0-9a-f]{64}$/);
    expect(SignInWithApple.authorize).toHaveBeenCalledWith(
      expect.objectContaining({
        nonce: expect.stringMatching(/^[0-9a-f]{64}$/), // hashed nonce sent to Apple
      })
    );
  });

  it('nativeAppleSignIn: throws on non-iOS', async () => {
    vi.mocked(isIOS).mockReturnValue(false);
    await expect(nativeAppleSignIn()).rejects.toThrow(/iOS/i);
  });

  it('nativeGoogleSignOut: calls plugin signOut on native', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    await nativeGoogleSignOut();
    expect(GoogleAuth.signOut).toHaveBeenCalled();
  });

  it('nativeGoogleSignOut: no-op on web', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    await nativeGoogleSignOut();
    expect(GoogleAuth.signOut).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run — verify failing**

```bash
npm run test -- nativeAuth
```

Expected: FAIL — module not found.

- [ ] **Step 3: Implement**

Create `src/lib/nativeAuth.ts`:

```ts
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { SignInWithApple, type SignInWithAppleOptions } from '@capacitor-community/apple-sign-in';
import { isNative, isIOS } from './platform';
import { generateNonce, sha256Hex } from './nonce';

let initialized = false;

export async function initNativeAuth(): Promise<void> {
  if (initialized) return;
  initialized = true;
  if (!isNative()) return;
  await GoogleAuth.initialize({
    clientId: import.meta.env.VITE_GOOGLE_AUTH_SERVER_CLIENT_ID,
    scopes: ['profile', 'email'],
    grantOfflineAccess: true,
  });
}

export async function nativeGoogleSignIn(): Promise<string> {
  const result = await GoogleAuth.signIn();
  const idToken = result.authentication?.idToken;
  if (!idToken) {
    throw new Error('Google sign-in returned no idToken');
  }
  return idToken;
}

export async function nativeGoogleSignOut(): Promise<void> {
  if (!isNative()) return;
  try {
    await GoogleAuth.signOut();
  } catch {
    // already signed out — ignore
  }
}

export interface AppleSignInResult {
  identityToken: string;
  nonce: string; // raw nonce — pass to Supabase
}

export async function nativeAppleSignIn(): Promise<AppleSignInResult> {
  if (!isIOS()) {
    throw new Error('Apple sign-in is only available on iOS');
  }
  const rawNonce = generateNonce();
  const hashedNonce = await sha256Hex(rawNonce);
  const options: SignInWithAppleOptions = {
    clientId: 'fr.paulbr.nookmind',
    redirectURI: '', // not needed for native flow
    scopes: 'email name',
    state: '',
    nonce: hashedNonce,
  };
  const result = await SignInWithApple.authorize(options);
  const identityToken = result.response?.identityToken;
  if (!identityToken) {
    throw new Error('Apple sign-in returned no identityToken');
  }
  return { identityToken, nonce: rawNonce };
}
```

- [ ] **Step 4: Run — verify passing**

```bash
npm run test -- nativeAuth
```

Expected: PASS — 8 tests passing.

- [ ] **Step 5: Commit**

```bash
git add src/lib/nativeAuth.ts src/lib/nativeAuth.test.ts
git commit -m "feat(auth): add nativeAuth wrapper around Google and Apple plugins"
```

---

## Task 5: Wire `nativeBoot` to initialize GoogleAuth

**Files:**
- Modify: `src/lib/nativeBoot.ts`

- [ ] **Step 1: Read the current `nativeBoot.ts`**

```bash
cat src/lib/nativeBoot.ts
```

- [ ] **Step 2: Add `initNativeAuth()` call**

Add the import at the top:
```ts
import { initNativeAuth } from './nativeAuth';
```

Inside the `nativeBoot` function, after the existing `if (!isNative()) return;` early exit, add:
```ts
  // Auth — initialize Google sign-in plugin (Apple needs no init)
  try {
    await initNativeAuth();
  } catch (e) {
    console.error('Failed to init native auth', e);
  }
```

Place it before the StatusBar block. Order matters: auth init is fire-and-forget but should run before the splash hide.

- [ ] **Step 3: Verify build still works**

```bash
npm run cap:sync
```

Expected: clean sync, no errors.

- [ ] **Step 4: Commit**

```bash
git add src/lib/nativeBoot.ts
git commit -m "feat(auth): initialize GoogleAuth during native boot"
```

---

## Task 6: Platform-aware `signInWithGoogle` + new `signInWithApple` + `deleteAccount` in `AuthContext`

**Files:**
- Modify: `src/context/AuthContext.tsx`

- [ ] **Step 1: Replace `AuthContext.tsx`**

Rewrite `src/context/AuthContext.tsx`:

```tsx
import React, { createContext, useContext, useEffect, useState } from 'react';
import type { User, Session } from '@supabase/supabase-js';
import { supabase } from '../lib/supabase';
import { isNative } from '../lib/platform';
import {
  nativeGoogleSignIn,
  nativeGoogleSignOut,
  nativeAppleSignIn,
} from '../lib/nativeAuth';

interface AuthContextValue {
  user: User | null;
  session: Session | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<{ error: Error | null }>;
  signUp: (email: string, password: string) => Promise<{ error: Error | null }>;
  signOut: () => Promise<void>;
  signInWithGoogle: () => Promise<{ error: Error | null }>;
  signInWithApple: () => Promise<{ error: Error | null }>;
  deleteAccount: () => Promise<{ error: Error | null }>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [session, setSession] = useState<Session | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      setUser(session?.user ?? null);
      setLoading(false);
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session);
      setUser(session?.user ?? null);
    });

    return () => subscription.unsubscribe();
  }, []);

  const signIn = async (email: string, password: string) => {
    const { error } = await supabase.auth.signInWithPassword({ email, password });
    return { error: error as Error | null };
  };

  const signUp = async (email: string, password: string) => {
    const { error } = await supabase.auth.signUp({ email, password });
    return { error: error as Error | null };
  };

  const signOut = async () => {
    if (isNative()) {
      await nativeGoogleSignOut();
      // Apple has no native signOut API — Supabase signOut is enough
    }
    await supabase.auth.signOut();
    localStorage.removeItem('nookmind_onboarding_completed');
  };

  const signInWithGoogle = async () => {
    if (isNative()) {
      try {
        const idToken = await nativeGoogleSignIn();
        const { error } = await supabase.auth.signInWithIdToken({
          provider: 'google',
          token: idToken,
        });
        return { error: error as Error | null };
      } catch (e) {
        return { error: e as Error };
      }
    }
    const { error } = await supabase.auth.signInWithOAuth({
      provider: 'google',
      options: { redirectTo: `${window.location.origin}/auth/callback` },
    });
    return { error: error as Error | null };
  };

  const signInWithApple = async () => {
    try {
      const { identityToken, nonce } = await nativeAppleSignIn();
      const { error } = await supabase.auth.signInWithIdToken({
        provider: 'apple',
        token: identityToken,
        nonce,
      });
      return { error: error as Error | null };
    } catch (e) {
      return { error: e as Error };
    }
  };

  const deleteAccount = async () => {
    try {
      if (!session) {
        return { error: new Error('Not signed in') };
      }
      const res = await fetch('/api/account/delete', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${session.access_token}`,
        },
      });
      if (!res.ok) {
        const body = await res.text();
        return { error: new Error(body || `HTTP ${res.status}`) };
      }
      await signOut();
      return { error: null };
    } catch (e) {
      return { error: e as Error };
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        session,
        loading,
        signIn,
        signUp,
        signOut,
        signInWithGoogle,
        signInWithApple,
        deleteAccount,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
```

- [ ] **Step 2: Type-check**

```bash
npm run build
```

Expected: build succeeds. If you get errors about unused imports in Login or SettingsPanel, ignore for now — Tasks 9 and 10 fix them.

- [ ] **Step 3: Run existing tests**

```bash
npm run test
```

Expected: existing tests pass; no new failures.

- [ ] **Step 4: Commit**

```bash
git add src/context/AuthContext.tsx
git commit -m "feat(auth): platform-aware Google sign-in, add Apple sign-in and account deletion"
```

---

## Task 7: i18n keys

**Files:**
- Modify: translation files identified in Task 0

- [ ] **Step 1: Add the new keys**

Add these keys to **every** locale file. English values shown — translate French as appropriate (or leave English placeholders for the user to refine):

```json
{
  "auth": {
    "continueWithApple": "Continue with Apple",
    "googleSignInFailed": "Google sign-in failed",
    "appleSignInFailed": "Apple sign-in failed"
  },
  "settings": {
    "deleteAccount": "Delete account",
    "deleteAccountSubtitle": "Permanently remove your account and all data",
    "deleteAccountConfirmTitle": "Delete your account?",
    "deleteAccountConfirmBody": "This will permanently delete your account, books, movies, series, categories, and notification subscriptions. This cannot be undone.",
    "deleteAccountConfirmCta": "Yes, delete my account",
    "deleteAccountCancel": "Cancel",
    "deleteAccountFailed": "Failed to delete account"
  }
}
```

Merge structurally — do not overwrite existing `auth` or `settings` blocks. If keys already exist with the same names, leave them.

- [ ] **Step 2: Verify the app loads with the new keys**

```bash
npm run dev
```

Open the browser, navigate to Settings, ensure no console errors about missing keys (the old paths should still resolve).

- [ ] **Step 3: Commit**

```bash
git add src/i18n
git commit -m "feat(i18n): add Apple sign-in and account deletion translations"
```

---

## Task 8: Account deletion API route

**Files:**
- Create: `api/account/delete.ts`

This runs as a Vercel function. It uses the **service-role** Supabase key (admin) — never expose this to clients.

- [ ] **Step 1: Inspect existing API conventions**

```bash
ls api/
cat api/push/subscribe.ts | head -40
```

Note the import style and how the function is exported (Vercel Node functions vs Edge). Match that style for consistency. The example below uses the Node runtime + classic handler signature; adjust if the existing files use a different pattern.

- [ ] **Step 2: Create the function**

Create `api/account/delete.ts`:

```ts
import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = process.env.VITE_SUPABASE_URL;
const SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;

const TABLES_WITH_USER_ID = [
  'books',
  'movies',
  'series',
  'series_episodes',
  'categories',
  'movie_categories',
  'series_categories',
  'push_subscriptions',
] as const;

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    res.setHeader('Allow', 'POST');
    return res.status(405).end('Method Not Allowed');
  }
  if (!SUPABASE_URL || !SERVICE_ROLE_KEY) {
    return res.status(500).send('Server misconfigured: missing Supabase env');
  }

  const auth = req.headers.authorization || '';
  const token = auth.startsWith('Bearer ') ? auth.slice(7) : '';
  if (!token) return res.status(401).send('Missing bearer token');

  const admin = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
    auth: { autoRefreshToken: false, persistSession: false },
  });

  const { data: userData, error: userErr } = await admin.auth.getUser(token);
  if (userErr || !userData?.user) {
    return res.status(401).send('Invalid token');
  }
  const userId = userData.user.id;

  // Best-effort row deletion (continue even if a single table errors — table may not exist in all envs)
  for (const table of TABLES_WITH_USER_ID) {
    const { error } = await admin.from(table).delete().eq('user_id', userId);
    if (error) {
      console.warn(`delete from ${table} failed`, error.message);
    }
  }

  const { error: delErr } = await admin.auth.admin.deleteUser(userId);
  if (delErr) {
    return res.status(500).send(`Failed to delete auth user: ${delErr.message}`);
  }

  return res.status(200).json({ ok: true });
}
```

If your API files use ESM with explicit `.js` extensions or a different request/response type, adapt those bits — the logic stays identical.

- [ ] **Step 3: Verify table list matches actual schema**

```bash
ls supabase-*-migration.sql | xargs grep -l "user_id" | head
grep -h "create table\|user_id" supabase-*-migration.sql | grep -i "create table\|user_id"
```

If a table with a `user_id` column is missing from `TABLES_WITH_USER_ID`, add it. If a table in the list does not exist in your schema, leave it — `.delete()` against a non-existent table is logged and skipped per the warn-only loop.

- [ ] **Step 4: Smoke test the function locally**

```bash
npm run dev
# In another shell, with a real session token from a logged-in browser:
# (DevTools → Application → Local Storage → sb-...-auth-token → access_token)
curl -X POST http://localhost:5173/api/account/delete \
  -H "Authorization: Bearer <paste-access-token>" -i
```

Expected on a throwaway test user: `200 OK`. **Do not run this against your real account** unless you intend to delete it. Use a dedicated test user.

- [ ] **Step 5: Commit**

```bash
git add api/account/delete.ts
git commit -m "feat(api): add account deletion endpoint using Supabase service role"
```

---

## Task 9: Login UI — Apple button (iOS only)

**Files:**
- Modify: `src/pages/Login.tsx`

- [ ] **Step 1: Add the Apple button**

In `src/pages/Login.tsx`:

1. Add to imports:
   ```ts
   import { isIOS } from '../lib/platform';
   import { useTranslation } from 'react-i18next'; // if not already present
   ```
2. Pull `signInWithApple` from `useAuth()`:
   ```ts
   const { user, signIn, signUp, signInWithGoogle, signInWithApple } = useAuth();
   ```
3. Immediately below the existing Google button block (the one currently using `signInWithGoogle` around line 134), add:
   ```tsx
   {isIOS() && (
     <button
       type="button"
       onClick={async () => {
         const { error } = await signInWithApple();
         if (error) {
           // surface the error using the same pattern the Google button uses
           console.error(error);
         }
       }}
       className="<COPY THE EXACT className FROM THE GOOGLE BUTTON>"
     >
       {t('auth.continueWithApple')}
     </button>
   )}
   ```

Match the Google button's styling exactly — copy its className verbatim, swap the icon if there's an icon component.

- [ ] **Step 2: Verify on web (button must NOT appear)**

```bash
npm run dev
```

Open `/login`. Confirm the Apple button is **not** rendered (since `isIOS()` returns false in a desktop browser). Google button still works as before.

- [ ] **Step 3: Run type-check + tests**

```bash
npm run build && npm run test
```

Expected: both succeed.

- [ ] **Step 4: Commit**

```bash
git add src/pages/Login.tsx
git commit -m "feat(login): add Sign in with Apple button on iOS"
```

---

## Task 10: Settings — delete account dialog

**Files:**
- Create: `src/components/DeleteAccountDialog.tsx`
- Modify: `src/components/SettingsPanel.tsx`

- [ ] **Step 1: Create the dialog component**

Create `src/components/DeleteAccountDialog.tsx`:

```tsx
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../context/AuthContext';

interface Props {
  open: boolean;
  onClose: () => void;
}

export function DeleteAccountDialog({ open, onClose }: Props) {
  const { t } = useTranslation();
  const { deleteAccount } = useAuth();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) return null;

  const onConfirm = async () => {
    setBusy(true);
    setError(null);
    const { error } = await deleteAccount();
    setBusy(false);
    if (error) {
      setError(error.message || t('settings.deleteAccountFailed'));
      return;
    }
    onClose();
    // signOut inside deleteAccount triggers onAuthStateChange → app routes to /login
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
    >
      <div className="w-full max-w-md rounded-2xl bg-zinc-900 p-6 text-zinc-100 shadow-xl">
        <h2 className="text-xl font-semibold">{t('settings.deleteAccountConfirmTitle')}</h2>
        <p className="mt-3 text-sm text-zinc-300">
          {t('settings.deleteAccountConfirmBody')}
        </p>
        {error && <p className="mt-3 text-sm text-red-400">{error}</p>}
        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="rounded-lg px-4 py-2 text-sm text-zinc-300 hover:bg-zinc-800"
          >
            {t('settings.deleteAccountCancel')}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-500 disabled:opacity-50"
          >
            {t('settings.deleteAccountConfirmCta')}
          </button>
        </div>
      </div>
    </div>
  );
}
```

Adjust the className list to match the project's existing dialog/sheet styling (look at `InstallPromptSheet` or any existing modal). Don't invent a new design language.

- [ ] **Step 2: Wire it into `SettingsPanel.tsx`**

In `src/components/SettingsPanel.tsx`:

1. Add the import:
   ```ts
   import { DeleteAccountDialog } from './DeleteAccountDialog';
   ```
2. Add state at the top of the component:
   ```ts
   const [deleteOpen, setDeleteOpen] = useState(false);
   ```
3. Below the existing sign-out button (line ~411), add a destructive section:
   ```tsx
   <button
     type="button"
     onClick={() => setDeleteOpen(true)}
     className="<MATCH SIGN-OUT BUTTON STYLING — but with red text/border>"
   >
     {t('settings.deleteAccount')}
   </button>
   ```
4. At the end of the component's JSX (just before the outer closing element), render the dialog:
   ```tsx
   <DeleteAccountDialog open={deleteOpen} onClose={() => setDeleteOpen(false)} />
   ```

- [ ] **Step 3: Type-check + tests**

```bash
npm run build && npm run test
```

Expected: succeeds.

- [ ] **Step 4: Smoke-test the flow on web with a throwaway user**

```bash
npm run dev
```

1. Sign up a throwaway user.
2. Open Settings → tap Delete account → confirm.
3. Verify in Supabase dashboard that the user is gone from Authentication → Users.
4. Verify their rows are gone from `books`, etc.

- [ ] **Step 5: Commit**

```bash
git add src/components/DeleteAccountDialog.tsx src/components/SettingsPanel.tsx
git commit -m "feat(settings): add in-app account deletion with confirmation dialog"
```

---

## Task 11: Android native wiring — `strings.xml` + `MainActivity`

**Files:**
- Modify: `android/app/src/main/res/values/strings.xml`
- Modify: `android/app/src/main/java/.../MainActivity.java`

- [ ] **Step 1: Add server_client_id to strings.xml**

Open `android/app/src/main/res/values/strings.xml`. Add inside `<resources>`:

```xml
<string name="server_client_id">PASTE_THE_WEB_OAUTH_CLIENT_ID_HERE</string>
```

The value must match prerequisite A.1 (the **web** client ID — yes, on Android the GoogleAuth plugin still wants the web client ID as `server_client_id` because that's what mints the ID token Supabase will accept).

- [ ] **Step 2: Confirm `MainActivity` registers GoogleAuth**

```bash
find android -name MainActivity.java | head -1
cat $(find android -name MainActivity.java | head -1)
```

Capacitor 6 auto-discovers plugins via Gradle, so explicit registration is usually NOT required. If `MainActivity.java` only has the empty `class MainActivity extends BridgeActivity {}` body, leave it. **Verification at runtime** (Task 13 Step 2) is what confirms registration worked.

- [ ] **Step 3: Sync**

```bash
npm run cap:sync
```

- [ ] **Step 4: Commit**

```bash
git add android/app/src/main/res/values/strings.xml
git commit -m "feat(android): add server_client_id resource for GoogleAuth"
```

---

## Task 12: iOS native wiring — `Info.plist` URL types + capabilities

**Files:**
- Modify: `ios/App/App/Info.plist`
- Modify: `ios/App/App/App.entitlements` (or create via Xcode)

- [ ] **Step 1: Add the Google reversed-client-id URL scheme**

Open `ios/App/App/Info.plist`. Inside `<dict>`, add (or extend if `CFBundleURLTypes` already exists):

```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>com.googleusercontent.apps.PASTE_THE_REVERSED_IOS_CLIENT_ID</string>
    </array>
  </dict>
</array>
```

The reversed-client-id was provided by Google Cloud Console when you created the iOS OAuth client (prerequisite A.2). Format: `com.googleusercontent.apps.<digits>-<hash>`.

- [ ] **Step 2: Add `NSAppleIDUsageDescription` (defensive — not strictly required for the system flow but harmless)**

In the same `Info.plist`:

```xml
<key>NSAppleIDUsageDescription</key>
<string>Sign in with your Apple ID to access NookMind</string>
```

- [ ] **Step 3: Enable Sign in with Apple capability in Xcode**

```bash
npm run cap:ios
```

In Xcode: select the **App** target → Signing & Capabilities → `+ Capability` → **Sign in with Apple**. Xcode generates / updates `App.entitlements`.

Commit the entitlements file.

- [ ] **Step 4: Pod install for the new plugins**

```bash
cd ios/App && pod install && cd ../..
```

Expected: pods for `CapacitorGoogleAuth` and `CapacitorCommunityAppleSignIn` installed. `Podfile.lock` updated.

- [ ] **Step 5: Commit**

```bash
git add ios/App/App/Info.plist ios/App/App/App.entitlements ios/App/Podfile.lock
git commit -m "feat(ios): wire Google URL scheme and Sign in with Apple entitlement"
```

---

## Task 13: First-run verification

**Files:** none modified — verification only.

### Android (Windows or Mac)

- [ ] **Step 1: Sync + open**

```bash
npm run cap:android
```

- [ ] **Step 2: Sign in with Google (native)**

Run on emulator / device. Tap "Continue with Google". Expected:
- Native Google account-picker sheet (not a Chrome tab)
- After picking: app returns to home screen, signed in
- Supabase dashboard → Authentication → Users shows the user with `provider: google`

If the picker shows then immediately closes with a toast like "Sign-in failed", the SHA-1 in your Android OAuth client does not match the keystore signing the APK. Re-run `keytool` against the active keystore and update the OAuth client.

- [ ] **Step 3: Sign out**

In Settings → tap "Sign out". Expected: returned to login page; tapping Google again shows the account picker fresh (not auto-selected, because `nativeGoogleSignOut` cleared the cached session).

- [ ] **Step 4: Delete account on a throwaway user**

Sign up a new email-password user → delete → confirm. Verify in Supabase that the user and rows are gone.

### iOS Simulator (Mac)

- [ ] **Step 5: Open + run**

```bash
npm run cap:ios
```

Pick an iPhone simulator → Run.

- [ ] **Step 6: Sign in with Google (native sheet)**

Tap "Continue with Google" → native sheet → pick account → returned signed in.

- [ ] **Step 7: Sign in with Apple**

From a fresh sign-out: tap "Continue with Apple" → native sheet → use a Sandbox Apple ID (Settings → Sign in to Simulator) → returns signed in. Verify in Supabase Users that `provider: apple` appears.

- [ ] **Step 8: Delete account from iOS**

Same as Android: throwaway user → delete → confirm gone in dashboard.

- [ ] **Step 9: Document anything that fails**

If any item fails, stop and add a follow-up task before declaring Plan 2 done.

---

## Verification & Definition of Done

Plan 2 is complete when:

- [ ] `npm run dev` works (web Google OAuth redirect unchanged)
- [ ] `npm run build` works
- [ ] `npm run test` passes (including new `nonce.test.ts` and `nativeAuth.test.ts`)
- [ ] `npm run cap:sync` works
- [ ] On Android: Google sign-in via native sheet → Supabase session created
- [ ] On iOS: Google sign-in via native sheet → Supabase session created
- [ ] On iOS: Apple sign-in → Supabase session with `provider: apple`
- [ ] Apple button does NOT render on Android or web
- [ ] Sign-out clears native Google session cache (next sign-in shows account picker)
- [ ] Settings → Delete account → confirm → user + rows deleted in Supabase
- [ ] `git status` is clean

After this plan, the auth surface meets the App Store guideline 4.8 (Sign in with Apple offered alongside Google) and both stores' account-deletion mandate.

## What's NOT in this plan (deferred)

- FCM push notifications → **Plan 3**
- Custom URL scheme + Universal Links (`nookmind://`, `paulbr.fr/*`) → **Plan 4**
- App icons / splash master assets / privacy policy audit / store listings → **Plan 5**
- Email-link / magic-link auth — current spec only covers password + Google + Apple
- Apple Sign-in on the web (would require Services ID return URL plumbing — not needed for store approval)
