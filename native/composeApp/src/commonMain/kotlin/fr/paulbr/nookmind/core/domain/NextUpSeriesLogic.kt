package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.TmdbEpisode
import fr.paulbr.nookmind.core.model.TmdbSeries
import kotlinx.datetime.LocalDate

// Port of the pure helpers of src/pages/NextUpSeries.tsx so they can be unit-tested.

sealed interface EpisodeState {
    data class Available(val season: Int, val episode: Int) : EpisodeState
    data class ComingSoon(val ep: TmdbEpisode) : EpisodeState
    data object UpToDate : EpisodeState
    data object Unknown : EpisodeState
}

data class LastWatched(val season: Int, val episode: Int)

fun episodeScore(season: Int, episode: Int): Int = season * 10_000 + episode

fun getUserLastWatched(watchedEpisodes: Map<String, List<Int>>): LastWatched? {
    val seasons = watchedEpisodes.keys.mapNotNull { it.toIntOrNull() }.sortedDescending()
    for (season in seasons) {
        val eps = watchedEpisodes[season.toString()]
        if (!eps.isNullOrEmpty()) return LastWatched(season, eps.max())
    }
    return null
}

/** Expands fully-watched seasons (without per-episode data) into explicit episode lists. */
fun resolveWatchedEpisodes(series: Series, tmdb: TmdbSeries?): Map<String, List<Int>> {
    val resolved = HashMap<String, List<Int>>()
    for ((season, episodes) in series.watchedEpisodes) resolved[season] = episodes.sorted()
    val seasons = tmdb?.seasons ?: return resolved
    for (watchedSeason in series.watchedSeasons) {
        val info = seasons.firstOrNull { it.seasonNumber == watchedSeason }
        val count = info?.episodeCount ?: 0
        if (count > 0 && (resolved[watchedSeason.toString()]?.size ?: 0) == 0) {
            resolved[watchedSeason.toString()] = (1..count).toList()
        }
    }
    return resolved
}

fun getEpisodeState(series: Series, tmdb: TmdbSeries?, today: LocalDate = todayLocal()): EpisodeState {
    if (tmdb == null) return EpisodeState.Unknown
    val resolvedWatched = resolveWatchedEpisodes(series, tmdb)
    val lastWatched = getUserLastWatched(resolvedWatched)
    val lastWatchedScore = lastWatched?.let { episodeScore(it.season, it.episode) } ?: -1

    val lastAired = tmdb.lastEpisodeToAir
    val nextAiring = tmdb.nextEpisodeToAir
    val lastAiredScore = lastAired?.let { episodeScore(it.seasonNumber, it.episodeNumber) } ?: -1

    if (lastWatchedScore < lastAiredScore) {
        val nextSeason = lastWatched?.season ?: 1
        val nextEp = if (lastWatched != null) lastWatched.episode + 1 else 1
        val seasonInfo = tmdb.seasons?.firstOrNull { it.seasonNumber == nextSeason }
        if (seasonInfo != null && nextEp > seasonInfo.episodeCount) {
            return EpisodeState.Available(nextSeason + 1, 1)
        }
        return EpisodeState.Available(nextSeason, nextEp)
    }

    if (nextAiring != null) {
        val availableToday = nextAiring.airDate != null && isTodayOrPast(nextAiring.airDate, today)
        if (availableToday) return EpisodeState.Available(nextAiring.seasonNumber, nextAiring.episodeNumber)
        return EpisodeState.ComingSoon(nextAiring)
    }

    return EpisodeState.UpToDate
}

fun getCardKey(series: Series, state: EpisodeState): String = when (state) {
    is EpisodeState.Available -> "${series.id}-available-${state.season}-${state.episode}"
    is EpisodeState.ComingSoon -> "${series.id}-coming-soon-${state.ep.seasonNumber}-${state.ep.episodeNumber}"
    EpisodeState.UpToDate -> "${series.id}-up_to_date"
    EpisodeState.Unknown -> "${series.id}-unknown"
}

fun episodeStateRank(state: EpisodeState): Int = when (state) {
    is EpisodeState.Available -> 0
    is EpisodeState.ComingSoon -> 1
    else -> 2
}

/**
 * Applies "mark S{season}E{episode} watched" to a series, also completing seasons whose every
 * episode is now watched. Returns the new (watchedEpisodes, watchedSeasons).
 */
fun markEpisodeWatched(
    series: Series,
    tmdb: TmdbSeries?,
    season: Int,
    episode: Int,
): Pair<Map<String, List<Int>>, List<Int>> {
    val resolved = resolveWatchedEpisodes(series, tmdb)
    val key = season.toString()
    val nextEpisodes = ((resolved[key] ?: emptyList()) + episode).distinct().sorted()
    val nextWatchedEpisodes = resolved + (key to nextEpisodes)
    val nextWatchedSeasons = series.watchedSeasons.toMutableSet()
    for (info in tmdb?.seasons ?: emptyList()) {
        val watchedForSeason = nextWatchedEpisodes[info.seasonNumber.toString()]?.size ?: 0
        if (info.episodeCount > 0 && watchedForSeason >= info.episodeCount) nextWatchedSeasons += info.seasonNumber
    }
    return nextWatchedEpisodes to nextWatchedSeasons.sorted()
}

/**
 * Finds the first not-yet-watched episode airing strictly after [today] across the seasons
 * given by [seasonEpisodes] (season number → episodes, fetched lazily by the caller).
 */
fun findNextUpcomingEpisode(
    seasons: List<Int>,
    seasonEpisodes: (Int) -> List<TmdbEpisode>?,
    watchedEpisodes: Map<String, List<Int>>,
    today: LocalDate = todayLocal(),
): TmdbEpisode? {
    val watchedScores = watchedEpisodes.entries.flatMap { (season, eps) ->
        val s = season.toIntOrNull() ?: return@flatMap emptyList()
        eps.map { episodeScore(s, it) }
    }.toHashSet()
    for (season in seasons.sorted()) {
        val next = seasonEpisodes(season)
            ?.filter { ep ->
                val date = parseDateOnly(ep.airDate) ?: return@filter false
                date > today && episodeScore(ep.seasonNumber, ep.episodeNumber) !in watchedScores
            }
            ?.minByOrNull { episodeScore(it.seasonNumber, it.episodeNumber) }
        if (next != null) return next
    }
    return null
}
