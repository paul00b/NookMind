package fr.paulbr.nookmind.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.components.StatusBadge
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.ProgressBar
import fr.paulbr.nookmind.feature.library.CardRemoveButton
import fr.paulbr.nookmind.feature.library.LibraryListRow
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.addBook_status_read
import fr.paulbr.nookmind.resources.addBook_status_reading
import fr.paulbr.nookmind.resources.addBook_status_want_to_read
import fr.paulbr.nookmind.resources.bookCard_read
import fr.paulbr.nookmind.resources.bookCard_wantToRead
import fr.paulbr.nookmind.resources.bookDetail_noPageYet
import fr.paulbr.nookmind.resources.bookDetail_pages
import fr.paulbr.nookmind.resources.bookDetail_save
import fr.paulbr.nookmind.resources.common_pageOf
import fr.paulbr.nookmind.resources.common_pageOnly
import fr.paulbr.nookmind.resources.library_reading
import fr.paulbr.nookmind.resources.library_updatePage
import org.jetbrains.compose.resources.stringResource

/** Badge colour of a book status (emerald read, blue reading, amber to read). */
fun bookStatusColor(status: BookStatus): Color = when (status) {
    BookStatus.READ -> Palette.Emerald500
    BookStatus.READING -> Palette.Blue600
    BookStatus.WANT_TO_READ -> Palette.Amber500
}

/** Badge label of a book status on cards and rows. */
@Composable
fun bookStatusLabel(status: BookStatus): String = when (status) {
    BookStatus.READ -> stringResource(Res.string.bookCard_read)
    BookStatus.READING -> stringResource(Res.string.library_reading)
    BookStatus.WANT_TO_READ -> stringResource(Res.string.bookCard_wantToRead)
}

/** Labels of the status selector (`addBook.status_*`). */
@Composable
fun bookStatusOptions(): List<Pair<String, String>> = listOf(
    BookStatus.WANT_TO_READ.key to stringResource(Res.string.addBook_status_want_to_read),
    BookStatus.READING.key to stringResource(Res.string.addBook_status_reading),
    BookStatus.READ.key to stringResource(Res.string.addBook_status_read),
)

/**
 * Port of BookCard.tsx: cover, serif title on two lines (minimum two, so a grid row lines up), author,
 * then what matters for the status: stars once read, progress while reading, year and length before.
 * [showStatus] puts the status badge on the cover, for mixed lists (a collection, a search); in a
 * status tab the tab already says it.
 */
@Composable
fun BookCard(book: Book, onClick: () -> Unit, modifier: Modifier = Modifier, onRemove: (() -> Unit)? = null, showStatus: Boolean = true) {
    NookCard(modifier, onClick = onClick) {
        Box {
            MediaImage(book.coverUrl, book.title, Modifier.fillMaxWidth(), mode = MediaMode.BOOKS) {
                if (showStatus) StatusBadge(bookStatusLabel(book.status), bookStatusColor(book.status), Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            if (onRemove != null) CardRemoveButton(onRemove, Modifier.align(Alignment.TopStart))
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(book.title, style = NookTheme.type.cardTitleSerif, color = NookTheme.colors.textStrong, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(book.author, style = NookTheme.type.xs, color = NookTheme.colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.CenterStart) { BookCardFooter(book) }
        }
    }
}

/** Last line of a book card: stars, progress or "year · pages", whichever the status calls for. */
@Composable
private fun BookCardFooter(book: Book) {
    val current = book.currentPage
    val total = book.pageCount
    when {
        book.status == BookStatus.READ && book.rating != null -> StarRating(book.rating, size = 13.dp)
        book.status == BookStatus.READING && current != null && total != null && total > 0 -> ProgressBar(current.toFloat() / total, color = Palette.Blue600, height = 4.dp)
        else -> bookMeta(book)?.let { Text(it, style = NookTheme.type.xs, color = NookTheme.colors.textSubtle, maxLines = 1) }
    }
}

/** "1965 · 412 pages", or whichever half is known; null with neither. */
@Composable
fun bookMeta(book: Book): String? {
    val pages = book.pageCount?.let { stringResource(Res.string.bookDetail_pages, it) }
    return listOfNotNull(yearOf(book.publishedDate), pages).joinToString(" · ").ifBlank { null }
}

/**
 * Wide card of the "Reading" tab: one rarely reads more than two or three books at once, so each
 * gets its progress and a way to move the page on without opening the book.
 */
@Composable
fun BookReadingCard(book: Book, onClick: () -> Unit, onSavePage: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    var editing by remember(book.id) { mutableStateOf(false) }
    var pageInput by remember(book.id) { mutableStateOf("") }
    val current = book.currentPage
    val total = book.pageCount
    NookCard(modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MediaImage(book.coverUrl, book.title, Modifier.width(84.dp), mode = MediaMode.BOOKS, shape = NookShapes.lg, placeholderIconSize = 24.dp)
            Column(Modifier.weight(1f)) {
                Text(book.title, style = NookTheme.type.serif(18), color = colors.textStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(book.author, style = NookTheme.type.xs, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (current != null && total != null && total > 0) {
                    Spacer(Modifier.height(12.dp))
                    ProgressBar(current.toFloat() / total, color = Palette.Blue600)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        current == null -> stringResource(Res.string.bookDetail_noPageYet)
                        total != null && total > 0 -> stringResource(Res.string.common_pageOf, current, total) + " · ${(current * 100 / total).coerceIn(0, 100)} %"
                        else -> stringResource(Res.string.common_pageOnly, current)
                    },
                    style = NookTheme.type.xs,
                    color = colors.textSubtle,
                )
                Spacer(Modifier.height(10.dp))
                if (editing) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NookTextField(
                            pageInput,
                            { if (it.isEmpty() || it.all(Char::isDigit)) pageInput = it },
                            modifier = Modifier.width(88.dp),
                            textStyle = NookTheme.type.sm,
                            keyboardType = KeyboardType.Number,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        )
                        PrimaryButton(
                            stringResource(Res.string.bookDetail_save),
                            onClick = { onSavePage(pageInput.toIntOrNull()); editing = false },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                } else {
                    GhostButton(
                        stringResource(Res.string.library_updatePage),
                        onClick = { pageInput = current?.toString() ?: ""; editing = true },
                        borderColor = colors.borderStrong,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        textStyle = NookTheme.type.sans(13, FontWeight.Medium, 18),
                    )
                }
            }
        }
    }
}

/** Port of BookListRow (Library.tsx). [showStatus] as on [BookCard]; without it a read book shows its stars. */
@Composable
fun BookListRow(book: Book, onClick: () -> Unit, onRemove: (() -> Unit)? = null, showStatus: Boolean = true) {
    val wide = LocalWideLayout.current
    LibraryListRow(book.coverUrl, book.title, book.author, onClick, mode = MediaMode.BOOKS, onRemove = onRemove) {
        if (wide && !book.genre.isNullOrBlank()) GenrePill(book.genre, small = true)
        if ((wide || !showStatus) && book.status == BookStatus.READ && book.rating != null) StarRating(book.rating, size = 12.dp)
        if (showStatus) SolidPill(bookStatusLabel(book.status), bookStatusColor(book.status), small = true)
    }
}
