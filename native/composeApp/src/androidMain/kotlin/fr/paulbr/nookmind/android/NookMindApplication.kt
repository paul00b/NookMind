package fr.paulbr.nookmind.android

import android.app.Application
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.platform.AndroidContextHolder

/** Process-wide composition root: builds the [AppContainer] with the Android services. */
class NookMindApplication : Application() {

    val push = AndroidPushPlatform()

    val container: AppContainer by lazy {
        AppContainer(googleSignIn = AndroidGoogleSignIn(), pushPlatform = push)
    }

    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.appContext = applicationContext
        ActivityTracker.install(this)
    }
}
