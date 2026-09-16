# Haptic feedback + onboarding polish — design

Date: 2026-09-16
Branch: `claude/optimistic-albattani-rj2h54`
Status: approved, ready for implementation plan

## Goal

Make the native app feel alive on device by adding short, intent-named haptic cues to
the moments that already matter: library mutations, ratings, toggles and sheets. Add a
tick when an onboarding slide commits, and a user-facing switch to turn all of it off.

## Scope

In scope:

- `NookHaptics`, a semantic haptic vocabulary in `commonMain`.
- `AppPreferences.hapticsEnabled` plus a Settings row.
- Six call sites, all in shared components, so the whole app inherits the feel.
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
enum class NookHaptic { Confirm, Reject, Tick, ToggleOn, ToggleOff, GestureEnd }

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
| `GestureEnd` | `GestureEnd` | sheet dismissed |

### Provision

`LocalNookHaptics` is provided in `AppRoot`, not in `NookTheme`. `NookTheme` is a pure
design-system concern and is used standalone by the screenshot catalog; haptics depend on
`AppPreferences`, which only `AppRoot` has.

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

Six edits, each in a component that exists once:

| Component | Cue | Trigger |
|---|---|---|
| `ToastHost` | `Confirm` / `Reject` | a new toast appears, by `ToastKind`; `INFO` is silent |
| `StarRating` | `Tick` | the rounded value changes during tap or drag — once per star, not per pixel |
| `NookToggle` | `ToggleOn` / `ToggleOff` | checked state changes |
| `ChoiceChip`, `PillTab` | `Tick` | selection changes; silent when re-tapping the active chip |
| `Sheet` | `GestureEnd` | dismissed, including swipe-to-dismiss |
| `OnboardingScreen` | `Tick` | `pagerState.currentPage` settles on a new page |

`ToastHost` fires on *new* toast ids only, so the 3-second lifetime and recomposition do
not retrigger it.

Navigation and tab switching are deliberately excluded: they fire constantly and would
read as noise.

## Android API levels

`minSdk` is 24, and the expressive constants are newer — `Confirm`/`Reject` require API
30, `ToggleOn`/`ToggleOff`/`SegmentTick` require API 34. Below those levels the platform
ignores the constant rather than crashing, so old devices get a subset.

Accepted for now. A fallback ladder is deliberately not built: it costs an `expect`/`actual`
for the SDK level and the Play Console will show whether meaningful old-API traffic exists.
Adding it later is one mapping function.

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

- Each of the six cues fires, and each is distinguishable from the others.
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
