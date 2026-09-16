package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private enum class Fill { EMPTY, HALF, FULL }

private fun fillFor(display: Double, star: Int): Fill = when {
    display >= star -> Fill.FULL
    display >= star - 0.5 -> Fill.HALF
    else -> Fill.EMPTY
}

/** The web `StarIcon` path (24 x 24 viewport). */
private fun starPath(size: Float): Path {
    val s = size / 24f
    return Path().apply {
        moveTo(12f * s, 2f * s)
        lineTo(15.09f * s, 8.26f * s)
        lineTo(22f * s, 9.27f * s)
        lineTo(17f * s, 14.14f * s)
        lineTo(18.18f * s, 21.02f * s)
        lineTo(12f * s, 17.77f * s)
        lineTo(5.82f * s, 21.02f * s)
        lineTo(7f * s, 14.14f * s)
        lineTo(2f * s, 9.27f * s)
        lineTo(8.91f * s, 8.26f * s)
        close()
    }
}

@Composable
private fun StarIcon(fill: Fill, size: Dp, emptyStroke: Color) {
    Canvas(Modifier.size(size)) {
        val px = this.size.minDimension
        val path = starPath(px)
        val stroke = Stroke(width = 1.5f * px / 24f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(path, color = if (fill == Fill.EMPTY) emptyStroke else Palette.Amber500, style = stroke)
        if (fill == Fill.FULL) {
            drawPath(path, color = Palette.Amber500)
        } else if (fill == Fill.HALF) {
            clipRect(left = 0f, top = 0f, right = px / 2f, bottom = px) {
                drawPath(path, color = Palette.Amber500)
            }
        }
    }
}

/**
 * Port of StarRating.tsx: five stars, half-star precision, tap or drag to rate.
 * [value] is 0.5..5 or null.
 */
@Composable
fun StarRating(
    value: Double?,
    modifier: Modifier = Modifier,
    onChange: ((Double) -> Unit)? = null,
    readonly: Boolean = onChange == null,
    size: Dp = 18.dp,
) {
    val colors = NookTheme.colors
    val emptyStroke = if (colors.isDark) Palette.Gray600 else Palette.Gray300
    var hover by remember { mutableStateOf<Double?>(null) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val display = hover ?: value ?: 0.0

    fun valueFromX(x: Float): Double {
        if (widthPx <= 0f) return 0.5
        val clamped = max(0f, min(x, widthPx - 1f))
        val starWidth = widthPx / 5f
        val index = floor(clamped / starWidth).toInt()
        val pos = (clamped - index * starWidth) / starWidth
        return min(max(index + (if (pos < 0.5f) 0.5 else 1.0), 0.5), 5.0)
    }

    val haptics = LocalNookHaptics.current
    val gesture = if (readonly || onChange == null) Modifier else Modifier
        .pointerInput(haptics) {
            detectTapGestures(onTap = { offset ->
                val next = valueFromX(offset.x)
                if (next != value) haptics.perform(HapticCue.TICK)
                onChange(next)
                hover = null
            })
        }
        .pointerInput(haptics) {
            detectDragGestures(
                onDragStart = { offset ->
                    hover = valueFromX(offset.x)
                    haptics.perform(HapticCue.TICK)
                },
                onDrag = { change, _ ->
                    val next = valueFromX(change.position.x)
                    // Only when the half-star value actually changes, not on every pixel.
                    if (next != hover) {
                        hover = next
                        haptics.perform(HapticCue.TICK)
                    }
                },
                onDragEnd = { hover?.let(onChange); hover = null },
                onDragCancel = { hover = null },
            )
        }

    Row(
        modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .then(gesture),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
    ) {
        for (star in 1..5) StarIcon(fillFor(display, star), size, emptyStroke)
    }
}
