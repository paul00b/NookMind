package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.patchOf
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.EditableNote
import fr.paulbr.nookmind.core.designsystem.components.ExpandableDescription
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.LabeledBlock
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.MetaPill
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.domain.buildFlatEpisodeLookup
import fr.paulbr.nookmind.core.domain.deriveSeriesStatus
import fr.paulbr.nookmind.core.domain.getEffectiveSeriesStatus
import fr.paulbr.nookmind.core.domain.isFutureDate
import fr.paulbr.nookmind.core.domain.isSeriesWaiting
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.model.TmdbEpisode
import fr.paulbr.nookmind.core.model.TmdbSeries
import fr.paulbr.nookmind.core.model.WatchProvidersResult
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.feature.books.CollectionChip
import fr.paulbr.nookmind.feature.common.CastAccordion
import fr.paulbr.nookmind.feature.common.ConfirmDeleteRow
import fr.paulbr.nookmind.feature.common.DeleteButton
import fr.paulbr.nookmind.feature.common.TrailerButton
import fr.paulbr.nookmind.feature.common.WatchProvidersRow
import fr.paulbr.nookmind.feature.movies.ActorSheet
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.seriesDetail_areYouSure
import fr.paulbr.nookmind.resources.seriesDetail_cancel
import fr.paulbr.nookmind.resources.seriesDetail_cast
import fr.paulbr.nookmind.resources.seriesDetail_collections
import fr.paulbr.nookmind.resources.seriesDetail_delete
import fr.paulbr.nookmind.resources.seriesDetail_episodesSection
import fr.paulbr.nookmind.resources.seriesDetail_imdbRatings
import fr.paulbr.nookmind.resources.seriesDetail_noNotes
import fr.paulbr.nookmind.resources.seriesDetail_notePlaceholder
import fr.paulbr.nookmind.resources.seriesDetail_noteSaved
import fr.paulbr.nookmind.resources.seriesDetail_personalNote
import fr.paulbr.nookmind.resources.seriesDetail_ratingUpdated
import fr.paulbr.nookmind.resources.seriesDetail_save
import fr.paulbr.nookmind.resources.seriesDetail_seasons
import fr.paulbr.nookmind.resources.seriesDetail_seeLess
import fr.paulbr.nookmind.resources.seriesDetail_seeMore
import fr.paulbr.nookmind.resources.seriesDetail_waitingNextSeason
import fr.paulbr.nookmind.resources.seriesDetail_wantToWatch
import fr.paulbr.nookmind.resources.seriesDetail_watched
import fr.paulbr.nookmind.resources.seriesDetail_watching
import fr.paulbr.nookmind.resources.seriesDetail_yesDelete
import fr.paulbr.nookmind.resources.seriesDetail_yourRating
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Port of SeriesDetailModal.tsx. */
@Composable
fun SeriesDetailSheet(container: AppContainer, series: Series, onClose: () -> Unit) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    val categories by container.seriesCategories.items.collectAsState()
    var local by remember(series.id) { mutableStateOf(series) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tmdbSeries by remember { mutableStateOf<TmdbSeries?>(null) }
    var providers by remember { mutableStateOf<WatchProvidersResult?>(null) }
    var loadingProviders by remember { mutableStateOf(series.tmdbId != null) }
    var selectedActorId by remember { mutableStateOf<Int?>(null) }
    var episodeCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var episodeAirDates by remember { mutableStateOf<Map<String, Map<Int, String?>>>(emptyMap()) }
    var loadingEpisodesSeason by remember { mutableStateOf<Int?>(null) }
    var selectedSeason by remember { mutableStateOf<Int?>(null) }
    var tmdbEpisodes by remember { mutableStateOf<Map<Int, List<TmdbEpisode>>>(emptyMap()) }
    var loadingTmdbSeason by remember { mutableStateOf<Int?>(null) }
    var selectedEpisode by remember { mutableStateOf<Pair<Int, EpisodeInfo>?>(null) }
    var imdbSectionOpen by remember { mutableStateOf(false) }
    val imdb = rememberSeriesImdbState(container, local.title, local.firstAirDate, local.seasons, imdbSectionOpen)

    // Silent TMDB refresh on open (keeps `next_air_date` and the season count in sync for watching series).
    LaunchedEffect(series.tmdbId) {
        val tmdbId = series.tmdbId ?: return@LaunchedEffect
        scope.launch {
            providers = container.tmdb.fetchSeriesWatchProviders(tmdbId)
            loadingProviders = false
        }
        val data = container.tmdb.fetchSeriesDetails(tmdbId) ?: return@LaunchedEffect
        tmdbSeries = data
        if (series.status != SeriesStatus.WATCHING) return@LaunchedEffect
        val extracted = TmdbApi.extractSeriesData(data)
        if (extracted.nextAirDate == series.nextAirDate && extracted.nextSeasonNumber == series.nextSeasonNumber &&
            extracted.nextEpisodeNumber == series.nextEpisodeNumber && extracted.seasons == series.seasons
        ) return@LaunchedEffect
        local = local.copy(
            seasons = extracted.seasons,
            nextAirDate = extracted.nextAirDate,
            nextSeasonNumber = extracted.nextSeasonNumber,
            nextEpisodeNumber = extracted.nextEpisodeNumber,
        )
        container.series.update(
            series.id,
            patchOf(
                "seasons" to extracted.seasons,
                "next_air_date" to extracted.nextAirDate,
                "next_season_number" to extracted.nextSeasonNumber,
                "next_episode_number" to extracted.nextEpisodeNumber,
            ),
        )
    }

    fun apply(patch: Map<String, Any?>, optimistic: Series, onDone: (() -> Unit)? = null) {
        val previous = local
        local = optimistic
        scope.launch {
            val stored = container.series.update(series.id, patchOf(*patch.entries.map { it.key to it.value }.toTypedArray()))
            if (stored == null) local = previous else { local = stored; onDone?.invoke() }
        }
    }

    fun loadSeasonCounts(seasonNumber: Int) {
        val tmdbId = local.tmdbId ?: return
        if (episodeCounts.containsKey(seasonNumber.toString())) return
        loadingEpisodesSeason = seasonNumber
        scope.launch {
            val details = container.tmdb.fetchSeasonDetails(tmdbId, seasonNumber)
            if (details != null) {
                episodeCounts = episodeCounts + (seasonNumber.toString() to details.episodes.size)
                episodeAirDates = episodeAirDates + (seasonNumber.toString() to details.episodes.associate { it.episodeNumber to it.airDate })
            }
            loadingEpisodesSeason = null
        }
    }

    fun selectSeason(season: Int) {
        selectedSeason = season
        val tmdbId = local.tmdbId ?: return
        if (tmdbEpisodes.containsKey(season)) return
        loadingTmdbSeason = season
        scope.launch {
            val details = container.tmdb.fetchSeasonDetails(tmdbId, season)
            if (details != null) tmdbEpisodes = tmdbEpisodes + (season to details.episodes)
            loadingTmdbSeason = null
        }
    }

    val cast = (tmdbSeries?.credits?.cast ?: emptyList()).take(12)
    val effectiveStatus = getEffectiveSeriesStatus(local)
    val waiting = isSeriesWaiting(local)

    val imdbLookup = buildFlatEpisodeLookup(
        imdb.seasonRatings,
        (tmdbSeries?.seasons ?: emptyList()).filter { it.seasonNumber > 0 }.associate { it.seasonNumber to it.episodeCount },
    )
    val season = selectedSeason
    val episodesToShow = when {
        season == null -> emptyList()
        tmdbEpisodes[season]?.isNotEmpty() == true ->
            tmdbEpisodes.getValue(season).map { EpisodeInfo(it.episodeNumber, it, imdbLookup(season, it.episodeNumber)) }
        else -> ((imdb.seasonRatings[season] as? fr.paulbr.nookmind.core.model.SeasonState.Loaded)?.episodes ?: emptyList())
            .map { EpisodeInfo(it.episode, null, it) }
    }

    NookSheet(onClose = onClose, maxWidth = 672.dp) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                // Header: poster + title + badges
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 56.dp, top = 24.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MediaImage(local.posterUrl, local.title, Modifier.width(104.dp), mode = MediaMode.SERIES, placeholderIconSize = 32.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text(local.title, style = NookTheme.type.h3Serif.copy(lineHeight = 24.sp), color = colors.textStrong)
                            Spacer(Modifier.height(2.dp))
                            Text(local.creator, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textMuted)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            local.genre?.takeIf { it.isNotBlank() }?.let { GenrePill(it, small = true) }
                            SolidPill(
                                when {
                                    effectiveStatus == SeriesStatus.WATCHED -> stringResource(Res.string.seriesDetail_watched)
                                    waiting -> stringResource(Res.string.seriesDetail_waitingNextSeason)
                                    effectiveStatus == SeriesStatus.WATCHING -> stringResource(Res.string.seriesDetail_watching)
                                    else -> stringResource(Res.string.seriesDetail_wantToWatch)
                                },
                                when {
                                    effectiveStatus == SeriesStatus.WATCHED -> Palette.Emerald500
                                    waiting -> Palette.Purple500
                                    effectiveStatus == SeriesStatus.WATCHING -> Palette.Blue500
                                    else -> Palette.Amber500
                                },
                                small = true,
                            )
                            yearOf(local.firstAirDate)?.let { MetaPill(it, small = true) }
                            local.seasons?.let { MetaPill(pluralStringResource(Res.plurals.seriesDetail_seasons, it, it), small = true) }
                        }
                    }
                }

                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    WatchProvidersRow(container, providers, local.title, loading = loadingProviders)
                    local.tmdbId?.let { TrailerButton(container, "tv", it) }
                    local.description?.takeIf { it.isNotBlank() }?.let {
                        ExpandableDescription(
                            it,
                            seeMoreText = stringResource(Res.string.seriesDetail_seeMore),
                            seeLessText = stringResource(Res.string.seriesDetail_seeLess),
                            clampLines = 3,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (cast.isNotEmpty()) {
                        CastAccordion(cast, stringResource(Res.string.seriesDetail_cast), onSelect = { selectedActorId = it })
                    }

                    val totalSeasons = local.seasons
                    if (totalSeasons != null && totalSeasons > 0) {
                        AccordionSection(stringResource(Res.string.seriesDetail_episodesSection), initiallyOpen = true) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                SeasonGrid(
                                    totalSeasons = totalSeasons,
                                    watchedSeasons = local.watchedSeasons,
                                    watchedEpisodes = local.watchedEpisodes,
                                    onChange = { seasons, episodes ->
                                        val hasUnreleased = episodeAirDates.values.any { dates -> dates.values.any { it == null || isFutureDate(it) } }
                                        val status = deriveSeriesStatus(seasons, local.seasons, hasUnreleased, episodes)
                                        val patch = mutableMapOf<String, Any?>(
                                            "watched_seasons" to seasons,
                                            "watched_episodes" to episodes,
                                            "status" to status.key,
                                        )
                                        var optimistic = local.copy(watchedSeasons = seasons, watchedEpisodes = episodes, status = status)
                                        if (status == SeriesStatus.WANT_TO_WATCH) {
                                            patch["rating"] = null
                                            optimistic = optimistic.copy(rating = null)
                                        }
                                        apply(patch, optimistic)
                                    },
                                    episodeCounts = if (local.tmdbId != null) episodeCounts else null,
                                    episodeAirDates = if (local.tmdbId != null) episodeAirDates else null,
                                    onSeasonExpand = if (local.tmdbId != null) ({ s: Int -> loadSeasonCounts(s) }) else null,
                                    loadingEpisodesSeason = loadingEpisodesSeason,
                                    onSeasonToggle = if (local.tmdbId != null) ({ s: Int? -> if (s != null) selectSeason(s) else { selectedSeason = null } }) else null,
                                )
                            }
                            if (local.tmdbId != null && season != null) {
                                HairlineDivider()
                                EpisodeTilesGrid(
                                    episodes = episodesToShow,
                                    loading = loadingTmdbSeason == season,
                                    onSelect = { selectedEpisode = season to it },
                                )
                            }
                        }
                    }

                    AccordionSection(
                        stringResource(Res.string.seriesDetail_imdbRatings),
                        onOpen = { imdbSectionOpen = true },
                    ) {
                        ImdbRatingsSection(
                            seasonRatings = imdb.seasonRatings,
                            notFound = imdb.notFound,
                            loading = imdb.loading,
                            onRetry = { imdb.retry() },
                            fallbackSeasons = local.seasons ?: 3,
                        )
                    }
                }

                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (local.status == SeriesStatus.WATCHED) {
                        LabeledBlock(stringResource(Res.string.seriesDetail_yourRating)) {
                            StarRating(local.rating, onChange = { rating ->
                                apply(mapOf("rating" to rating), local.copy(rating = rating)) { container.toasts.success(Res.string.seriesDetail_ratingUpdated) }
                            }, size = 26.dp)
                        }
                    }
                    EditableNote(
                        note = local.personalNote,
                        labelText = stringResource(Res.string.seriesDetail_personalNote),
                        placeholderText = stringResource(Res.string.seriesDetail_notePlaceholder),
                        saveText = stringResource(Res.string.seriesDetail_save),
                        cancelText = stringResource(Res.string.seriesDetail_cancel),
                        noNotesText = stringResource(Res.string.seriesDetail_noNotes),
                        onSave = { note -> apply(mapOf("personal_note" to note), local.copy(personalNote = note)) { container.toasts.success(Res.string.seriesDetail_noteSaved) } },
                    )
                    if (categories.isNotEmpty()) {
                        Column {
                            Text(stringResource(Res.string.seriesDetail_collections), style = NookTheme.type.sm, color = colors.textSubtle)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                categories.forEach { cat ->
                                    val isIn = local.id in cat.itemIds
                                    CollectionChip(cat.title, isIn) {
                                        scope.launch {
                                            if (isIn) container.seriesCategories.removeMappedItem(cat.id, local.id)
                                            else container.seriesCategories.addMappedItems(cat.id, listOf(local.id))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (!confirmDelete) {
                            DeleteButton(stringResource(Res.string.seriesDetail_delete), onClick = { confirmDelete = true })
                        } else {
                            ConfirmDeleteRow(
                                question = stringResource(Res.string.seriesDetail_areYouSure),
                                yesText = stringResource(Res.string.seriesDetail_yesDelete),
                                cancelText = stringResource(Res.string.seriesDetail_cancel),
                                onConfirm = { scope.launch { if (container.series.delete(series.id)) onClose() } },
                                onCancel = { confirmDelete = false },
                            )
                        }
                    }
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
        }
    }

    selectedEpisode?.let { (seasonNum, info) ->
        EpisodeDetailSheet(info, seasonNum, onClose = { selectedEpisode = null })
    }
    selectedActorId?.let { ActorSheet(container, it, onClose = { selectedActorId = null }) }
}
