package fr.paulbr.nookmind

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import fr.paulbr.nookmind.app.AppContainer

/** Desktop preview of the shared app (same code as Android, without native sign-in / push). */
fun main() {
    val container = AppContainer()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "NookMind",
            state = rememberWindowState(width = 430.dp, height = 932.dp),
        ) {
            App(container)
        }
    }
}
