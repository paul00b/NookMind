package fr.paulbr.nookmind.android

import android.app.Application
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.platform.AndroidContextHolder

/** Process-wide composition root: builds the [AppContainer] with the Android services. */
class NookMindApplication : Application() {

    val push = AndroidPushPlatform()

    val container: AppContainer by lazy {
        AppContainer(
            googleSignIn = AndroidGoogleSignIn(),
            pushPlatform = push,
            barcodeScanner = AndroidBarcodeScanner(),
            // The .debug build is the owner's own, installed by hand next to the Play one.
            personalBuild = packageName.endsWith(".debug"),
            // Features turned on per account (user_features): read on Android only.
            accountFlags = true,
        )
    }

    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.appContext = applicationContext
        ActivityTracker.install(this)
    }
}
