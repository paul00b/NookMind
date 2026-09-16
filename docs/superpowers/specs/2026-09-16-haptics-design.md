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
enum class NookHaptic { Confirm, Reject, Tick, ToggleOn, ToggleOff }

fun interface NookHaptics { fun perform(haptic: NookHaptic) }
```

Call sites name the *intent*, never the buzz. Retuning the app's entire feel later is a
single edit to the mapping function.

Mapping to Compose's `HapticFeedbackType` (all verified present in
`org.jetbrains.compose.ui:ui:1.8.2`):

| `NookHaptic` | `HapticFeedbackType` | Used for |
|---|---|---|
| `Confirm` | `Confirm` | successful mutation |
| `Reject` | `Reject` | failed mutation |
| `Tick` | `SegmentTick` | one step of a continuous value |
| `ToggleOn` | `ToggleOn` | switch turned on |
| `ToggleOff` | `ToggleOff` | switch turned off |

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

Five edits, each in a component that exists once:

| Component | Cue | Trigger |
|---|---|---|
| `ToastHost` | `Confirm` / `Reject` | a toast is *raised*, by `ToastKind`; `INFO` is silent |
| `StarRating` | `Tick` | the rounded value changes during tap or drag — once per star, not per pixel |
| `NookToggle` | `ToggleOn` / `ToggleOff` | checked state changes |
| `ChoiceChip`, `PillTab` | `Tick` | selection changes; silent when re-tapping the active chip |
| `OnboardingScreen` | `Tick` | `pagerState.settledPage` changes |

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

Four tiers, and the distinctness they buy:

| Tier | `TICK` | Distinct cues |
|---|---|---|
| 34+ | `SegmentTick` | 5 — full |
| 30–33 | `TextHandleMove` | **5 — full** |
| 27–29 | `TextHandleMove` | 4 — `CONFIRM`/`TOGGLE_ON` collide |
| 24–26 | `ContextClick` | 3 — the hard ceiling |

`TOGGLE_OFF` uses `VirtualKey` (API 5) below 34 and `TOGGLE_ON` uses `ContextClick`
(API 23), so on/off stay distinct everywhere. Below 30, `CONFIRM` falls back to
`ContextClick` and `REJECT` to `LongPress` (API 3).

**Below API 27 the cues cannot all be distinct.** Of the vocabulary Compose exposes, only
`ContextClick`, `LongPress` and `VirtualKey` predate API 24 — three constants for five cues,
so the 24–26 ceiling is arithmetic, not a design choice. It is accepted: the collisions fall
across unrelated contexts (a chip tick versus a switch turning on), never within a single
interaction, and firing something beats the silence this replaces. Since API 30+ is the bulk
of the live install base, most users get the full vocabulary.

One caveat that cannot be settled from code: some OEMs flatten several constants onto one
waveform, so `TextHandleMove` and `ContextClick` may not actually feel different on every
device. The tiering is best-effort, not a guarantee.

Taking the level as a parameter rather than reading it inside the function is what keeps the
mapping testable: the tests pin all three tiers on both platforms, which matters because a
plain Android unit test reports `SDK_INT` as 0.

## No new permission

Approach A uses `View.performHapticFeedback`, which needs no permission and respects the
user's system haptics setting. Confirmed against the built APK: it requests no `VIBRATE`.
A `Vibrator`-based approach would have added one, which matters for a Play Store update.

## Testing

Unit tests in `commonTest`, alongside the existing 56:

- The mapping from every `NookHaptic` to a `HapticFeedbackType` is total.
- The disabled instance performs nothing for every enum value.
- `AppPreferences.hapticsEnabled` defaults to true, round-trips, and emits on change.

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
