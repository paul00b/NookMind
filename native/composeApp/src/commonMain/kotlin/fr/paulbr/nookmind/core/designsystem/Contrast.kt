package fr.paulbr.nookmind.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** WCAG AA minimum for body text. */
const val MIN_TEXT_CONTRAST = 4.5f

/** WCAG 2.x contrast ratio of two opaque colours: 1 for identical, 21 for black on white. */
fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}

/**
 * Text and icon colour for a solid accent [fill]. White where it reaches WCAG AA (blue-600,
 * indigo-600, purple-600…). Otherwise a near-black ink tinted with the fill itself: the light
 * accents (amber, emerald, teal, sky) only give 2.1 to 2.8:1 with white, and 6 to 8.5:1 with it.
 * A translucent fill is judged as if opaque, which holds for the 90 % badges over posters.
 */
fun onFill(fill: Color): Color {
    val opaque = fill.copy(alpha = 1f)
    val white = contrastRatio(Palette.White, opaque)
    if (white >= MIN_TEXT_CONTRAST) return Palette.White
    val ink = lerp(opaque, Palette.Black, 0.85f)
    return if (contrastRatio(ink, opaque) > white) ink else Palette.White
}
