package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.network.TmdbApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.round

/** Port of useSeriesStats (local part). */
data class LocalSeriesStats(
    val totalSeries: Int,
    val watchedSeasonsCount: Int,
    val averageRating: Double?,
    val favoriteGenre: String?,
    val favoriteCreator: String?,
)

data class TmdbSeriesStats(val minutes: Int, val episodes: Int)

fun relevantSeriesForStats(series: List<Series>): List<Series> =
    series.filter { it.status == SeriesStatus.WATCHED || it.status == SeriesStatus.WATCHING }

fun computeLocalStats(series: List<Series>): LocalSeriesStats {
    val watchedSeasonsCount = series.sumOf { it.watchedSeasons.size }
    val rated = series.filter { (it.rating ?: 0.0) > 0 }
    val averageRating = if (rated.isEmpty()) null else round(rated.sumOf { it.rating ?: 0.0 } / rated.size * 10) / 10
    val favoriteGenre = series.mapNotNull { it.genre?.takeIf { g -> g.isNotBlank() } }
        .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    val favoriteCreator = series.mapNotNull { it.creator.takeIf { c -> c.isNotBlank() } }
        .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    return LocalSeriesStats(series.size, watchedSeasonsCount, averageRating, favoriteGenre, favoriteCreator)
}

/** Sums TMDB runtimes of watched episodes (complete seasons + partial seasons). */
suspend fun computeTmdbStats(series: List<Series>, tmdb: TmdbApi): TmdbSeriesStats = coroutineScope {
    val perSeries = series.map { s ->
        async {
            val tmdbId = s.tmdbId ?: return@async TmdbSeriesStats(0, 0)
            var minutes = 0
            var episodes = 0
            val watchedSet = s.watchedSeasons.toSet()
            val partialSeasons = s.watchedEpisodes.keys.mapNotNull { it.toIntOrNull() }.filter { it !in watchedSet }

            for (season in s.watchedSeasons) {
                val details = tmdb.fetchSeasonDetails(tmdbId, season) ?: continue
                minutes += details.episodes.sumOf { it.runtime ?: 0 }
                episodes += details.episodes.size
            }
            for (season in partialSeasons) {
                val details = tmdb.fetchSeasonDetails(tmdbId, season) ?: continue
                val watched = s.watchedEpisodes[season.toString()]?.toSet() ?: emptySet()
                val eps = details.episodes.filter { it.episodeNumber in watched }
                minutes += eps.sumOf { it.runtime ?: 0 }
                episodes += eps.size
            }
            TmdbSeriesStats(minutes, episodes)
        }
    }
    val results = perSeries.map { it.await() }
    TmdbSeriesStats(results.sumOf { it.minutes }, results.sumOf { it.episodes })
}
