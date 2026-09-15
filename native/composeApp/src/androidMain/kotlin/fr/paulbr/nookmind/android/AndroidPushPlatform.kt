package fr.paulbr.nookmind.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import fr.paulbr.nookmind.core.platform.AndroidContextHolder
import fr.paulbr.nookmind.core.platform.PushPlatform
import fr.paulbr.nookmind.core.platform.logDebug
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** FCM transport of the push subscription (replaces `@capacitor-firebase/messaging`). */
class AndroidPushPlatform : PushPlatform {

    /** Set by MainActivity: shows the POST_NOTIFICATIONS system dialog and returns the answer. */
    var permissionRequest: (suspend () -> Boolean)? = null

    override val isSupported: Boolean get() = true

    override suspend fun requestPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(AndroidContextHolder.appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) return true
        return permissionRequest?.invoke() ?: false
    }

    override suspend fun getToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> continuation.resume(token) }
            .addOnFailureListener { error ->
                logDebug("push", "FCM token request failed", error)
                continuation.resume(null)
            }
    }
}
