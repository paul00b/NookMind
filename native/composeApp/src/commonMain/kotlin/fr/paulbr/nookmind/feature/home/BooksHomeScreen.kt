package fr.paulbr.nookmind.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.domain.SearchSections
import fr.paulbr.nookmind.core.domain.normalizeTitle
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.GoogleBookVolume
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.network.GoogleBooksApi
import fr.paulbr.nookmind.feature.books.AddBookSheet
import fr.paulbr.nookmind.feature.books.BookDetailSheet
import fr.paulbr.nookmind.feature.common.AlreadyAddedPill
import fr.paulbr.nookmind.feature.common.HomeScreenScaffold
import fr.paulbr.nookmind.feature.common.HomeSection
import fr.paulbr.nookmind.feature.common.PosterSlideCard
import fr.paulbr.nookmind.feature.common.PosterSlider
import fr.paulbr.nookmind.feature.common.SearchResultRow
import fr.paulbr.nookmind.feature.common.rememberSearchController
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.addBook_inLibrary
import fr.paulbr.nookmind.resources.home_lastRead
import fr.paulbr.nookmind.resources.home_noBooksFound
import fr.paulbr.nookmind.resources.home_searchPlaceholder
import fr.paulbr.nookmind.resources.home_searchTimeout
import fr.paulbr.nookmind.resources.home_subtitle
import fr.paulbr.nookmind.resources.home_title
import fr.paulbr.nookmind.resources.home_unknownAuthor
import fr.paulbr.nookmind.resources.home_wantToRead
import org.jetbrains.compose.resources.stringResource

/** Port of Home.tsx: Google Books search, "Want to read" and "Last read" sliders. */
@Composable
fun BooksHomeScreen(container: AppContainer, contentPadding: PaddingValues) {
    val books by container.books.items.collectAsState()
    val sectionPrefs by container.prefs.searchSections(MediaMode.BOOKS).collectAsState()
    val search = rememberSearchController<GoogleBookVolume>(timeoutText = stringResource(Res.string.home_searchTimeout)) {
        container.googleBooks.searchBooks(it)
    }
    var prefill by remember { mutableStateOf<Book?>(null) }
    var selected by remember { mutableStateOf<Book?>(null) }
    val unknownAuthor = stringResource(Res.string.home_unknownAuthor)
    val inLibrary = stringResource(Res.string.addBook_inLibrary)
    val visibility = sectionPrefs.associate { it.id to it.visible }

    val sections = SearchSections.orderSections(
        listOf(
            HomeSection("want_to_read", visibility["want_to_read"] != false) {
                WantToReadSlider(books.filter { it.status == BookStatus.WANT_TO_READ }.take(10)) { selected = it }
            },
            HomeSection("last_read", visibility["last_read"] != false) {
                LastReadSlider(books.filter { it.status == BookStatus.READ }.take(10)) { selected = it }
            },
        ),
        sectionPrefs,
    ) { it.id }

    HomeScreenScaffold(
        contentPadding = contentPadding,
        title = stringResource(Res.string.home_title),
        subtitle = stringResource(Res.string.home_subtitle),
        titleColor = NookTheme.colors.amberText,
        search = search,
        searchPlaceholder = stringResource(Res.string.home_searchPlaceholder),
        noResultsText = stringResource(Res.string.home_noBooksFound, search.query),
        sections = sections,
        resultKey = { it.id },
    ) { volume ->
        val info = volume.volumeInfo
        val firstAuthor = info.authors?.firstOrNull() ?: ""
        val alreadyAdded = books.any { normalizeTitle(it.title) == normalizeTitle(info.title) && normalizeTitle(it.author) == normalizeTitle(firstAuthor) }
        val authors = info.authors?.joinToString(", ")?.ifBlank { null } ?: unknownAuthor
        SearchResultRow(
            imageUrl = GoogleBooksApi.secureThumbnail(info.imageLinks?.smallThumbnail),
            title = info.title,
            subtitle = yearOf(info.publishedDate)?.let { "$authors · $it" } ?: authors,
            onClick = {
                prefill = GoogleBooksApi.extractBookData(volume)
                search.close()
            },
            trailing = if (alreadyAdded) { { AlreadyAddedPill(inLibrary) } } else null,
        )
    }

    prefill?.let { AddBookSheet(container, it, onClose = { prefill = null }) }
    selected?.let { BookDetailSheet(container, it, onClose = { selected = null }) }
}

@Composable
private fun WantToReadSlider(books: List<Book>, onSelect: (Book) -> Unit) {
    if (books.isEmpty()) return
    PosterSlider(stringResource(Res.string.home_wantToRead)) {
        items(books, key = { it.id }) { book ->
            PosterSlideCard(book.coverUrl, book.title, onClick = { onSelect(book) }, mode = MediaMode.BOOKS)
        }
    }
}

@Composable
private fun LastReadSlider(books: List<Book>, onSelect: (Book) -> Unit) {
    if (books.isEmpty()) return
    PosterSlider(stringResource(Res.string.home_lastRead)) {
        items(books, key = { it.id }) { book ->
            PosterSlideCard(book.coverUrl, book.title, onClick = { onSelect(book) }, rating = book.rating, mode = MediaMode.BOOKS)
        }
    }
}
