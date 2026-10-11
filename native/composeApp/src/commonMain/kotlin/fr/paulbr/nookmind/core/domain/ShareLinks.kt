package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.config.ApiUrl

/**
 * Links to a movie, a series or a book that anyone can open: `https://<api>/s/movie/438631`, a page
 * with the poster that adds it to NookMind (or sends to the store), and `nookmind://add/movie/438631`,
 * which the page uses to open the app.
 */
object ShareLinks {
    private val kinds = setOf("movie", "series", "book")
    private val app = Regex("""^nookmind://add/(movie|series|book)/([A-Za-z0-9_-]+)/?(?:[?#].*)?$""")
    private val web = Regex("""^https?://[^/]+/s/(movie|series|book)/([A-Za-z0-9_-]+)/?(?:[?#].*)?$""")

    /** The link of an item, or null without an API domain or a usable key. */
    fun linkFor(baseUrl: String?, kind: String, key: String, sharer: String?, language: String): String? {
        if (kind !in kinds || !key.matches(Regex("[A-Za-z0-9_-]+"))) return null
        val path = runCatching { ApiUrl.build(baseUrl, "/s/$kind/$key") }.getOrNull() ?: return null
        val query = buildList {
            sharer?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }?.let { add("by=${encode(it.take(24))}") }
            add("l=${if (language.lowercase().startsWith("en")) "en" else "fr"}")
        }
        return "$path?${query.joinToString("&")}"
    }

    /** The in-app route a link leads to, `/add/movie/438631`, or null for anything else. */
    fun routeOf(url: String): String? {
        val match = app.find(url.trim()) ?: web.find(url.trim()) ?: return null
        val (kind, key) = match.destructured
        return "/add/$kind/$key"
    }

    /** `kind` and `key` of an `/add/...` route. */
    fun parseRoute(route: String): Pair<String, String>? {
        val parts = route.removePrefix("/").split('/')
        if (parts.size != 3 || parts[0] != "add" || parts[1] !in kinds || parts[2].isEmpty()) return null
        return parts[1] to parts[2]
    }

    /** Percent-encoding of a query value (UTF-8, unreserved characters kept). */
    private fun encode(value: String): String = buildString {
        value.encodeToByteArray().forEach { byte ->
            val c = byte.toInt() and 0xFF
            val ch = c.toChar()
            if (ch.isLetterOrDigit() && c < 0x80 || ch == '-' || ch == '_' || ch == '.' || ch == '~') append(ch)
            else append('%').append("0123456789ABCDEF"[c shr 4]).append("0123456789ABCDEF"[c and 0xF])
        }
    }
}
