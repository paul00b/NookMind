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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonArray
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

    private val _states = MutableStateFlow<Map<String, State>>(emptyMap())
    /** Everything the server knows of, by [key]: asked for, downloading, there. For the posters. */
    val states: StateFlow<Map<String, State>> = _states

    /** Reads every title the server has, a page of 100 at a time. Keeps the old list on failure. */
    suspend fun refreshAll() {
        if (!isConfigured) { _states.value = emptyMap(); return }
        runCatching {
            val found = mutableMapOf<String, State>()
            var page = 0
            var pages = 1
            while (page < pages && page < 50) {
                val response = call(prefs.jellyseerrUrl, prefs.jellyseerrKey) { base, key ->
                    client.get("$base/api/v1/media?take=100&skip=${page * 100}&filter=all&sort=added") {
                        header(KEY_HEADER, key); timeout { requestTimeoutMillis = TIMEOUT }
                    }
                }
                if (response.status.value !in 200..299) return
                val body = AppJson.parseToJsonElement(response.bodyAsText()).jsonObject
                pages = (body["pageInfo"] as? JsonObject)?.get("pages")?.jsonPrimitive?.intOrNull ?: 1
                (body["results"] as? JsonArray).orEmpty().forEach { item ->
                    val media = item as? JsonObject ?: return@forEach
                    val id = media["tmdbId"]?.jsonPrimitive?.intOrNull ?: return@forEach
                    val type = media["mediaType"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                    val state = stateOf(media["status"]?.jsonPrimitive?.intOrNull)
                    if (state != State.NOT_REQUESTED) found[key(type, id)] = state
                }
                page++
            }
            _states.value = found
        }.onFailure { logDebug("jellyseerr", "refreshAll failed", it) }
    }

    private fun record(type: String, tmdbId: Int, state: State?) {
        if (state == null) return
        _states.update { if (state == State.NOT_REQUESTED) it - key(type, tmdbId) else it + (key(type, tmdbId) to state) }
    }

    /** Where [tmdbId] stands on the server, or null when the server does not answer. */
    suspend fun state(type: String, tmdbId: Int): State? = fetchState(type, tmdbId).also { record(type, tmdbId, it) }

    private suspend fun fetchState(type: String, tmdbId: Int): State? = runCatching {
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

        /** "movie:603", "tv:1396": how [states] files a title. */
        fun key(type: String, tmdbId: Int) = "${mediaType(type)}:$tmdbId"

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
