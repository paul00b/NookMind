package fr.paulbr.nookmind

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NookMind",
        state = rememberWindowState(width = 430.dp, height = 932.dp),
    ) {
        App()
    }
}
