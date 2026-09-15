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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.patchOf
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.EditableNote
import fr.paulbr.nookmind.core.designsystem.components.ExpandableDescription
import fr.paulbr.nookmind.core.designsystem.components.GenrePill
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.LabeledBlock
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.MetaPill
import fr.paulbr.nookmind.core.designsystem.components.NookPressable
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SolidPill
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.common.ConfirmDeleteRow
import fr.paulbr.nookmind.feature.common.DeleteButton
import fr.paulbr.nookmind.feature.common.ProgressBar
import fr.paulbr.nookmind.feature.common.StatusChipRow
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.bookDetail_areYouSure
import fr.paulbr.nookmind.resources.bookDetail_cancel
import fr.paulbr.nookmind.resources.bookDetail_collections
import fr.paulbr.nookmind.resources.bookDetail_currentPage
import fr.paulbr.nookmind.resources.bookDetail_delete
import fr.paulbr.nookmind.resources.bookDetail_description
import fr.paulbr.nookmind.resources.bookDetail_movedToRead
import fr.paulbr.nookmind.resources.bookDetail_movedToReading
import fr.paulbr.nookmind.resources.bookDetail_movedToWantToRead
import fr.paulbr.nookmind.resources.bookDetail_noNotes
import fr.paulbr.nookmind.resources.bookDetail_noPageYet
import fr.paulbr.nookmind.resources.bookDetail_notePlaceholder
import fr.paulbr.nookmind.resources.bookDetail_noteSaved
import fr.paulbr.nookmind.resources.bookDetail_pages
import fr.paulbr.nookmind.resources.bookDetail_personalNote
import fr.paulbr.nookmind.resources.bookDetail_ratingUpdated
import fr.paulbr.nookmind.resources.bookDetail_read
import fr.paulbr.nookmind.resources.bookDetail_save
import fr.paulbr.nookmind.resources.bookDetail_seeLess
import fr.paulbr.nookmind.resources.bookDetail_seeMore
import fr.paulbr.nookmind.resources.bookDetail_wantToRead
import fr.paulbr.nookmind.resources.bookDetail_yesDelete
import fr.paulbr.nookmind.resources.bookDetail_yourRating
import fr.paulbr.nookmind.resources.common_pageOf
import fr.paulbr.nookmind.resources.common_pageOnly
import fr.paulbr.nookmind.resources.library_reading
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of BookDetailModal.tsx. */
@Composable
fun BookDetailSheet(container: AppContainer, book: Book, onClose: () -> Unit) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    val categories by container.bookCategories.items.collectAsState()
    var local by remember(book.id) { mutableStateOf(book) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingPage by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf(book.currentPage?.toString() ?: "") }

    fun apply(patch: Map<String, Any?>, optimistic: Book, onDone: (() -> Unit)? = null) {
        val previous = local
        local = optimistic
        scope.launch {
            val stored = container.books.update(book.id, patchOf(*patch.entries.map { it.key to it.value }.toTypedArray()))
            if (stored == null) local = previous else { local = stored; onDone?.invoke() }
        }
    }

    val movedToRead = stringResource(Res.string.bookDetail_movedToRead)
    val movedToReading = stringResource(Res.string.bookDetail_movedToReading)
    val movedToWant = stringResource(Res.string.bookDetail_movedToWantToRead)

    NookSheet(onClose = onClose, maxWidth = 672.dp, showHandle = true) { controller ->
        Box(Modifier.fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(24.dp)) {
                val sideBySide = maxWidth >= 560.dp
                val cover: @Composable () -> Unit = {
                    MediaImage(local.coverUrl, local.title, Modifier.width(if (sideBySide) 160.dp else 128.dp), mode = MediaMode.BOOKS)
                }
                val details: @Composable () -> Unit = {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(local.title, style = NookTheme.type.h2Serif.copy(lineHeight = 28.sp()), color = colors.textStrong, modifier = Modifier.padding(end = 32.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(local.author, style = NookTheme.type.sans(16, FontWeight.Medium, 24), color = colors.textMuted)
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!local.genre.isNullOrBlank()) GenrePill(local.genre!!)
                            SolidPill(
                                when (local.status) {
                                    BookStatus.READ -> stringResource(Res.string.bookDetail_read)
                                    BookStatus.READING -> stringResource(Res.string.library_reading)
                                    BookStatus.WANT_TO_READ -> stringResource(Res.string.bookDetail_wantToRead)
                                },
                                bookStatusColor(local.status),
                            )
                            yearOf(local.publishedDate)?.let { MetaPill(it) }
                            local.pageCount?.let { MetaPill(stringResource(Res.string.bookDetail_pages, it)) }
                        }

                        StatusChipRow(
                            options = bookStatusOptions(),
                            selected = local.status.key,
                            compact = false,
                            onSelect = { key ->
                                val status = BookStatus.fromKey(key)
                                if (status == local.status) return@StatusChipRow
                                val patch = mutableMapOf<String, Any?>("status" to status.key)
                                if (status == BookStatus.WANT_TO_READ) { patch["rating"] = null; patch["current_page"] = null }
                                if (status != BookStatus.READING) patch["current_page"] = null
                                apply(
                                    patch,
                                    local.copy(
                                        status = status,
                                        rating = if (status == BookStatus.WANT_TO_READ) null else local.rating,
                                        currentPage = if (status != BookStatus.READING) null else local.currentPage,
                                    ),
                                ) {
                                    container.toasts.success(
                                        when (status) {
                                            BookStatus.READ -> movedToRead
                                            BookStatus.READING -> movedToReading
                                            BookStatus.WANT_TO_READ -> movedToWant
                                        },
                                    )
                                }
                            },
                        )

                        if (local.status == BookStatus.READING) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(Res.string.bookDetail_currentPage), style = NookTheme.type.sm, color = colors.textSubtle)
                                    if (!editingPage) {
                                        IconGhostButton(LucideIcons.Pencil, null, onClick = { pageInput = local.currentPage?.toString() ?: ""; editingPage = true }, size = 13.dp, padding = 2.dp, tint = colors.textFaint)
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                if (editingPage) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        NookTextField(
                                            pageInput,
                                            { if (it.isEmpty() || it.all(Char::isDigit)) pageInput = it },
                                            modifier = Modifier.width(112.dp),
                                            textStyle = NookTheme.type.sm,
                                            keyboardType = KeyboardType.Number,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        )
                                        local.pageCount?.let { Text("/ $it", style = NookTheme.type.sm, color = colors.textFaint, modifier = Modifier.padding(top = 8.dp)) }
                                        PrimaryButton(
                                            stringResource(Res.string.bookDetail_save),
                                            onClick = {
                                                val page = pageInput.toIntOrNull()
                                                apply(mapOf("current_page" to page), local.copy(currentPage = page))
                                                editingPage = false
                                            },
                                            icon = LucideIcons.Check, iconSize = 14.dp,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                        GhostButton(stringResource(Res.string.bookDetail_cancel), onClick = { editingPage = false }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp))
                                    }
                                } else {
                                    val current = local.currentPage
                                    if (current != null) {
                                        val total = local.pageCount
                                        Text(
                                            if (total != null) stringResource(Res.string.common_pageOf, current, total) else stringResource(Res.string.common_pageOnly, current),
                                            style = NookTheme.type.sm, color = colors.textBody2,
                                        )
                                    } else {
                                        Text(stringResource(Res.string.bookDetail_noPageYet), style = NookTheme.type.sm.copy(fontStyle = FontStyle.Italic), color = colors.textFaint)
                                    }
                                }
                                val current = local.currentPage
                                val total = local.pageCount
                                if (current != null && total != null && total > 0) {
                                    Spacer(Modifier.height(8.dp))
                                    ProgressBar(current.toFloat() / total.toFloat())
                                }
                            }
                        }

                        if (local.status == BookStatus.READ) {
                            LabeledBlock(stringResource(Res.string.bookDetail_yourRating)) {
                                StarRating(local.rating, onChange = { rating ->
                                    apply(mapOf("rating" to rating), local.copy(rating = rating)) { container.toasts.success(Res.string.bookDetail_ratingUpdated) }
                                }, size = 26.dp)
                            }
                        }

                        if (!local.description.isNullOrBlank()) {
                            ExpandableDescription(
                                description = local.description!!,
                                label = stringResource(Res.string.bookDetail_description),
                                seeMoreText = stringResource(Res.string.bookDetail_seeMore),
                                seeLessText = stringResource(Res.string.bookDetail_seeLess),
                            )
                        }

                        EditableNote(
                            note = local.personalNote,
                            labelText = stringResource(Res.string.bookDetail_personalNote),
                            placeholderText = stringResource(Res.string.bookDetail_notePlaceholder),
                            saveText = stringResource(Res.string.bookDetail_save),
                            cancelText = stringResource(Res.string.bookDetail_cancel),
                            noNotesText = stringResource(Res.string.bookDetail_noNotes),
                            onSave = { note -> apply(mapOf("personal_note" to note), local.copy(personalNote = note)) { container.toasts.success(Res.string.bookDetail_noteSaved) } },
                        )

                        if (categories.isNotEmpty()) {
                            Column {
                                Text(stringResource(Res.string.bookDetail_collections), style = NookTheme.type.sm, color = colors.textSubtle)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categories.forEach { cat ->
                                        val isIn = local.id in cat.itemIds
                                        CollectionChip(cat.title, isIn) {
                                            scope.launch {
                                                if (isIn) container.bookCategories.removeMappedItem(cat.id, local.id)
                                                else container.bookCategories.addMappedItems(cat.id, listOf(local.id))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Box(Modifier.padding(top = 8.dp)) {
                            if (!confirmDelete) {
                                DeleteButton(stringResource(Res.string.bookDetail_delete), onClick = { confirmDelete = true })
                            } else {
                                ConfirmDeleteRow(
                                    question = stringResource(Res.string.bookDetail_areYouSure),
                                    yesText = stringResource(Res.string.bookDetail_yesDelete),
                                    cancelText = stringResource(Res.string.bookDetail_cancel),
                                    onConfirm = { scope.launch { if (container.books.delete(book.id)) onClose() } },
                                    onCancel = { confirmDelete = false },
                                )
                            }
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
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

/** Collection toggle chip of the detail sheets (FolderMinus when in, FolderPlus otherwise). */
@Composable
fun CollectionChip(title: String, isIn: Boolean, accent: androidx.compose.ui.graphics.Color = Palette.Amber500, onClick: () -> Unit) {
    val colors = NookTheme.colors
    NookPressable(
        onClick = onClick,
        shape = NookShapes.full,
        background = if (isIn) accent.alpha(0.15f) else androidx.compose.ui.graphics.Color.Transparent,
        borderColor = if (isIn) accent.alpha(0.5f) else colors.borderNeutral,
        contentColor = if (isIn) colors.amberTextStrong else colors.textMuted,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(if (isIn) LucideIcons.FolderMinus else LucideIcons.FolderPlus, null, Modifier.width(12.dp).height(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = NookTheme.type.sans(12, FontWeight.Medium, 16))
    }
}
