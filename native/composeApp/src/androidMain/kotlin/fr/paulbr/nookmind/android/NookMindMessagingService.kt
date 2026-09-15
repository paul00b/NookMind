package fr.paulbr.nookmind.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import fr.paulbr.nookmind.R
import fr.paulbr.nookmind.core.platform.logDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Receives the notifications sent by the daily Vercel cron (the push routes of the API) and keeps
 * the stored FCM token in sync when Firebase rotates it.
 */
class NookMindMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        logDebug("push", "FCM token rotated")
        val container = (application as? NookMindApplication)?.container ?: return
        scope.launch { container.push.onTokenRefreshed(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Data-only messages need an explicit notification; notification payloads are shown by the
        // system when the app is in the background, and here when it is in the foreground.
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val route = message.data["route"]

        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = getString(R.string.notification_channel_description)
                },
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            route?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching { NotificationManagerCompat.from(this).notify(Random.nextInt(), notification) }
            .onFailure { logDebug("push", "notify failed (permission?)", it) }
    }

    companion object {
        const val CHANNEL_ID = "default"
    }
}
