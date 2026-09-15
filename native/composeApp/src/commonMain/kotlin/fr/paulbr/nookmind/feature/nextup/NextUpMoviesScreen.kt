package fr.paulbr.nookmind.feature.nextup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.MetaPill
import fr.paulbr.nookmind.core.designsystem.components.NookPressable
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.daysUntil
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.TrailerButton
import fr.paulbr.nookmind.feature.common.formatIsoDate
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.movieDetail_description
import fr.paulbr.nookmind.resources.movieDetail_runtime
import fr.paulbr.nookmind.resources.movieHome_inWatchlist
import fr.paulbr.nookmind.resources.nextUp_addToWatchlist
import fr.paulbr.nookmind.resources.nextUp_basedOnGenres
import fr.paulbr.nookmind.resources.nextUp_comingSoonMovies
import fr.paulbr.nookmind.resources.nextUp_detailComingSoon
import fr.paulbr.nookmind.resources.nextUp_inDays
import fr.paulbr.nookmind.resources.nextUp_inDaysPlural
import fr.paulbr.nookmind.resources.nextUp_loadingMovieDetails
import fr.paulbr.nookmind.resources.nextUp_newlyReleasedMovies
import fr.paulbr.nookmind.resources.nextUp_closeMovieDetails
import fr.paulbr.nookmind.resources.nextUp_noMovieOverview
import fr.paulbr.nookmind.resources.nextUp_openMovieDetails
import fr.paulbr.nookmind.resources.nextUp_popularReleases
import fr.paulbr.nookmind.resources.nextUp_recentReleasesMovies
import fr.paulbr.nookmind.resources.nextUp_releasedOn
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.compose.resources.stringResource

private const val UPCOMING_CACHE_KEY = "nookmind_upcoming_movies_v3"
private const val RECENT_CACHE_KEY = "nookmind_recent_movies_v3"
private const val DAY_MS: Long = 24 * 60 * 60 * 1000

/** Port of NextUpMovies.tsx: recent releases and upcoming movies from TMDB, ordered by the user's favourite genres. */
@Composable
fun NextUpMoviesScreen(container: AppContainer, contentPadding: PaddingValues) {
    val movies by container.movies.items.collectAsState()
    val wide = LocalWideLayout.current
    var upcoming by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var recent by remember { mutableStateOf<List<TmdbMovie>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedMovie by remember { mutableStateOf<TmdbMovie?>(null) }
    val scope = rememberCoroutineScope()

    val watched = movies.filter { it.status == MovieStatus.WATCHED }
    val hasGenreData = watched.size >= 3
    val topGenres = watched.mapNotNull { it.genre?.takeIf(String::isNotBlank) }.groupingBy { it }.eachCount().entries
        .sortedByDescending { it.value }.map { it.key }

    fun sortByTopGenres(results: List<TmdbMovie>): List<TmdbMovie> = if (!hasGenreData) results else results.sortedWith { a, b ->
        val aScore = topGenres.indexOf(a.genres?.firstOrNull()?.name ?: "")
        val bScore = topGenres.indexOf(b.genres?.firstOrNull()?.name ?: "")
        when {
            aScore == -1 && bScore == -1 -> 0
            aScore == -1 -> 1
            bScore == -1 -> -1
            else -> aScore - bScore
        }
    }

    LaunchedEffect(Unit) {
        val serializer = ListSerializer(TmdbMovie.serializer())
        val cachedUpcoming = container.cache.get(UPCOMING_CACHE_KEY, serializer, DAY_MS)?.takeIf { it.isNotEmpty() }
        val cachedRecent = container.cache.get(RECENT_CACHE_KEY, serializer, DAY_MS)?.takeIf { it.isNotEmpty() }
        if (cachedUpcoming != null) upcoming = cachedUpcoming
        if (cachedRecent != null) recent = cachedRecent
        if (cachedUpcoming != null || cachedRecent != null) loading = false
        coroutineScope {
            val up = async { container.tmdb.fetchUpcomingMovies(20) }
            val rec = async { container.tmdb.fetchRecentMovies(20) }
            val nextUpcoming = sortByTopGenres(up.await()).take(10)
            val nextRecent = sortByTopGenres(rec.await()).take(10)
            if (nextUpcoming.isNotEmpty()) container.cache.put(UPCOMING_CACHE_KEY, serializer, nextUpcoming)
            if (nextRecent.isNotEmpty()) container.cache.put(RECENT_CACHE_KEY, serializer, nextRecent)
            upcoming = nextUpcoming
            recent = nextRecent
        }
        loading = false
    }

    val watchlistIds = movies.mapNotNull { it.tmdbId }.toSet()
    val byTmdbId = movies.filter { it.tmdbId != null }.associateBy { it.tmdbId!! }

    fun addToWatchlist(movie: TmdbMovie) {
        scope.launch { container.movies.add(TmdbApi.extractMovieData(movie).copy(status = MovieStatus.WANT_TO_WATCH, rating = null, personalNote = null, watchedDate = null)) }
    }

    val pagePadding = PaddingValues(horizontal = if (wide) 32.dp else 0.dp, vertical = if (wide) 32.dp else 16.dp)
    val rowPadding = if (wide) 0.dp else 16.dp

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 1152.dp).padding(pagePadding)) {
            when {
                loading -> {
                    SkeletonBox(Modifier.padding(horizontal = rowPadding).width(160.dp).height(28.dp), shape = NookShapes.lg, color = NookTheme.colors.surfaceMuted)
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.padding(horizontal = rowPadding), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(5) {
                            Column(Modifier.width(144.dp)) {
                                SkeletonBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f), shape = NookShapes.xl, color = NookTheme.colors.surfaceMuted)
                                Spacer(Modifier.height(8.dp))
                                SkeletonBox(Modifier.fillMaxWidth().height(16.dp), shape = NookShapes.sm, color = NookTheme.colors.surfaceMuted)
                            }
                        }
                    }
                }
                upcoming.isEmpty() && recent.isEmpty() -> Unit
                else -> {
                    MovieReleaseSection(
                        title = stringResource(Res.string.nextUp_recentReleasesMovies),
                        subtitle = stringResource(Res.string.nextUp_newlyReleasedMovies),
                        movies = recent.filter { it.id !in watchlistIds },
                        rowPadding = rowPadding,
                        onOpen = { selectedMovie = it },
                        onAdd = ::addToWatchlist,
                    )
                    if (recent.any { it.id !in watchlistIds } && upcoming.any { it.id !in watchlistIds }) Spacer(Modifier.height(32.dp))
                    MovieReleaseSection(
                        title = stringResource(Res.string.nextUp_comingSoonMovies),
                        subtitle = if (hasGenreData) stringResource(Res.string.nextUp_basedOnGenres) else stringResource(Res.string.nextUp_popularReleases),
                        movies = upcoming.filter { it.id !in watchlistIds },
                        rowPadding = rowPadding,
                        onOpen = { selectedMovie = it },
                        onAdd = ::addToWatchlist,
                    )
                }
            }
        }
    }

    selectedMovie?.let { movie ->
        MoviePreviewSheet(container, movie, existing = byTmdbId[movie.id], onAdd = { addToWatchlist(movie) }, onClose = { selectedMovie = null })
    }
}

/** Date label under a release card: "Released 12 Sep 2026", the date itself today, or "In N days". */
@Composable
private fun releaseLabel(releaseDate: String): String? {
    val dateStr = formatIsoDate(releaseDate, DateStyle.DAY_MONTH_SHORT_YEAR) ?: return null
    val days = daysUntil(releaseDate) ?: return null
    return when {
        days < 0 -> stringResource(Res.string.nextUp_releasedOn, dateStr)
        days == 0 -> dateStr
        days == 1 -> stringResource(Res.string.nextUp_inDays, days)
        else -> stringResource(Res.string.nextUp_inDaysPlural, days)
    }
}

@Composable
private fun MovieReleaseSection(
    title: String,
    subtitle: String,
    movies: List<TmdbMovie>,
    rowPadding: androidx.compose.ui.unit.Dp,
    onOpen: (TmdbMovie) -> Unit,
    onAdd: (TmdbMovie) -> Unit,
) {
    if (movies.isEmpty()) return
    val colors = NookTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = rowPadding)) {
            Text(title, style = NookTheme.type.h2Serif, color = colors.indigoText)
            Text(subtitle, style = NookTheme.type.sm, color = colors.textSubtle)
        }
        Spacer(Modifier.height(16.dp))
        LazyRow(contentPadding = PaddingValues(start = rowPadding, end = rowPadding, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            items(movies, key = { it.id }) { movie ->
                Column(Modifier.width(144.dp).heightIn(min = 336.dp)) {
                    MediaImage(
                        TmdbApi.posterUrl(movie.posterPath, "w300"),
                        stringResource(Res.string.nextUp_openMovieDetails, movie.title),
                        Modifier.fillMaxWidth().clip(NookShapes.xl).clickable { onOpen(movie) },
                        mode = MediaMode.MOVIES, placeholderIconSize = 24.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(movie.title, style = NookTheme.type.sans(14, FontWeight.Medium, 17), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    movie.releaseDate.takeIf { it.isNotBlank() }?.let { releaseLabel(it) }?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(it, style = NookTheme.type.xs, color = colors.amberText)
                    }
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(8.dp))
                    NookPressable(
                        onClick = { onAdd(movie) },
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 32.dp),
                        background = if (colors.isDark) Palette.Gray800 else Palette.White,
                        borderColor = colors.borderNeutral,
                        contentColor = if (colors.isDark) Palette.Gray300 else Palette.Gray600,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(stringResource(Res.string.nextUp_addToWatchlist), style = NookTheme.type.sans(12, FontWeight.Medium, 16), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** `MoviePreviewSheet` of NextUpMovies.tsx: poster, "Coming soon" overline, meta, synopsis, trailer, add button. */
@Composable
fun MoviePreviewSheet(container: AppContainer, movie: TmdbMovie, existing: Movie?, onAdd: () -> Unit, onClose: () -> Unit) {
    val colors = NookTheme.colors
    var details by remember(movie.id) { mutableStateOf<TmdbMovie?>(null) }
    var loading by remember(movie.id) { mutableStateOf(true) }
    LaunchedEffect(movie.id) {
        details = container.tmdb.fetchMovieDetails(movie.id)
        loading = false
    }
    val merged = details ?: movie
    val posterUrl = TmdbApi.posterUrl(merged.posterPath) ?: existing?.posterUrl
    val director = existing?.director?.takeIf { it.isNotBlank() } ?: TmdbApi.extractDirector(merged)
    val runtime = (merged.runtime ?: existing?.runtime)?.takeIf { it > 0 }
    val releaseDate = merged.releaseDate.ifBlank { null } ?: existing?.releaseDate
    val genre = merged.genres?.firstOrNull()?.name ?: existing?.genre
    val overview = merged.overview.trim().ifBlank { null } ?: existing?.description ?: ""
    val wide = LocalWideLayout.current

    NookSheet(onClose = onClose, maxWidth = 576.dp) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(if (wide) 24.dp else 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MediaImage(posterUrl, movie.title, Modifier.width(if (wide) 112.dp else 96.dp), mode = MediaMode.MOVIES, shape = NookShapes.xl2, placeholderIconSize = 28.dp)
                    Column(Modifier.weight(1f).padding(end = 32.dp)) {
                        Text(
                            stringResource(Res.string.nextUp_detailComingSoon).uppercase(),
                            style = NookTheme.type.sans(12, FontWeight.SemiBold, 16).copy(letterSpacing = 2.sp),
                            color = colors.amberText,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(movie.title, style = NookTheme.type.h2Serif.copy(lineHeight = 28.sp), color = colors.textStrong)
                        if (director.isNotBlank() && director != "Unknown Director") {
                            Spacer(Modifier.height(4.dp))
                            Text(director, style = NookTheme.type.sm, color = colors.textMuted)
                        }
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            genre?.let { GenrePill(it, small = true) }
                            formatIsoDate(releaseDate, DateStyle.DAY_MONTH_SHORT_YEAR)?.let { MetaPill(it, icon = LucideIcons.CalendarDays, small = true) }
                            runtime?.let { MetaPill(stringResource(Res.string.movieDetail_runtime, it), icon = LucideIcons.Clock3, small = true) }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(Res.string.movieDetail_description), style = NookTheme.type.sm, color = colors.textSubtle)
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        loading && overview.isBlank() -> stringResource(Res.string.nextUp_loadingMovieDetails)
                        overview.isBlank() -> stringResource(Res.string.nextUp_noMovieOverview)
                        else -> overview
                    },
                    style = NookTheme.type.sans(14, lineHeight = 23), color = colors.textBody2,
                )
                Spacer(Modifier.height(16.dp))
                TrailerButton(container, "movie", movie.id)
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (existing != null) {
                        Box(
                            Modifier.clip(NookShapes.full)
                                .background(if (colors.isDark) Palette.Emerald500.alpha(0.10f) else Palette.Emerald50, NookShapes.full)
                                .border(1.dp, if (colors.isDark) Palette.Emerald900.alpha(0.6f) else Palette.Emerald200, NookShapes.full)
                                .defaultMinSize(minHeight = 40.dp).padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(stringResource(Res.string.movieHome_inWatchlist), style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = if (colors.isDark) Palette.Emerald300 else Palette.Emerald700)
                        }
                    } else {
                        PrimaryButton(
                            stringResource(Res.string.nextUp_addToWatchlist),
                            onClick = onAdd,
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp, minWidth = 152.dp),
                            icon = LucideIcons.Plus, iconSize = 18.dp,
                            textStyle = NookTheme.type.sans(14, FontWeight.SemiBold, 20),
                        )
                    }
                }
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(16.dp).size(40.dp)
                    .shadow(1.dp, NookShapes.full)
                    .clip(NookShapes.full)
                    .background(colors.surface.alpha(0.85f))
                    .clickable { controller.close() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(LucideIcons.X, stringResource(Res.string.nextUp_closeMovieDetails), Modifier.size(18.dp), tint = colors.textBody2)
            }
        }
    }
}
