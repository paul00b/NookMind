package fr.paulbr.nookmind.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDeviceTilt(): State<Offset> {
    val tilt = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(Unit) {
        val manager = CMMotionManager()
        if (!manager.deviceMotionAvailable) return@DisposableEffect onDispose { }
        val follower = TiltFollower()
        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        manager.startDeviceMotionUpdatesToQueue(NSOperationQueue.mainQueue) { motion, _ ->
            // CoreMotion's axes point the other way round from Android's gravity sensor.
            motion?.gravity?.useContents { tilt.value = follower.next(-x.toFloat(), -y.toFloat()) }
        }
        onDispose { manager.stopDeviceMotionUpdates() }
    }
    return tilt
}
