package fr.paulbr.nookmind

import androidx.compose.ui.window.ComposeViewport
import fr.paulbr.nookmind.app.AppContainer
import kotlinx.browser.document

/**
 * The web app (nookmind.paulbr.fr): the shared app drawn in the browser by Compose, like desktop,
 * without native sign-in or push for now.
 */
fun main() {
    val container = AppContainer()
    ComposeViewport(document.body!!) {
        App(container)
    }
    // The placeholder of index.html, shown while the WebAssembly downloads.
    document.getElementById("loading")?.remove()
}
