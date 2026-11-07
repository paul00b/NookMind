package fr.paulbr.nookmind.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import androidx.compose.ui.graphics.asAndroidBitmap

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

actual fun shareImage(png: ByteArray, text: String) {
    runCatching {
        val context = AndroidContextHolder.appContext
        // The cache folder FileProvider exposes (res/xml/shared_files.xml), one file overwritten each time.
        val dir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
        val file = java.io.File(dir, "nookmind.png").apply { writeBytes(png) }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(send, null)
        val activity = fr.paulbr.nookmind.android.ActivityTracker.current
        if (activity != null) activity.startActivity(chooser)
        else context.startActivity(chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Log.w("platform", "cannot share the image", it)
        shareText(text)
    }
}

actual fun androidx.compose.ui.graphics.ImageBitmap.encodePng(): ByteArray? = runCatching {
    val out = java.io.ByteArrayOutputStream()
    this.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
    out.toByteArray()
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
