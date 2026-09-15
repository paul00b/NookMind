package fr.paulbr.nookmind.core.data

import fr.paulbr.nookmind.core.domain.getEffectiveSeriesStatus
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.network.TmdbApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock

/**
 * Port of useTmdbSeriesRefresh: once a day, refresh the TMDB metadata (season count, next
 * episode) of every watching / watched series, 300 ms apart to respect TMDB rate limits.
 */
class SeriesRefreshJob(
    private val series: LibraryRepository<Series>,
    private val tmdb: TmdbApi,
    private val prefs: AppPreferences,
    private val scope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val list = series.items.first { it.isNotEmpty() }
            val now = Clock.System.now().toEpochMilliseconds()
            val last = prefs.seriesTmdbRefreshAt
            if (last != null && now - last < REFRESH_INTERVAL_MS) return@launch
            prefs.seriesTmdbRefreshAt = now
            val toRefresh = list.filter { s ->
                s.tmdbId != null && getEffectiveSeriesStatus(s) in setOf(SeriesStatus.WATCHING, SeriesStatus.WATCHED)
            }
            toRefresh.forEachIndexed { index, s ->
                if (index > 0) delay(REQUEST_DELAY_MS)
                refreshOne(s)
            }
        }
    }

    suspend fun refreshOne(s: Series): Series? {
        val tmdbId = s.tmdbId ?: return null
        val data = tmdb.fetchSeriesDetails(tmdbId) ?: return null
        val extracted = TmdbApi.extractSeriesData(data)
        if (extracted.nextAirDate == s.nextAirDate &&
            extracted.nextSeasonNumber == s.nextSeasonNumber &&
            extracted.nextEpisodeNumber == s.nextEpisodeNumber &&
            extracted.seasons == s.seasons
        ) return null
        return series.update(s.id, buildJsonObject {
            extracted.seasons?.let { put("seasons", it) } ?: put("seasons", JsonNull)
            extracted.nextAirDate?.let { put("next_air_date", it) } ?: put("next_air_date", JsonNull)
            extracted.nextSeasonNumber?.let { put("next_season_number", it) } ?: put("next_season_number", JsonNull)
            extracted.nextEpisodeNumber?.let { put("next_episode_number", it) } ?: put("next_episode_number", JsonNull)
        })
    }

    companion object {
        const val REFRESH_INTERVAL_MS: Long = 24 * 60 * 60 * 1000
        const val REQUEST_DELAY_MS: Long = 300
    }
}
