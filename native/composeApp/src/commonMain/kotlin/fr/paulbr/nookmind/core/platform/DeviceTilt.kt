package fr.paulbr.nookmind.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset

/**
 * How far the device has tilted since the screen appeared, in g on each axis (about -1..1):
 * x grows when the left edge goes down, y when the top edge comes towards the user. The starting
 * angle slowly becomes the new rest, so holding the phone differently does not leave things askew.
 * Always zero where there is no motion sensor (desktop).
 */
@Composable
expect fun rememberDeviceTilt(): State<Offset>
