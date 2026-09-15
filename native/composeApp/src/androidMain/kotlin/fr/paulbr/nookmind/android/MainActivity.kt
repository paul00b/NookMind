package fr.paulbr.nookmind.android

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import fr.paulbr.nookmind.App
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/** Single activity of the app: hosts the shared Compose UI and routes notification taps. */
class MainActivity : ComponentActivity() {

    private var permissionContinuation: Continuation<Boolean>? = null
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionContinuation?.resume(granted)
        permissionContinuation = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NookMindApplication
        app.push.permissionRequest = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                true
            } else {
                suspendCancellableCoroutine { continuation ->
                    permissionContinuation = continuation
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    continuation.invokeOnCancellation { permissionContinuation = null }
                }
            }
        }
        consumeRoute(intent)

        setContent { App(app.container) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeRoute(intent)
    }

    /** A notification carries the in-app route it should open (`/library`, `/discover`, …). */
    private fun consumeRoute(intent: Intent?) {
        val route = intent?.getStringExtra(EXTRA_ROUTE) ?: return
        (application as NookMindApplication).container.pendingRoute.value = route
        intent.removeExtra(EXTRA_ROUTE)
    }

    companion object {
        const val EXTRA_ROUTE = "nookmind.route"
    }
}
