package fr.paulbr.nookmind.core.network

import fr.paulbr.nookmind.core.data.AppPreferences
import fr.paulbr.nookmind.core.platform.logDebug
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The owner's Jellyseerr, in the personal Android build only: asks it for a movie or a series by
 * its TMDB id, and reads where that request stands. Jellyseerr passes it on to Radarr or Sonarr.
 * API: the one of Overseerr, which Jellyseerr forks (`/api/v1`, key in `X-Api-Key`).
 */
class JellyseerrApi(private val client: HttpClient, private val prefs: AppPreferences) {

    enum class State { NOT_REQUESTED, PENDING, PROCESSING, PARTIAL, AVAILABLE }

    val isConfigured: Boolean get() = prefs.jellyseerrUrl.isNotBlank() && prefs.jellyseerrKey.isNotBlank()

    /** Where [tmdbId] stands on the server, or null when the server does not answer. */
    suspend fun state(type: String, tmdbId: Int): State? = runCatching {
        val response = call(prefs.jellyseerrUrl, prefs.jellyseerrKey) { base, key ->
            client.get("$base/api/v1/${mediaType(type)}/$tmdbId") { header(KEY_HEADER, key); timeout { requestTimeoutMillis = TIMEOUT } }
        }
        if (response.status.value !in 200..299) return@runCatching null
        val info = AppJson.parseToJsonElement(response.bodyAsText()).jsonObject["mediaInfo"] as? JsonObject
        stateOf(info?.get("status")?.jsonPrimitive?.intOrNull)
    }.onFailure { logDebug("jellyseerr", "state failed", it) }.getOrNull()

    /** Asks for [tmdbId], every season for a series. Null when it went through, else the reason. */
    suspend fun request(type: String, tmdbId: Int): String? = runCatching {
        val response = call(prefs.jellyseerrUrl, prefs.jellyseerrKey) { base, key ->
            client.post("$base/api/v1/request") {
                header(KEY_HEADER, key)
                timeout { requestTimeoutMillis = TIMEOUT }
                setBody(buildJsonObject {
                    put("mediaType", mediaType(type))
                    put("mediaId", tmdbId)
                    if (mediaType(type) == "tv") put("seasons", JsonPrimitive("all"))
                })
            }
        }
        when (response.status.value) {
            in 200..299, 409 -> null // 409: already asked for.
            else -> message(response)
        }
    }.getOrElse { logDebug("jellyseerr", "request failed", it); it.message ?: "?" }

    /** The name of the account the key belongs to, or null when [url] and [key] do not work. */
    suspend fun check(url: String, key: String): String? = runCatching {
        val response = call(url, key) { base, k ->
            client.get("$base/api/v1/auth/me") { header(KEY_HEADER, k); timeout { requestTimeoutMillis = TIMEOUT } }
        }
        if (response.status.value !in 200..299) return@runCatching null
        val me = AppJson.parseToJsonElement(response.bodyAsText()).jsonObject
        listOf("displayName", "username", "jellyfinUsername", "email")
            .firstNotNullOfOrNull { me[it]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) } ?: "Jellyseerr"
    }.onFailure { logDebug("jellyseerr", "check failed", it) }.getOrNull()

    private suspend fun call(url: String, key: String, block: suspend (String, String) -> HttpResponse): HttpResponse =
        block(normalize(url), key.trim())

    private suspend fun message(response: HttpResponse): String {
        val text = runCatching { response.bodyAsText() }.getOrDefault("")
        val detail = runCatching { AppJson.parseToJsonElement(text).jsonObject["message"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        return detail ?: "HTTP ${response.status.value}"
    }

    companion object {
        private const val KEY_HEADER = "X-Api-Key"
        private const val TIMEOUT = 10_000L

        private fun mediaType(type: String) = if (type == "movie") "movie" else "tv"

        /** "host:5055/" and the like: a scheme added when missing, no trailing slash. */
        fun normalize(url: String): String {
            val trimmed = url.trim().trimEnd('/')
            return if ("://" in trimmed) trimmed else "http://$trimmed"
        }

        /** Overseerr's media status: 2 pending, 3 processing, 4 partly available, 5 available. */
        internal fun stateOf(status: Int?): State = when (status) {
            2 -> State.PENDING
            3 -> State.PROCESSING
            4 -> State.PARTIAL
            5 -> State.AVAILABLE
            else -> State.NOT_REQUESTED
        }
    }
}
