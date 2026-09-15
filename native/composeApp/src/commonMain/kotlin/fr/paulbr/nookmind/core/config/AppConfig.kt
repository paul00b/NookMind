package fr.paulbr.nookmind.core.config

/** Runtime configuration derived from the generated [AppSecrets] (see native/secrets.properties). */
object AppConfig {
    val supabaseUrl: String = AppSecrets.SUPABASE_URL.ifBlank { "https://placeholder.supabase.co" }
    val supabaseAnonKey: String = AppSecrets.SUPABASE_ANON_KEY.ifBlank { "placeholder_key" }
    val apiBaseUrl: String? = AppSecrets.API_BASE_URL.takeIf { it.isNotBlank() }
    val googleBooksApiKey: String? = AppSecrets.GOOGLE_BOOKS_API_KEY.takeIf { it.isNotBlank() }
    val tmdbApiKey: String? = AppSecrets.TMDB_API_KEY.takeIf { it.isNotBlank() }
    val googleWebClientId: String? = AppSecrets.GOOGLE_AUTH_WEB_CLIENT_ID.takeIf { it.isNotBlank() }

    val isConfigured: Boolean get() = AppSecrets.SUPABASE_URL.isNotBlank() && AppSecrets.SUPABASE_ANON_KEY.isNotBlank()

    /** Absolute URL of a Vercel `/api/...` route. Port of `getApiUrl` (native branch). */
    fun apiUrl(path: String): String = ApiUrl.build(apiBaseUrl, path)
}

object ApiUrl {
    private fun normalizeBaseUrl(value: String): String =
        value.trim().trim('"', '\'').trimEnd('/')

    /**
     * @throws IllegalStateException when [baseUrl] is missing: unlike the web app, a native app has
     * no same-origin `/api` to fall back to.
     */
    fun build(baseUrl: String?, path: String): String {
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        val base = baseUrl?.let(::normalizeBaseUrl)?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("API_BASE_URL is required for native builds.")
        return base + normalizedPath
    }
}
