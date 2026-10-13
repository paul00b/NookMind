package fr.paulbr.nookmind

import androidx.compose.ui.window.ComposeViewport
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.platform.RedirectGoogleSignIn
import kotlinx.browser.document

/**
 * The web app (beta.nookmind.paulbr.fr for now): the shared app drawn in the browser by Compose.
 * Google sign-in goes through Supabase's redirect; no Apple sign-in nor push.
 */
fun main() {
    val container = AppContainer(googleSignIn = RedirectGoogleSignIn)
    ComposeViewport(document.body!!) {
        App(container)
    }
    // The placeholder of index.html, shown while the WebAssembly downloads.
    document.getElementById("loading")?.remove()
}
