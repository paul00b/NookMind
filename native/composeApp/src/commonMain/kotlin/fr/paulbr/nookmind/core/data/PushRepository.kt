package fr.paulbr.nookmind.core.data

import fr.paulbr.nookmind.core.model.NotificationPreferences
import fr.paulbr.nookmind.core.network.NookMindApi
import fr.paulbr.nookmind.core.network.PushTestResult
import fr.paulbr.nookmind.core.platform.PushPlatform
import fr.paulbr.nookmind.core.platform.logDebug
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

sealed interface PushEnableResult {
    data object Enabled : PushEnableResult
    data object PermissionDenied : PushEnableResult
    data object TokenUnavailable : PushEnableResult
    data class Failed(val message: String?) : PushEnableResult
}

/**
 * FCM subscription stored in `push_subscriptions` (`transport = 'fcm'`), as the web app did on
 * native. Unlike the web code, disabling really deletes the row and preference toggles are saved
 * on the row (so the daily cron honours them).
 */
class PushRepository(
    private val client: SupabaseClient,
    private val auth: AuthRepository,
    private val api: NookMindApi,
    private val platform: PushPlatform,
) {
    @Serializable
    private data class SubscriptionRow(
        val id: String? = null,
        @kotlinx.serialization.SerialName("fcm_token") val fcmToken: String? = null,
        @kotlinx.serialization.SerialName("notify_episodes") val notifyEpisodes: Boolean = true,
        @kotlinx.serialization.SerialName("notify_seasons") val notifySeasons: Boolean = true,
        @kotlinx.serialization.SerialName("notify_movies") val notifyMovies: Boolean = true,
    )

    val isSupported: Boolean get() = platform.isSupported

    private val _subscribed = MutableStateFlow(false)
    val subscribed: StateFlow<Boolean> = _subscribed

    private val _preferences = MutableStateFlow(NotificationPreferences())
    val preferences: StateFlow<NotificationPreferences> = _preferences

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    /** Reads the current user's FCM row (mirrors the mount check of SettingsPanel). */
    suspend fun refresh() {
        val user = auth.currentUser ?: return
        runCatching {
            val row = client.from("push_subscriptions").select(Columns.raw("id, fcm_token, notify_episodes, notify_seasons, notify_movies")) {
                filter {
                    eq("user_id", user.id)
                    eq("transport", "fcm")
                }
                limit(1)
            }.decodeSingleOrNull<SubscriptionRow>()
            _subscribed.value = row?.fcmToken != null
            if (row != null) _preferences.value = NotificationPreferences(row.notifyEpisodes, row.notifySeasons, row.notifyMovies)
        }.onFailure { logDebug("push", "refresh failed", it) }
    }

    suspend fun enable(): PushEnableResult {
        val user = auth.currentUser ?: return PushEnableResult.Failed(null)
        _loading.value = true
        try {
            if (!platform.requestPermission()) return PushEnableResult.PermissionDenied
            val token = platform.getToken() ?: return PushEnableResult.TokenUnavailable
            return runCatching {
                client.from("push_subscriptions").delete {
                    filter {
                        eq("user_id", user.id)
                        eq("fcm_token", token)
                    }
                }
                val prefs = _preferences.value
                client.from("push_subscriptions").insert(buildJsonObject {
                    put("user_id", user.id)
                    put("transport", "fcm")
                    put("fcm_token", token)
                    put("notify_episodes", prefs.notifyEpisodes)
                    put("notify_seasons", prefs.notifySeasons)
                    put("notify_movies", prefs.notifyMovies)
                    put("updated_at", Clock.System.now().toString())
                })
                _subscribed.value = true
                PushEnableResult.Enabled as PushEnableResult
            }.getOrElse {
                logDebug("push", "insert failed", it)
                PushEnableResult.Failed(it.message)
            }
        } finally {
            _loading.value = false
        }
    }

    suspend fun disable(): Boolean {
        val user = auth.currentUser ?: return false
        _loading.value = true
        return try {
            client.from("push_subscriptions").delete {
                filter {
                    eq("user_id", user.id)
                    eq("transport", "fcm")
                }
            }
            _subscribed.value = false
            true
        } catch (t: Throwable) {
            logDebug("push", "disable failed", t)
            false
        } finally {
            _loading.value = false
        }
    }

    suspend fun updatePreferences(update: (NotificationPreferences) -> NotificationPreferences): Boolean {
        val next = update(_preferences.value)
        _preferences.value = next
        if (!_subscribed.value) return true
        val user = auth.currentUser ?: return false
        return runCatching {
            client.from("push_subscriptions").update(buildJsonObject {
                put("notify_episodes", next.notifyEpisodes)
                put("notify_seasons", next.notifySeasons)
                put("notify_movies", next.notifyMovies)
                put("updated_at", Clock.System.now().toString())
            }) {
                filter {
                    eq("user_id", user.id)
                    eq("transport", "fcm")
                }
            }
            true
        }.getOrElse { logDebug("push", "updatePreferences failed", it); false }
    }

    /** Keeps the stored token in sync when FCM rotates it (called from the Android service). */
    suspend fun onTokenRefreshed(token: String) {
        val user = auth.currentUser ?: return
        runCatching {
            val existing = client.from("push_subscriptions").select(Columns.raw("id")) {
                filter {
                    eq("user_id", user.id)
                    eq("transport", "fcm")
                }
                limit(1)
            }.decodeSingleOrNull<SubscriptionRow>() ?: return
            if (existing.id == null) return
            client.from("push_subscriptions").update(buildJsonObject {
                put("fcm_token", token)
                put("updated_at", Clock.System.now().toString())
            }) {
                filter {
                    eq("user_id", user.id)
                    eq("transport", "fcm")
                }
            }
            logDebug("push", "FCM token refreshed in DB")
        }.onFailure { logDebug("push", "token refresh failed", it) }
    }

    suspend fun sendTest(
        sentMessage: String,
        rejectedMessage: String,
        nobodyMessage: String,
        unavailableMessage: String,
        noTokenMessage: String,
    ): PushTestResult {
        val token = platform.getToken() ?: return PushTestResult(false, noTokenMessage)
        val accessToken = auth.accessToken() ?: return PushTestResult(false, unavailableMessage)
        return api.sendTestNotification(token, accessToken, sentMessage, rejectedMessage, nobodyMessage, unavailableMessage)
    }
}
