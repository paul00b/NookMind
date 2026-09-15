package fr.paulbr.nookmind.feature.series

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.ExpandableDescription
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.SeasonState
import fr.paulbr.nookmind.core.model.TmdbEpisode
import fr.paulbr.nookmind.core.model.TmdbSeries
import fr.paulbr.nookmind.core.domain.buildFlatEpisodeLookup
import fr.paulbr.nookmind.feature.common.CastAccordion
import fr.paulbr.nookmind.feature.common.TrailerButton
import fr.paulbr.nookmind.feature.movies.ActorSheet
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.seriesDetail_addToCollection
import fr.paulbr.nookmind.resources.seriesDetail_cast
import fr.paulbr.nookmind.resources.seriesDetail_episodesSection
import fr.paulbr.nookmind.resources.seriesDetail_imdbRatings
import fr.paulbr.nookmind.resources.seriesDetail_loadingEpisodes
import fr.paulbr.nookmind.resources.seriesDetail_noEpisodeData
import fr.paulbr.nookmind.resources.seriesDetail_seasons
import fr.paulbr.nookmind.resources.seriesDetail_seeLess
import fr.paulbr.nookmind.resources.seriesDetail_seeMore
import fr.paulbr.nookmind.resources.seriesHome_inList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Port of SeriesPreviewSheet.tsx: read-only look at a series that is not in the library yet
 * (IMDb ratings, episodes, cast, trailer) with an "add" affordance.
 */
@Composable
fun SeriesPreviewSheet(
    container: AppContainer,
    title: String,
    tmdbId: Int?,
    onClose: () -> Unit,
    creator: String? = null,
    description: String? = null,
    posterUrl: String? = null,
    firstAirDate: String? = null,
    totalSeasons: Int? = null,
    genre: String? = null,
    showAddButton: Boolean = false,
    alreadyAdded: Boolean = false,
    onAdd: (() -> Unit)? = null,
) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    var tmdbSeries by remember(tmdbId) { mutableStateOf<TmdbSeries?>(null) }
    var tmdbSeasonCount by remember(tmdbId) { mutableStateOf<Int?>(null) }
    var selectedSeason by remember(tmdbId) { mutableStateOf<Int?>(null) }
    var tmdbEpisodes by remember(tmdbId) { mutableStateOf<Map<Int, List<TmdbEpisode>>>(emptyMap()) }
    var loadingTmdbSeason by remember { mutableStateOf<Int?>(null) }
    var selectedEpisode by remember { mutableStateOf<Pair<Int, EpisodeInfo>?>(null) }
    var selectedActorId by remember { mutableStateOf<Int?>(null) }
    val imdb = rememberSeriesImdbState(container, title, firstAirDate, totalSeasons, enabled = true)

    LaunchedEffect(tmdbId) {
        val id = tmdbId ?: return@LaunchedEffect
        val details = container.tmdb.fetchSeriesDetails(id) ?: return@LaunchedEffect
        tmdbSeries = details
        tmdbSeasonCount = details.numberOfSeasons
    }

    val effectiveSeasonCount = totalSeasons ?: tmdbSeasonCount
    val availableSeasons = if (effectiveSeasonCount != null && effectiveSeasonCount > 0) (1..effectiveSeasonCount).toList()
    else imdb.seasonRatings.keys.sorted()
    val cast = (tmdbSeries?.credits?.cast ?: emptyList()).take(12)

    fun selectSeason(season: Int) {
        selectedSeason = season
        val id = tmdbId ?: return
        if (tmdbEpisodes.containsKey(season)) return
        loadingTmdbSeason = season
        scope.launch {
            val details = container.tmdb.fetchSeasonDetails(id, season)
            if (details != null) tmdbEpisodes = tmdbEpisodes + (season to details.episodes)
            loadingTmdbSeason = null
        }
    }

    val imdbLookup = buildFlatEpisodeLookup(
        imdb.seasonRatings,
        (tmdbSeries?.seasons ?: emptyList()).filter { it.seasonNumber > 0 }.associate { it.seasonNumber to it.episodeCount },
    )
    val season = selectedSeason
    val episodesToShow = when {
        season == null -> emptyList()
        tmdbEpisodes[season]?.isNotEmpty() == true ->
            tmdbEpisodes.getValue(season).map { EpisodeInfo(it.episodeNumber, it, imdbLookup(season, it.episodeNumber)) }
        else -> ((imdb.seasonRatings[season] as? SeasonState.Loaded)?.episodes ?: emptyList()).map { EpisodeInfo(it.episode, null, it) }
    }

    val metaLine = listOfNotNull(
        creator?.takeIf { it.isNotBlank() },
        genre?.takeIf { it.isNotBlank() },
        yearOf(firstAirDate),
        totalSeasons?.let { pluralStringResource(Res.plurals.seriesDetail_seasons, it, it) },
    ).joinToString(" · ")

    NookSheet(onClose = onClose, maxWidth = 672.dp) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 56.dp, top = 24.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MediaImage(posterUrl, title, Modifier.width(64.dp), mode = MediaMode.SERIES, shape = NookShapes.lg, placeholderIconSize = 22.dp)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = NookTheme.type.sans(16, FontWeight.Bold, 22), color = colors.textStrong)
                        if (metaLine.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(metaLine, style = NookTheme.type.xs, color = colors.textSubtle)
                        }
                        if (showAddButton) {
                            Spacer(Modifier.height(8.dp))
                            if (alreadyAdded) {
                                Row(
                                    Modifier.clip(NookShapes.lg).background(Palette.Emerald500.alpha(0.05f), NookShapes.lg)
                                        .border(1.dp, Palette.Emerald500.alpha(0.3f), NookShapes.lg)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(LucideIcons.CheckCircle2, null, Modifier.size(13.dp), tint = colors.emeraldText)
                                    Text(stringResource(Res.string.seriesHome_inList), style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = colors.emeraldText)
                                }
                            } else if (onAdd != null) {
                                Row(
                                    Modifier.clip(NookShapes.lg).border(1.dp, Palette.Amber500.alpha(0.4f), NookShapes.lg)
                                        .clickable { controller.close(); onAdd() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(LucideIcons.Plus, null, Modifier.size(13.dp), tint = colors.amberText)
                                    Text(stringResource(Res.string.seriesDetail_addToCollection), style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = colors.amberText)
                                }
                            }
                        }
                    }
                }

                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tmdbId?.let { TrailerButton(container, "tv", it) }
                    description?.takeIf { it.isNotBlank() }?.let {
                        ExpandableDescription(
                            it,
                            seeMoreText = stringResource(Res.string.seriesDetail_seeMore),
                            seeLessText = stringResource(Res.string.seriesDetail_seeLess),
                            clampLines = 3,
                            textStyle = NookTheme.type.sans(12, lineHeight = 19),
                            textColor = colors.textSubtle,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (cast.isNotEmpty()) {
                        CastAccordion(cast, stringResource(Res.string.seriesDetail_cast), onSelect = { selectedActorId = it })
                    }
                    AccordionSection(
                        stringResource(Res.string.seriesDetail_episodesSection),
                        onOpen = { if (selectedSeason == null) availableSeasons.firstOrNull()?.let { selectSeason(it) } },
                    ) {
                        when {
                            availableSeasons.isEmpty() && tmdbId != null -> Text(
                                stringResource(Res.string.seriesDetail_loadingEpisodes),
                                style = NookTheme.type.sm, color = colors.textFaint,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            availableSeasons.isEmpty() -> Text(
                                stringResource(Res.string.seriesDetail_noEpisodeData),
                                style = NookTheme.type.sm, color = colors.textFaint,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            else -> {
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    availableSeasons.forEach { s ->
                                        val active = selectedSeason == s
                                        Box(
                                            Modifier
                                                .clip(NookShapes.lg)
                                                .background(if (active) Palette.Amber500 else colors.surfaceMuted, NookShapes.lg)
                                                .clickable { selectSeason(s) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                        ) {
                                            Text("S$s", style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = if (active) Palette.White else colors.textMuted)
                                        }
                                    }
                                }
                                HairlineDivider()
                                if (season != null) {
                                    EpisodeTilesGrid(
                                        episodes = episodesToShow,
                                        loading = loadingTmdbSeason == season,
                                        onSelect = { selectedEpisode = season to it },
                                    )
                                }
                            }
                        }
                    }
                    AccordionSection(stringResource(Res.string.seriesDetail_imdbRatings)) {
                        ImdbRatingsSection(
                            seasonRatings = imdb.seasonRatings,
                            notFound = imdb.notFound,
                            loading = imdb.loading,
                            onRetry = { imdb.retry() },
                            fallbackSeasons = totalSeasons ?: 3,
                        )
                    }
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
        }
    }

    selectedEpisode?.let { (seasonNum, info) -> EpisodeDetailSheet(info, seasonNum, onClose = { selectedEpisode = null }) }
    selectedActorId?.let { ActorSheet(container, it, onClose = { selectedActorId = null }) }
}
