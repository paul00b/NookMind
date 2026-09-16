# Haptic feedback + onboarding polish — design

Date: 2026-09-16
Branch: `claude/optimistic-albattani-rj2h54`
Status: approved, ready for implementation plan

## Goal

Make the native app feel alive on device by adding short, intent-named haptic cues to
the moments that already matter: library mutations, ratings and toggles. Add a tick when
an onboarding slide commits, and a user-facing switch to turn all of it off.

## Scope

In scope:

- `NookHaptics`, a semantic haptic vocabulary in `commonMain`.
- `AppPreferences.hapticsEnabled` plus a Settings row.
- Five call sites, all in shared components, so the whole app inherits the feel.
- A tick on onboarding page change.

Explicitly **not** in scope — deferred to a second spec:

- Long-press quick-action menu on library cards.
- Pull-to-refresh on every main screen.

Both were requested and are agreed; they are separate features rather than haptic
wiring, and they inherit haptics for free once this spec ships.

## Findings that shaped the design

Four facts from reading the branch, each of which changed the design:

1. **Onboarding swipe already works.** `OnboardingScreen` uses `HorizontalPager`, whose
   `userScrollEnabled` defaults to `true`. Verified by hand on device: left/right swipe
   moves between the three slides. The original request needs **zero code**; it gets
   only the page-change tick.

2. **`ToastController` is already the app's central "something happened" signal.**
   21 `success` and 12 `error` call sites, and `LibraryRepository.add`/`delete` raise
   theirs centrally. Hooking haptics where toasts are observed covers every library
   mutation in one place instead of chasing individual call sites.

3. **The design system is well factored.** `StarRating`, `NookToggle`, `ChoiceChip`,
   `PillTab` and `Sheet` each exist exactly once. Editing them changes the feel app-wide.

4. **`ScreenshotCatalog` renders components without `AppRoot`.** It calls
   `NookTheme(...)` directly, including `OnboardingScreen`. This constrains how the
   composition local may be declared — see "Default must be a no-op" below.

## Architecture

### `core/ui/Haptics.kt`

New file, alongside `Toasts.kt` in the same controller layer.

```kotlin
enum class HapticCue { CONFIRM, REJECT, TICK, TOGGLE_ON, TOGGLE_OFF }

fun interface NookHaptics { fun perform(cue: HapticCue) }

internal fun nookHaptics(enabled: Boolean, feedback: HapticFeedback): NookHaptics
```

Call sites name the *intent*, never the buzz. Retuning the app's entire feel later is a
single edit to the mapping function.

`nookHaptics` is split out of the `@Composable` factory deliberately: it is the pure half, so
a fake `HapticFeedback` can verify what call sites actually depend on — that a disabled
instance forwards nothing and an enabled one forwards the mapped constant.

The nominal mapping onto Compose's `HapticFeedbackType` (all verified present in
`org.jetbrains.compose.ui:ui:1.8.2`) is below. It is **not** the whole story — see
"Android API levels", where every row degrades on older devices:

| `HapticCue` | `HapticFeedbackType` | Used for |
|---|---|---|
| `CONFIRM` | `Confirm` | successful mutation |
| `REJECT` | `Reject` | failed mutation |
| `TICK` | `SegmentTick` | one step of a continuous value |
| `TOGGLE_ON` | `ToggleOn` | switch turned on |
| `TOGGLE_OFF` | `ToggleOff` | switch turned off |

### Provision

`LocalNookHaptics` is provided in `App.kt`, inside `NookTheme` but not by it. `App.kt` is
where the container, the preferences and the theme already meet, and it wraps `AppRoot`.
`NookTheme` itself stays clean: it is a pure design-system concern used standalone by the
screenshot catalog, and it has no access to `AppPreferences`.

The provided instance reads `hapticsEnabled`. When off, a no-op instance is provided, so
**no call site ever branches on the setting**.

### Default must be a no-op

`LocalNookHaptics` is declared with a no-op default, never
`error("not provided")`. `ScreenshotCatalog` renders 35 screens through `NookTheme`
without `AppRoot`, so an erroring default would break
`./gradlew :composeApp:screenshots`. This is a hard constraint, not a preference.

### `AppPreferences.hapticsEnabled`

Follows the existing `theme` pattern exactly: a `MutableStateFlow` seeded from
`Settings`, exposed as `StateFlow<Boolean>`, with a setter that writes through.

- Key: `nookmind_haptics_enabled`
- Default: **on**

## Call sites

Six sites. The first five are each in a component that exists exactly once, so the whole app
inherits the feel from them:

| Component | Cue | Trigger |
|---|---|---|
| `ToastHost` | `CONFIRM` / `REJECT` | a toast is *raised*, by `ToastKind`; `INFO` is silent |
| `StarRating` | `TICK` | the rounded value changes during tap or drag — once per star, not per pixel, and silent on a tap that changes nothing |
| `NookToggle` | `TOGGLE_ON` / `TOGGLE_OFF` | checked state changes |
| `ChoiceChip`, `PillTab` | `TICK` | selection changes; silent when re-tapping the active chip |
| `OnboardingScreen` | `TICK` | `pagerState.settledPage` changes |
| `SettingsPanel` | `CONFIRM` | the Vibrations switch is turned **on** |

The sixth is the deliberate exception. `NookToggle` reads `LocalNookHaptics` at composition,
which still holds the no-op in the frame where vibrations are switched on — so enabling the
feature was silent, at exactly the moment a user wants proof it works. `SettingsPanel` builds
an instance directly with `nookHaptics(true, rawHaptics)`. It still goes through `HapticCue`
rather than reaching for a raw `HapticFeedbackType`, so it picks up the API fallback; a
direct `HapticFeedbackType.Confirm` is API 30 and would have done nothing on Android 7–10 —
reinstating the very defect it exists to fix.

Note the asymmetry this creates: switching off gives `TOGGLE_OFF`, switching on gives
`CONFIRM`. Every other switch in the app gives the toggle pair both ways. That is intended —
enabling warrants the stronger cue — but it is a decision, not an accident.

The cue is driven by a `SharedFlow` event emitted from `ToastController.show()`, **not** by
diffing the toast list. Review of the first implementation, which diffed the list, found
three defects that the event model removes outright: two toasts raised in the same frame
conflated into one cue that took the *newest* kind (so an error followed by a success
buzzed `Confirm` while an error sat on screen); the de-duplication watermark reset whenever
`MainScaffold` was replaced by the Legal overlay, re-cueing a live toast; and a list diff
cannot express which toasts deserve a cue at all — which the next paragraph needs.

**Automatic failures do not buzz.** `LibraryRepository.fetch()` runs from an `init` block on
auth state, and `AppContainer` builds six such repositories. Opening the app offline
therefore raised up to six error toasts — and would have buzzed `Reject` six times for
something the user never did. Those paths use `errorSilently`; every user-initiated toast
still cues.

Two categories are deliberately excluded. **Navigation and tab switching** fire constantly
and would read as noise. **Sheet dismissal** was considered and cut: closing a sheet
already has unmistakable visual feedback, so a cue adds frequency without information.
Haptics are reserved for moments where the outcome is otherwise ambiguous — did the rating
register, did the toggle flip, did the save succeed. Genuine gesture haptics arrive with
the second spec, where long-press activation and the pull-to-refresh threshold do carry
information the eye cannot yet confirm.

## Android API levels

**This section originally said old devices "get a subset" and accepted it. That was wrong,
and code review caught it.** Disassembling `PlatformHapticFeedback` from
`androidx.compose.ui:ui-android:1.8.2` shows it passes raw platform constants with **no
`SDK_INT` guard and no compat fallback**. An unrecognised constant is dropped silently — no
vibration, no crash, no log.

`minSdk` is 24. `Confirm`(16)/`Reject`(17) are API 30; `SegmentTick`(26), `ToggleOn`(21) and
`ToggleOff`(22) are API **34**. So without a fallback:

| Android | Cues that fire |
|---|---|
| 7–10 (API 24–29) | none |
| 11–13 (API 30–33) | toast confirm/reject only — ratings, chips, toggles and onboarding all silent |
| 14+ (API 34+) | all five |

That is four of the five call sites dead for every user below Android 14, not a minor
degradation on ancient hardware. And the primary test device runs **Android 16 (API 36)**,
so hand-verification cannot reveal it: the feature would be signed off as working while
shipping dead to most of the supported range.

So the fallback ladder **is** built. `toFeedbackType(apiLevel)` takes the level as a
defaulted parameter — `expect val hapticApiLevel` resolves to `Build.VERSION.SDK_INT` on
Android and `Int.MAX_VALUE` on desktop, where haptics are a no-op anyway. Taking the level
as a parameter rather than reading it inside the function is what keeps the mapping
testable: the tests pin every tier on both platforms, which matters because a plain Android
unit test reports `SDK_INT` as 0.

Two tiers. Below 34, `TICK` and `TOGGLE_ON` fall back to `ContextClick` (API 23) and
`TOGGLE_OFF` to `VirtualKey` (API 5), so on/off stay distinct everywhere; below 30,
`CONFIRM` falls back to `ContextClick` and `REJECT` to `LongPress` (API 3).

**The five cues are not all distinct below API 34, and cannot usefully be made so.** Of the
vocabulary Compose exposes, only `ContextClick`, `LongPress` and `VirtualKey` predate
minSdk 24 — three constants for five cues, so the floor is arithmetic, not a design choice.

A third tier routing `TICK` through `TextHandleMove` (API 27) was implemented and then
**reverted**. It would have bought full distinctness from API 30, but the platform gates
that constant on `config_enableHapticTextHandle`, which AOSP defaults to `false` — so on
most devices it produces no vibration at all. That trades a guaranteed-but-colliding buzz
for a distinct-but-absent one, and neither the Android 16 test device (which uses
`SegmentTick`) nor a Pixel (which overlays the flag true) would reveal it. `GestureEnd` is
ungated and API 30, but the platform resolves it and `CONTEXT_CLICK` to the same waveform —
a green test over an unchanged feel, which is worse than an honest collision.

So the collisions are accepted. They fall across unrelated contexts (a chip tick versus a
switch turning on), never within a single interaction, and firing something beats the
silence this replaces. One caveat that cannot be settled from code: some OEMs flatten
several constants onto one waveform regardless, so even the distinct tiers are best-effort.

## No new permission

Approach A uses `View.performHapticFeedback`, which needs no permission and respects the
user's system haptics setting. Confirmed against the built APK: it requests no `VIBRATE`.
A `Vibrator`-based approach would have added one, which matters for a Play Store update.

## Testing

Unit tests in `commonTest`, alongside the existing 56:

- Every `HapticCue` maps to the right `HapticFeedbackType` at each API tier, and the cues
  that must stay distinguishable (`TOGGLE_ON`/`TOGGLE_OFF`, `CONFIRM`/`REJECT`) never
  collapse onto one constant at any tier.
- A disabled instance forwards nothing to a fake `HapticFeedback`; an enabled one forwards
  each cue's mapped constant, in order.
- `AppPreferences.hapticsEnabled` defaults to true, round-trips, and stores the *string*
  `"false"` — the encoding the web app's `localStorage` depends on.

Verification that cannot be unit-tested, to be done on device:

- Each of the five cues fires, and each is distinguishable from the others.
- Opening the app in airplane mode is **silent**, despite the error toasts.
- On a pre-Android-14 device, ratings, chips, toggles and onboarding still vibrate (this
  cannot be checked on the API 36 primary device, and is the failure mode most likely to
  ship unnoticed).
- Turning the Settings switch off silences all of them.
- Turning the OS haptics setting off silences all of them regardless of the in-app switch.
- `./gradlew :composeApp:screenshots` still renders, proving the no-op default holds.

## Rejected alternatives

**`expect`/`actual` + Android `Vibrator`/`VibrationEffect`.** Full control over amplitude
and custom patterns, and it matches the existing `NativeServices` pattern. Rejected: it
needs the `VIBRATE` permission, three actuals, and it bypasses the user's system haptics
preference unless that check is reimplemented by hand — users who have deliberately
turned haptics off would still be buzzed.

**Inline `LocalHapticFeedback` calls at each site.** Smallest diff, but the vocabulary
drifts as sites multiply and there is no single place to retune or globally disable.
