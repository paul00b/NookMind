package fr.paulbr.nookmind.tools

import androidx.compose.runtime.Composable
import fr.paulbr.nookmind.App
import fr.paulbr.nookmind.app.AppContainer

class ScreenshotEntry(val name: String, val content: @Composable () -> Unit)

/** Screens rendered by the `screenshots` tool. Extended as features land. */
object ScreenshotCatalog {
    private val container by lazy { AppContainer() }

    val entries: List<ScreenshotEntry> = listOf(
        ScreenshotEntry("smoke") { App(container) },
    )
}
