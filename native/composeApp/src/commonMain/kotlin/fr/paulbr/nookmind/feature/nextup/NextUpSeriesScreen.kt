package fr.paulbr.nookmind.feature.nextup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.patchOf
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.MetaPill
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.Pill
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.components.pulse
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.EpisodeState
import fr.paulbr.nookmind.core.domain.daysUntil
import fr.paulbr.nookmind.core.domain.deriveSeriesStatus
import fr.paulbr.nookmind.core.domain.episodeStateRank
import fr.paulbr.nookmind.core.domain.findNextUpcomingEpisode
import fr.paulbr.nookmind.core.domain.getCardKey
import fr.paulbr.nookmind.core.domain.getEffectiveSeriesStatus
import fr.paulbr.nookmind.core.domain.getEpisodeState
import fr.paulbr.nookmind.core.domain.markEpisodeWatched
import fr.paulbr.nookmind.core.domain.parseDateOnly
import fr.paulbr.nookmind.core.domain.toFixed1
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.core.model.TmdbEpisode
import fr.paulbr.nookmind.core.model.TmdbSeries
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.formatIsoDate
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.discover_title
import fr.paulbr.nookmind.resources.nextUp_available
import fr.paulbr.nookmind.resources.nextUp_comingSoon
import fr.paulbr.nookmind.resources.nextUp_comingSoonPlural
import fr.paulbr.nookmind.resources.nextUp_detailComingSoon
import fr.paulbr.nookmind.resources.nextUp_episodeFallback
import fr.paulbr.nookmind.resources.nextUp_episodeSynopsis
import fr.paulbr.nookmind.resources.nextUp_markEpisodeWatched
import fr.paulbr.nookmind.resources.nextUp_noEpisodeOverview
import fr.paulbr.nookmind.resources.nextUp_noWatching
import fr.paulbr.nookmind.resources.nextUp_runtimeMinutes
import fr.paulbr.nookmind.resources.nextUp_seasonEpisode
import fr.paulbr.nookmind.resources.nextUp_today
import fr.paulbr.nookmind.resources.nextUp_upToDate
import fr.paulbr.nookmind.resources.nextUp_upToDateSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val DISMISS_DURATION_MS = 380
private const val ENTER_DURATION_MS = 420

/** One card of the list: the series, its episode state and the key that drives the animations. */
private class NextUpCard(val series: Series, val state: EpisodeState, val key: String, val dismissing: Boolean)

/** Port of NextUpSeries.tsx: next episode per watching series, with the swipe-out on "watched". */
@Composable
fun NextUpSeriesScreen(container: AppContainer, contentPadding: PaddingValues) {
    val colors = NookTheme.colors
    val allSeries by container.series.items.collectAsState()
    val scope = rememberCoroutineScope()
    val watching = allSeries.filter { getEffectiveSeriesStatus(it) == SeriesStatus.WATCHING }
    var tmdbData by remember { mutableStateOf<Map<String, TmdbSeries>>(emptyMap()) }
    var loading by remember { mutableStateOf(watching.isNotEmpty()) }
    var dismissing by remember { mutableStateOf<Map<String, NextUpCard>>(emptyMap()) }
    var selectedEpisode by remember { mutableStateOf<Pair<Series, EpisodeState>?>(null) }
    val wide = LocalWideLayout.current
    val watchingKey = watching.joinToString(",") { it.id }

    LaunchedEffect(watchingKey) {
        if (watching.isEmpty()) { loading = false; return@LaunchedEffect }
        val results = HashMap<String, TmdbSeries>()
        for (s in watching) {
            val tmdbId = s.tmdbId ?: continue
            container.tmdb.fetchSeriesDetails(tmdbId)?.let { results[s.id] = it }
        }
        tmdbData = results
        loading = false
    }

    fun markWatched(series: Series, season: Int, episode: Int) {
        val tmdb = tmdbData[series.id]
        val key = "${series.id}-available-$season-$episode"
        dismissing = dismissing + (series.id to NextUpCard(series, EpisodeState.Available(season, episode), key, dismissing = true))
        scope.launch {
            delay(DISMISS_DURATION_MS.toLong())
            val (nextWatchedEpisodes, nextWatchedSeasons) = markEpisodeWatched(series, tmdb, season, episode)
            val upcoming = series.tmdbId?.let { tmdbId ->
                val seasons = (tmdb?.seasons ?: emptyList()).filter { it.seasonNumber > 0 && it.episodeCount > 0 }.map { it.seasonNumber }
                val loaded = HashMap<Int, List<TmdbEpisode>>()
                for (s in seasons.sorted()) loaded[s] = container.tmdb.fetchSeasonDetails(tmdbId, s)?.episodes ?: emptyList()
                findNextUpcomingEpisode(seasons, { loaded[it] }, nextWatchedEpisodes)
            }
            if (tmdb != null) tmdbData = tmdbData + (series.id to tmdb.copy(nextEpisodeToAir = upcoming))
            container.series.update(
                series.id,
                patchOf(
                    "watched_episodes" to nextWatchedEpisodes,
                    "watched_seasons" to nextWatchedSeasons,
                    "next_air_date" to upcoming?.airDate,
                    "next_season_number" to upcoming?.seasonNumber,
                    "next_episode_number" to upcoming?.episodeNumber,
                    "status" to deriveSeriesStatus(nextWatchedSeasons, series.seasons, upcoming != null, nextWatchedEpisodes).key,
                ),
            )
            dismissing = dismissing - series.id
        }
    }

    val liveCards = watching.map { s ->
        val tmdb = tmdbData[s.id]
        val state = if (loading && tmdb == null) EpisodeState.Unknown else getEpisodeState(s, tmdb)
        NextUpCard(s, state, getCardKey(s, state), dismissing = false)
    }
    val activeCards = watching.mapNotNull { s ->
        dismissing[s.id] ?: liveCards.firstOrNull { it.series.id == s.id }?.takeIf { it.state != EpisodeState.UpToDate }
    }.sortedWith(
        compareBy<NextUpCard> { episodeStateRank(it.state) }
            .thenBy(nullsLast()) { (it.state as? EpisodeState.ComingSoon)?.ep?.airDate?.let(::parseDateOnly) },
    )
    val upToDateCards = liveCards.filter { it.state == EpisodeState.UpToDate && it.series.id !in dismissing }

    if (watching.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Column(Modifier.heightIn(min = 256.dp).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(LucideIcons.Tv, null, Modifier.size(40.dp), tint = colors.textDisabled)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(Res.string.nextUp_noWatching), style = NookTheme.type.sm, color = colors.textSubtle, textAlign = TextAlign.Center)
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 672.dp).padding(if (wide) 32.dp else 16.dp)) {
            Text(stringResource(Res.string.discover_title), style = NookTheme.type.h2Serif, color = colors.tealText)
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                activeCards.forEach { card ->
                    key(card.key) {
                        SeriesNextCard(
                            card = card,
                            onMarkWatched = { season, episode -> markWatched(card.series, season, episode) },
                            onOpen = { selectedEpisode = card.series to card.state },
                        )
                    }
                }
            }
            if (upToDateCards.isNotEmpty()) {
                Spacer(Modifier.height(if (activeCards.isNotEmpty()) 32.dp else 0.dp))
                Text(
                    stringResource(Res.string.nextUp_upToDateSection).uppercase(),
                    style = NookTheme.type.sans(14, FontWeight.SemiBold, 20).copy(letterSpacing = 2.5.sp),
                    color = colors.textSubtle,
                )
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    upToDateCards.forEach { card ->
                        key(card.key) { SeriesNextCard(card = card, onMarkWatched = null, onOpen = null) }
                    }
                }
            }
        }
    }

    selectedEpisode?.let { (series, state) ->
        NextUpEpisodeSheet(
            container = container,
            series = series,
            state = state,
            onMarkWatched = { season, episode -> markWatched(series, season, episode) },
            onClose = { selectedEpisode = null },
        )
    }
}

/** Badge of one card: "Available", "Today · 12 Sep", "In 3 days · …" or "Up to date". */
@Composable
private fun StateBadge(state: EpisodeState) {
    val colors = NookTheme.colors
    when (state) {
        EpisodeState.Unknown -> Text("…", Modifier.pulse(), style = NookTheme.type.xs, color = colors.textFaint)
        EpisodeState.UpToDate -> Pill(
            stringResource(Res.string.nextUp_upToDate),
            background = colors.surfaceMuted,
            color = colors.textSubtle,
            textStyle = NookTheme.type.sans(12, FontWeight.Medium, 16),
        )
        is EpisodeState.Available -> Pill(
            stringResource(Res.string.nextUp_available),
            background = if (colors.isDark) Palette.Emerald900.alpha(0.4f) else Palette.Emerald100,
            color = if (colors.isDark) Palette.Emerald400 else Palette.Emerald700,
            textStyle = NookTheme.type.sans(12, FontWeight.Medium, 16),
        )
        is EpisodeState.ComingSoon -> {
            val airDate = state.ep.airDate
            if (airDate != null) {
                val dateStr = formatIsoDate(airDate, DateStyle.DAY_MONTH_SHORT_YEAR) ?: airDate
                val days = daysUntil(airDate) ?: 0
                val label = when {
                    days == 0 -> stringResource(Res.string.nextUp_today, dateStr)
                    days == 1 -> stringResource(Res.string.nextUp_comingSoon, days, dateStr)
                    else -> stringResource(Res.string.nextUp_comingSoonPlural, days, dateStr)
                }
                Pill(
                    label,
                    background = if (colors.isDark) Palette.Amber900.alpha(0.4f) else Palette.Amber100,
                    color = if (colors.isDark) Palette.Amber400 else Palette.Amber700,
                    textStyle = NookTheme.type.sans(12, FontWeight.Medium, 16),
                )
            }
        }
    }
}

@Composable
private fun SeriesNextCard(card: NextUpCard, onMarkWatched: ((Int, Int) -> Unit)?, onOpen: (() -> Unit)?) {
    val colors = NookTheme.colors
    val offset = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }

    // Enter animation (`next-up-card-enter`), skipped on the first composition of a card.
    LaunchedEffect(Unit) {
        if (!card.dismissing) {
            offset.snapTo(-28f); alpha.snapTo(0f); scale.snapTo(0.985f)
            val easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
            launch { offset.animateTo(0f, tween(ENTER_DURATION_MS, easing = easing)) }
            launch { scale.animateTo(1f, tween(ENTER_DURATION_MS, easing = easing)) }
            alpha.animateTo(1f, tween(ENTER_DURATION_MS / 2))
        }
    }
    // Dismiss animation (`next-up-card-dismiss`).
    LaunchedEffect(card.dismissing) {
        if (card.dismissing) {
            val easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            launch { offset.animateTo(56f, tween(DISMISS_DURATION_MS, easing = easing)) }
            launch { scale.animateTo(0.985f, tween(DISMISS_DURATION_MS, easing = easing)) }
            alpha.animateTo(0f, tween(DISMISS_DURATION_MS, easing = easing))
        }
    }

    val clickable = onOpen != null && (card.state is EpisodeState.Available || card.state is EpisodeState.ComingSoon)
    NookCard(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offset.value
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            }
            .then(if (clickable) Modifier.clickable { onOpen?.invoke() } else Modifier),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MediaImage(card.series.posterUrl, card.series.title, Modifier.width(48.dp), mode = MediaMode.SERIES, shape = NookShapes.lg, placeholderIconSize = 20.dp)
            Column(Modifier.weight(1f)) {
                Text(card.series.title, style = NookTheme.type.sans(16, FontWeight.SemiBold, 24), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val detail = when (val state = card.state) {
                    is EpisodeState.Available -> stringResource(Res.string.nextUp_seasonEpisode, state.season, state.episode)
                    is EpisodeState.ComingSoon -> {
                        val base = stringResource(Res.string.nextUp_seasonEpisode, state.ep.seasonNumber, state.ep.episodeNumber)
                        if (state.ep.name.isNotBlank()) "$base · ${state.ep.name}" else base
                    }
                    else -> null
                }
                if (detail != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(detail, style = NookTheme.type.sm, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                StateBadge(card.state)
            }
            val state = card.state
            if (state is EpisodeState.Available && onMarkWatched != null) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(NookShapes.full)
                        .background(Palette.Emerald500)
                        .clickable(enabled = !card.dismissing) { onMarkWatched(state.season, state.episode) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LucideIcons.Check, contentDescription = stringResource(Res.string.nextUp_markEpisodeWatched), modifier = Modifier.size(18.dp), tint = Palette.White)
                }
            }
        }
    }
}

/** Episode sheet of the Next Up list: still, meta pills, synopsis and the "Watched" action. */
@Composable
private fun NextUpEpisodeSheet(
    container: AppContainer,
    series: Series,
    state: EpisodeState,
    onMarkWatched: (Int, Int) -> Unit,
    onClose: () -> Unit,
) {
    val colors = NookTheme.colors
    val seasonNumber = when (state) {
        is EpisodeState.Available -> state.season
        is EpisodeState.ComingSoon -> state.ep.seasonNumber
        else -> return
    }
    val episodeNumber = when (state) {
        is EpisodeState.Available -> state.episode
        is EpisodeState.ComingSoon -> state.ep.episodeNumber
        else -> return
    }
    var episode by remember(series.id, seasonNumber, episodeNumber) { mutableStateOf<TmdbEpisode?>((state as? EpisodeState.ComingSoon)?.ep) }
    var loading by remember(series.id, seasonNumber, episodeNumber) { mutableStateOf(series.tmdbId != null) }
    LaunchedEffect(series.tmdbId, seasonNumber, episodeNumber) {
        val tmdbId = series.tmdbId ?: return@LaunchedEffect
        val details = container.tmdb.fetchSeasonDetails(tmdbId, seasonNumber)
        episode = details?.episodes?.firstOrNull { it.episodeNumber == episodeNumber } ?: episode
        loading = false
    }
    val name = episode?.name?.ifBlank { null } ?: stringResource(Res.string.nextUp_episodeFallback, seasonNumber, episodeNumber)
    val airDate = episode?.airDate ?: (state as? EpisodeState.ComingSoon)?.ep?.airDate
    val overview = episode?.overview?.trim()
    val still = episode?.stillPath?.let { TmdbApi.posterUrl(it) }

    NookSheet(onClose = onClose, maxWidth = 576.dp) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(24.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MediaImage(series.posterUrl, series.title, Modifier.width(80.dp), mode = MediaMode.SERIES, shape = NookShapes.xl2, placeholderIconSize = 28.dp)
                    Column(Modifier.weight(1f).padding(end = 32.dp)) {
                        Text(
                            (if (state is EpisodeState.Available) stringResource(Res.string.nextUp_available) else stringResource(Res.string.nextUp_detailComingSoon)).uppercase(),
                            style = NookTheme.type.sans(12, FontWeight.SemiBold, 16).copy(letterSpacing = 2.sp),
                            color = colors.tealText,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(name, style = NookTheme.type.h2Serif.copy(lineHeight = 28.sp), color = colors.textStrong)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${series.title} · ${stringResource(Res.string.nextUp_seasonEpisode, seasonNumber, episodeNumber)}",
                            style = NookTheme.type.sm, color = colors.textSubtle,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    formatIsoDate(airDate, DateStyle.DAY_MONTH_SHORT_YEAR)?.let { MetaPill(it, icon = LucideIcons.CalendarDays) }
                    episode?.runtime?.takeIf { it > 0 }?.let { MetaPill(pluralStringResource(Res.plurals.nextUp_runtimeMinutes, it, it), icon = LucideIcons.Clock3) }
                    episode?.voteAverage?.takeIf { it > 0 }?.let {
                        Pill(
                            it.toFixed1(),
                            background = Palette.Amber500.alpha(0.10f),
                            color = colors.amberTextStrong,
                            icon = LucideIcons.Star,
                            iconSize = 14.dp,
                            textStyle = NookTheme.type.sm,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
                if (still != null) {
                    Spacer(Modifier.height(20.dp))
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(NookShapes.xl2).background(colors.surfaceMuted)) {
                        AsyncImage(model = still, contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(Res.string.nextUp_episodeSynopsis), style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textSubtle)
                Spacer(Modifier.height(4.dp))
                when {
                    loading && overview.isNullOrBlank() -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBox(Modifier.fillMaxWidth().height(14.dp), shape = NookShapes.sm)
                        SkeletonBox(Modifier.fillMaxWidth(0.9f).height(14.dp), shape = NookShapes.sm)
                        SkeletonBox(Modifier.fillMaxWidth(0.65f).height(14.dp), shape = NookShapes.sm)
                    }
                    !overview.isNullOrBlank() -> Text(overview, style = NookTheme.type.sans(14, lineHeight = 23), color = colors.textBody2)
                    else -> Text(stringResource(Res.string.nextUp_noEpisodeOverview), style = NookTheme.type.sm, color = colors.textSubtle)
                }
                if (state is EpisodeState.Available) {
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PrimaryButton(
                            stringResource(Res.string.nextUp_markEpisodeWatched),
                            onClick = { onMarkWatched(seasonNumber, episodeNumber); controller.close() },
                            modifier = Modifier.defaultMinSize(minWidth = 144.dp),
                            icon = LucideIcons.Check, iconSize = 18.dp,
                        )
                    }
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
        }
    }
}
