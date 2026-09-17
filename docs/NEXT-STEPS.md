# Where this branch stands, and what is left

Branch: `claude/optimistic-albattani-rj2h54`
Last worked: 2026-09-16

## Done

The haptics feature is complete, reviewed and verified as far as automation allows:

- 5 intent-named cues (`HapticCue`) with an API-level fallback, a Settings switch, and
  automatic background failures deliberately silent
- **140 tests, 0 failures** (70 desktop + 70 Android), 35 screenshots, debug APK builds
- Design: `docs/superpowers/specs/2026-09-16-haptics-design.md`
- Plan and review history: `docs/superpowers/plans/2026-09-16-haptics.md`

Confirmed working on device: all five cues feel right.

Backend status as of the last check — all green:

| | |
|---|---|
| Google Books | 200 (after replacing a key that was returning 503) |
| TMDB | 200 |
| Vercel routes | 200 |
| Supabase | 200 |
| Firebase / push | wired into the debug APK |

## Machine setup — read this first on any machine

Three things live outside git and must exist before anything builds.

**1. `native/secrets.properties`** (git-ignored). Six keys, mirroring the web app's `VITE_*`
values in `.env`. If it is missing, regenerate it from `.env` — the mapping is in
`native/README.md` §2.

> **The `GOOGLE_BOOKS_API_KEY` in `.env` is stale** and returns **503** from Google. A newer key
> replaced it; rebuilding `secrets.properties` from `.env` without carrying the newer one across
> breaks book search again. It is not lost if the local file disappears: Google Cloud Console
> shows API keys in full, so the canonical copy is *APIs & Services → Credentials* in the project
> that owns it. Worth also updating `.env` and the Vercel environment so the web app and the
> native app stop diverging.
>
> This key is not a password. It is compiled into the APK and served inside the web bundle, so
> anyone can read it either way. What actually protects it is the restriction set on it in Google
> Cloud: limit it to the Books API, and to the platforms that should be calling it.

**2. `native/composeApp/google-services.json`** (git-ignored). From Firebase project
`nookmind-8f5be`. It must declare **both** `fr.paulbr.nookmind` and
`fr.paulbr.nookmind.debug`, or the Gradle plugin fails the debug build outright. The current
copy has both.

**3. Build environment (Windows machine specifically).** `native/build-debug.ps1` now does
all of this for you; what follows is what it sets, for reference.
 Java 8 is on `PATH` and cannot
configure the build, and Avast's Web/Mail Shield intercepts TLS with a root CA the JVM does
not trust — which breaks every Gradle download. Every Gradle invocation needs:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStore=C:/Users/brous/.gradle/cacerts-avast -Djavax.net.ssl.trustStorePassword=changeit"
cd native
```

`~/.gradle/cacerts-avast` is a copy of the JBR truststore plus the Avast root CA;
`~/.gradle/gradle.properties` points the Gradle daemon at it. Both are user-level, not in the
repo. Removing Avast's HTTPS scanning would make all of this unnecessary.

Verify everything at once with `./gradlew.bat :composeApp:checkApis` — it reports each secret
and calls every backend with the real HTTP status.

## Remaining steps, in order

### 1. Test the debug build on the phone — *not yet done*

`NookMind-debug.apk` was built with Firebase included. Install it (same package, so data
survives) and check:

- [ ] Search in all three tabs — Livres especially, that was the 503
- [ ] Google sign-in. If it fails: the `google-services.json` lists **zero Android OAuth
      clients** for either package. That is normal when Google Sign-In is not a Firebase Auth
      provider (this app authenticates through Supabase), but it is the first place to look.
      The debug SHA-1 is `09:19:27:C2:25:15:6B:2C:4D:B7:C8:2C:C0:76:79:2C:56:47:15:DF` and
      needs an OAuth **Android** client in Google Cloud for `fr.paulbr.nookmind.debug`.
- [ ] Notifications: Paramètres → enable → grant the permission → *Tester les notifications*
      → receive it → **tap it** and confirm it opens the right screen, not just home

### 2. Find the release keystore — the one real blocker

`nookmind-release.jks` and its passwords are **not on the Windows machine** (searched). Without
it there is no way to ship an update existing users can install — Android refuses a
differently-signed APK over an installed one.

If it is genuinely lost, check Play Console → *Setup → App signing*. If **Play App Signing** is
enabled, Google holds the real signing key and the upload key can be reset — recoverable. If it
is not enabled, that listing can never be updated and the app would have to be republished
under a new package name.

Once found, create `native/keystore.properties`:

```properties
storeFile=/absolute/path/to/nookmind-release.jks
storePassword=…
keyAlias=nookmind
keyPassword=…
```

### 3. Build and test a signed release

```powershell
.\gradlew.bat :composeApp:assembleRelease   # APK
.\gradlew.bat :composeApp:bundleRelease     # AAB for Play
```

Release uses applicationId `fr.paulbr.nookmind` — the published package — so the SHA-1 already
registered with Google works and no console changes are needed.

**Re-test on the release build specifically.** R8 shrinking can break exactly three things,
and nothing else catches them:

- [ ] Google sign-in
- [ ] Receiving a notification
- [ ] External links (streaming platforms) opening

Also bump `versionCode` / `versionName` in `composeApp/build.gradle.kts` (currently 2 / `2.0.0`;
the Capacitor build stopped at 1 / `1.0`).

### 4. What existing users will experience on upgrade

The Capacitor app stored its session in the WebView's `localStorage`; the native app uses
Android preferences. Same keys, different backing store — **nothing migrates automatically**.

| | |
|---|---|
| Books, films, series, collections, notes, progress | Intact — they live in Supabase |
| Session | Lost, one re-login |
| Onboarding | Shown once more |
| Theme, display mode, section order | Reset to defaults |
| Notifications | Must be re-enabled (the FCM token changes) |

Worth testing deliberately: install the native build over the old one and confirm re-login
brings the whole library back.

## Deferred work

**Spec 2, agreed but not written:** long-press quick-action menu on library cards, and
pull-to-refresh on every main screen. Both inherit the haptic vocabulary for free —
long-press activation and the pull threshold are exactly the ambiguous moments haptics serve.

**Pre-existing issues found during review, none introduced by this work:**

- `NookToggle` is invisible to TalkBack — a `Box` with `.clickable`, no `Modifier.toggleable`,
  no `Role.Switch`, no `stateDescription`. Affects all four switches.
- Tapping an already-selected star still issues a network PATCH and a success toast for a
  no-op. The haptic is correctly silent there; the write is not.
- `CollectionChip` looks like a chip, sits directly under a chip row that ticks, and is
  entirely silent — no haptic, and its success path raises no toast either. The most visible
  seam in the current scoping.
- `gen-strings.mjs` had never run on Windows (bare absolute path passed to ESM `import()`).
  Fixed in passing; anyone who regenerated strings before did it elsewhere.

**Untested tier:** the API fallback cannot be verified on the Android 16 phone, which sits on
the top tier where everything works natively. An API 29 or 31 emulator would confirm that
chips, ratings, toggles and onboarding actually vibrate on older devices. Worth doing before a
store release; the system image is a ~1 GB download and no such AVD exists yet.
