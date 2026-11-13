package fr.paulbr.nookmind.android

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import fr.paulbr.nookmind.core.domain.ShareLinks
import fr.paulbr.nookmind.core.platform.logDebug

/**
 * Someone without the app scans a friend's QR code, or opens a shared movie: the web page sends
 * them to Google Play with `referrer=route=/friend/K7PQ2M`. Play hands that referrer to the app
 * once installed, and its first launch opens the invitation or the item instead of the home page.
 * Read once per install; nothing happens for an install from the store itself.
 */
object InstallReferrer {
    private const val PREFS = "install_referrer"
    private const val DONE = "done"

    fun read(context: Context, onRoute: (String) -> Unit) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(DONE, false)) return
        val client = InstallReferrerClient.newBuilder(context).build()
        runCatching {
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(code: Int) {
                    when (code) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            val referrer = runCatching { client.installReferrer.installReferrer }.getOrNull()
                            routeOf(referrer)?.let(onRoute)
                            prefs.edit().putBoolean(DONE, true).apply()
                        }
                        // Sideloaded, or no Play Store: nothing to read, ever.
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                            prefs.edit().putBoolean(DONE, true).apply()
                        else -> Unit // Play busy or unreachable: next launch tries again.
                    }
                    runCatching { client.endConnection() }
                }

                override fun onInstallReferrerServiceDisconnected() = Unit
            })
        }.onFailure { logDebug("referrer", "install referrer failed", it) }
    }

    /** `route=/friend/K7PQ2M` (as Play hands it back) to the route, if it is one the app opens. */
    internal fun routeOf(referrer: String?): String? {
        val value = referrer?.split('&')?.firstNotNullOfOrNull { part ->
            part.substringAfter("route=", "").takeIf { part.startsWith("route=") }
        }?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrNull() } ?: return null
        return value.takeIf { ShareLinks.parseFriendRoute(it) != null || ShareLinks.parseRoute(it) != null }
    }
}
