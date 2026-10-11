package fr.paulbr.nookmind.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.domain.ShareLinks
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.network.GoogleBooksApi
import fr.paulbr.nookmind.core.network.TmdbApi
import fr.paulbr.nookmind.core.platform.deviceLanguageTag
import fr.paulbr.nookmind.core.platform.shareText
import fr.paulbr.nookmind.feature.books.AddBookSheet
import fr.paulbr.nookmind.feature.books.BookDetailSheet
import fr.paulbr.nookmind.feature.movies.AddMovieSheet
import fr.paulbr.nookmind.feature.movies.MovieDetailSheet
import fr.paulbr.nookmind.feature.series.AddSeriesSheet
import fr.paulbr.nookmind.feature.series.SeriesDetailSheet
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_share
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Opens a movie, a series or a book known only by its kind and key (a friend's item, a shared
 * link): my own sheet when it is in my library, else the add sheet filled from TMDB or Google
 * Books. [ItemSheetsHost] draws whichever is open.
 */
@Stable
class ItemSheets(private val container: AppContainer, private val scope: CoroutineScope) {
    /** "kind:key" of the item being fetched, for a spinner on it. */
    var opening by mutableStateOf<String?>(null)
        private set
    internal var moviePrefill by mutableStateOf<Movie?>(null)
    internal var seriesPrefill by mutableStateOf<Series?>(null)
    internal var bookPrefill by mutableStateOf<Book?>(null)
    internal var movieSelected by mutableStateOf<Movie?>(null)
    internal var seriesSelected by mutableStateOf<Series?>(null)
    internal var bookSelected by mutableStateOf<Book?>(null)

    fun isOpening(kind: String, key: String): Boolean = opening == "$kind:$key"

    /**
     * [title], [subtitle] and [imageUrl] stand in when the details cannot be fetched; without a
     * [title] (a link), [onMissing] is called instead.
     */
    fun open(kind: String, key: String, title: String = "", subtitle: String? = null, imageUrl: String? = null, onMissing: () -> Unit = {}) {
        if (opening != null) return
        when (kind) {
            "movie" -> container.movies.items.value.firstOrNull { it.tmdbId?.toString() == key }?.let { movieSelected = it; return }
            "series" -> container.series.items.value.firstOrNull { it.tmdbId?.toString() == key }?.let { seriesSelected = it; return }
            "book" -> container.books.items.value.firstOrNull { it.googleBooksId == key }?.let { bookSelected = it; return }
            else -> return
        }
        opening = "$kind:$key"
        scope.launch {
            val found = when (kind) {
                "movie" -> {
                    val id = key.toIntOrNull()
                    val fetched = id?.let { container.tmdb.fetchMovieDetails(it) }?.let { TmdbApi.extractMovieData(it) }
                    (fetched ?: if (title.isNotBlank()) Movie(tmdbId = id, title = title, director = subtitle.orEmpty(), posterUrl = imageUrl) else null)
                        ?.also { moviePrefill = it } != null
                }
                "series" -> {
                    val id = key.toIntOrNull()
                    val fetched = id?.let { container.tmdb.fetchSeriesDetails(it) }?.let { TmdbApi.extractSeriesData(it) }
                    (fetched ?: if (title.isNotBlank()) Series(tmdbId = id, title = title, creator = subtitle.orEmpty(), posterUrl = imageUrl) else null)
                        ?.also { seriesPrefill = it } != null
                }
                else -> {
                    val fetched = container.googleBooks.fetchVolume(key)?.let { GoogleBooksApi.extractBookData(it) }
                    (fetched ?: if (title.isNotBlank()) Book(googleBooksId = key, title = title, author = subtitle.orEmpty(), coverUrl = imageUrl) else null)
                        ?.also { bookPrefill = it } != null
                }
            }
            if (!found) onMissing()
            opening = null
        }
    }
}

@Composable
fun rememberItemSheets(container: AppContainer): ItemSheets {
    val scope = rememberCoroutineScope()
    return remember { ItemSheets(container, scope) }
}

/** The sheet [sheets] opened, in the colours of its own kind whatever the mode on screen. */
@Composable
fun ItemSheetsHost(container: AppContainer, sheets: ItemSheets) {
    val themeMode by container.prefs.theme.collectAsState()
    sheets.moviePrefill?.let { NookTheme(themeMode, MediaMode.MOVIES) { AddMovieSheet(container, it, onClose = { sheets.moviePrefill = null }) } }
    sheets.seriesPrefill?.let { NookTheme(themeMode, MediaMode.SERIES) { AddSeriesSheet(container, it, onClose = { sheets.seriesPrefill = null }) } }
    sheets.bookPrefill?.let { NookTheme(themeMode, MediaMode.BOOKS) { AddBookSheet(container, it, onClose = { sheets.bookPrefill = null }) } }
    sheets.movieSelected?.let { NookTheme(themeMode, MediaMode.MOVIES) { MovieDetailSheet(container, it, onClose = { sheets.movieSelected = null }) } }
    sheets.seriesSelected?.let { NookTheme(themeMode, MediaMode.SERIES) { SeriesDetailSheet(container, it, onClose = { sheets.seriesSelected = null }) } }
    sheets.bookSelected?.let { NookTheme(themeMode, MediaMode.BOOKS) { BookDetailSheet(container, it, onClose = { sheets.bookSelected = null }) } }
}

/** "Share" on a sheet: a link with the poster that anyone can open, to add the item to NookMind. */
@Composable
fun ShareItemButton(container: AppContainer, kind: String, key: String?) {
    if (key.isNullOrBlank()) return
    val sharer = container.auth.currentUser?.displayName("")
    val link = ShareLinks.linkFor(AppConfig.apiBaseUrl, kind, key, sharer, deviceLanguageTag()) ?: return
    IconGhostButton(LucideIcons.Share, stringResource(Res.string.common_share), onClick = { shareText(link) }, size = 19.dp)
}
