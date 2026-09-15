package fr.paulbr.nookmind.core.network

import fr.paulbr.nookmind.core.model.EpisodeRating
import fr.paulbr.nookmind.core.model.MovieImdbRating
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.math.abs

/** Port of src/lib/imdb.ts — IMDb suggestion + GraphQL through the Vercel proxy, 24 h cache. */
class ImdbApi(private val api: NookMindApi, private val cache: JsonCache) {

    private val episodeInFlight = InFlightRequests<String, Map<Int, List<EpisodeRating>>?>()
    private val imdbIdInFlight = InFlightRequests<String, String?>()
    private val movieInFlight = InFlightRequests<String, MovieImdbRating?>()

    private class Candidate(val id: String, val l: String?, val q: String?, val y: Int?)

    private fun candidates(data: JsonObject?): List<Candidate> =
        data?.get("d")?.let { runCatching { it.jsonArray }.getOrNull() }
            ?.mapNotNull { el ->
                val obj = runCatching { el.jsonObject }.getOrNull() ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                Candidate(
                    id = id,
                    l = obj["l"]?.jsonPrimitive?.contentOrNull,
                    q = obj["q"]?.jsonPrimitive?.contentOrNull,
                    y = obj["y"]?.jsonPrimitive?.intOrNull,
                )
            } ?: emptyList()

    /** Best series candidate: exact title first, then closest year. */
    private fun pickSeriesMatch(candidates: List<Candidate>, normalizedTitle: String, year: Int?): String? {
        val exact = candidates.filter { (it.l ?: "").trim().lowercase() == normalizedTitle }
        val pool = if (exact.isNotEmpty()) exact else candidates
        if (year != null) {
            pool.firstOrNull { it.y != null && abs(it.y - year) <= 1 }?.let { return it.id }
        }
        return pool.firstOrNull()?.id
    }

    suspend fun fetchSeriesImdbId(title: String, year: Int?): String? {
        val normalizedTitle = title.trim().lowercase()
        if (normalizedTitle.isEmpty()) return null
        val cacheKey = "nookmind_imdb_id_${normalizedTitle}_${year ?: "na"}"
        cache.get(cacheKey, String.serializer().nullable, IMDB_CACHE_TTL_MS)?.let { return it }
        return imdbIdInFlight.run(normalizedTitle) {
            val data = api.imdbSuggest(normalizedTitle.take(1), normalizedTitle) ?: return@run null
            val series = candidates(data).filter { it.q == "TV series" || it.q == "TV mini-series" || it.q == "TV short" }
            val imdbId = pickSeriesMatch(series, normalizedTitle, year)
            cache.put(cacheKey, String.serializer().nullable, imdbId)
            imdbId
        }
    }

    private suspend fun fetchAllEpisodesRaw(imdbId: String): Map<Int, List<EpisodeRating>>? {
        val cacheKey = "nookmind_imdb_episodes_$imdbId"
        val serializer = MapSerializer(Int.serializer(), ListSerializer(EpisodeRating.serializer()))
        cache.get(cacheKey, serializer, IMDB_CACHE_TTL_MS)?.let { return it }

        val result = HashMap<Int, MutableList<EpisodeRating>>()
        var cursor: String? = null
        var any = false
        for (page in 0 until 10) {
            val hasCursor = cursor != null
            val body = buildJsonObject {
                put(
                    "query",
                    """query AllEpisodes(${'$'}id: ID!, ${'$'}first: Int!${if (hasCursor) ", ${'$'}after: ID" else ""}) {
        title(id: ${'$'}id) {
          episodes {
            episodes(first: ${'$'}first${if (hasCursor) ", after: ${'$'}after" else ""}) {
              pageInfo { hasNextPage endCursor }
              edges {
                node {
                  id
                  titleText { text }
                  ratingsSummary { aggregateRating }
                  series {
                    displayableEpisodeNumber {
                      displayableSeason { text }
                      episodeNumber { episodeNumber }
                    }
                  }
                }
              }
            }
          }
        }
      }""",
                )
                putJsonObject("variables") {
                    put("id", imdbId)
                    put("first", 100)
                    if (hasCursor) put("after", cursor)
                }
            }
            val data = api.imdbGraphql(body) ?: return null
            if (data["errors"]?.let { runCatching { it.jsonArray.size }.getOrNull() ?: 0 } ?: 0 > 0) return null
            val episodes = data["data"]?.jsonObject?.get("title")?.jsonObject?.get("episodes")?.jsonObject?.get("episodes")?.jsonObject
                ?: return null
            val edges = episodes["edges"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return null
            for (edge in edges) {
                val node = edge.jsonObject["node"]?.jsonObject ?: continue
                val display = node["series"]?.jsonObject?.get("displayableEpisodeNumber")?.jsonObject
                val season = display?.get("displayableSeason")?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                val episode = display?.get("episodeNumber")?.jsonObject?.get("episodeNumber")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                if (season == null || episode == null || season <= 0 || episode <= 0) continue
                result.getOrPut(season) { ArrayList() } += EpisodeRating(
                    episode = episode,
                    title = node["titleText"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull ?: "",
                    imdbRating = node["ratingsSummary"]?.jsonObject?.get("aggregateRating")?.jsonPrimitive?.doubleOrNull,
                    imdbId = node["id"]?.jsonPrimitive?.contentOrNull,
                )
                any = true
            }
            val pageInfo = episodes["pageInfo"]?.jsonObject
            if (pageInfo?.get("hasNextPage")?.jsonPrimitive?.contentOrNull != "true") break
            cursor = pageInfo["endCursor"]?.jsonPrimitive?.contentOrNull ?: break
        }
        if (!any) return null
        val sorted = result.mapValues { (_, eps) -> eps.sortedBy { it.episode } }
        cache.put(cacheKey, serializer, sorted)
        return sorted
    }

    private suspend fun getAllEpisodes(imdbId: String): Map<Int, List<EpisodeRating>>? =
        episodeInFlight.run(imdbId) { fetchAllEpisodesRaw(imdbId) }

    /** Ratings of one season, or null when IMDb has nothing for it. */
    suspend fun fetchSeasonRatings(imdbId: String, season: Int): List<EpisodeRating>? =
        getAllEpisodes(imdbId)?.get(season)

    /** IMDb rating of a movie: suggestion API for the id, then GraphQL for the rating. */
    suspend fun fetchMovieImdbRating(title: String, year: Int?): MovieImdbRating? {
        val normalizedTitle = title.trim().lowercase()
        if (normalizedTitle.isEmpty()) return null
        val cacheKey = "nookmind_imdb_movie_${normalizedTitle}_${year ?: ""}"
        cache.get(cacheKey, MovieImdbRating.serializer().nullable, IMDB_CACHE_TTL_MS)?.let { return it }
        return movieInFlight.run(cacheKey) {
            val suggest = api.imdbSuggest(normalizedTitle.take(1), normalizedTitle) ?: return@run null
            val movies = candidates(suggest).filter { it.q == "feature" || it.q == "TV movie" }
            val imdbId = (if (year != null) movies.firstOrNull { it.y == year } else null)?.id ?: movies.firstOrNull()?.id
                ?: return@run null
            val body = buildJsonObject {
                put("query", "query MovieRating(${'$'}id: ID!) { title(id: ${'$'}id) { ratingsSummary { aggregateRating } } }")
                putJsonObject("variables") { put("id", imdbId) }
            }
            val data = api.imdbGraphql(body)
            val rating = data?.takeIf { (it["errors"]?.let { e -> runCatching { e.jsonArray.size }.getOrNull() ?: 0 } ?: 0) == 0 }
                ?.get("data")?.jsonObject?.get("title")?.jsonObject?.get("ratingsSummary")?.jsonObject
                ?.get("aggregateRating")?.jsonPrimitive?.doubleOrNull
            val result = MovieImdbRating(imdbId, rating)
            cache.put(cacheKey, MovieImdbRating.serializer().nullable, result)
            result
        }
    }

    companion object {
        const val IMDB_CACHE_TTL_MS: Long = 24 * 60 * 60 * 1000
        fun titleUrl(imdbId: String) = "https://www.imdb.com/title/$imdbId/"
    }
}
