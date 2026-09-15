package fr.paulbr.nookmind.feature.nextup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.patchOf
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.TextLink
import fr.paulbr.nookmind.core.designsystem.components.dashedBorder
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.ProgressBar
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.discover_title
import fr.paulbr.nookmind.resources.nextUp_markAsRead
import fr.paulbr.nookmind.resources.nextUp_nextToRead
import fr.paulbr.nookmind.resources.nextUp_noReading
import fr.paulbr.nookmind.resources.nextUp_pageOf
import fr.paulbr.nookmind.resources.nextUp_pageOnly
import fr.paulbr.nookmind.resources.nextUp_startReading
import fr.paulbr.nookmind.resources.nextUp_updatePage
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of NextUpBooks.tsx: books in progress with page tracking, or the next book to start. */
@Composable
fun NextUpBooksScreen(container: AppContainer, contentPadding: PaddingValues) {
    val books by container.books.items.collectAsState()
    val reading = books.filter { it.status == BookStatus.READING }
    val nextToRead = books.firstOrNull { it.status == BookStatus.WANT_TO_READ }
    val wide = LocalWideLayout.current
    val pad = if (wide) 32.dp else 16.dp

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 672.dp).padding(pad)) {
            if (reading.isEmpty() && nextToRead == null) {
                Column(Modifier.fillMaxWidth().heightIn(min = 256.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(LucideIcons.BookOpen, null, Modifier.size(40.dp), tint = NookTheme.colors.textDisabled)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(Res.string.nextUp_noReading), style = NookTheme.type.sm, color = NookTheme.colors.textSubtle, textAlign = TextAlign.Center)
                }
            } else {
                Text(stringResource(Res.string.discover_title), style = NookTheme.type.h2Serif, color = NookTheme.colors.amberText)
                Spacer(Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    reading.forEach { book -> ReadingCard(container, book) }
                    if (reading.isEmpty() && nextToRead != null) NextToReadCard(container, nextToRead)
                }
            }
        }
    }
}

@Composable
private fun ReadingCard(container: AppContainer, book: Book) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    var editingPage by remember { mutableStateOf(false) }
    var pageInput by remember(book.id) { mutableStateOf(book.currentPage?.toString() ?: "") }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    val progress = if (book.currentPage != null && book.pageCount != null && book.pageCount > 0) (book.currentPage.toFloat() / book.pageCount).coerceAtMost(1f) else null
    val markAsReadText = stringResource(Res.string.nextUp_markAsRead)

    fun savePage() {
        val page = pageInput.toIntOrNull()
        editingPage = false
        hadFocus = false
        scope.launch { container.books.update(book.id, patchOf("current_page" to page)) }
    }

    LaunchedEffect(editingPage) { if (editingPage) runCatching { focusRequester.requestFocus() } }

    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            MediaImage(book.coverUrl, book.title, Modifier.width(48.dp), mode = MediaMode.BOOKS, shape = fr.paulbr.nookmind.core.designsystem.NookShapes.lg, placeholderIconSize = 20.dp)
            Column(Modifier.weight(1f)) {
                Text(book.title, style = NookTheme.type.sans(16, FontWeight.SemiBold, 24), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author, style = NookTheme.type.sm, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (progress != null) {
                    Spacer(Modifier.height(8.dp))
                    ProgressBar(progress)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingPage) {
                        NookTextField(
                            pageInput,
                            { if (it.isEmpty() || it.all(Char::isDigit)) pageInput = it },
                            modifier = Modifier.width(80.dp).onFocusChanged {
                                if (it.hasFocus) hadFocus = true else if (hadFocus) savePage()
                            },
                            textStyle = NookTheme.type.sm,
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                            keyboardActions = KeyboardActions(onDone = { savePage() }),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            focusRequester = focusRequester,
                        )
                        book.pageCount?.let { Text("/ $it", style = NookTheme.type.xs, color = colors.textFaint) }
                    } else {
                        val current = book.currentPage
                        val label = when {
                            current == null -> stringResource(Res.string.nextUp_updatePage)
                            book.pageCount != null -> stringResource(Res.string.nextUp_pageOf, current, book.pageCount)
                            else -> stringResource(Res.string.nextUp_pageOnly, current)
                        }
                        TextLink(label, onClick = { pageInput = book.currentPage?.toString() ?: ""; editingPage = true }, color = colors.textSubtle, modifier = Modifier)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(LucideIcons.Check, null, Modifier.size(12.dp), tint = colors.emeraldText)
                    TextLink(
                        markAsReadText,
                        onClick = {
                            scope.launch {
                                val stored = container.books.update(book.id, patchOf("status" to BookStatus.READ.key, "current_page" to null))
                                if (stored != null) container.toasts.success(markAsReadText)
                            }
                        },
                        color = colors.emeraldText,
                    )
                }
            }
        }
    }
}

@Composable
private fun NextToReadCard(container: AppContainer, book: Book) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    NookCard(
        Modifier.fillMaxWidth().alpha(0.8f).dashedBorder(colors.border),
        borderColor = Color.Transparent,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
            MediaImage(book.coverUrl, book.title, Modifier.width(48.dp), mode = MediaMode.BOOKS, shape = fr.paulbr.nookmind.core.designsystem.NookShapes.lg, placeholderIconSize = 20.dp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.nextUp_nextToRead), style = NookTheme.type.xs, color = colors.textFaint)
                Spacer(Modifier.height(2.dp))
                Text(book.title, style = NookTheme.type.sans(16, FontWeight.SemiBold, 24), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.author, style = NookTheme.type.sm, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    stringResource(Res.string.nextUp_startReading),
                    onClick = { scope.launch { container.books.update(book.id, patchOf("status" to BookStatus.READING.key)) } },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    textStyle = NookTheme.type.sans(12, FontWeight.Medium, 16),
                )
            }
        }
    }
}
