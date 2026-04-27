# NookMind — Native App via Capacitor

**Date:** 2026-04-27
**Status:** Design approved, awaiting implementation plan
**Author:** Paul (paul.broussolle@helloprimary.care)

## 1. Goal

Ship the existing NookMind PWA (React 19 + Vite + Supabase) to the Apple App Store and Google Play Store as native iOS and Android apps, using **Capacitor** as the wrapper. The web app on Vercel continues to work as a first-class PWA — same codebase, three deploy targets.

## 2. Why Capacitor (over alternatives)

| Option | Verdict | Reason |
|---|---|---|
| **Capacitor** | ✅ Chosen | Wraps existing React/Vite app with no UI rewrite. Modern Cordova successor. First-class plugin ecosystem. Works with Vite-PWA. |
| React Native / Flutter | ❌ | Full UI rewrite. Throws away existing React app. |
| PWA only | ❌ | App Store won't accept; iOS PWA support uneven. |
| Cordova | ❌ | Legacy. Capacitor is its modern successor. |
| Tauri Mobile | ❌ | Still maturing. No Vite-PWA story. |

## 3. Key decisions (locked in)

| Decision | Choice |
|---|---|
| Bundle ID | `fr.paulbr.nookmind` (reverse-DNS of owned domain) |
| Display name | `NookMind` |
| Dev environment split | Windows for daily dev + Android; Mac for iOS-specific work only |
| Web/native coexistence | Both maintained (single codebase) |
| Google sign-in (native) | Native sheet → Supabase `signInWithIdToken` |
| Sign in with Apple | **Required** for App Store approval — included in v1 |
| Account deletion in-app | **Required** for both stores — included in v1 |
| Push transport | Web → web-push (existing); Native → FCM (new); single dispatcher picks per row |
| Service worker | Disabled inside Capacitor shell |
| Deep links v1 | Custom scheme `nookmind://` + Universal/App Links on `paulbr.fr` |
| Monetization v1 | Free, no IAP |
| OTA updates v1 | Out of scope (defer to v2 if store cycles become painful) |
| Asset creation | Owned by Paul (designer) |

## 4. Architecture

```
┌──────────────────────────────────────────┐
│  src/  (React code — unchanged)          │
└──────────────────────────────────────────┘
            │ vite build
            ▼
┌──────────────────────────────────────────┐
│  dist/  (web assets)                     │
└──────────────────────────────────────────┘
     │              │              │
     ▼              ▼              ▼
  Vercel        Capacitor      Capacitor
  (PWA)         iOS shell     Android shell
                  ▼              ▼
              Xcode (Mac)    Android Studio
                  ▼              ▼
              App Store      Play Store
```

**Repo additions:** `capacitor.config.ts`, `ios/`, `android/`, several `@capacitor/*` packages, asset master files.

**Backend unchanged:** Vercel `/api/*` functions and Supabase remain as-is. Native apps call them as a remote API.

**Code touchpoints:** ~5 small platform-specific gates via `Capacitor.isNativePlatform()`. No component refactor.

## 5. Capacitor setup

### Packages

```
@capacitor/core
@capacitor/cli                                 (dev)
@capacitor/ios
@capacitor/android
@capacitor/app                                 lifecycle, deep link events
@capacitor/splash-screen
@capacitor/status-bar
@capacitor/preferences                         survives app updates (replaces some localStorage)
@capacitor/push-notifications
@capacitor/browser                             external links
@capacitor/keyboard                            iOS input handling
@capacitor/assets                              (dev) icon/splash generator
@codetrix-studio/capacitor-google-auth         native Google sheet
@capacitor-community/apple-sign-in             Sign in with Apple
```

### `capacitor.config.ts`

```ts
{
  appId: 'fr.paulbr.nookmind',
  appName: 'NookMind',
  webDir: 'dist',
  server: { androidScheme: 'https' },
  plugins: {
    SplashScreen: { launchShowDuration: 1500, backgroundColor: '#0f1117' },
    PushNotifications: { presentationOptions: ['badge', 'sound', 'alert'] },
    GoogleAuth: { scopes: ['profile', 'email'], serverClientId: '<web-oauth-client-id>' }
  }
}
```

### Scripts (in `package.json`)

```
"cap:sync":    "vite build && cap sync"
"cap:android": "npm run cap:sync && cap open android"
"cap:ios":     "npm run cap:sync && cap open ios"
```

### Git policy

`ios/` and `android/` directories are committed. `ios/App/Pods` and `android/.gradle` are gitignored (Capacitor's default `.gitignore` handles this).

## 6. Authentication

### Google sign-in flow on native

```
Tap "Continue with Google"
   → Native Google Sign-In sheet (iOS / Android system UI)
   → Returns Google ID token (JWT)
   → supabase.auth.signInWithIdToken({ provider: 'google', token })
   → Supabase validates, creates session
   → Existing onAuthStateChange listener fires (unchanged)
```

### Sign in with Apple (new in v1)

Required by Apple's App Store Review Guidelines (4.8) when any third-party sign-in is offered. Apple's rule applies to the iOS app submission — Play Store does not require it.

```
Tap "Sign in with Apple"  (iOS only — button hidden on Android and web)
   → Native Apple sheet
   → Returns Apple ID token (JWT) + nonce
   → supabase.auth.signInWithIdToken({ provider: 'apple', token, nonce })
   → Same downstream flow as Google
```

### Google Cloud Console — three OAuth client IDs

| Client type | Used by |
|---|---|
| Web (existing) | Supabase OAuth callback for PWA |
| iOS (new) | iOS native sheet |
| Android (new) | Android native sheet (requires SHA-1 of every signing key) |

All three configured as "additional client IDs" in Supabase's Google provider so Supabase accepts ID tokens minted for any of them.

### Code change

`src/context/AuthContext.tsx#signInWithGoogle` becomes platform-aware:

```ts
if (Capacitor.isNativePlatform()) {
  const result = await GoogleAuth.signIn();
  return supabase.auth.signInWithIdToken({
    provider: 'google',
    token: result.authentication.idToken,
  });
}
// existing web OAuth redirect — unchanged
```

`signOut` also calls `GoogleAuth.signOut()` and `AppleSignIn.signOut()` on native to clear cached system sessions.

### Account deletion (Settings page)

New "Delete my account" button → confirms → calls a new Vercel API route or Supabase RPC that:
1. Deletes user rows across all tables (books, movies, series, categories, push_subscriptions)
2. Deletes the auth user via Supabase admin SDK
3. Signs out

## 7. Push notifications

### Database migration

Add columns to `push_subscriptions` (or create new `device_tokens` table — chosen during implementation):

```sql
ALTER TABLE push_subscriptions
  ADD COLUMN transport text NOT NULL DEFAULT 'webpush'
    CHECK (transport IN ('webpush', 'fcm')),
  ADD COLUMN fcm_token text;
```

### Firebase setup

1. Create Firebase project `nookmind`.
2. Add Android app `fr.paulbr.nookmind` → download `google-services.json` → place in `android/app/`.
3. Add iOS app `fr.paulbr.nookmind` → download `GoogleService-Info.plist` → place in `ios/App/App/`.
4. Generate APNs Authentication Key (`.p8`) in Apple Developer portal → upload to Firebase. Firebase relays APNs through FCM.

### Client flow (native)

```
App launch (post-permission)
  → PushNotifications.requestPermissions()  (after user interaction, never cold)
  → PushNotifications.register()
  → 'registration' event fires with FCM token
  → POST /api/push/subscribe { transport: 'fcm', token, user_id }
```

New hook `useNativePushSubscription` mirrors the existing `useNotificationSubscription` for FCM. Runtime switch picks one.

### Server dispatcher

```ts
for (const sub of subscriptions) {
  if (sub.transport === 'webpush') {
    await webpush.sendNotification(sub, payload);
  } else if (sub.transport === 'fcm') {
    await fcmAdmin.send({ token: sub.fcm_token, notification: payload });
  }
}
```

New env var: `FIREBASE_SERVICE_ACCOUNT_JSON` (service-account credentials).

### Permission UX

Trigger from existing `NotificationPromptSheet` after user interaction (not at cold launch — App Store rejection risk).

## 8. Runtime gating & service worker

### Detector

```ts
// src/lib/platform.ts
import { Capacitor } from '@capacitor/core';
export const isNative = () => Capacitor.isNativePlatform();
export const isIOS = () => Capacitor.getPlatform() === 'ios';
export const isAndroid = () => Capacitor.getPlatform() === 'android';
export const isWeb = () => Capacitor.getPlatform() === 'web';
```

### Touchpoints

| Location | Behavior on native |
|---|---|
| `AuthContext.signInWithGoogle` | Native sheet path |
| `useNotificationSubscription` | Replaced by `useNativePushSubscription` |
| `main.tsx` SW registration | Skipped |
| `useInstallPrompt` | Disabled |
| `InstallPromptSheet` | Not rendered |
| `AppLayout` / `BottomNav` / `MobileTopBar` | Safe-area inset CSS |

### Service worker disablement

In `main.tsx`:

```ts
if (!isNative()) {
  registerSW({ immediate: true });
}
```

In `vite.config.ts`, gate `VitePWA()` on a `VITE_NATIVE_BUILD` env flag set by `cap:sync`. Native builds skip SW generation entirely.

**Why:** Capacitor loads from `capacitor://` (iOS) / `https://localhost` (Android). The SW would try to cache and intercept these URLs, fighting the native shell and causing white-screen-after-update bugs. Web push events also never arrive on native — FCM doesn't go through the SW.

### Safe-area insets

```css
.app-layout {
  padding-top: env(safe-area-inset-top);
  padding-bottom: env(safe-area-inset-bottom);
}
```

`AppLayout.tsx` and `BottomNav.tsx` get this treatment.

### Status bar

```ts
if (isNative()) StatusBar.setStyle({ style: themeIsDark ? Style.Dark : Style.Light });
```

Wired to existing theme context.

### Storage

Onboarding flag (`localStorage.getItem('nookmind_onboarding_completed')`) migrates to `@capacitor/preferences` on native — survives app updates and is reliable inside WKWebView.

## 9. Deep links

### Custom URL scheme — `nookmind://`

- iOS: registered in `Info.plist` `CFBundleURLTypes`
- Android: registered in `AndroidManifest.xml` intent-filter

Used for Supabase email confirmation / password-reset links and any `nookmind://...` deep link.

### Universal Links / App Links — `paulbr.fr/*`

- iOS: `apple-app-site-association` JSON file at `https://paulbr.fr/.well-known/apple-app-site-association` (no extension, `Content-Type: application/json`)
- Android: `assetlinks.json` at `https://paulbr.fr/.well-known/assetlinks.json`

Both files served from `public/.well-known/` in the Vercel-hosted PWA project (assuming PWA continues to be hosted from `paulbr.fr` or a subdomain — to be confirmed during implementation).

### Routing on link arrival

```ts
App.addListener('appUrlOpen', ({ url }) => {
  const path = new URL(url).pathname;
  navigate(path);
});
```

### Supabase redirect URL allowlist

Add in Supabase dashboard:
- `https://<vercel-domain>/auth/callback` (existing)
- `nookmind://auth/callback` (new — fallback)
- `https://paulbr.fr/auth/callback` (if PWA migrates to that domain)

## 10. Build & release pipeline

### Daily (Windows)

```
1. Edit React code (Windows VS Code)
2. npm run dev               (browser test)
3. npm run cap:sync          (web → native projects)
4. npm run cap:android       (Android Studio + emulator)
```

Capacitor live-reload mode points the Android emulator at the Vite dev server (`http://<lan-ip>:5173`) for instant iteration.

### iOS pass (Mac, ~weekly)

```
1. git pull
2. npm install                          (if deps changed)
3. cd ios/App && pod install            (if plugins changed)
4. npm run cap:ios                      (Xcode + Simulator)
```

### Accounts required

| Account | Cost | Purpose |
|---|---|---|
| Apple Developer Program | $99/year | TestFlight, App Store, APNs |
| Google Play Console | $25 one-time | Play Store |
| Firebase | Free | FCM |
| Google Cloud OAuth clients | Free | Native sign-in |

### Signing

**Android:**
- Capacitor scaffolds debug keystore.
- Generate one upload keystore (`.jks`) for release; password in gitignored `.env.local`.
- Enroll in Play App Signing (Google manages release key).
- Add SHA-1 of upload key + Play managed key to Android Google OAuth client.

**iOS:**
- Apple Developer → create App ID `fr.paulbr.nookmind`.
- Xcode "Automatically manage signing".
- TestFlight for internal testing (no review).
- App Store submission with review (24-72h).

### Release path for v1

1. **Internal testing** — TestFlight + Play Internal Testing (1-2 weeks)
2. **Closed beta** — TestFlight external + Play Closed Testing
3. **Production** — App Store + Play Store submission

### Release cadence

- Web: instant on every Vercel deploy.
- Native: every 2-4 weeks realistically. Server-driven feature flags (Supabase) absorb urgent changes without a native release.

### CI/CD

Out of scope for v1. Manual builds. Add GitHub Actions for Android post-launch, then macOS runners or Codemagic for iOS later.

## 11. Store submission requirements

### Apple

- App Privacy "Nutrition Label" questionnaire — declare collected data (Supabase auth, push tokens, IP for analytics).
- **Sign in with Apple** — required because Google sign-in is offered. Built into v1.
- **Account deletion in-app** — required. Built into v1.
- App Tracking Transparency — not required (no cross-app tracking).

### Google Play

- Data Safety form — same answers as Apple Nutrition Label.
- Target API level 34+ (Capacitor 6+ defaults to this).
- Content rating questionnaire.
- Account deletion — required (post-2024 policy). Built into v1.

### Privacy policy

Public URL required. Existing `src/pages/Privacy.tsx` route. Audit and expand to cover:
- Data collected (Supabase auth, push tokens)
- Third-party services (Supabase, Google OAuth, Apple OAuth, Google Books, TMDB, IMDb, Vercel, Firebase, Vercel Analytics)
- GDPR (France-based)
- Account deletion instructions

### Owned by Paul (designer)

- App icon master (1024×1024 PNG, exported from existing SVG)
- Splash background (`#0f1117`)
- Screenshots (App Store: 6.7" iPhone + iPad optional; Play: phone + tablet optional) — ~5-8 polished shots showing home, library, book detail, search, "next up"
- Store listing copy (name, subtitle, description, keywords, category)
- Support URL

A checklist of asset specifications will be included in the implementation plan.

## 12. Risks & mitigations

| Risk | Likelihood | Mitigation |
|---|---|---|
| App Store rejection: missing Sign in with Apple | High | Build in v1 |
| App Store rejection: no in-app account deletion | High | Build in v1 |
| App Store rejection: "looks like a website" | Medium | Bundle assets locally, use native plugins, no remote-URL mode |
| APNs key generation friction | Medium | Use `.p8` key path (not legacy certs); budget 2-3h |
| OAuth SHA-1 mismatch on Android | High at first install | Document keys; add SHA-1 to OAuth Android client after every keystore change |
| WKWebView storage cleared on iOS update | Medium | `@capacitor/preferences` for persistent flags |
| Splash screen sticks too long | Medium | `SplashScreen.hide()` on app hydration, not on timer |
| Safe-area inset clipping | Medium | Audit on real iPhone before submission |
| Keyboard covers iOS inputs | Medium | `@capacitor/keyboard` scroll behavior |
| Hardware back button exits app on Android | Medium | `App.addListener('backButton', ...)` → `navigate(-1)` |
| Pull-to-refresh bounces app | Low | `overscroll-behavior: none` + iOS WebView config |
| External links replace webview | Medium | Force `@capacitor/browser` for external URLs |
| API keys extractable from APK | Low | Same as web today (`VITE_*` already in bundle). Long-term: proxy through Vercel functions. |

## 13. Out of scope for v1

- OTA updates (Capgo / Appflow Live Updates)
- IAP / subscriptions
- Native widgets (iOS Home Screen widgets, Android widgets)
- Apple Watch / Wear OS
- Custom share extensions
- Background sync / background tasks
- CI/CD (manual builds for v1)
- API key proxying through Vercel (security improvement, not regression)

## 14. Success criteria

- [ ] App installs from App Store and Play Store.
- [ ] Google sign-in works on both stores via native sheet.
- [ ] Sign in with Apple works on iOS.
- [ ] Email/password sign-in works on both.
- [ ] Account can be deleted in-app on both.
- [ ] Push notifications received on both via FCM.
- [ ] Web PWA still functional and unchanged for browser users.
- [ ] Email confirmation links open in-app on both.
- [ ] No App Store / Play Store policy violations on submission.
- [ ] App passes both store reviews on first or second submission.
