package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.model.MediaMode

fun modeGlowColor(mode: MediaMode): Color = when (mode) {
    MediaMode.BOOKS -> Palette.Amber500
    MediaMode.MOVIES -> Palette.Indigo500
    MediaMode.SERIES -> Palette.Teal500
}

/**
 * Port of the `.mode-bg-*` ambiance: a radial halo of the mode colour (ellipse 80 % x 40 % at
 * 50 % / -10 %), at 25 % (light) / 30 % (dark).
 * Colour changes are cross-faded over 700 ms like the web transition.
 *
 * The web app tiles an `feTurbulence` noise over the halo. The native app deliberately drops it:
 * on a high-density screen that grain reads as sandblasting rather than film grain, and the plain
 * gradient is smoother. The web still has it, so the two differ here on purpose.
 */
@Composable
fun ModeAmbianceBackground(mode: MediaMode, modifier: Modifier = Modifier) {
    val isDark = NookTheme.colors.isDark
    val glow by animateColorAsState(modeGlowColor(mode), tween(700), label = "glow")
    val layerAlpha = if (isDark) 0.30f else 0.25f
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // Ellipse 80% x 40% of the viewport centred at (50%, -10%): draw a circle scaled on Y.
        val radiusX = w * 0.8f
        val radiusY = h * 0.4f
        val center = Offset(w / 2f, -h * 0.1f)
        val scaleY = radiusY / radiusX
        scale(scaleX = 1f, scaleY = scaleY, pivot = center) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = layerAlpha), glow.copy(alpha = 0f)),
                    center = center,
                    radius = radiusX,
                ),
                radius = radiusX,
                center = center,
            )
        }
    }
}
