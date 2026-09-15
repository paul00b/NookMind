package fr.paulbr.nookmind.core.platform

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual val platformKind: PlatformKind = PlatformKind.DESKTOP

actual fun createPlatformSettings(name: String): Settings =
    PreferencesSettings(Preferences.userRoot().node("fr.paulbr.nookmind.$name"))

actual fun exitApplication() {
    kotlin.system.exitProcess(0)
}

actual fun logDebug(tag: String, message: String, throwable: Throwable?) {
    println("[$tag] $message")
    throwable?.printStackTrace()
}

actual fun openExternalUrl(url: String) {
    runCatching {
        if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().browse(java.net.URI(url))
    }.onFailure { println("[platform] cannot open $url: ${it.message}") }
}
