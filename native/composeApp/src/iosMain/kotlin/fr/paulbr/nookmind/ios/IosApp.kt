package fr.paulbr.nookmind.ios

import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.platform.UnavailableAppleSignIn
import fr.paulbr.nookmind.core.platform.UnavailableGoogleSignIn
import fr.paulbr.nookmind.core.platform.UnsupportedPushPlatform
import kotlinx.coroutines.launch

/**
 * Process-wide composition root, the counterpart of `NookMindApplication` on Android. Swift calls
 * [initialize] once from the AppDelegate, then everything else reads [container].
 *
 * Each bridge is optional: a `nil` from Swift means the feature is not wired in this build, and
 * the shared code gets the same "unavailable" implementation the desktop target uses. So an app
 * built without Firebase or without a paid Apple team still runs, with that one feature off.
 */
object IosApp {
    private var built: AppContainer? = null

    val container: AppContainer
        get() = built ?: error("IosApp.initialize() must run in the AppDelegate before the UI is built.")

    fun initialize(appleSignIn: IosAppleSignInBridge?, googleSignIn: IosGoogleSignInBridge?, push: IosPushBridge?) {
        if (built != null) return
        built = AppContainer(
            googleSignIn = googleSignIn?.let(::IosGoogleSignIn) ?: UnavailableGoogleSignIn,
            appleSignIn = appleSignIn?.let(::IosAppleSignIn) ?: UnavailableAppleSignIn,
            pushPlatform = push?.let(::IosPushPlatform) ?: UnsupportedPushPlatform,
        )
    }

    /** A notification tap carries the in-app route to open (`/library`, `/discover`, ...), like the Android intent extra. */
    fun openRoute(route: String) {
        container.pendingRoute.value = route
    }

    /** Firebase rotated the FCM token: keep the stored subscription in sync, like the Android messaging service. */
    fun onPushTokenRefreshed(token: String) {
        val current = container
        current.scope.launch { current.push.onTokenRefreshed(token) }
    }
}
