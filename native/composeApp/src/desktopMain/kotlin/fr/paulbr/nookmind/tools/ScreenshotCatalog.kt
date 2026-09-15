package fr.paulbr.nookmind.tools

import androidx.compose.runtime.Composable
import fr.paulbr.nookmind.App

class ScreenshotEntry(val name: String, val content: @Composable () -> Unit)

/** Screens rendered by the `screenshots` tool. Extended as features land. */
object ScreenshotCatalog {
    val entries: List<ScreenshotEntry> = listOf(
        ScreenshotEntry("smoke") { App() },
    )
}
