package fr.paulbr.nookmind.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import fr.paulbr.nookmind.core.platform.hapticApiLevel

/**
 * Intent-named haptic cues. Call sites say what happened, never which buzz — so the whole
 * app's feel can be retuned by editing [toFeedbackType] alone.
 */
enum class HapticCue { CONFIRM, REJECT, TICK, TOGGLE_ON, TOGGLE_OFF }

fun interface NookHaptics {
    fun perform(cue: HapticCue)

    companion object {
        /** Performs nothing. Used when the user has switched vibrations off. */
        val None: NookHaptics = NookHaptics { }
    }
}

/**
 * `internal` is deliberate: it lets `HapticsTest` pin the mapping while keeping call sites
 * unable to reach it and bypass the semantic vocabulary in [HapticCue].
 *
 * Takes [apiLevel] as a defaulted parameter (rather than reading [hapticApiLevel] directly in
 * each branch) so tests can pin every tier without touching the platform. `SegmentTick`(26),
 * `ToggleOn`(21) and `ToggleOff`(22) constants exist below API 34 but are silently dropped by
 * `PlatformHapticFeedback` there; `Confirm`(16)/`Reject`(17) are dropped below API 30. Every
 * fallback below is safe at `minSdk = 24`.
 */
internal fun HapticCue.toFeedbackType(apiLevel: Int = hapticApiLevel): HapticFeedbackType = when (this) {
    HapticCue.CONFIRM -> if (apiLevel >= 30) HapticFeedbackType.Confirm else HapticFeedbackType.ContextClick
    HapticCue.REJECT -> if (apiLevel >= 30) HapticFeedbackType.Reject else HapticFeedbackType.LongPress
    HapticCue.TICK -> if (apiLevel >= 34) HapticFeedbackType.SegmentTick else HapticFeedbackType.ContextClick
    HapticCue.TOGGLE_ON -> if (apiLevel >= 34) HapticFeedbackType.ToggleOn else HapticFeedbackType.ContextClick
    HapticCue.TOGGLE_OFF -> if (apiLevel >= 34) HapticFeedbackType.ToggleOff else HapticFeedbackType.VirtualKey
}

/**
 * Defaults to [NookHaptics.None] and must never `error()` when unprovided: ScreenshotCatalog
 * renders 35 screens through NookTheme without AppRoot, and an erroring default would break
 * `:composeApp:screenshots` for reasons that are not obvious from the stack trace.
 */
val LocalNookHaptics = staticCompositionLocalOf { NookHaptics.None }

/** Chooses and builds the instance: the no-op when [enabled] is false, otherwise one that forwards to [feedback]. */
internal fun nookHaptics(enabled: Boolean, feedback: HapticFeedback): NookHaptics =
    if (!enabled) NookHaptics.None
    else NookHaptics { cue -> feedback.performHapticFeedback(cue.toFeedbackType()) }

/**
 * Builds the instance provided by `App`. The result must stay identity-stable across
 * recompositions: [LocalNookHaptics] is `staticCompositionLocalOf`, so an inlined lambda
 * passed straight into `CompositionLocalProvider` in `App.kt` would recompose the whole
 * subtree on every recomposition — silently, with no test to catch it.
 */
@Composable
fun rememberNookHaptics(enabled: Boolean): NookHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(enabled, feedback) { nookHaptics(enabled, feedback) }
}
