package fr.paulbr.nookmind.core.network

import fr.paulbr.nookmind.core.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Result of a JSON call to the Vercel functions (port of `callApiJson`). */
class ApiJsonResult(val ok: Boolean, val status: Int, val data: JsonObject?, val rawText: String?)

class PushTestResult(val ok: Boolean, val message: String)

/**
 * Client of the app's own serverless routes on Vercel (the `api/` folder of the repo):
 * IMDb proxy, push notifications, account deletion, streaming deep links.
 */
class NookMindApi(private val client: HttpClient) {

    private val HttpResponse.ok: Boolean get() = status.value in 200..299

    // ── IMDb proxy ─────────────────────────────────────────────────────────

    suspend fun imdbSuggest(firstChar: String, query: String): JsonObject? = runCatching {
        val res = client.get(AppConfig.apiUrl("/api/imdb-suggest")) {
            parameter("firstChar", firstChar)
            parameter("query", query)
            timeout { requestTimeoutMillis = 10_000 }
        }
        if (!res.ok) null else AppJson.parseToJsonElement(res.bodyAsText()).jsonObject
    }.getOrNull()

    suspend fun imdbGraphql(body: JsonObject): JsonObject? = runCatching {
        val res = client.post(AppConfig.apiUrl("/api/imdb-graphql")) {
            setBody(body)
            timeout { requestTimeoutMillis = 10_000 }
        }
        if (!res.ok) null else AppJson.parseToJsonElement(res.bodyAsText()).jsonObject
    }.getOrNull()

    // ── Streaming deep links ───────────────────────────────────────────────

    suspend fun fetchWatchProviderDeepLinks(tmdbWatchUrl: String): Map<String, String> = runCatching {
        val res = client.get(AppConfig.apiUrl("/api/watch-providers")) { parameter("url", tmdbWatchUrl) }
        if (!res.ok) return emptyMap()
        val providers = AppJson.parseToJsonElement(res.bodyAsText()).jsonObject["providers"]?.jsonObject ?: return emptyMap()
        providers.mapNotNull { (k, v) -> v.jsonPrimitive.contentOrNull?.let { k to it } }.toMap()
    }.getOrDefault(emptyMap())

    // ── Account ────────────────────────────────────────────────────────────

    /** Returns null on success, otherwise an error message. */
    suspend fun deleteAccount(accessToken: String): String? = runCatching {
        val res = client.post(AppConfig.apiUrl("/api/account/delete")) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        if (res.ok) null else res.bodyAsText().ifBlank { "HTTP ${res.status.value}" }
    }.getOrElse { it.message ?: "Network error" }

    // ── Push ───────────────────────────────────────────────────────────────

    suspend fun callJson(path: String, method: String, body: JsonObject, accessToken: String): ApiJsonResult {
        val response = try {
            when (method) {
                "DELETE" -> client.delete(AppConfig.apiUrl(path)) { header(HttpHeaders.Authorization, "Bearer $accessToken"); setBody(body) }
                else -> client.post(AppConfig.apiUrl(path)) { header(HttpHeaders.Authorization, "Bearer $accessToken"); setBody(body) }
            }
        } catch (t: Throwable) {
            return ApiJsonResult(false, 0, null, t.message ?: "Request failed")
        }
        val text = runCatching { response.bodyAsText() }.getOrNull()
        val json = text?.let { runCatching { AppJson.parseToJsonElement(it).jsonObject }.getOrNull() }
        return ApiJsonResult(response.ok, response.status.value, json, text)
    }

    /**
     * Sends a test notification to the given FCM token (port of `sendTestNotification`).
     * [messages] provides the localized outcomes.
     */
    suspend fun sendTestNotification(
        fcmToken: String,
        accessToken: String,
        sentMessage: String,
        rejectedMessage: String,
        nobodyMessage: String,
        unavailableMessage: String,
    ): PushTestResult {
        val body = buildJsonObject {
            put("token", fcmToken)
            put("transport", "fcm")
        }
        val result = callJson("/api/push/test", "POST", body, accessToken)
        if (!result.ok) {
            val message = result.data?.get("error")?.jsonPrimitive?.contentOrNull
                ?: result.rawText?.takeIf { it.isNotBlank() }?.let { "HTTP ${result.status} - ${it.take(220)}" }
                ?: unavailableMessage
            return PushTestResult(false, message)
        }
        val sent = result.data?.get("sent")?.jsonPrimitive?.intOrNull() ?: 0
        if (sent > 0) return PushTestResult(true, sentMessage)
        val firstFailure = result.data?.get("results")?.let { runCatching { it.jsonArray }.getOrNull() }
            ?.map { it.jsonObject }
            ?.firstOrNull { it["error"] != null }
        if (firstFailure != null) {
            val details = listOfNotNull(
                firstFailure["statusCode"]?.jsonPrimitive?.intOrNull()?.let { "HTTP $it" },
                firstFailure["error"]?.jsonPrimitive?.contentOrNull,
                firstFailure["details"]?.jsonPrimitive?.contentOrNull,
            ).joinToString(" - ")
            return PushTestResult(false, details.ifBlank { rejectedMessage })
        }
        return PushTestResult(false, nobodyMessage)
    }

    private fun kotlinx.serialization.json.JsonPrimitive.intOrNull(): Int? = runCatching { int }.getOrNull()
}

fun JsonElement?.asStringOrNull(): String? = this?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
