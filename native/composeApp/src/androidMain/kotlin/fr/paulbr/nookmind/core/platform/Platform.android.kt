package fr.paulbr.nookmind.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/** Application context holder, initialised by `NookMindApplication`. */
object AndroidContextHolder {
    lateinit var appContext: Context
}

actual val platformKind: PlatformKind = PlatformKind.ANDROID

actual val hapticApiLevel: Int get() = android.os.Build.VERSION.SDK_INT

actual fun createPlatformSettings(name: String): Settings {
    val prefs = AndroidContextHolder.appContext.getSharedPreferences("nookmind_$name", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(prefs)
}

actual fun exitApplication() {
    // Back at the root of the app: leave it like the hardware back button on the launcher task.
    fr.paulbr.nookmind.android.ActivityTracker.current?.finish()
}

actual fun logDebug(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
}

actual fun openExternalUrl(url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AndroidContextHolder.appContext.startActivity(intent)
    }.onFailure { Log.w("platform", "cannot open $url", it) }
}

actual fun appVersionName(): String? = runCatching {
    val context = AndroidContextHolder.appContext
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull()

actual fun shareText(text: String) {
    runCatching {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        val activity = fr.paulbr.nookmind.android.ActivityTracker.current
        val chooser = Intent.createChooser(send, null)
        if (activity != null) activity.startActivity(chooser)
        else AndroidContextHolder.appContext.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { Log.w("platform", "cannot share", it) }
}
