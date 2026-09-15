package fr.paulbr.nookmind.core.domain

import fr.paulbr.nookmind.core.model.EpisodeRating
import fr.paulbr.nookmind.core.model.SeasonState

/**
 * Maps IMDb ratings onto TMDB episodes by *global airing position* rather than by
 * (season, episode): IMDb episodes are flattened in (season, episode) order and then
 * re-assigned following TMDB's per-season episode counts. Robust to season splits that
 * differ between the two sources (typical for anime). Port of `buildFlatEpisodeLookup`.
 */
fun buildFlatEpisodeLookup(
    seasonRatings: Map<Int, SeasonState>,
    tmdbSeasonCounts: Map<Int, Int>,
): (season: Int, episode: Int) -> EpisodeRating? {
    val flat = seasonRatings.keys.sorted().flatMap { season ->
        (seasonRatings[season] as? SeasonState.Loaded)?.episodes ?: emptyList()
    }
    val base = HashMap<Int, Int>()
    var acc = 0
    val maxSeason = (tmdbSeasonCounts.keys.maxOrNull() ?: 0).coerceAtLeast(0)
    for (s in 1..maxSeason) {
        base[s] = acc
        acc += tmdbSeasonCounts[s] ?: 0
    }
    return { season, episode ->
        val start = base[season]
        if (start == null) null else flat.getOrNull(start + episode - 1)
    }
}

data class ImdbStats(val average: String, val best: String, val worst: String)

fun computeImdbStats(seasonRatings: Map<Int, SeasonState>): ImdbStats? {
    val all = seasonRatings.values
        .filterIsInstance<SeasonState.Loaded>()
        .flatMap { it.episodes }
        .mapNotNull { it.imdbRating }
    if (all.isEmpty()) return null
    return ImdbStats(
        average = (all.sum() / all.size).toFixed1(),
        best = all.max().toFixed1(),
        worst = all.min().toFixed1(),
    )
}
