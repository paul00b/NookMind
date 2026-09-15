package fr.paulbr.nookmind.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.Spinner
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.SearchSections
import fr.paulbr.nookmind.core.domain.getEffectiveSeriesStatus
import fr.paulbr.nookmind.core.domain.isSeriesWaiting
import fr.paulbr.nookmind.core.domain.normalizeTitle
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.model.TmdbSeries
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.feature.common.AlreadyAddedPill
import fr.paulbr.nookmind.feature.common.HomeScreenScaffold
import fr.paulbr.nookmind.feature.common.HomeSection
import fr.paulbr.nookmind.feature.common.PosterCornerBadge
import fr.paulbr.nookmind.feature.common.PosterSlideCard
import fr.paulbr.nookmind.feature.common.PosterSlider
import fr.paulbr.nookmind.feature.common.SearchResultRow
import fr.paulbr.nookmind.feature.common.TrendingCategory
import fr.paulbr.nookmind.feature.common.TrendingSlider
import fr.paulbr.nookmind.feature.common.rememberSearchController
import fr.paulbr.nookmind.feature.series.AddSeriesSheet
import fr.paulbr.nookmind.feature.series.SeriesDetailSheet
import fr.paulbr.nookmind.feature.series.SeriesPreviewSheet
import fr.paulbr.nookmind.feature.series.waitingLabel
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.seriesHome_categoryOnAir
import fr.paulbr.nookmind.resources.seriesHome_categoryTopRated
import fr.paulbr.nookmind.resources.seriesHome_categoryTrending
import fr.paulbr.nookmind.resources.seriesHome_inList
import fr.paulbr.nookmind.resources.seriesHome_lastWatched
import fr.paulbr.nookmind.resources.seriesHome_noSeriesFound
import fr.paulbr.nookmind.resources.seriesHome_searchPlaceholder
import fr.paulbr.nookmind.resources.seriesHome_searchTimeout
import fr.paulbr.nookmind.resources.seriesHome_subtitle
import fr.paulbr.nookmind.resources.seriesHome_title
import fr.paulbr.nookmind.resources.seriesHome_trendingTitle
import fr.paulbr.nookmind.resources.seriesHome_waitingNextSeason
import fr.paulbr.nookmind.resources.seriesHome_wantToWatch
import fr.paulbr.nookmind.resources.seriesHome_watching
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of SeriesHome.tsx: TMDB search (with the IMDb preview button), Discover and library sliders. */
@Composable
fun SeriesHomeScreen(container: AppContainer, contentPadding: PaddingValues) {
    val colors = NookTheme.colors
    val allSeries by container.series.items.collectAsState()
    val sectionPrefs by container.prefs.searchSections(MediaMode.SERIES).collectAsState()
    val scope = rememberCoroutineScope()
    val search = rememberSearchController<TmdbSeries>(timeoutText = stringResource(Res.string.seriesHome_searchTimeout)) {
        container.tmdb.searchSeries(it)
    }
    var prefill by remember { mutableStateOf<Series?>(null) }
    var selected by remember { mutableStateOf<Series?>(null) }
    var previewTarget by remember { mutableStateOf<TmdbSeries?>(null) }
    var selectingId by remember { mutableStateOf<Int?>(null) }
    val inList = stringResource(Res.string.seriesHome_inList)
    val visibility = sectionPrefs.associate { it.id to it.visible }
    val trackedIds = allSeries.mapNotNull { it.tmdbId }.toSet()

    fun findExisting(tmdb: TmdbSeries): Series? =
        allSeries.firstOrNull { it.tmdbId == tmdb.id || normalizeTitle(it.title) == normalizeTitle(tmdb.name) }

    fun selectSeries(tmdb: TmdbSeries) {
        val existing = findExisting(tmdb)
        if (existing != null) {
            selected = existing
            search.close()
            return
        }
        if (selectingId != null) return
        selectingId = tmdb.id
        scope.launch {
            val details = container.tmdb.fetchSeriesDetails(tmdb.id)
            selectingId = null
            prefill = TmdbApi.extractSeriesData(details ?: tmdb)
            search.close()
        }
    }

    val trendingCategories = listOf(
        TrendingCategory("trending", stringResource(Res.string.seriesHome_categoryTrending)) { container.tmdb.fetchTrendingSeries(it) },
        TrendingCategory("on_air", stringResource(Res.string.seriesHome_categoryOnAir)) { container.tmdb.fetchOnAirSeries(it) },
        TrendingCategory("top_rated", stringResource(Res.string.seriesHome_categoryTopRated)) { container.tmdb.fetchTopRatedSeries(it) },
    )

    val sections = SearchSections.orderSections(
        listOf(
            HomeSection("trending", visibility["trending"] != false) {
                TrendingSlider(
                    title = stringResource(Res.string.seriesHome_trendingTitle),
                    categories = trendingCategories,
                    trackedIds = trackedIds,
                    idOf = { it.id },
                    titleOf = { it.name },
                    posterOf = { TmdbApi.posterUrl(it.posterPath) },
                    onSelect = { previewTarget = it },
                    mode = MediaMode.SERIES,
                )
            },
            HomeSection("watching", visibility["watching"] != false) {
                val list = allSeries.filter { getEffectiveSeriesStatus(it) == SeriesStatus.WATCHING && !isSeriesWaiting(it) }.take(10)
                if (list.isNotEmpty()) {
                    PosterSlider(stringResource(Res.string.seriesHome_watching), icon = LucideIcons.Play, titleColor = colors.blueText) {
                        items(list, key = { it.id }) { s ->
                            PosterSlideCard(s.posterUrl, s.title, onClick = { selected = s }, mode = MediaMode.SERIES) {
                                PosterCornerBadge("S${s.watchedSeasons.size}/${s.seasons ?: "?"}", Palette.Blue500)
                            }
                        }
                    }
                }
            },
            HomeSection("waiting", visibility["waiting"] != false) {
                val list = allSeries
                    .filter { val st = getEffectiveSeriesStatus(it); (st == SeriesStatus.WATCHING || st == SeriesStatus.WATCHED) && isSeriesWaiting(it) }
                    .sortedWith(compareBy(nullsLast()) { it.nextAirDate })
                    .take(10)
                if (list.isNotEmpty()) {
                    PosterSlider(stringResource(Res.string.seriesHome_waitingNextSeason), icon = LucideIcons.Clock, titleColor = colors.purpleText) {
                        items(list, key = { it.id }) { s ->
                            val label = waitingLabel(s.nextAirDate)
                            PosterSlideCard(s.posterUrl, s.title, onClick = { selected = s }, mode = MediaMode.SERIES) {
                                PosterCornerBadge(label, Palette.Purple500)
                            }
                        }
                    }
                }
            },
            HomeSection("want_to_watch", visibility["want_to_watch"] != false) {
                val list = allSeries.filter { getEffectiveSeriesStatus(it) == SeriesStatus.WANT_TO_WATCH }.take(10)
                if (list.isNotEmpty()) {
                    PosterSlider(stringResource(Res.string.seriesHome_wantToWatch), icon = LucideIcons.Bookmark) {
                        items(list, key = { it.id }) { s ->
                            val future = waitingLabel(s.firstAirDate, fallback = "")
                            PosterSlideCard(s.posterUrl, s.title, onClick = { selected = s }, mode = MediaMode.SERIES, badge = if (future.isNotEmpty()) ({ PosterCornerBadge(future, Palette.Sky500) }) else null)
                        }
                    }
                }
            },
            HomeSection("last_watched", visibility["last_watched"] != false) {
                val list = allSeries.filter { getEffectiveSeriesStatus(it) == SeriesStatus.WATCHED }.take(10)
                if (list.isNotEmpty()) {
                    PosterSlider(stringResource(Res.string.seriesHome_lastWatched), icon = LucideIcons.CheckCheck, titleColor = colors.emeraldText) {
                        items(list, key = { it.id }) { s ->
                            PosterSlideCard(s.posterUrl, s.title, onClick = { selected = s }, rating = s.rating, mode = MediaMode.SERIES)
                        }
                    }
                }
            },
        ),
        sectionPrefs,
    ) { it.id }

    HomeScreenScaffold(
        contentPadding = contentPadding,
        title = stringResource(Res.string.seriesHome_title),
        subtitle = stringResource(Res.string.seriesHome_subtitle),
        titleColor = colors.tealText,
        search = search,
        searchPlaceholder = stringResource(Res.string.seriesHome_searchPlaceholder),
        noResultsText = stringResource(Res.string.seriesHome_noSeriesFound, search.query),
        sections = sections,
        resultKey = { it.id },
    ) { tmdb ->
        val alreadyAdded = findExisting(tmdb) != null
        SearchResultRow(
            imageUrl = TmdbApi.posterUrl(tmdb.posterPath),
            title = tmdb.name,
            subtitle = yearOf(tmdb.firstAirDate) ?: "—",
            onClick = { selectSeries(tmdb) },
            enabled = selectingId == null,
            placeholderIcon = LucideIcons.Tv,
            trailing = {
                when {
                    selectingId == tmdb.id -> Spinner(size = 16.dp, color = colors.textMuted, trackColor = colors.borderNeutral)
                    alreadyAdded -> AlreadyAddedPill(inList)
                    else -> Unit
                }
                Box(
                    Modifier
                        .padding(start = 8.dp)
                        .size(34.dp)
                        .clip(NookShapes.lg)
                        .background(Palette.Amber500, NookShapes.lg)
                        .clickable(enabled = selectingId == null) { search.hideDropdown(); previewTarget = tmdb },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LucideIcons.Eye, null, Modifier.size(17.dp), tint = Palette.White)
                }
            },
        )
    }

    prefill?.let { AddSeriesSheet(container, it, onClose = { prefill = null }) }
    selected?.let { SeriesDetailSheet(container, it, onClose = { selected = null }) }
    previewTarget?.let { tmdb ->
        SeriesPreviewSheet(
            container = container,
            title = tmdb.name,
            tmdbId = tmdb.id,
            onClose = { previewTarget = null },
            description = tmdb.overview.ifBlank { null },
            posterUrl = TmdbApi.posterUrl(tmdb.posterPath),
            firstAirDate = tmdb.firstAirDate.ifBlank { null },
            totalSeasons = tmdb.numberOfSeasons,
            showAddButton = true,
            alreadyAdded = findExisting(tmdb) != null,
            onAdd = { selectSeries(tmdb) },
        )
    }
}
