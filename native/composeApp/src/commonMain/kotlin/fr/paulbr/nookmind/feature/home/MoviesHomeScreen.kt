package fr.paulbr.nookmind.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.Spinner
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.SearchSections
import fr.paulbr.nookmind.core.domain.normalizeTitle
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.core.model.TmdbMovie
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.feature.common.AlreadyAddedPill
import fr.paulbr.nookmind.feature.common.HomeScreenScaffold
import fr.paulbr.nookmind.feature.common.HomeSection
import fr.paulbr.nookmind.feature.common.PosterSlideCard
import fr.paulbr.nookmind.feature.common.PosterSlider
import fr.paulbr.nookmind.feature.common.SearchResultRow
import fr.paulbr.nookmind.feature.common.TrendingCategory
import fr.paulbr.nookmind.feature.common.TrendingSlider
import fr.paulbr.nookmind.feature.common.rememberSearchController
import fr.paulbr.nookmind.feature.movies.AddMovieSheet
import fr.paulbr.nookmind.feature.movies.MovieDetailSheet
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.movieHome_categoryNowPlaying
import fr.paulbr.nookmind.resources.movieHome_categoryTopRated
import fr.paulbr.nookmind.resources.movieHome_categoryTrending
import fr.paulbr.nookmind.resources.movieHome_inWatchlist
import fr.paulbr.nookmind.resources.movieHome_lastWatched
import fr.paulbr.nookmind.resources.movieHome_noMoviesFound
import fr.paulbr.nookmind.resources.movieHome_searchPlaceholder
import fr.paulbr.nookmind.resources.movieHome_searchTimeout
import fr.paulbr.nookmind.resources.movieHome_subtitle
import fr.paulbr.nookmind.resources.movieHome_title
import fr.paulbr.nookmind.resources.movieHome_trendingTitle
import fr.paulbr.nookmind.resources.movieHome_wantToWatch
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of MovieHome.tsx: TMDB search, Discover slider, "Want to watch" and "Last watched" sliders. */
@Composable
fun MoviesHomeScreen(container: AppContainer, contentPadding: PaddingValues) {
    val colors = NookTheme.colors
    val movies by container.movies.items.collectAsState()
    val sectionPrefs by container.prefs.searchSections(MediaMode.MOVIES).collectAsState()
    val scope = rememberCoroutineScope()
    val search = rememberSearchController<TmdbMovie>(timeoutText = stringResource(Res.string.movieHome_searchTimeout)) {
        container.tmdb.searchMovies(it)
    }
    var prefill by remember { mutableStateOf<Movie?>(null) }
    var selected by remember { mutableStateOf<Movie?>(null) }
    var selectingId by remember { mutableStateOf<Int?>(null) }
    val inWatchlist = stringResource(Res.string.movieHome_inWatchlist)
    val visibility = sectionPrefs.associate { it.id to it.visible }
    val trackedIds = movies.mapNotNull { it.tmdbId }.toSet()

    fun selectTmdbMovie(movie: TmdbMovie) {
        if (selectingId != null) return
        selectingId = movie.id
        scope.launch {
            val details = container.tmdb.fetchMovieDetails(movie.id)
            selectingId = null
            prefill = TmdbApi.extractMovieData(details ?: movie)
            search.close()
        }
    }

    val trendingCategories = listOf(
        TrendingCategory("trending", stringResource(Res.string.movieHome_categoryTrending)) { container.tmdb.fetchTrendingMoviesPaged(it) },
        TrendingCategory("now_playing", stringResource(Res.string.movieHome_categoryNowPlaying)) { container.tmdb.fetchNowPlayingMoviesPaged(it) },
        TrendingCategory("top_rated", stringResource(Res.string.movieHome_categoryTopRated)) { container.tmdb.fetchTopRatedMoviesPaged(it) },
    )

    val sections = SearchSections.orderSections(
        listOf(
            HomeSection("trending", visibility["trending"] != false) {
                TrendingSlider(
                    title = stringResource(Res.string.movieHome_trendingTitle),
                    categories = trendingCategories,
                    trackedIds = trackedIds,
                    idOf = { it.id },
                    titleOf = { it.title },
                    posterOf = { TmdbApi.posterUrl(it.posterPath) },
                    onSelect = ::selectTmdbMovie,
                    mode = MediaMode.MOVIES,
                )
            },
            HomeSection("want_to_watch", visibility["want_to_watch"] != false) {
                val list = movies.filter { it.status == MovieStatus.WANT_TO_WATCH }.take(10)
                if (list.isNotEmpty()) {
                    PosterSlider(stringResource(Res.string.movieHome_wantToWatch), icon = LucideIcons.Bookmark, titleColor = if (colors.isDark) Palette.Amber400 else Palette.Amber500) {
                        items(list, key = { it.id }) { m -> PosterSlideCard(m.posterUrl, m.title, onClick = { selected = m }, mode = MediaMode.MOVIES) }
                    }
                }
            },
            HomeSection("last_watched", visibility["last_watched"] != false) {
                val list = movies.filter { it.status == MovieStatus.WATCHED }.take(10)
                if (list.isNotEmpty()) {
                    PosterSlider(stringResource(Res.string.movieHome_lastWatched), icon = LucideIcons.CheckCheck, titleColor = if (colors.isDark) Palette.Emerald400 else Palette.Emerald500) {
                        items(list, key = { it.id }) { m -> PosterSlideCard(m.posterUrl, m.title, onClick = { selected = m }, rating = m.rating, mode = MediaMode.MOVIES) }
                    }
                }
            },
        ),
        sectionPrefs,
    ) { it.id }

    HomeScreenScaffold(
        contentPadding = contentPadding,
        title = stringResource(Res.string.movieHome_title),
        subtitle = stringResource(Res.string.movieHome_subtitle),
        titleColor = colors.indigoText,
        search = search,
        searchPlaceholder = stringResource(Res.string.movieHome_searchPlaceholder),
        noResultsText = stringResource(Res.string.movieHome_noMoviesFound, search.query),
        sections = sections,
        resultKey = { it.id },
    ) { movie ->
        val alreadyAdded = movies.any { normalizeTitle(it.title) == normalizeTitle(movie.title) }
        SearchResultRow(
            imageUrl = TmdbApi.posterUrl(movie.posterPath),
            title = movie.title,
            subtitle = yearOf(movie.releaseDate) ?: "—",
            onClick = { selectTmdbMovie(movie) },
            enabled = selectingId == null,
            placeholderIcon = LucideIcons.Film,
            trailing = when {
                selectingId == movie.id -> ({ Spinner(size = 16.dp, color = colors.textMuted, trackColor = colors.borderNeutral) })
                alreadyAdded -> ({ AlreadyAddedPill(inWatchlist) })
                else -> null
            },
        )
    }

    prefill?.let { AddMovieSheet(container, it, onClose = { prefill = null }) }
    selected?.let { MovieDetailSheet(container, it, onClose = { selected = null }) }
}
