package fr.paulbr.nookmind.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Lenient JSON used for every third-party API (unknown keys are the norm with TMDB/IMDb). */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

/**
 * JSON used to build the bodies written to Supabase.
 *
 * `encodeDefaults` is what separates it from [AppJson]: without it a property still equal to its
 * declared default is left out of the payload, and a column declared NOT NULL with no DEFAULT then
 * rejects the row. A book added without an author is exactly that case.
 *
 * `explicitNulls` stays off so a nullable property at null is omitted and Postgres applies its own
 * default, which is what the web app does by sending `undefined`.
 */
val DbJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = true
}

/** Ktor picks the platform engine from the classpath (OkHttp on Android, Java on desktop, Darwin on iOS). */
fun createHttpClient(): HttpClient = HttpClient {
    expectSuccess = false
    install(ContentNegotiation) { json(AppJson) }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 15_000
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}
