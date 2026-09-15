package fr.paulbr.nookmind.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.LabeledField
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextArea
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.normalizeTitle
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of AddBookModal.tsx. [prefill] comes from a Google Books result; null means a manual add. */
@Composable
fun AddBookSheet(container: AppContainer, prefill: Book?, onClose: () -> Unit) {
    val books by container.books.items.collectAsState()
    val scope = rememberCoroutineScope()
    var form by remember { mutableStateOf(prefill ?: Book(title = "")) }
    var saving by remember { mutableStateOf(false) }
    val fromSearch = prefill != null
    val isDuplicate = form.title.isNotBlank() && form.author.isNotBlank() && books.any {
        normalizeTitle(it.title) == normalizeTitle(form.title) && normalizeTitle(it.author) == normalizeTitle(form.author)
    }

    fun digitsOnly(value: String) = value.isEmpty() || value.all(Char::isDigit)

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
                StatusChipRow(
                    options = bookStatusOptions(),
                    selected = form.status.key,
                    onSelect = { key ->
                        val status = BookStatus.fromKey(key)
                        form = form.copy(
                            status = status,
                            rating = if (status == BookStatus.READ) form.rating else null,
                            currentPage = if (status == BookStatus.READING) form.currentPage else null,
                        )
                    },
                )
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

            if (isDuplicate) {
                InlineBanner(stringResource(Res.string.addBook_alreadyInLibrary), tone = BannerTone.WARNING, icon = LucideIcons.AlertTriangle)
            }

            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton(stringResource(Res.string.addBook_cancel), onClick = { controller.close() }, modifier = Modifier.weight(1f))
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
                    modifier = Modifier.weight(1f),
                    enabled = form.title.isNotBlank(),
                    loading = saving,
                    icon = LucideIcons.BookOpen,
                )
            }
        }
    }
}
