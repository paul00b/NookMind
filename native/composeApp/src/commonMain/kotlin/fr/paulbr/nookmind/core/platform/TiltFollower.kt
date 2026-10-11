package fr.paulbr.nookmind.core.platform

import androidx.compose.ui.geometry.Offset

/**
 * Turns raw gravity readings (in g) into the smoothed tilt of [rememberDeviceTilt]: a low-pass
 * filter against shake, measured from a rest angle that slowly follows the hand.
 */
class TiltFollower(private val smoothing: Float = 0.18f, private val restFollow: Float = 0.008f) {
    private var rest: Offset? = null
    private var current = Offset.Zero

    fun next(x: Float, y: Float): Offset {
        val reading = Offset(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f))
        val base = rest ?: reading.also { current = it }
        current += (reading - current) * smoothing
        rest = base + (current - base) * restFollow
        return current - (rest ?: base)
    }
}
