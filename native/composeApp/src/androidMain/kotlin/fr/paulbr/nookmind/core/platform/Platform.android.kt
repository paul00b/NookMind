package fr.paulbr.nookmind.core.platform

import android.content.Context
import android.util.Log
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/** Application context holder, initialised by `NookMindApplication`. */
object AndroidContextHolder {
    lateinit var appContext: Context
}

actual val platformKind: PlatformKind = PlatformKind.ANDROID

actual fun createPlatformSettings(name: String): Settings {
    val prefs = AndroidContextHolder.appContext.getSharedPreferences("nookmind_$name", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(prefs)
}

actual fun exitApplication() {
    // Handled by the Activity (finishAffinity) through AndroidBackHandler; nothing to do here.
}

actual fun logDebug(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
}
