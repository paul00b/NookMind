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
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.ambiance_noise
import org.jetbrains.compose.resources.imageResource

fun modeGlowColor(mode: MediaMode): Color = when (mode) {
    MediaMode.BOOKS -> Palette.Amber500
    MediaMode.MOVIES -> Palette.Indigo500
    MediaMode.SERIES -> Palette.Teal500
}

/**
 * Port of the `.mode-bg-*` ambiance: a radial halo of the mode colour (ellipse 80 % x 40 % at
 * 50 % / -10 %) under a tiled noise texture, the whole layer at 25 % (light) / 30 % (dark).
 * Colour changes are cross-faded over 700 ms like the web transition.
 *
 * The noise is `drawable/ambiance_noise.png`, the 256 x 256 tile of the web app's own
 * `feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="4" stitchTiles="stitch"`,
 * rasterised once from that SVG so both apps show the same grain.
 *
 * It used to be generated in Kotlin as a plain fractal sum, which is what made the background look
 * sandblasted: measured against the browser, that produced a standard deviation of about 22 levels
 * where the browser draws about 2.4. A browser clamps the colour channels against the alpha channel
 * of the turbulence, which flattens the result far more than the raw formula suggests.
 */
@Composable
fun ModeAmbianceBackground(mode: MediaMode, modifier: Modifier = Modifier) {
    val isDark = NookTheme.colors.isDark
    val glow by animateColorAsState(modeGlowColor(mode), tween(700), label = "glow")
    val noise = imageResource(Res.drawable.ambiance_noise)
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
        drawRect(
            brush = ShaderBrush(ImageShader(noise, TileMode.Repeated, TileMode.Repeated)),
            alpha = layerAlpha,
        )
    }
}
