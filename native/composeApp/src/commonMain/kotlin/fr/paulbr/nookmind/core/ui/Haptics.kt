package fr.paulbr.nookmind.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Intent-named haptic cues. Call sites say what happened, never which buzz — so the whole
 * app's feel can be retuned by editing [toFeedbackType] alone.
 */
enum class NookHaptic { Confirm, Reject, Tick, ToggleOn, ToggleOff }

fun interface NookHaptics {
    fun perform(haptic: NookHaptic)

    companion object {
        /** Performs nothing. Used when the user has switched vibrations off. */
        val None: NookHaptics = NookHaptics { }
    }
}

internal fun NookHaptic.toFeedbackType(): HapticFeedbackType = when (this) {
    NookHaptic.Confirm -> HapticFeedbackType.Confirm
    NookHaptic.Reject -> HapticFeedbackType.Reject
    NookHaptic.Tick -> HapticFeedbackType.SegmentTick
    NookHaptic.ToggleOn -> HapticFeedbackType.ToggleOn
    NookHaptic.ToggleOff -> HapticFeedbackType.ToggleOff
}

/**
 * Defaults to [NookHaptics.None] and must never `error()` when unprovided: ScreenshotCatalog
 * renders 35 screens through NookTheme without AppRoot, and an erroring default would break
 * `:composeApp:screenshots` for reasons that are not obvious from the stack trace.
 */
val LocalNookHaptics = staticCompositionLocalOf { NookHaptics.None }

/** Builds the instance provided by `App`. Returns the no-op when [enabled] is false. */
@Composable
fun rememberNookHaptics(enabled: Boolean): NookHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(enabled, feedback) {
        if (!enabled) NookHaptics.None
        else NookHaptics { haptic -> feedback.performHapticFeedback(haptic.toFeedbackType()) }
    }
}
