package fr.paulbr.nookmind.tools

import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.network.GoogleBooksApi
import fr.paulbr.nookmind.core.network.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking

/**
 * Checks every backend the app talks to and says which one is failing, with the real HTTP status.
 *
 * The three search screens report any failure as "the search timed out" (the web app does the same),
 * which hides a wrong or missing API key behind a message about the network. Run this to find out:
 *
 *     ./gradlew :composeApp:checkApis
 */
object CheckApis {
    private const val OK = "  ok   "
    private const val KO = "  FAIL "

    private fun configured(name: String, value: String?): Boolean {
        val placeholder = value == null || value.isBlank() ||
            value.startsWith("your_") || value.startsWith("your-") || value.contains("placeholder")
        println(if (placeholder) "$KO $name is missing or still holds the example value" else "$OK $name is set")
        return !placeholder
    }

    private suspend fun probe(client: HttpClient, label: String, url: String, params: Map<String, String>) {
        try {
            val res = client.get(url) { params.forEach { (k, v) -> parameter(k, v) } }
            val code = res.status.value
            if (code in 200..299) {
                println("$OK $label responded $code")
            } else {
                val body = res.bodyAsText().take(200).replace(Regex("\\s+"), " ")
                println("$KO $label responded $code: $body")
            }
        } catch (t: Throwable) {
            println("$KO $label threw ${t::class.simpleName}: ${t.message}")
            t.cause?.let { println("         cause: ${it::class.simpleName}: ${it.message}") }
        }
    }

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        println("-- Configuration (native/secrets.properties) --")
        val hasSupabase = configured("SUPABASE_URL", AppConfig.supabaseUrl.takeIf { !it.contains("placeholder") })
        configured("SUPABASE_ANON_KEY", AppConfig.supabaseAnonKey.takeIf { it != "placeholder_key" })
        val hasApi = configured("API_BASE_URL", AppConfig.apiBaseUrl)
        val hasTmdb = configured("TMDB_API_KEY", AppConfig.tmdbApiKey)
        configured("GOOGLE_BOOKS_API_KEY", AppConfig.googleBooksApiKey)
        configured("GOOGLE_AUTH_WEB_CLIENT_ID", AppConfig.googleWebClientId)

        val client = createHttpClient()
        println()
        println("-- Network --")

        probe(
            client, "Google Books (book search)", "https://www.googleapis.com/books/v1/volumes",
            buildMap {
                put("q", "dune"); put("maxResults", "1"); put("printType", "books")
                AppConfig.googleBooksApiKey?.let { put("key", it) }
            },
        )

        if (hasTmdb) {
            probe(
                client, "TMDB (movie and series search, Next Up)", "https://api.themoviedb.org/3/search/movie",
                mapOf("api_key" to AppConfig.tmdbApiKey.orEmpty(), "query" to "dune", "language" to "fr-FR"),
            )
        } else {
            println("$KO TMDB not tested: the key is missing. Movie and series search and the Next Up tab will stay empty.")
        }

        if (hasApi) {
            probe(
                client, "Vercel routes (IMDb ratings, watch providers)", AppConfig.apiUrl("/api/imdb-suggest"),
                mapOf("firstChar" to "d", "query" to "dune"),
            )
        } else {
            println("$KO Vercel routes not tested: API_BASE_URL is missing. IMDb ratings and streaming providers will not work.")
        }

        if (hasSupabase) {
            probe(client, "Supabase (sign-in, library)", "${AppConfig.supabaseUrl}/auth/v1/settings", mapOf("apikey" to AppConfig.supabaseAnonKey))
        }

        println()
        println("A FAIL line above explains the \"search timed out\" message: the app shows it for every kind of failure.")
        client.close()
    }
}
