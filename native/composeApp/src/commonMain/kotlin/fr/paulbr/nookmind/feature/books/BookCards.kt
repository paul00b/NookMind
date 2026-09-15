package fr.paulbr.nookmind.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.components.StatusBadge
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.library.CardRemoveButton
import fr.paulbr.nookmind.feature.library.LibraryListRow
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.addBook_status_read
import fr.paulbr.nookmind.resources.addBook_status_reading
import fr.paulbr.nookmind.resources.addBook_status_want_to_read
import fr.paulbr.nookmind.resources.bookCard_read
import fr.paulbr.nookmind.resources.bookCard_wantToRead
import fr.paulbr.nookmind.resources.library_reading
import org.jetbrains.compose.resources.stringResource

/** Badge colour of a book status (emerald read, blue reading, amber to read). */
fun bookStatusColor(status: BookStatus): Color = when (status) {
    BookStatus.READ -> Palette.Emerald500
    BookStatus.READING -> Palette.Blue500
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

/** Port of BookCard.tsx: cover with status badge, serif title, author, stars when read. */
@Composable
fun BookCard(book: Book, onClick: () -> Unit, modifier: Modifier = Modifier, onRemove: (() -> Unit)? = null) {
    NookCard(modifier, onClick = onClick) {
        Box {
            MediaImage(book.coverUrl, book.title, Modifier.fillMaxWidth(), mode = MediaMode.BOOKS) {
                StatusBadge(bookStatusLabel(book.status), bookStatusColor(book.status), Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
            if (onRemove != null) CardRemoveButton(onRemove, Modifier.align(Alignment.TopStart))
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(book.title, style = NookTheme.type.cardTitleSerif, color = NookTheme.colors.textStrong, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(book.author, style = NookTheme.type.xs, color = NookTheme.colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (book.status == BookStatus.READ && book.rating != null) StarRating(book.rating, size = 13.dp)
        }
    }
}

/** Port of BookListRow (Library.tsx). */
@Composable
fun BookListRow(book: Book, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    val wide = LocalWideLayout.current
    LibraryListRow(book.coverUrl, book.title, book.author, onClick, mode = MediaMode.BOOKS, onRemove = onRemove) {
        if (wide && !book.genre.isNullOrBlank()) GenrePill(book.genre, small = true)
        if (wide && book.status == BookStatus.READ && book.rating != null) StarRating(book.rating, size = 12.dp)
        SolidPill(bookStatusLabel(book.status), bookStatusColor(book.status), small = true)
    }
}
