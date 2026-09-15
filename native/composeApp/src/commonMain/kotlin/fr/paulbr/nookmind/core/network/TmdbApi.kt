package fr.paulbr.nookmind.core.network

import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.domain.parseDateOnly
import fr.paulbr.nookmind.core.domain.todayLocal
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.model.TmdbPerson
import fr.paulbr.nookmind.core.model.TmdbSeasonDetails
import fr.paulbr.nookmind.core.model.TmdbSeries
import fr.paulbr.nookmind.core.model.WatchProvider
import fr.paulbr.nookmind.core.model.WatchProvidersResult
import fr.paulbr.nookmind.core.platform.isFrenchDevice
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** Port of src/lib/tmdb.ts (same endpoints, same caching windows, same extraction rules). */
class TmdbApi(
    private val client: HttpClient,
    private val cache: JsonCache,
    private val nookMindApi: NookMindApi,
) {
    @Serializable
    private data class PagedResults<T>(val results: List<T>? = null, @kotlinx.serialization.SerialName("total_pages") val totalPages: Int? = null)

    @Serializable
    private data class TmdbVideo(
        val key: String = "",
        val site: String = "",
        val type: String = "",
        val official: Boolean = false,
    )

    @Serializable
    private data class VideosResponse(val results: List<TmdbVideo> = emptyList())

    private val seriesDetailsInFlight = InFlightRequests<Int, TmdbSeries?>()
    private val seasonDetailsInFlight = InFlightRequests<String, TmdbSeasonDetails?>()

    fun tmdbLocale(): String = if (isFrenchDevice()) "fr-FR" else "en-US"
    private fun watchRegion(): String = if (isFrenchDevice()) "FR" else "US"

    private suspend fun getJson(path: String, params: Map<String, String> = emptyMap(), timeoutMs: Long? = null): HttpResponse =
        client.get("$BASE_URL$path") {
            AppConfig.tmdbApiKey?.let { parameter("api_key", it) }
            params.forEach { (k, v) -> parameter(k, v) }
            if (timeoutMs != null) timeout { requestTimeoutMillis = timeoutMs }
        }

    private val HttpResponse.ok: Boolean get() = status.value in 200..299

    // ── Movies ─────────────────────────────────────────────────────────────

    suspend fun searchMovies(query: String, maxResults: Int = 8): List<TmdbMovie> {
        if (query.isBlank()) return emptyList()
        val res = getJson("/search/movie", mapOf("query" to query, "include_adult" to "false", "language" to tmdbLocale()), 8_000)
        if (!res.ok) throw IllegalStateException("Failed to fetch movies")
        return (res.body<PagedResults<TmdbMovie>>().results ?: emptyList()).take(maxResults)
    }

    suspend fun fetchMovieDetails(tmdbId: Int): TmdbMovie? = runCatching {
        val res = getJson("/movie/$tmdbId", mapOf("append_to_response" to "credits", "language" to tmdbLocale()))
        if (!res.ok) null else res.body<TmdbMovie>()
    }.getOrNull()

    suspend fun fetchPersonDetails(personId: Int): TmdbPerson? = runCatching {
        val res = getJson("/person/$personId", mapOf("append_to_response" to "combined_credits", "language" to tmdbLocale()))
        if (!res.ok) null else res.body<TmdbPerson>()
    }.getOrNull()

    private suspend fun fetchMovieListPages(path: String, maxResults: Int, maxPages: Int = 3, filter: (TmdbMovie) -> Boolean): List<TmdbMovie> {
        val results = ArrayList<TmdbMovie>()
        val seen = HashSet<Int>()
        var page = 1
        while (page <= maxPages && results.size < maxResults) {
            val res = getJson(path, mapOf("language" to tmdbLocale(), "page" to page.toString()))
            if (!res.ok) break
            val data = res.body<PagedResults<TmdbMovie>>()
            for (movie in data.results ?: emptyList()) {
                if (movie.id in seen || !filter(movie)) continue
                seen += movie.id
                results += movie
            }
            val totalPages = data.totalPages ?: page
            if (page >= totalPages) break
            page++
        }
        return results.take(maxResults)
    }

    suspend fun fetchTrendingMovies(maxResults: Int = 12): List<TmdbMovie> = runCatching {
        val res = getJson("/trending/movie/week", mapOf("language" to tmdbLocale()))
        if (!res.ok) emptyList() else (res.body<PagedResults<TmdbMovie>>().results ?: emptyList()).take(maxResults)
    }.getOrDefault(emptyList())

    class Page<T>(val results: List<T>, val hasMore: Boolean)

    private suspend inline fun <reified T> fetchList(path: String, page: Int): Page<T> = runCatching {
        val res = getJson(path, mapOf("language" to tmdbLocale(), "page" to page.toString()))
        if (!res.ok) return Page(emptyList(), false)
        val data = res.body<PagedResults<T>>()
        val totalPages = data.totalPages ?: 1
        Page(data.results ?: emptyList(), page < totalPages && page < 5)
    }.getOrDefault(Page(emptyList(), false))

    suspend fun fetchTrendingSeries(page: Int = 1): Page<TmdbSeries> = fetchList("/trending/tv/week", page)
    suspend fun fetchTopRatedSeries(page: Int = 1): Page<TmdbSeries> = fetchList("/tv/top_rated", page)
    suspend fun fetchOnAirSeries(page: Int = 1): Page<TmdbSeries> = fetchList("/tv/on_the_air", page)

    suspend fun fetchTopRatedMoviesPaged(page: Int = 1): Page<TmdbMovie> = fetchList("/movie/top_rated", page)
    suspend fun fetchTrendingMoviesPaged(page: Int = 1): Page<TmdbMovie> = fetchList("/trending/movie/week", page)
    suspend fun fetchNowPlayingMoviesPaged(page: Int = 1): Page<TmdbMovie> = fetchList("/movie/now_playing", page)

    suspend fun fetchUpcomingMovies(maxResults: Int = 10): List<TmdbMovie> = runCatching {
        val today = todayLocal()
        fetchMovieListPages("/movie/upcoming", maxResults) { movie ->
            val release = parseDateOnly(movie.releaseDate)
            release != null && release > today
        }
    }.getOrDefault(emptyList())

    suspend fun fetchRecentMovies(maxResults: Int = 10): List<TmdbMovie> = runCatching {
        val today = todayLocal()
        val cutoff = today.minus(DatePeriod(days = 45))
        fetchMovieListPages("/movie/now_playing", maxResults) { movie ->
            val release = parseDateOnly(movie.releaseDate)
            release != null && release >= cutoff && release <= today
        }
    }.getOrDefault(emptyList())

    // ── Series ─────────────────────────────────────────────────────────────

    suspend fun searchSeries(query: String, maxResults: Int = 8): List<TmdbSeries> {
        if (query.isBlank()) return emptyList()
        val res = getJson("/search/tv", mapOf("query" to query, "include_adult" to "false", "language" to tmdbLocale()), 8_000)
        if (!res.ok) throw IllegalStateException("Failed to fetch series")
        return (res.body<PagedResults<TmdbSeries>>().results ?: emptyList()).take(maxResults)
    }

    suspend fun fetchSeasonEpisodeCount(tmdbId: Int, seasonNumber: Int): Int? =
        fetchSeasonDetails(tmdbId, seasonNumber)?.episodes?.size

    suspend fun fetchSeasonDetails(tmdbId: Int, seasonNumber: Int): TmdbSeasonDetails? {
        val cacheKey = "nookmind_tmdb_season_${tmdbId}_$seasonNumber"
        cache.get(cacheKey, TmdbSeasonDetails.serializer(), TMDB_CACHE_TTL_MS)?.let { return it }
        return seasonDetailsInFlight.run("$tmdbId:$seasonNumber") {
            runCatching {
                val res = getJson("/tv/$tmdbId/season/$seasonNumber", mapOf("language" to tmdbLocale()))
                if (!res.ok) return@run null
                val data = res.body<TmdbSeasonDetails>()
                cache.put(cacheKey, TmdbSeasonDetails.serializer(), data)
                data
            }.getOrNull()
        }
    }

    suspend fun fetchSeriesDetails(tmdbId: Int): TmdbSeries? {
        val cacheKey = "nookmind_tmdb_series_$tmdbId"
        cache.get(cacheKey, TmdbSeries.serializer(), TMDB_CACHE_TTL_MS)?.let { return it }
        return seriesDetailsInFlight.run(tmdbId) {
            runCatching {
                val res = getJson("/tv/$tmdbId", mapOf("language" to tmdbLocale(), "append_to_response" to "credits"))
                if (!res.ok) return@run null
                val data = res.body<TmdbSeries>()
                cache.put(cacheKey, TmdbSeries.serializer(), data)
                data
            }.getOrNull()
        }
    }

    // ── Watch providers ─────────────────────────────────────────────────────

    @Serializable
    private data class CountryProviders(val flatrate: List<WatchProvider>? = null, val link: String? = null)

    @Serializable
    private data class WatchProvidersResponse(val results: Map<String, CountryProviders>? = null)

    private fun filterProviders(providers: List<WatchProvider>): List<WatchProvider> {
        val ad = Regex("\\bwith ads\\b", RegexOption.IGNORE_CASE)
        val channel = Regex("\\bamazon channel\\b", RegexOption.IGNORE_CASE)
        return providers.filter { !ad.containsMatchIn(it.providerName) && !channel.containsMatchIn(it.providerName) }
    }

    private suspend fun fetchWatchProviders(kind: String, tmdbId: Int): WatchProvidersResult {
        val cacheKey = "nookmind_tmdb_${kind}_wp_$tmdbId"
        cache.get(cacheKey, WatchProvidersResult.serializer(), TMDB_CACHE_TTL_MS)?.let { return it }
        return runCatching {
            val res = getJson("/$kind/$tmdbId/watch/providers")
            if (!res.ok) return WatchProvidersResult()
            val country = res.body<WatchProvidersResponse>().results?.get(watchRegion())
            val result = WatchProvidersResult(filterProviders(country?.flatrate ?: emptyList()), country?.link)
            cache.put(cacheKey, WatchProvidersResult.serializer(), result)
            result
        }.getOrDefault(WatchProvidersResult())
    }

    suspend fun fetchMovieWatchProviders(tmdbId: Int): WatchProvidersResult = fetchWatchProviders("movie", tmdbId)
    suspend fun fetchSeriesWatchProviders(tmdbId: Int): WatchProvidersResult = fetchWatchProviders("tv", tmdbId)

    suspend fun fetchWatchProviderDeepLinks(tmdbWatchUrl: String): Map<String, String> {
        val cacheKey = "nookmind_wp_deep_$tmdbWatchUrl"
        cache.get(cacheKey, MapSerializer(String.serializer(), String.serializer()), TMDB_CACHE_TTL_MS)?.let { return it }
        val links = nookMindApi.fetchWatchProviderDeepLinks(tmdbWatchUrl)
        cache.put(cacheKey, MapSerializer(String.serializer(), String.serializer()), links)
        return links
    }

    // ── Trailers ───────────────────────────────────────────────────────────

    private fun pickBestTrailer(videos: List<TmdbVideo>): TmdbVideo? {
        val youtube = videos.filter { it.site == "YouTube" }
        return youtube.firstOrNull { it.type == "Trailer" && it.official }
            ?: youtube.firstOrNull { it.type == "Trailer" }
            ?: youtube.firstOrNull { it.type == "Teaser" }
            ?: youtube.firstOrNull()
    }

    /** [type] is `movie` or `tv`. Falls back to English when nothing exists in the device language. */
    suspend fun fetchTrailerKey(type: String, tmdbId: Int): String? = runCatching {
        val locale = tmdbLocale()
        val res = getJson("/$type/$tmdbId/videos", mapOf("language" to locale))
        if (!res.ok) return@runCatching null
        pickBestTrailer(res.body<VideosResponse>().results)?.key?.let { return@runCatching it }
        if (locale != "en-US") {
            val fallback = getJson("/$type/$tmdbId/videos", mapOf("language" to "en-US"))
            if (!fallback.ok) return@runCatching null
            return@runCatching pickBestTrailer(fallback.body<VideosResponse>().results)?.key
        }
        null
    }.getOrNull()

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3"
        const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"
        const val TMDB_CACHE_TTL_MS: Long = 6 * 60 * 60 * 1000

        fun posterUrl(path: String?, size: String = "w500"): String? =
            path?.takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/$size$it" }

        fun extractDirector(movie: TmdbMovie): String =
            movie.credits?.crew?.firstOrNull { it.job == "Director" }?.name ?: "Unknown Director"

        /** `extractMovieData` — not yet persisted. */
        fun extractMovieData(movie: TmdbMovie): Movie = Movie(
            tmdbId = movie.id,
            title = movie.title.ifBlank { "Unknown Title" },
            director = extractDirector(movie),
            description = movie.overview.ifBlank { null },
            posterUrl = posterUrl(movie.posterPath),
            releaseDate = movie.releaseDate.ifBlank { null },
            runtime = movie.runtime?.takeIf { it > 0 },
            genre = movie.genres?.firstOrNull()?.name?.ifBlank { null },
        )

        /** `extractSeriesData` — only counts seasons with at least one episode. */
        fun extractSeriesData(series: TmdbSeries): Series {
            val next = series.nextEpisodeToAir
            val airedSeasons = series.seasons
                ?.count { it.seasonNumber > 0 && it.episodeCount > 0 }
                ?.takeIf { it > 0 }
                ?: series.numberOfSeasons?.takeIf { it > 0 }
            return Series(
                tmdbId = series.id,
                title = series.name.ifBlank { "Unknown Title" },
                creator = series.createdBy?.firstOrNull()?.name ?: "",
                description = series.overview.ifBlank { null },
                posterUrl = posterUrl(series.posterPath),
                firstAirDate = series.firstAirDate.ifBlank { null },
                seasons = airedSeasons,
                genre = series.genres?.firstOrNull()?.name?.ifBlank { null },
                nextAirDate = next?.airDate,
                nextSeasonNumber = next?.seasonNumber,
                nextEpisodeNumber = next?.episodeNumber,
            )
        }
    }
}
