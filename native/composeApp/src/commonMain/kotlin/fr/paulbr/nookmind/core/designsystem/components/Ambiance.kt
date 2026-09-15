package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.model.MediaMode

/** 256 x 256 RGBA noise tile (the SVG `feTurbulence type="fractalNoise"` of `mode-bg-*`). */
expect fun noiseImageBitmap(size: Int = 256, seed: Int = 7): ImageBitmap

/**
 * Pixels of the noise tile as non-premultiplied ARGB ints. Mirrors `feTurbulence` with
 * `baseFrequency=0.9 numOctaves=4`: every channel, alpha included, is an independent fractal sum
 * of four octaves (amplitudes 1, 1/2, 1/4, 1/8) mapped from [-1, 1] to [0, 1]. At 0.9 cycles per
 * pixel the lattice is finer than a pixel, so each pixel is effectively an independent sample.
 */
fun noisePixels(size: Int, seed: Int): IntArray {
    val random = kotlin.random.Random(seed)
    fun channel(): Int {
        var sum = 0f
        var amplitude = 1f
        repeat(4) {
            sum += (random.nextFloat() * 2f - 1f) * amplitude
            amplitude /= 2f
        }
        return (((sum + 1f) / 2f).coerceIn(0f, 1f) * 255f).toInt()
    }
    return IntArray(size * size) {
        val r = channel()
        val g = channel()
        val b = channel()
        val a = channel()
        (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}

fun modeGlowColor(mode: MediaMode): Color = when (mode) {
    MediaMode.BOOKS -> Palette.Amber500
    MediaMode.MOVIES -> Palette.Indigo500
    MediaMode.SERIES -> Palette.Teal500
}

/**
 * Port of the `.mode-bg-*` ambiance: a radial halo of the mode colour (ellipse 80 % x 40 % at
 * 50 % / -10 %) under a tiled noise texture, the whole layer at 25 % (light) / 30 % (dark).
 * Colour changes are cross-faded over 700 ms like the web transition.
 */
@Composable
fun ModeAmbianceBackground(mode: MediaMode, modifier: Modifier = Modifier) {
    val isDark = NookTheme.colors.isDark
    val glow by animateColorAsState(modeGlowColor(mode), tween(700), label = "glow")
    val noise = remember { noiseImageBitmap() }
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
