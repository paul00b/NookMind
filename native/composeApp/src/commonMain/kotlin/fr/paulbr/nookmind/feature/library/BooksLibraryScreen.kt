package fr.paulbr.nookmind.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.ViewMode
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.EmptyState
import fr.paulbr.nookmind.core.designsystem.components.NookSelect
import fr.paulbr.nookmind.core.designsystem.components.SelectOption
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookCategory
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.books.BookCard
import fr.paulbr.nookmind.feature.books.BookDetailSheet
import fr.paulbr.nookmind.feature.books.BookListRow
import fr.paulbr.nookmind.feature.collections.CategoryItemPickerSheet
import fr.paulbr.nookmind.feature.collections.PickerItem
import fr.paulbr.nookmind.feature.collections.PickerLabels
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.horizontalBleed
import fr.paulbr.nookmind.feature.shell.TABLET_BREAKPOINT_DP
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.bookDetail_yesDelete
import fr.paulbr.nookmind.resources.common_collectionDeleted
import fr.paulbr.nookmind.resources.common_itemsCountBooks
import fr.paulbr.nookmind.resources.library_addBooks
import fr.paulbr.nookmind.resources.library_addBooksTo
import fr.paulbr.nookmind.resources.library_addNBooks
import fr.paulbr.nookmind.resources.library_allAuthors
import fr.paulbr.nookmind.resources.library_allGenres
import fr.paulbr.nookmind.resources.library_alreadyInCategory
import fr.paulbr.nookmind.resources.library_authorAZ
import fr.paulbr.nookmind.resources.library_booksCount
import fr.paulbr.nookmind.resources.library_cancel
import fr.paulbr.nookmind.resources.library_categoryEmpty
import fr.paulbr.nookmind.resources.library_categoryEmptyDesc
import fr.paulbr.nookmind.resources.library_confirmAdd
import fr.paulbr.nookmind.resources.library_confirmDeleteCategory
import fr.paulbr.nookmind.resources.library_dateAdded
import fr.paulbr.nookmind.resources.library_newCategory
import fr.paulbr.nookmind.resources.library_newCategoryPlaceholder
import fr.paulbr.nookmind.resources.library_noBooksFound
import fr.paulbr.nookmind.resources.library_noBooksRead
import fr.paulbr.nookmind.resources.library_ratingDesc
import fr.paulbr.nookmind.resources.library_read
import fr.paulbr.nookmind.resources.library_reading
import fr.paulbr.nookmind.resources.library_searchBooks
import fr.paulbr.nookmind.resources.library_title
import fr.paulbr.nookmind.resources.library_titleAZ
import fr.paulbr.nookmind.resources.library_wantToRead
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private enum class BookSort(val key: String) { CREATED("created_at"), TITLE("title"), AUTHOR("author"), RATING("rating") }

/** Port of Library.tsx (books). */
@Composable
fun BooksLibraryScreen(container: AppContainer, contentPadding: PaddingValues) {
    val books by container.books.items.collectAsState()
    val loading by container.books.loading.collectAsState()
    val categories by container.bookCategories.items.collectAsState()
    val scope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf(BookStatus.WANT_TO_READ.key) }
    var genreFilter by rememberSaveable { mutableStateOf("") }
    var authorFilter by rememberSaveable { mutableStateOf("") }
    var sortKey by rememberSaveable { mutableStateOf(BookSort.CREATED.key) }
    var viewMode by remember { mutableStateOf(container.prefs.viewMode(MediaMode.BOOKS)) }
    var selected by remember { mutableStateOf<Book?>(null) }
    var pickerCategory by remember { mutableStateOf<BookCategory?>(null) }
    var deletingCategoryId by remember { mutableStateOf<String?>(null) }

    val activeCategory = categories.firstOrNull { it.id == activeTab }
    val activeStatus = BookStatus.entries.firstOrNull { it.key == activeTab }
    val isStatusTab = activeStatus != null
    val tabBooks = if (activeStatus != null) books.filter { it.status == activeStatus } else emptyList()
    val genres = tabBooks.mapNotNull { it.genre?.takeIf(String::isNotBlank) }.distinct()
    val authors = tabBooks.map { it.author }.filter { it.isNotBlank() }.distinct()
    val filtered = tabBooks
        .filter { genreFilter.isEmpty() || it.genre == genreFilter }
        .filter { authorFilter.isEmpty() || it.author == authorFilter }
        .let { list ->
            when (BookSort.entries.first { it.key == sortKey }) {
                BookSort.CREATED -> list.sortedByDescending { it.createdAt }
                BookSort.TITLE -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                BookSort.AUTHOR -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.author })
                BookSort.RATING -> list.sortedByDescending { it.rating ?: 0.0 }
            }
        }
    val categoryBooks = activeCategory?.let { cat -> books.filter { it.id in cat.itemIds } } ?: emptyList()

    val tabs = listOf(
        LibraryTab(BookStatus.READING.key, stringResource(Res.string.library_reading), books.count { it.status == BookStatus.READING }),
        LibraryTab(BookStatus.WANT_TO_READ.key, stringResource(Res.string.library_wantToRead), books.count { it.status == BookStatus.WANT_TO_READ }),
        LibraryTab(BookStatus.READ.key, stringResource(Res.string.library_read), books.count { it.status == BookStatus.READ }),
    )
    val sortOptions = buildList {
        add(SelectOption(BookSort.CREATED.key, stringResource(Res.string.library_dateAdded)))
        add(SelectOption(BookSort.TITLE.key, stringResource(Res.string.library_titleAZ)))
        add(SelectOption(BookSort.AUTHOR.key, stringResource(Res.string.library_authorAZ)))
        if (activeStatus == BookStatus.READ) add(SelectOption(BookSort.RATING.key, stringResource(Res.string.library_ratingDesc)))
    }
    if (sortOptions.none { it.value == sortKey }) sortKey = BookSort.CREATED.key

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= TABLET_BREAKPOINT_DP.dp
        val columns = libraryColumns(maxWidth)
        CompositionLocalProvider(LocalWideLayout provides wide) {
            val pad = libraryPagePadding()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.fillMaxHeight().fillMaxWidth().widthIn(max = LIBRARY_MAX_WIDTH),
                    contentPadding = PaddingValues(start = pad, end = pad, top = pad, bottom = pad + contentPadding.calculateBottomPadding()),
                ) {
                    item(key = "header") {
                        LibraryHeader(
                            title = stringResource(Res.string.library_title),
                            titleColor = NookTheme.colors.amberText,
                            subtitle = pluralStringResource(Res.plurals.library_booksCount, books.size, books.size),
                            viewMode = viewMode,
                            onViewMode = { viewMode = it; container.prefs.setViewMode(MediaMode.BOOKS, it) },
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                    item(key = "tabs") {
                        LibraryTabsRow(
                            tabs = tabs,
                            categories = categories,
                            activeId = activeTab,
                            onSelect = { id ->
                                activeTab = id
                                if (BookStatus.entries.any { it.key == id }) { genreFilter = ""; authorFilter = "" }
                            },
                            onDeleteCategory = { deletingCategoryId = it },
                            newCategoryLabel = stringResource(Res.string.library_newCategory),
                            newCategoryPlaceholder = stringResource(Res.string.library_newCategoryPlaceholder),
                            onCreateCategory = { name -> scope.launch { container.bookCategories.create(name)?.let { activeTab = it.id } } },
                            modifier = if (wide) Modifier else Modifier.horizontalBleed(pad),
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                    deletingCategoryId?.let { id ->
                        val cat = categories.firstOrNull { it.id == id }
                        if (cat != null) item(key = "delete-banner") {
                            DeleteCategoryBanner(
                                text = stringResource(Res.string.library_confirmDeleteCategory, cat.title),
                                yesText = stringResource(Res.string.bookDetail_yesDelete),
                                cancelText = stringResource(Res.string.library_cancel),
                                onConfirm = {
                                    if (activeTab == id) activeTab = BookStatus.WANT_TO_READ.key
                                    deletingCategoryId = null
                                    scope.launch {
                                        if (container.bookCategories.delete(id)) container.toasts.success(Res.string.common_collectionDeleted, cat.title)
                                    }
                                },
                                onCancel = { deletingCategoryId = null },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (activeCategory != null) {
                        item(key = "category-toolbar") {
                            CategoryToolbar(
                                countText = pluralStringResource(Res.plurals.common_itemsCountBooks, activeCategory.itemIds.size, activeCategory.itemIds.size),
                                addLabel = stringResource(Res.string.library_addBooks),
                                onAdd = { pickerCategory = activeCategory },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (isStatusTab && tabBooks.isNotEmpty()) {
                        item(key = "filters") {
                            FilterRow(if (wide) Modifier else Modifier.horizontalBleed(pad)) {
                                NookSelect(genreFilter, listOf(SelectOption("", stringResource(Res.string.library_allGenres))) + genres.map { SelectOption(it, it) }, onChange = { genreFilter = it })
                                NookSelect(authorFilter, listOf(SelectOption("", stringResource(Res.string.library_allAuthors))) + authors.map { SelectOption(it, it) }, onChange = { authorFilter = it })
                                NookSelect(sortKey, sortOptions, onChange = { sortKey = it })
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    when {
                        loading -> skeletonGrid(columns)
                        activeCategory != null -> when {
                            categoryBooks.isEmpty() -> item(key = "empty-category") {
                                CategoryEmptyState(
                                    title = stringResource(Res.string.library_categoryEmpty),
                                    description = stringResource(Res.string.library_categoryEmptyDesc),
                                    addLabel = stringResource(Res.string.library_addBooks),
                                    onAdd = { pickerCategory = activeCategory },
                                )
                            }
                            viewMode == ViewMode.GRID -> gridRows(categoryBooks, columns, key = { it.id }) { book ->
                                BookCard(book, onClick = { selected = book }, onRemove = { scope.launch { container.bookCategories.removeMappedItem(activeCategory.id, book.id) } })
                            }
                            else -> listRows(categoryBooks, key = { it.id }) { book ->
                                BookListRow(book, onClick = { selected = book }, onRemove = { scope.launch { container.bookCategories.removeMappedItem(activeCategory.id, book.id) } })
                            }
                        }
                        filtered.isEmpty() -> item(key = "empty") {
                            EmptyState(icon = LucideIcons.BookOpen, title = stringResource(Res.string.library_noBooksRead))
                        }
                        viewMode == ViewMode.GRID -> gridRows(filtered, columns, key = { it.id }) { book -> BookCard(book, onClick = { selected = book }) }
                        else -> listRows(filtered, key = { it.id }) { book -> BookListRow(book, onClick = { selected = book }) }
                    }
                }
            }
        }
    }

    selected?.let { BookDetailSheet(container, it, onClose = { selected = null }) }
    pickerCategory?.let { category ->
        CategoryItemPickerSheet(
            existingIds = category.itemIds,
            items = books.map { PickerItem(it.id, it.title, it.author, it.coverUrl) },
            labels = PickerLabels(
                header = stringResource(Res.string.library_addBooksTo, category.title),
                searchPlaceholder = stringResource(Res.string.library_searchBooks),
                noItemsFound = stringResource(Res.string.library_noBooksFound),
                alreadyInCategory = stringResource(Res.string.library_alreadyInCategory),
                cancel = stringResource(Res.string.library_cancel),
                confirmLabel = { n -> if (n > 0) stringResource(Res.string.library_addNBooks, n) else stringResource(Res.string.library_confirmAdd) },
            ),
            mode = MediaMode.BOOKS,
            onConfirm = { ids -> scope.launch { container.bookCategories.addMappedItems(category.id, ids); pickerCategory = null } },
            onClose = { pickerCategory = null },
        )
    }
}
