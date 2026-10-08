package fr.paulbr.nookmind.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.EditableNote
import fr.paulbr.nookmind.core.designsystem.components.ExpandableDescription
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.LabeledBlock
import fr.paulbr.nookmind.core.designsystem.components.LabeledField
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.MetaPill
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextArea
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SheetController
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.normalizeTitle
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.common.SheetHeader
import fr.paulbr.nookmind.feature.common.StatusChipRow
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.addBook_addAnyway
import fr.paulbr.nookmind.resources.addBook_addToLibrary
import fr.paulbr.nookmind.resources.addBook_alreadyInLibrary
import fr.paulbr.nookmind.resources.addBook_authorLabel
import fr.paulbr.nookmind.resources.addBook_authorPlaceholder
import fr.paulbr.nookmind.resources.addBook_cancel
import fr.paulbr.nookmind.resources.addBook_coverUrlLabel
import fr.paulbr.nookmind.resources.addBook_coverUrlPlaceholder
import fr.paulbr.nookmind.resources.addBook_currentPageLabel
import fr.paulbr.nookmind.resources.addBook_currentPagePlaceholder
import fr.paulbr.nookmind.resources.addBook_descriptionLabel
import fr.paulbr.nookmind.resources.addBook_edit
import fr.paulbr.nookmind.resources.addBook_genreLabel
import fr.paulbr.nookmind.resources.addBook_genrePlaceholder
import fr.paulbr.nookmind.resources.addBook_noteLabel
import fr.paulbr.nookmind.resources.addBook_notePlaceholder
import fr.paulbr.nookmind.resources.addBook_pagesLabel
import fr.paulbr.nookmind.resources.addBook_pagesPlaceholder
import fr.paulbr.nookmind.resources.addBook_publishedLabel
import fr.paulbr.nookmind.resources.addBook_publishedPlaceholder
import fr.paulbr.nookmind.resources.addBook_ratingLabel
import fr.paulbr.nookmind.resources.addBook_saving
import fr.paulbr.nookmind.resources.addBook_statusLabel
import fr.paulbr.nookmind.resources.addBook_title
import fr.paulbr.nookmind.resources.addBook_titleLabel
import fr.paulbr.nookmind.resources.addBook_titlePlaceholder
import fr.paulbr.nookmind.resources.bookDetail_cancel
import fr.paulbr.nookmind.resources.bookDetail_currentPage
import fr.paulbr.nookmind.resources.bookDetail_description
import fr.paulbr.nookmind.resources.bookDetail_noNotes
import fr.paulbr.nookmind.resources.bookDetail_notePlaceholder
import fr.paulbr.nookmind.resources.bookDetail_pages
import fr.paulbr.nookmind.resources.bookDetail_personalNote
import fr.paulbr.nookmind.resources.bookDetail_read
import fr.paulbr.nookmind.resources.bookDetail_save
import fr.paulbr.nookmind.resources.bookDetail_seeLess
import fr.paulbr.nookmind.resources.bookDetail_seeMore
import fr.paulbr.nookmind.resources.bookDetail_wantToRead
import fr.paulbr.nookmind.resources.bookDetail_yourRating
import fr.paulbr.nookmind.resources.library_reading
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Port of AddBookModal.tsx. [prefill] comes from a Google Books result; null means a manual add.
 *
 * A search result opens as a preview laid out like [BookDetailSheet] (cover, pills, status, rating,
 * description, note), so adding a book reads like looking at one already in the library. "Edit"
 * switches to the full form, for when Google Books got the title, genre or cover wrong.
 */
@Composable
fun AddBookSheet(container: AppContainer, prefill: Book?, onClose: () -> Unit) {
    val books by container.books.items.collectAsState()
    val scope = rememberCoroutineScope()
    var form by remember { mutableStateOf(prefill ?: Book(title = "")) }
    var saving by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val fromSearch = prefill != null
    val isDuplicate = form.title.isNotBlank() && form.author.isNotBlank() && books.any {
        normalizeTitle(it.title) == normalizeTitle(form.title) && normalizeTitle(it.author) == normalizeTitle(form.author)
    }

    fun digitsOnly(value: String) = value.isEmpty() || value.all(Char::isDigit)

    fun selectStatus(key: String) {
        val status = BookStatus.fromKey(key)
        form = form.copy(
            status = status,
            rating = if (status == BookStatus.READ) form.rating else null,
            currentPage = if (status == BookStatus.READING) form.currentPage else null,
        )
    }

    val addButton: @Composable (SheetController, Modifier) -> Unit = { controller, modifier ->
        PrimaryButton(
            text = when {
                saving -> stringResource(Res.string.addBook_saving)
                isDuplicate -> stringResource(Res.string.addBook_addAnyway)
                else -> stringResource(Res.string.addBook_addToLibrary)
            },
            onClick = {
                if (form.title.isBlank() || saving) return@PrimaryButton
                saving = true
                scope.launch {
                    val stored = container.books.add(form)
                    saving = false
                    if (stored != null) controller.close()
                }
            },
            modifier = modifier,
            enabled = form.title.isNotBlank(),
            loading = saving,
            icon = LucideIcons.BookOpen,
        )
    }
    val duplicateWarning: @Composable () -> Unit = {
        if (isDuplicate) InlineBanner(stringResource(Res.string.addBook_alreadyInLibrary), tone = BannerTone.WARNING, icon = LucideIcons.AlertTriangle)
    }

    if (fromSearch && !editing) {
        NookSheet(onClose = onClose, maxWidth = 672.dp) { controller ->
            val colors = NookTheme.colors
            Box(Modifier.fillMaxWidth()) {
                BoxWithConstraints(Modifier.fillMaxWidth().padding(24.dp)) {
                    val sideBySide = maxWidth >= 560.dp
                    val cover: @Composable () -> Unit = {
                        MediaImage(form.coverUrl, form.title, Modifier.width(if (sideBySide) 160.dp else 128.dp), mode = MediaMode.BOOKS)
                    }
                    val details: @Composable () -> Unit = {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text(form.title, style = NookTheme.type.h2Serif.copy(lineHeight = 28.sp()), color = colors.textStrong, modifier = Modifier.padding(end = 32.dp))
                                if (form.author.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(form.author, style = NookTheme.type.sans(16, FontWeight.Medium, 24), color = colors.textMuted)
                                }
                            }

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                form.genre?.takeIf { it.isNotBlank() }?.let { GenrePill(it) }
                                SolidPill(
                                    when (form.status) {
                                        BookStatus.READ -> stringResource(Res.string.bookDetail_read)
                                        BookStatus.READING -> stringResource(Res.string.library_reading)
                                        BookStatus.WANT_TO_READ -> stringResource(Res.string.bookDetail_wantToRead)
                                    },
                                    bookStatusColor(form.status),
                                )
                                yearOf(form.publishedDate)?.let { MetaPill(it) }
                                form.pageCount?.let { MetaPill(stringResource(Res.string.bookDetail_pages, it)) }
                            }

                            StatusChipRow(options = bookStatusOptions(), selected = form.status.key, compact = false, onSelect = ::selectStatus)

                            if (form.status == BookStatus.READING) {
                                LabeledBlock(stringResource(Res.string.bookDetail_currentPage)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        NookTextField(
                                            form.currentPage?.toString() ?: "",
                                            { if (digitsOnly(it)) form = form.copy(currentPage = it.toIntOrNull()) },
                                            modifier = Modifier.width(112.dp),
                                            placeholder = stringResource(Res.string.addBook_currentPagePlaceholder),
                                            textStyle = NookTheme.type.sm,
                                            keyboardType = KeyboardType.Number,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        )
                                        form.pageCount?.let { Text("/ $it", style = NookTheme.type.sm, color = colors.textFaint) }
                                    }
                                }
                            }

                            if (form.status == BookStatus.READ) {
                                LabeledBlock(stringResource(Res.string.bookDetail_yourRating)) {
                                    StarRating(form.rating, onChange = { form = form.copy(rating = it) }, size = 26.dp)
                                }
                            }

                            form.description?.takeIf { it.isNotBlank() }?.let {
                                ExpandableDescription(
                                    description = it,
                                    label = stringResource(Res.string.bookDetail_description),
                                    seeMoreText = stringResource(Res.string.bookDetail_seeMore),
                                    seeLessText = stringResource(Res.string.bookDetail_seeLess),
                                )
                            }

                            EditableNote(
                                note = form.personalNote,
                                labelText = stringResource(Res.string.bookDetail_personalNote),
                                placeholderText = stringResource(Res.string.bookDetail_notePlaceholder),
                                saveText = stringResource(Res.string.bookDetail_save),
                                cancelText = stringResource(Res.string.bookDetail_cancel),
                                noNotesText = stringResource(Res.string.bookDetail_noNotes),
                                onSave = { form = form.copy(personalNote = it.ifBlank { null }) },
                            )

                            duplicateWarning()

                            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                GhostButton(stringResource(Res.string.addBook_edit), onClick = { editing = true }, modifier = Modifier.weight(1f), icon = LucideIcons.Pencil)
                                addButton(controller, Modifier.weight(1f))
                            }
                        }
                    }
                    if (sideBySide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            cover()
                            Box(Modifier.weight(1f)) { details() }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            cover()
                            Spacer(Modifier.height(24.dp))
                            details()
                        }
                    }
                }
                SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(16.dp))
            }
        }
    } else {
        NookSheet(
            onClose = onClose,
            maxWidth = 512.dp,
            header = { controller -> SheetHeader(stringResource(Res.string.addBook_title), controller) },
        ) { controller ->
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!form.coverUrl.isNullOrBlank()) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        MediaImage(form.coverUrl, form.title, Modifier.width(80.dp), mode = MediaMode.BOOKS)
                    }
                }

                LabeledField(stringResource(Res.string.addBook_titleLabel)) {
                    NookTextField(form.title, { form = form.copy(title = it) }, placeholder = stringResource(Res.string.addBook_titlePlaceholder))
                }
                LabeledField(stringResource(Res.string.addBook_authorLabel)) {
                    NookTextField(form.author, { form = form.copy(author = it) }, placeholder = stringResource(Res.string.addBook_authorPlaceholder), readOnly = fromSearch)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(stringResource(Res.string.addBook_genreLabel), Modifier.weight(1f)) {
                        NookTextField(form.genre ?: "", { form = form.copy(genre = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addBook_genrePlaceholder))
                    }
                    LabeledField(stringResource(Res.string.addBook_publishedLabel), Modifier.weight(1f)) {
                        NookTextField(form.publishedDate ?: "", { form = form.copy(publishedDate = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addBook_publishedPlaceholder), readOnly = fromSearch)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LabeledField(stringResource(Res.string.addBook_pagesLabel), Modifier.weight(1f)) {
                        NookTextField(
                            form.pageCount?.toString() ?: "",
                            { if (digitsOnly(it)) form = form.copy(pageCount = it.toIntOrNull()) },
                            placeholder = stringResource(Res.string.addBook_pagesPlaceholder),
                            readOnly = fromSearch,
                            keyboardType = KeyboardType.Number,
                        )
                    }
                    LabeledField(stringResource(Res.string.addBook_coverUrlLabel), Modifier.weight(1f)) {
                        NookTextField(form.coverUrl ?: "", { form = form.copy(coverUrl = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addBook_coverUrlPlaceholder), keyboardType = KeyboardType.Uri)
                    }
                }

                LabeledField(stringResource(Res.string.addBook_statusLabel)) {
                    Spacer(Modifier.height(4.dp))
                    StatusChipRow(options = bookStatusOptions(), selected = form.status.key, onSelect = ::selectStatus)
                }

                if (form.status == BookStatus.READING) {
                    LabeledField(stringResource(Res.string.addBook_currentPageLabel)) {
                        NookTextField(
                            form.currentPage?.toString() ?: "",
                            { if (digitsOnly(it)) form = form.copy(currentPage = it.toIntOrNull()) },
                            placeholder = stringResource(Res.string.addBook_currentPagePlaceholder),
                            keyboardType = KeyboardType.Number,
                        )
                    }
                }

                if (form.status == BookStatus.READ) {
                    LabeledField(stringResource(Res.string.addBook_ratingLabel)) {
                        Spacer(Modifier.height(4.dp))
                        StarRating(form.rating, onChange = { form = form.copy(rating = it) }, size = 28.dp)
                    }
                }

                LabeledField(stringResource(Res.string.addBook_noteLabel)) {
                    NookTextArea(form.personalNote ?: "", { form = form.copy(personalNote = it.ifBlank { null }) }, placeholder = stringResource(Res.string.addBook_notePlaceholder))
                }

                if (!form.description.isNullOrBlank()) {
                    LabeledField(stringResource(Res.string.addBook_descriptionLabel)) {
                        NookTextArea(form.description ?: "", {}, readOnly = true)
                    }
                }

                duplicateWarning()

                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // From a search result, Cancel goes back to the preview rather than losing the book.
                    GhostButton(stringResource(Res.string.addBook_cancel), onClick = { if (fromSearch) editing = false else controller.close() }, modifier = Modifier.weight(1f))
                    addButton(controller, Modifier.weight(1f))
                }
            }
        }
    }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
