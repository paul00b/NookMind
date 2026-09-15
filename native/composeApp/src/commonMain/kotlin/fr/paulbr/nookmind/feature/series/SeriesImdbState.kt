package fr.paulbr.nookmind.feature.series

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.domain.yearInt
import fr.paulbr.nookmind.core.model.SeasonState
import kotlinx.coroutines.launch

/** IMDb episode ratings of one series: id lookup, then one request per season (max 20). */
@Stable
class SeriesImdbState {
    var imdbId by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var notFound by mutableStateOf(false)
    var seasonRatings by mutableStateOf<Map<Int, SeasonState>>(emptyMap())
    internal var retryKey by mutableIntStateOf(0)

    fun retry() {
        notFound = false
        imdbId = null
        seasonRatings = emptyMap()
        retryKey += 1
    }
}

const val MAX_SEASONS_FALLBACK = 20

/**
 * Loads the IMDb ratings once [enabled] turns true (the section is opened), like the web effects:
 * suggestion API for the title id, then the per-season ratings in parallel.
 */
@Composable
fun rememberSeriesImdbState(
    container: AppContainer,
    title: String,
    firstAirDate: String?,
    totalSeasons: Int?,
    enabled: Boolean,
): SeriesImdbState {
    val state = remember(title, firstAirDate) { SeriesImdbState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(enabled, title, firstAirDate, state.retryKey) {
        if (!enabled || state.imdbId != null || state.loading || state.notFound) return@LaunchedEffect
        state.loading = true
        val id = container.imdb.fetchSeriesImdbId(title, yearInt(firstAirDate))
        state.loading = false
        if (id == null) state.notFound = true else state.imdbId = id
    }

    LaunchedEffect(state.imdbId, totalSeasons) {
        val id = state.imdbId ?: return@LaunchedEffect
        val max = totalSeasons ?: MAX_SEASONS_FALLBACK
        for (season in 1..max) {
            state.seasonRatings = state.seasonRatings + (season to SeasonState.Loading)
            scope.launch {
                val ratings = container.imdb.fetchSeasonRatings(id, season)
                state.seasonRatings = when {
                    ratings != null -> state.seasonRatings + (season to SeasonState.Loaded(ratings))
                    totalSeasons == null -> state.seasonRatings - season
                    else -> state.seasonRatings + (season to SeasonState.Error)
                }
            }
        }
    }

    return state
}
