# Native Authentication — Plan 2 of 5

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate auth to native flows on iOS and Android — Google sign-in via the system sheet, Sign in with Apple on iOS, and in-app account deletion. Web (PWA) continues to use the existing OAuth-redirect flow unchanged.

**Architecture:** Platform-aware `AuthContext`. On native: `@codetrix-studio/capacitor-google-auth` and `@capacitor-community/apple-sign-in` produce ID tokens that are exchanged with Supabase via `signInWithIdToken`. On web: existing `signInWithOAuth` redirect flow is unchanged. Account deletion is a new Vercel API route that uses the Supabase service-role key to wipe user rows + delete the auth user, callable from a "Delete account" button in `SettingsPanel`.

**Tech Stack:** Capacitor 8, `@codetrix-studio/capacitor-google-auth`, `@capacitor-community/apple-sign-in`, `@capacitor/preferences`, Supabase JS, Vercel serverless, vitest.

**Spec reference:** `docs/superpowers/specs/2026-04-27-capacitor-native-app-design.md` — Section 6 (Authentication), Section 11 (App Privacy / Apple's Sign-in-with-Apple rule, in-app account deletion requirement).

---

## File map

**Created:**
- `src/lib/storage.ts` — Capacitor Preferences wrapper with web `localStorage` fallback (so onboarding flag survives app updates on native and stays in sync on web)
- `src/lib/storage.test.ts` — unit tests for the storage helper
- `src/lib/auth/googleSignIn.ts` — native Google sign-in helper (initializes the plugin, performs sign-in, exchanges token with Supabase)
- `src/lib/auth/appleSignIn.ts` — Sign in with Apple helper (iOS only)
- `src/lib/auth/auth.test.ts` — tests for the platform branching in AuthContext
- `api/auth/delete-account.ts` — Vercel function to delete a user + their data
- `ios/App/App/App.entitlements` — iOS entitlements file (gets the Sign-in-with-Apple capability)

**Modified:**
- `package.json` (deps + scripts)
- `capacitor.config.ts` — add `GoogleAuth` plugin config block
- `src/context/AuthContext.tsx` — add `signInWithApple`, make `signInWithGoogle` platform-aware, clean up native sessions in `signOut`, migrate onboarding flag to storage helper
- `src/pages/Login.tsx` — show "Sign in with Apple" button on iOS (hidden elsewhere)
- `src/components/SettingsPanel.tsx` — add a "Delete account" button + confirm dialog wiring
- `src/i18n/locales/en.ts`, `src/i18n/locales/fr.ts` — new strings: `login.continueWithApple`, `settings.deleteAccount`, `settings.deleteAccountConfirmTitle`, `settings.deleteAccountConfirmBody`, `settings.deleteAccountCancel`, `settings.deleteAccountConfirm`, `settings.deleteAccountSuccess`, `settings.deleteAccountFailed`
- `ios/App/App.xcodeproj/project.pbxproj` — reference the new `App.entitlements` file

**Untouched:**
- All other `src/components/*`, `src/pages/*`, `src/context/*` — no changes here. `useNotificationSubscription`, all media library code, the discover page, etc. remain as-is.

---

## Phase 0 — Prerequisites (USER, no code)

**Do NOT start Task 1 until all of these are confirmed.** They take ~1-2 hours total and require accounts to be enrolled. Each is independent — work on them in any order.

### 0.A Apple Developer Program enrollment ($99/year)

1. Sign in at https://developer.apple.com → Account → Enroll
2. Choose Individual or Organization (Individual is faster — Org takes a few days for D-U-N-S verification)
3. Pay the $99 annual fee
4. **Wait for "Membership active" status** before doing 0.D

### 0.B Google Cloud Console — iOS OAuth client

1. Open https://console.cloud.google.com → APIs & Services → Credentials
2. Use the existing project that already has the **Web** OAuth client (the one Supabase Google provider points at today)
3. Click **Create Credentials → OAuth client ID**
4. Application type: **iOS**
5. Bundle ID: `fr.paulbr.nookmind`
6. App Store ID: leave blank for now
7. Team ID: paste your Apple Developer Team ID (find it at https://developer.apple.com/account → Membership)
8. Save. Copy the resulting **Client ID** (long string ending in `.apps.googleusercontent.com`)
9. Note: the Console also gives you a **Reversed iOS Client ID** (same string but with `com.googleusercontent.apps.` reversed). You'll need both later.

### 0.C Google Cloud Console — Android OAuth client

You need an **SHA-1 fingerprint** of the Android signing key. For development we use the debug key; for release we'll add the upload + Play managed keys later (Plan 5).

Get the debug SHA-1:
```bash
keytool -keystore ~/.android/debug.keystore -list -v
# password: android (default)
```
(On Windows the path is `%USERPROFILE%\.android\debug.keystore`. On Mac it's `~/.android/debug.keystore`.)

Then in Google Cloud Console:
1. **Create Credentials → OAuth client ID**
2. Application type: **Android**
3. Package name: `fr.paulbr.nookmind`
4. SHA-1 certificate fingerprint: paste the SHA-1 from `keytool` output
5. Save. Copy the resulting **Client ID**

### 0.D Apple Developer — Sign in with Apple setup

(Requires 0.A complete.)

1. https://developer.apple.com/account → Certificates, Identifiers & Profiles → **Identifiers**
2. Find or create the App ID for `fr.paulbr.nookmind`
3. Edit it → tick **Sign In with Apple** capability → Save
4. Back in Identifiers, click **+** to create a new **Services ID** (this is what Supabase uses on the server side):
   - Description: `NookMind Apple Auth`
   - Identifier: `fr.paulbr.nookmind.signin` (any unique reverse-DNS works)
   - Tick **Sign In with Apple** → Configure → Primary App ID = `fr.paulbr.nookmind` → Save
5. Go to **Keys** → **+** → check **Sign In with Apple** → Configure → Primary App ID = `fr.paulbr.nookmind` → Save
6. Download the `.p8` file (you can only download it ONCE — keep it safe)
7. Note the **Key ID** (10-char string) and your **Team ID**

### 0.E Supabase — configure providers

1. Open Supabase dashboard → Authentication → Providers
2. **Google provider:**
   - Should already be enabled with the Web client. Verify.
   - Under "Authorized client IDs (for native sign-in)" or "Additional Client IDs", add the **iOS Client ID** and **Android Client ID** from 0.B and 0.C, comma-separated.
   - Save.
3. **Apple provider:**
   - Enable.
   - Service ID: `fr.paulbr.nookmind.signin` (from 0.D step 4)
   - Team ID: your Apple Team ID
   - Key ID: from 0.D step 7
   - Secret key contents: paste the contents of the `.p8` file from 0.D step 6
   - Save.

### 0.F Local environment

Create or update `.env.local` (gitignored) with the new client IDs:

```
# Native Google Sign-In
VITE_GOOGLE_AUTH_WEB_CLIENT_ID=<the existing web client ID>.apps.googleusercontent.com
VITE_GOOGLE_AUTH_IOS_CLIENT_ID=<from 0.B>.apps.googleusercontent.com
VITE_GOOGLE_AUTH_IOS_REVERSED_CLIENT_ID=com.googleusercontent.apps.<reversed iOS client ID>
```

The Web client ID is critical — it's what Supabase validates ID tokens against, and the iOS/Android plugins use it as `serverClientId` to mint tokens that Supabase will accept.

### 0.G iOS Info.plist URL scheme (manual edit)

This is a one-time edit because `cap sync` won't add it. Open `ios/App/App/Info.plist` in a text editor and add (anywhere inside the top-level `<dict>`):

```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>com.googleusercontent.apps.YOUR_IOS_CLIENT_ID</string>
    </array>
  </dict>
</array>
```

Replace `com.googleusercontent.apps.YOUR_IOS_CLIENT_ID` with the actual reversed iOS client ID from 0.F.

This is required for the Google native sheet to redirect back to the app after sign-in.

If `CFBundleURLTypes` already exists in your Info.plist (e.g. from Plan 4 deep-link work), append to its array rather than redeclaring.

### Confirming Phase 0 is done

Before starting Task 1, confirm:

- [ ] Apple Developer membership is **active**
- [ ] Three Google OAuth client IDs exist (Web — pre-existing, iOS — new, Android — new)
- [ ] Apple Service ID + Key + `.p8` file exist
- [ ] Supabase Google provider has all 3 client IDs as additional client IDs
- [ ] Supabase Apple provider is configured with Service ID + Team ID + Key ID + secret key
- [ ] `.env.local` has `VITE_GOOGLE_AUTH_WEB_CLIENT_ID`, `VITE_GOOGLE_AUTH_IOS_CLIENT_ID`, `VITE_GOOGLE_AUTH_IOS_REVERSED_CLIENT_ID`
- [ ] `ios/App/App/Info.plist` has the iOS URL scheme entry

---

## Task 1: Install native auth plugins + Capacitor config

**Files:**
- Modify: `package.json`
- Modify: `capacitor.config.ts`

- [ ] **Step 1: Install the two auth plugins**

```bash
npm install @codetrix-studio/capacitor-google-auth @capacitor-community/apple-sign-in
```

Expected: both packages added to `dependencies`. No peer-dep warnings about Capacitor version (both must be Capacitor 6/7/8 compatible — the published versions are).

- [ ] **Step 2: Update `capacitor.config.ts`**

Read the current file. Add a `GoogleAuth` block inside the existing `plugins: { ... }` object. After the change, the `plugins` block should look like:

```ts
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
    serverClientId: process.env.VITE_GOOGLE_AUTH_WEB_CLIENT_ID,
    forceCodeForRefreshToken: true,
  },
},
```

Note: Capacitor reads env vars at native build time via `cap sync`. The web client ID will be inlined into the generated native project files. If the env var is undefined when `cap sync` runs, the plugin will fail at runtime with a clear error.

- [ ] **Step 3: Sync and verify**

```bash
npm run cap:sync
```

Expected: sync completes. `npx cap ls` should now show `@codetrix-studio/capacitor-google-auth` and `@capacitor-community/apple-sign-in` among the listed plugins.

- [ ] **Step 4: Commit**

```bash
git add package.json package-lock.json capacitor.config.ts
git commit -m "chore: install native Google Auth and Apple Sign-In plugins"
```

---

## Task 2: iOS Sign-in-with-Apple entitlement

**Files:**
- Create: `ios/App/App/App.entitlements`
- Modify: `ios/App/App.xcodeproj/project.pbxproj`

The Sign-in-with-Apple capability requires an Xcode entitlement file referenced by the project. Capacitor doesn't generate this. We'll create it manually and tell the project to use it.

- [ ] **Step 1: Create `ios/App/App/App.entitlements`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>com.apple.developer.applesignin</key>
  <array>
    <string>Default</string>
  </array>
</dict>
</plist>
```

- [ ] **Step 2: Reference the entitlements in the Xcode project**

Open `ios/App/App.xcodeproj/project.pbxproj` in a text editor. Find both `XCBuildConfiguration` sections for the `App` target (one is "Debug", one is "Release" — both have `name = Debug;` / `name = Release;` lines and contain `PRODUCT_BUNDLE_IDENTIFIER = fr.paulbr.nookmind;`).

Inside each of those config sections, add a line:

```
CODE_SIGN_ENTITLEMENTS = App/App.entitlements;
```

Place it alongside `PRODUCT_BUNDLE_IDENTIFIER`. The order doesn't matter alphabetically; the file is grouped by target+config, not alphabetically sorted.

If `CODE_SIGN_ENTITLEMENTS` already exists in those config sections (e.g. from Plan 4 work), do nothing — both plans want the same entitlements file.

- [ ] **Step 3: Verify the change is well-formed**

```bash
git diff ios/App/App.xcodeproj/project.pbxproj
```

Expected: 2 lines added (one per build config), each `CODE_SIGN_ENTITLEMENTS = App/App.entitlements;`. No other lines changed.

- [ ] **Step 4: Commit**

```bash
git add ios/App/App/App.entitlements ios/App/App.xcodeproj/project.pbxproj
git commit -m "feat(ios): add Sign in with Apple entitlement"
```

---

## Task 3: Storage helper (Capacitor Preferences wrapper) — TDD

**Files:**
- Create: `src/lib/storage.ts`
- Create: `src/lib/storage.test.ts`

A thin async wrapper over Capacitor Preferences that falls back to `localStorage` on web. Used for the onboarding flag (Task 4) and any future persistent flags.

- [ ] **Step 1: Write failing tests**

Create `src/lib/storage.test.ts`:

```ts
import { describe, expect, it, beforeEach, vi } from 'vitest';

vi.mock('@capacitor/preferences', () => ({
  Preferences: {
    get: vi.fn(),
    set: vi.fn(),
    remove: vi.fn(),
  },
}));

vi.mock('@capacitor/core', () => ({
  Capacitor: {
    isNativePlatform: vi.fn(),
  },
}));

import { Preferences } from '@capacitor/preferences';
import { Capacitor } from '@capacitor/core';
import { storageGet, storageSet, storageRemove } from './storage';

describe('storage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('on web', () => {
    beforeEach(() => {
      vi.mocked(Capacitor.isNativePlatform).mockReturnValue(false);
    });

    it('writes via localStorage and reads it back', async () => {
      await storageSet('foo', 'bar');
      expect(localStorage.getItem('foo')).toBe('bar');
      await expect(storageGet('foo')).resolves.toBe('bar');
    });

    it('returns null for missing keys', async () => {
      await expect(storageGet('missing')).resolves.toBeNull();
    });

    it('removes a key', async () => {
      localStorage.setItem('foo', 'bar');
      await storageRemove('foo');
      expect(localStorage.getItem('foo')).toBeNull();
    });

    it('does not call Capacitor Preferences on web', async () => {
      await storageSet('foo', 'bar');
      await storageGet('foo');
      await storageRemove('foo');
      expect(Preferences.set).not.toHaveBeenCalled();
      expect(Preferences.get).not.toHaveBeenCalled();
      expect(Preferences.remove).not.toHaveBeenCalled();
    });
  });

  describe('on native', () => {
    beforeEach(() => {
      vi.mocked(Capacitor.isNativePlatform).mockReturnValue(true);
    });

    it('writes via Capacitor Preferences', async () => {
      vi.mocked(Preferences.set).mockResolvedValue(undefined);
      await storageSet('foo', 'bar');
      expect(Preferences.set).toHaveBeenCalledWith({ key: 'foo', value: 'bar' });
    });

    it('reads via Capacitor Preferences', async () => {
      vi.mocked(Preferences.get).mockResolvedValue({ value: 'bar' });
      await expect(storageGet('foo')).resolves.toBe('bar');
      expect(Preferences.get).toHaveBeenCalledWith({ key: 'foo' });
    });

    it('returns null when Preferences returns null value', async () => {
      vi.mocked(Preferences.get).mockResolvedValue({ value: null });
      await expect(storageGet('missing')).resolves.toBeNull();
    });

    it('removes via Capacitor Preferences', async () => {
      vi.mocked(Preferences.remove).mockResolvedValue(undefined);
      await storageRemove('foo');
      expect(Preferences.remove).toHaveBeenCalledWith({ key: 'foo' });
    });
  });
});
```

- [ ] **Step 2: Run tests — should fail**

```bash
npm run test -- storage
```

Expected: FAIL — `Cannot find module './storage'`.

- [ ] **Step 3: Write the implementation**

Create `src/lib/storage.ts`:

```ts
import { Preferences } from '@capacitor/preferences';
import { isNative } from './platform';

/**
 * Cross-platform persistent key/value storage.
 * On web: localStorage. On native: Capacitor Preferences (survives app updates).
 */

export async function storageGet(key: string): Promise<string | null> {
  if (isNative()) {
    const { value } = await Preferences.get({ key });
    return value ?? null;
  }
  return localStorage.getItem(key);
}

export async function storageSet(key: string, value: string): Promise<void> {
  if (isNative()) {
    await Preferences.set({ key, value });
    return;
  }
  localStorage.setItem(key, value);
}

export async function storageRemove(key: string): Promise<void> {
  if (isNative()) {
    await Preferences.remove({ key });
    return;
  }
  localStorage.removeItem(key);
}
```

- [ ] **Step 4: Run tests — should pass**

```bash
npm run test -- storage
```

Expected: PASS — 8 tests.

- [ ] **Step 5: Commit**

```bash
git add src/lib/storage.ts src/lib/storage.test.ts
git commit -m "feat(storage): cross-platform key-value storage with native fallback"
```

---

## Task 4: Migrate onboarding flag to storage helper

**Files:**
- Modify: `src/context/AuthContext.tsx`
- Modify: `src/components/SettingsPanel.tsx`

The onboarding flag `nookmind_onboarding_completed` is currently accessed via `localStorage` in two places. Migrate them both to the async storage helper.

This task is small but necessary so that on iOS native, the flag survives app updates (WKWebView's `localStorage` is wiped when the app version changes).

- [ ] **Step 1: Update `src/context/AuthContext.tsx`**

The `signOut` function currently has:

```ts
const signOut = async () => {
  await supabase.auth.signOut();
  localStorage.removeItem('nookmind_onboarding_completed');
};
```

Replace with:

```ts
const signOut = async () => {
  await supabase.auth.signOut();
  await storageRemove('nookmind_onboarding_completed');
};
```

Also add at the top of the file:

```ts
import { storageRemove } from '../lib/storage';
```

- [ ] **Step 2: Update `src/components/SettingsPanel.tsx`**

Find the `handleReplayOnboarding` function (around line 150):

```ts
const handleReplayOnboarding = () => {
  localStorage.removeItem('nookmind_onboarding_completed');
  onClose();
  navigate('/onboarding');
};
```

Replace with:

```ts
const handleReplayOnboarding = async () => {
  await storageRemove('nookmind_onboarding_completed');
  onClose();
  navigate('/onboarding');
};
```

Also add at the top of the file:

```ts
import { storageRemove } from '../lib/storage';
```

- [ ] **Step 3: Find any other localStorage read of the onboarding flag**

```bash
grep -rn "nookmind_onboarding_completed" src/
```

If `src/pages/Onboarding.tsx` or any other file reads/writes this key directly via `localStorage`, also migrate those calls to `storageGet` / `storageSet` from the helper. Show all changes via `git diff`.

- [ ] **Step 4: Run tests + build**

```bash
npm run test
npm run build
```

Expected: all tests pass, build succeeds. (No new tests added in this task — the storage layer is already covered by Task 3.)

- [ ] **Step 5: Commit**

```bash
git add src/context/AuthContext.tsx src/components/SettingsPanel.tsx
# include any other files modified in step 3
git commit -m "refactor: migrate onboarding flag to cross-platform storage helper"
```

---

## Task 5: Native Google Sign-In helper + AuthContext integration — TDD

**Files:**
- Create: `src/lib/auth/googleSignIn.ts`
- Create: `src/lib/auth/auth.test.ts`
- Modify: `src/lib/nativeBoot.ts` — initialize GoogleAuth on app boot
- Modify: `src/context/AuthContext.tsx`

- [ ] **Step 1: Write failing tests**

Create `src/lib/auth/auth.test.ts`:

```ts
import { describe, expect, it, vi, beforeEach } from 'vitest';

vi.mock('@codetrix-studio/capacitor-google-auth', () => ({
  GoogleAuth: {
    initialize: vi.fn(),
    signIn: vi.fn(),
    signOut: vi.fn(),
  },
}));

vi.mock('../supabase', () => ({
  supabase: {
    auth: {
      signInWithIdToken: vi.fn(),
      signInWithOAuth: vi.fn(),
    },
  },
}));

vi.mock('../platform', () => ({
  isNative: vi.fn(),
}));

import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { supabase } from '../supabase';
import { isNative } from '../platform';
import { signInWithGoogleNative } from './googleSignIn';

describe('signInWithGoogleNative', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns an error when not running on native', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const result = await signInWithGoogleNative();
    expect(result.error).toBeInstanceOf(Error);
    expect(result.error?.message).toMatch(/native/i);
    expect(GoogleAuth.signIn).not.toHaveBeenCalled();
  });

  it('calls GoogleAuth.signIn and exchanges the token with Supabase on success', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(GoogleAuth.signIn).mockResolvedValue({
      authentication: { idToken: 'test-id-token', accessToken: 'a', refreshToken: 'r' },
      email: 'u@example.com',
      familyName: '',
      givenName: '',
      id: '1',
      imageUrl: '',
      name: '',
      serverAuthCode: '',
    });
    vi.mocked(supabase.auth.signInWithIdToken).mockResolvedValue({
      data: { session: null, user: null },
      error: null,
    } as never);

    const result = await signInWithGoogleNative();

    expect(GoogleAuth.signIn).toHaveBeenCalledTimes(1);
    expect(supabase.auth.signInWithIdToken).toHaveBeenCalledWith({
      provider: 'google',
      token: 'test-id-token',
    });
    expect(result.error).toBeNull();
  });

  it('returns the Supabase error if signInWithIdToken fails', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(GoogleAuth.signIn).mockResolvedValue({
      authentication: { idToken: 'test-id-token', accessToken: 'a', refreshToken: 'r' },
      email: '', familyName: '', givenName: '', id: '1', imageUrl: '', name: '', serverAuthCode: '',
    });
    const supabaseErr = new Error('supabase fail');
    vi.mocked(supabase.auth.signInWithIdToken).mockResolvedValue({
      data: { session: null, user: null },
      error: supabaseErr,
    } as never);

    const result = await signInWithGoogleNative();
    expect(result.error).toBe(supabaseErr);
  });

  it('returns an error if the Google sheet returns no idToken', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(GoogleAuth.signIn).mockResolvedValue({
      authentication: { idToken: '', accessToken: 'a', refreshToken: 'r' },
      email: '', familyName: '', givenName: '', id: '1', imageUrl: '', name: '', serverAuthCode: '',
    });

    const result = await signInWithGoogleNative();
    expect(result.error).toBeInstanceOf(Error);
    expect(result.error?.message).toMatch(/id ?token/i);
    expect(supabase.auth.signInWithIdToken).not.toHaveBeenCalled();
  });

  it('returns an error if the user cancels the Google sheet', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(GoogleAuth.signIn).mockRejectedValue(new Error('User cancelled'));

    const result = await signInWithGoogleNative();
    expect(result.error).toBeInstanceOf(Error);
    expect(supabase.auth.signInWithIdToken).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run tests — should fail**

```bash
npm run test -- auth
```

Expected: FAIL — `Cannot find module './googleSignIn'`.

- [ ] **Step 3: Write the implementation**

Create `src/lib/auth/googleSignIn.ts`:

```ts
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { supabase } from '../supabase';
import { isNative } from '../platform';

export async function signInWithGoogleNative(): Promise<{ error: Error | null }> {
  if (!isNative()) {
    return { error: new Error('signInWithGoogleNative called outside a native runtime') };
  }

  let idToken: string;
  try {
    const result = await GoogleAuth.signIn();
    idToken = result.authentication?.idToken ?? '';
  } catch (err) {
    return { error: err instanceof Error ? err : new Error(String(err)) };
  }

  if (!idToken) {
    return { error: new Error('Google sign-in returned no idToken') };
  }

  const { error } = await supabase.auth.signInWithIdToken({
    provider: 'google',
    token: idToken,
  });
  return { error: error as Error | null };
}

/**
 * Initialize the GoogleAuth plugin once at app boot. Safe to call on web (no-op).
 * Called from src/lib/nativeBoot.ts.
 */
export async function initGoogleAuth(): Promise<void> {
  if (!isNative()) return;
  try {
    await GoogleAuth.initialize({
      clientId: import.meta.env.VITE_GOOGLE_AUTH_WEB_CLIENT_ID,
      scopes: ['profile', 'email'],
      grantOfflineAccess: true,
    });
  } catch {
    // initialize() can fail benignly if called twice; the plugin tolerates that
  }
}
```

- [ ] **Step 4: Wire init into `nativeBoot`**

Edit `src/lib/nativeBoot.ts`. Add the import:

```ts
import { initGoogleAuth } from './auth/googleSignIn';
```

Inside the `nativeBoot` function, after the `if (!isNative()) return;` line and before the status-bar block, add:

```ts
  await initGoogleAuth();
```

- [ ] **Step 5: Update AuthContext to use the native helper**

Edit `src/context/AuthContext.tsx`. Add imports:

```ts
import { isNative } from '../lib/platform';
import { signInWithGoogleNative } from '../lib/auth/googleSignIn';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
```

Replace the existing `signInWithGoogle` function with:

```ts
const signInWithGoogle = async () => {
  if (isNative()) {
    return signInWithGoogleNative();
  }
  const { error } = await supabase.auth.signInWithOAuth({
    provider: 'google',
    options: { redirectTo: `${window.location.origin}/auth/callback` },
  });
  return { error: error as Error | null };
};
```

Update `signOut` to also clear the native Google session:

```ts
const signOut = async () => {
  if (isNative()) {
    try { await GoogleAuth.signOut(); } catch { /* not signed in via Google */ }
  }
  await supabase.auth.signOut();
  await storageRemove('nookmind_onboarding_completed');
};
```

- [ ] **Step 6: Run tests + build**

```bash
npm run test
npm run build
npm run cap:sync
```

Expected: all tests pass (5 new in `auth.test.ts` + existing 28 = 33), build succeeds, cap:sync succeeds.

- [ ] **Step 7: Commit**

```bash
git add src/lib/auth/googleSignIn.ts src/lib/auth/auth.test.ts src/lib/nativeBoot.ts src/context/AuthContext.tsx
git commit -m "feat(auth): native Google sign-in via system sheet + Supabase signInWithIdToken"
```

---

## Task 6: Sign in with Apple (iOS) — TDD

**Files:**
- Create: `src/lib/auth/appleSignIn.ts`
- Modify: `src/lib/auth/auth.test.ts` — add Apple tests
- Modify: `src/context/AuthContext.tsx` — add `signInWithApple` to context

- [ ] **Step 1: Add failing tests for Apple flow**

Append to `src/lib/auth/auth.test.ts`, OUTSIDE the existing `describe` block:

```ts
vi.mock('@capacitor-community/apple-sign-in', () => ({
  SignInWithApple: {
    authorize: vi.fn(),
  },
}));

import { SignInWithApple } from '@capacitor-community/apple-sign-in';
import { signInWithAppleNative } from './appleSignIn';

describe('signInWithAppleNative', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns an error on web', async () => {
    vi.mocked(isNative).mockReturnValue(false);
    const result = await signInWithAppleNative();
    expect(result.error).toBeInstanceOf(Error);
    expect(SignInWithApple.authorize).not.toHaveBeenCalled();
  });

  it('exchanges the Apple identity token with Supabase on success', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(SignInWithApple.authorize).mockResolvedValue({
      response: {
        identityToken: 'apple-id-token',
        authorizationCode: 'auth-code',
        email: 'u@privaterelay.appleid.com',
        familyName: '',
        givenName: '',
        user: 'apple-user-id',
      },
    });
    vi.mocked(supabase.auth.signInWithIdToken).mockResolvedValue({
      data: { session: null, user: null },
      error: null,
    } as never);

    const result = await signInWithAppleNative();

    expect(SignInWithApple.authorize).toHaveBeenCalledWith({
      clientId: 'fr.paulbr.nookmind.signin',
      redirectURI: 'https://paulbr.fr/auth/callback',
      scopes: 'email name',
      state: expect.any(String),
      nonce: expect.any(String),
    });
    expect(supabase.auth.signInWithIdToken).toHaveBeenCalledWith(
      expect.objectContaining({
        provider: 'apple',
        token: 'apple-id-token',
      })
    );
    expect(result.error).toBeNull();
  });

  it('returns an error when authorize returns no identityToken', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(SignInWithApple.authorize).mockResolvedValue({
      response: { identityToken: '', authorizationCode: '', email: '', familyName: '', givenName: '', user: '' },
    });

    const result = await signInWithAppleNative();
    expect(result.error).toBeInstanceOf(Error);
    expect(supabase.auth.signInWithIdToken).not.toHaveBeenCalled();
  });

  it('propagates user-cancellation errors', async () => {
    vi.mocked(isNative).mockReturnValue(true);
    vi.mocked(SignInWithApple.authorize).mockRejectedValue(new Error('The user canceled the authorization attempt.'));

    const result = await signInWithAppleNative();
    expect(result.error).toBeInstanceOf(Error);
    expect(supabase.auth.signInWithIdToken).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run tests — should fail**

```bash
npm run test -- auth
```

Expected: FAIL — `Cannot find module './appleSignIn'`.

- [ ] **Step 3: Write the implementation**

Create `src/lib/auth/appleSignIn.ts`:

```ts
import { SignInWithApple } from '@capacitor-community/apple-sign-in';
import { supabase } from '../supabase';
import { isNative } from '../platform';

const APPLE_SERVICE_ID = 'fr.paulbr.nookmind.signin';
const APPLE_REDIRECT_URI = 'https://paulbr.fr/auth/callback';

function randomString(length = 32): string {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('').slice(0, length);
}

export async function signInWithAppleNative(): Promise<{ error: Error | null }> {
  if (!isNative()) {
    return { error: new Error('signInWithAppleNative called outside a native runtime') };
  }

  const nonce = randomString();
  const state = randomString();

  let identityToken: string;
  try {
    const result = await SignInWithApple.authorize({
      clientId: APPLE_SERVICE_ID,
      redirectURI: APPLE_REDIRECT_URI,
      scopes: 'email name',
      state,
      nonce,
    });
    identityToken = result.response.identityToken ?? '';
  } catch (err) {
    return { error: err instanceof Error ? err : new Error(String(err)) };
  }

  if (!identityToken) {
    return { error: new Error('Apple sign-in returned no identity token') };
  }

  const { error } = await supabase.auth.signInWithIdToken({
    provider: 'apple',
    token: identityToken,
    nonce,
  });
  return { error: error as Error | null };
}
```

- [ ] **Step 4: Add `signInWithApple` to AuthContext**

Edit `src/context/AuthContext.tsx`. Update the `AuthContextValue` interface:

```ts
interface AuthContextValue {
  user: User | null;
  session: Session | null;
  loading: boolean;
  signIn: (email: string, password: string) => Promise<{ error: Error | null }>;
  signUp: (email: string, password: string) => Promise<{ error: Error | null }>;
  signOut: () => Promise<void>;
  signInWithGoogle: () => Promise<{ error: Error | null }>;
  signInWithApple: () => Promise<{ error: Error | null }>;
}
```

Add the import:

```ts
import { signInWithAppleNative } from '../lib/auth/appleSignIn';
```

Add the implementation inside `AuthProvider`:

```ts
const signInWithApple = async () => {
  if (!isNative()) {
    return { error: new Error('Apple sign-in is only available in the native iOS app') };
  }
  return signInWithAppleNative();
};
```

Wire it into the provider value:

```tsx
<AuthContext.Provider value={{ user, session, loading, signIn, signUp, signOut, signInWithGoogle, signInWithApple }}>
```

- [ ] **Step 5: Run tests + build**

```bash
npm run test
npm run build
npm run cap:sync
```

Expected: all tests pass (4 new in Apple tests = 37 total), build succeeds.

- [ ] **Step 6: Commit**

```bash
git add src/lib/auth/appleSignIn.ts src/lib/auth/auth.test.ts src/context/AuthContext.tsx
git commit -m "feat(auth): native Sign in with Apple for iOS"
```

---

## Task 7: Login UI — Sign in with Apple button on iOS

**Files:**
- Modify: `src/pages/Login.tsx`
- Modify: `src/i18n/locales/en.ts`
- Modify: `src/i18n/locales/fr.ts`

- [ ] **Step 1: Add i18n strings**

In `src/i18n/locales/en.ts`, find the `login` section and add:

```ts
login: {
  // ... existing strings
  continueWithApple: 'Continue with Apple',
},
```

In `src/i18n/locales/fr.ts`:

```ts
login: {
  // ... existing strings
  continueWithApple: 'Continuer avec Apple',
},
```

- [ ] **Step 2: Update `src/pages/Login.tsx`**

Add imports at the top:

```ts
import { isIOS } from '../lib/platform';
```

Update the destructured auth hooks:

```ts
const { user, signIn, signUp, signInWithGoogle, signInWithApple } = useAuth();
```

Add a state for Apple loading:

```ts
const [appleLoading, setAppleLoading] = useState(false);
```

After the existing Google button (around line 152, inside the `card` div, after the closing `</button>` of Google), insert the Apple button:

```tsx
{isIOS() && (
  <button
    onClick={async () => {
      setAppleLoading(true);
      const { error } = await signInWithApple();
      if (error) setError(error.message);
      setAppleLoading(false);
    }}
    disabled={appleLoading}
    className="w-full flex items-center justify-center gap-3 py-2.5 px-4 rounded-xl bg-black text-white text-sm font-medium hover:bg-gray-900 transition-colors disabled:opacity-50"
  >
    {appleLoading ? (
      <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
    ) : (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
        <path d="M17.05 20.28c-.98.95-2.05.8-3.08.35-1.09-.46-2.09-.48-3.24 0-1.44.62-2.2.44-3.06-.35C2.79 15.25 3.51 7.59 9.05 7.31c1.35.07 2.29.74 3.08.8 1.18-.24 2.31-.93 3.57-.84 1.51.12 2.65.72 3.4 1.8-3.12 1.87-2.38 5.98.48 7.13-.57 1.5-1.31 2.99-2.54 4.09l.01-.01zM12.03 7.25c-.15-2.23 1.66-4.07 3.74-4.25.29 2.58-2.34 4.5-3.74 4.25z"/>
      </svg>
    )}
    {t('login.continueWithApple')}
  </button>
)}
```

The Apple button **only renders on iOS** (`isIOS()` returns true). On Android and web it's not rendered at all — Apple's rule applies to iOS App Store submissions only.

- [ ] **Step 3: Verify in browser**

```bash
npm run dev
```

Open http://localhost:5173 → should NOT see the Apple button (web, not iOS). Existing Google button still works for web OAuth.

- [ ] **Step 4: Run tests + build**

```bash
npm run test
npm run build
npm run cap:sync
```

Expected: pass.

- [ ] **Step 5: Commit**

```bash
git add src/pages/Login.tsx src/i18n/locales/en.ts src/i18n/locales/fr.ts
git commit -m "feat(login): add Sign in with Apple button on iOS"
```

---

## Task 8: Account deletion API endpoint

**Files:**
- Create: `api/auth/delete-account.ts`

This is a Vercel serverless function. Pattern follows `api/push/test.ts`: authenticate via Bearer token, then perform the deletion using the service-role Supabase client.

- [ ] **Step 1: Create `api/auth/delete-account.ts`**

```ts
import type { VercelRequest, VercelResponse } from '@vercel/node';
import { createClient } from '@supabase/supabase-js';

const supabase = createClient(
  process.env.VITE_SUPABASE_URL!,
  process.env.SUPABASE_SERVICE_ROLE_KEY!
);

export default async function handler(req: VercelRequest, res: VercelResponse) {
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

  const userId = user.id;

  // Tables to wipe in order. Must include every table with a user_id FK.
  // Order matters only if cascading is not configured at the DB level — most are independent.
  const tables = [
    'books',
    'movies',
    'series',
    'series_episodes',
    'categories',
    'movie_categories',
    'series_categories',
    'push_subscriptions',
  ];

  for (const table of tables) {
    const { error } = await supabase.from(table).delete().eq('user_id', userId);
    if (error) {
      // Some tables may not exist in this user's DB or may not have a user_id column —
      // log but don't bail, so partial deletion still progresses.
      console.warn('[delete-account] table delete failed', { table, userId, error: error.message });
    }
  }

  // Delete the auth user itself (this revokes all sessions and tokens)
  const { error: deleteUserError } = await supabase.auth.admin.deleteUser(userId);
  if (deleteUserError) {
    console.error('[delete-account] auth user deletion failed', { userId, error: deleteUserError.message });
    return res.status(500).json({ error: deleteUserError.message });
  }

  return res.status(200).json({ ok: true });
}
```

- [ ] **Step 2: Verify the new endpoint compiles**

```bash
npx tsc --noEmit api/auth/delete-account.ts
```

Expected: no errors. (`npm run build` doesn't compile the `api/` folder; Vercel does that during deploy.)

- [ ] **Step 3: Confirm tables list against the actual DB schema**

Run this read-only query against Supabase (via dashboard SQL editor) to list all tables that have a `user_id` column:

```sql
SELECT table_name
FROM information_schema.columns
WHERE column_name = 'user_id'
  AND table_schema = 'public'
ORDER BY table_name;
```

Compare against the `tables` array in step 1. **If any tables exist in the DB that aren't in the array, add them.** If any tables in the array don't exist, remove them. The goal is a complete wipe of the user's data.

If the list mismatches, edit the `tables` array in `api/auth/delete-account.ts` and re-run step 2.

- [ ] **Step 4: Commit**

```bash
git add api/auth/delete-account.ts
git commit -m "feat(api): account deletion endpoint with full data wipe"
```

---

## Task 9: Account deletion UI in SettingsPanel

**Files:**
- Modify: `src/components/SettingsPanel.tsx`
- Modify: `src/i18n/locales/en.ts`, `src/i18n/locales/fr.ts`

- [ ] **Step 1: Add i18n strings**

In `src/i18n/locales/en.ts`, find the `settings` section and add:

```ts
settings: {
  // ... existing
  deleteAccount: 'Delete my account',
  deleteAccountConfirmTitle: 'Delete your NookMind account?',
  deleteAccountConfirmBody: 'This will permanently remove your books, movies, series, categories, and notification subscriptions. This cannot be undone.',
  deleteAccountCancel: 'Cancel',
  deleteAccountConfirm: 'Delete forever',
  deleteAccountSuccess: 'Account deleted',
  deleteAccountFailed: 'Account deletion failed. Please try again or contact support.',
},
```

In `src/i18n/locales/fr.ts`:

```ts
settings: {
  // ... existing
  deleteAccount: 'Supprimer mon compte',
  deleteAccountConfirmTitle: 'Supprimer votre compte NookMind ?',
  deleteAccountConfirmBody: 'Cela supprimera définitivement vos livres, films, séries, catégories et abonnements aux notifications. Cette action est irréversible.',
  deleteAccountCancel: 'Annuler',
  deleteAccountConfirm: 'Supprimer définitivement',
  deleteAccountSuccess: 'Compte supprimé',
  deleteAccountFailed: 'Échec de la suppression. Réessayez ou contactez le support.',
},
```

- [ ] **Step 2: Add delete-account state and handler in `SettingsPanel.tsx`**

Inside `SettingsPanel`, after the existing state declarations (e.g. after `const [editingName, setEditingName]`), add:

```ts
const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
const [deleting, setDeleting] = useState(false);
const { session } = useAuth();
```

Update the `useAuth` destructure at the top of the component to include `session`:

```ts
const { user, signOut, session } = useAuth();
```

(Note: `session` is already exposed by `AuthContext`. We need it for the Bearer token.)

Add the handler:

```ts
const handleDeleteAccount = async () => {
  if (!session?.access_token) return;
  setDeleting(true);
  try {
    const res = await fetch('/api/auth/delete-account', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${session.access_token}`,
        'Content-Type': 'application/json',
      },
    });
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      toast.error(data.error || t('settings.deleteAccountFailed'));
      setDeleting(false);
      return;
    }
    toast.success(t('settings.deleteAccountSuccess'));
    await signOut();
    onClose();
    navigate('/login');
  } catch {
    toast.error(t('settings.deleteAccountFailed'));
    setDeleting(false);
  }
};
```

- [ ] **Step 3: Add the "Delete account" button to the About section**

Locate the About card near the bottom of the component (the section with `APP_VERSION`, `clearCache`, `replayOnboarding`). After the `replayOnboarding` button block, add:

```tsx
<div className="border-t border-black/[0.06] dark:border-white/[0.06] pt-3 mt-1">
  <button
    onClick={() => setDeleteConfirmOpen(true)}
    className="w-full flex items-center justify-center gap-2 py-2 rounded-xl text-sm font-medium text-red-600 dark:text-red-400 hover:bg-red-500/10 transition-colors"
  >
    {t('settings.deleteAccount')}
  </button>
</div>
```

- [ ] **Step 4: Add the confirmation modal**

At the very end of the component's JSX (just before the final closing `</div>` of the root, or alongside the existing `dragPreview && (...)` block), add:

```tsx
{deleteConfirmOpen && (
  <div className="fixed inset-0 z-[90] flex items-center justify-center p-4">
    <div
      className="absolute inset-0 bg-black/60 backdrop-blur-sm"
      onClick={() => !deleting && setDeleteConfirmOpen(false)}
    />
    <div className="relative card max-w-sm w-full p-6 space-y-4">
      <h3 className="font-serif text-lg font-bold text-gray-900 dark:text-gray-100">
        {t('settings.deleteAccountConfirmTitle')}
      </h3>
      <p className="text-sm text-gray-600 dark:text-gray-400">
        {t('settings.deleteAccountConfirmBody')}
      </p>
      <div className="flex gap-2 pt-2">
        <button
          onClick={() => setDeleteConfirmOpen(false)}
          disabled={deleting}
          className="btn-ghost flex-1 disabled:opacity-50"
        >
          {t('settings.deleteAccountCancel')}
        </button>
        <button
          onClick={handleDeleteAccount}
          disabled={deleting}
          className="flex-1 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-sm font-semibold transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {deleting && <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
          {t('settings.deleteAccountConfirm')}
        </button>
      </div>
    </div>
  </div>
)}
```

- [ ] **Step 5: Run tests + build**

```bash
npm run test
npm run build
```

Expected: pass.

- [ ] **Step 6: Commit**

```bash
git add src/components/SettingsPanel.tsx src/i18n/locales/en.ts src/i18n/locales/fr.ts
git commit -m "feat(settings): in-app account deletion with confirmation"
```

---

## Task 10: End-to-end native verification

**Files:** none modified — verification only.

This is hands-on testing on a real device + Mac for iOS. Do NOT skip — Plan 2 introduces store-policy-required flows that absolutely must work.

- [ ] **Step 1: Sync to native**

```bash
npm run cap:sync
```

- [ ] **Step 2: Test on Android**

```bash
npm run cap:android
```

In Android Studio, run on your physical Android phone. On the device:

- [ ] Open the app → Login screen
- [ ] Tap "Continue with Google" → **Native Google sheet appears** (not a browser tab)
- [ ] Choose your account → app receives session, redirects to home
- [ ] Open Settings → scroll to bottom → tap "Delete my account"
- [ ] Confirm dialog → tap "Delete forever"
- [ ] App signs out, returns to Login
- [ ] Try logging back in with the same email — your old data is gone (clean slate)
- [ ] (Recreate the test account if needed for further testing.)

- [ ] **Step 3: Test on iOS Simulator (Mac)**

```bash
git pull
npm install
npm run cap:ios
```

In Xcode, pick an iPhone 15 Pro simulator and run:

- [ ] Login screen → see BOTH "Continue with Google" AND "Continue with Apple" buttons
- [ ] Tap Google → native sheet → sign in
- [ ] Sign out (Settings → Sign out)
- [ ] Tap "Continue with Apple" → native Apple sheet → sign in (use Face ID in simulator: Features → Face ID → Matching Face)
- [ ] App receives session, lands on home
- [ ] Settings → Delete my account → confirm → succeeds, returns to Login

- [ ] **Step 4: Test on physical iPhone (optional but recommended)**

If you have a paired iPhone, plug it in and run from Xcode. Both Google and Apple sheets should look polished. **This is what App Store reviewers will see.**

- [ ] **Step 5: Document any issues**

If anything fails, capture the error and note it. Common issues:
- "Sign-in was cancelled" — usually a misconfigured client ID or missing iOS URL scheme (Phase 0.G)
- "Invalid client ID" from Supabase — the iOS/Android client IDs aren't in the Supabase Google provider's "additional client IDs" (Phase 0.E)
- Apple sign-in returns no token — Service ID misconfigured in Apple Developer Console (Phase 0.D step 4)
- Account deletion 401 — `SUPABASE_SERVICE_ROLE_KEY` env var not set on Vercel (production) or in `.env.local` (dev)

---

## Verification & Definition of Done

Plan 2 is complete when:

- [ ] All 9 implementation tasks committed
- [ ] `npm run test` passes (target ~37 tests after this plan)
- [ ] `npm run build` succeeds
- [ ] `npm run cap:sync` succeeds
- [ ] Native Google sign-in works on Android phone (system sheet, not browser)
- [ ] Native Google sign-in works on iOS Simulator/device (system sheet, not browser)
- [ ] Sign in with Apple works on iOS Simulator/device
- [ ] Account deletion works end-to-end on at least one platform
- [ ] Web OAuth flow on `npm run dev` browser is unchanged (no regression)
- [ ] No PWA install prompt or other regressions on native (Plan 1 behavior preserved)
- [ ] Branch ready to merge

After this plan, the app is **store-policy compliant for auth + account deletion**. Push notifications still don't work natively (Plan 3) and deep links / Universal Links still aren't wired (Plan 4) — but the auth gates are open.

## What's NOT in this plan

- FCM push notifications → **Plan 3**
- Custom URL scheme + Universal Links → **Plan 4**
- App icon / splash assets from designer masters → **Plan 5**
- Privacy policy audit → **Plan 5**
- Store listing copy + screenshots + first submission → **Plan 5**
