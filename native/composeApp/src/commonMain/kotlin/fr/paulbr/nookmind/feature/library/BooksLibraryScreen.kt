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
import androidx.compose.runtime.LaunchedEffect
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
import fr.paulbr.nookmind.core.data.patchOf
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.EmptyState
import fr.paulbr.nookmind.core.designsystem.components.SelectOption
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookCategory
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.books.BookCard
import fr.paulbr.nookmind.feature.books.BookDetailSheet
import fr.paulbr.nookmind.feature.books.BookListRow
import fr.paulbr.nookmind.feature.books.BookReadingCard
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
import fr.paulbr.nookmind.resources.library_author
import fr.paulbr.nookmind.resources.library_authorAZ
import fr.paulbr.nookmind.resources.library_booksCount
import fr.paulbr.nookmind.resources.library_cancel
import fr.paulbr.nookmind.resources.library_categoryEmpty
import fr.paulbr.nookmind.resources.library_categoryEmptyDesc
import fr.paulbr.nookmind.resources.library_confirmAdd
import fr.paulbr.nookmind.resources.library_confirmDeleteCategory
import fr.paulbr.nookmind.resources.library_dateAdded
import fr.paulbr.nookmind.resources.library_filters
import fr.paulbr.nookmind.resources.library_filtersDone
import fr.paulbr.nookmind.resources.library_filtersReset
import fr.paulbr.nookmind.resources.library_genre
import fr.paulbr.nookmind.resources.library_newCategory
import fr.paulbr.nookmind.resources.library_newCategoryPlaceholder
import fr.paulbr.nookmind.resources.library_noBooksFound
import fr.paulbr.nookmind.resources.library_noBooksRead
import fr.paulbr.nookmind.resources.library_ratingDesc
import fr.paulbr.nookmind.resources.library_read
import fr.paulbr.nookmind.resources.library_reading
import fr.paulbr.nookmind.resources.library_search
import fr.paulbr.nookmind.resources.library_searchBooks
import fr.paulbr.nookmind.resources.library_title
import fr.paulbr.nookmind.resources.library_titleAZ
import fr.paulbr.nookmind.resources.library_wantToRead
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private enum class BookSort(val key: String) { CREATED("created_at"), TITLE("title"), AUTHOR("author"), RATING("rating") }

/**
 * Port of Library.tsx (books), reorganised: status tabs, then the collections as their own row, then
 * a sort menu and a filters sheet instead of a row of selects. Cards drop the status badge the tab
 * already gives, and "Reading" shows wide cards with the progress. Search looks through every book.
 * [initialTab] opens a given tab instead of "Reading"-if-any (screenshots).
 */
@Composable
fun BooksLibraryScreen(container: AppContainer, contentPadding: PaddingValues, initialTab: String? = null) {
    val books by container.books.items.collectAsState()
    val loading by container.books.loading.collectAsState()
    val categories by container.bookCategories.items.collectAsState()
    val scope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf(initialTab ?: BookStatus.WANT_TO_READ.key) }
    var tabPicked by rememberSaveable { mutableStateOf(initialTab != null) }
    var genreFilter by rememberSaveable { mutableStateOf("") }
    var authorFilter by rememberSaveable { mutableStateOf("") }
    var sortKey by rememberSaveable { mutableStateOf(BookSort.CREATED.key) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filtersOpen by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(container.prefs.viewMode(MediaMode.BOOKS)) }
    var selected by remember { mutableStateOf<Book?>(null) }
    var pickerCategory by remember { mutableStateOf<BookCategory?>(null) }
    var deletingCategoryId by remember { mutableStateOf<String?>(null) }

    // Open on "Reading" when a book is in progress, until the reader picks a tab themselves.
    LaunchedEffect(loading, books.any { it.status == BookStatus.READING }) {
        if (!loading && !tabPicked && books.any { it.status == BookStatus.READING }) activeTab = BookStatus.READING.key
    }

    val activeCategory = categories.firstOrNull { it.id == activeTab }
    val activeStatus = BookStatus.entries.firstOrNull { it.key == activeTab }
    val isStatusTab = activeStatus != null
    val searching = searchOpen && query.isNotBlank()
    val tabBooks = if (activeStatus != null) books.filter { it.status == activeStatus } else emptyList()
    val genres = tabBooks.mapNotNull { it.genre?.takeIf(String::isNotBlank) }.distinct().sorted()
    val authors = tabBooks.map { it.author }.filter { it.isNotBlank() }.distinct().sorted()
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
    val searchResults = if (searching) {
        val q = query.trim()
        books.filter { it.title.contains(q, ignoreCase = true) || it.author.contains(q, ignoreCase = true) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    } else emptyList()
    val categoryBooks = activeCategory?.let { cat -> books.filter { it.id in cat.itemIds } } ?: emptyList()
    val activeFilters = listOf(genreFilter, authorFilter).count { it.isNotEmpty() }

    val tabs = listOf(
        LibraryTab(BookStatus.READING.key, stringResource(Res.string.library_reading), books.count { it.status == BookStatus.READING }, TabTone.BLUE),
        LibraryTab(BookStatus.WANT_TO_READ.key, stringResource(Res.string.library_wantToRead), books.count { it.status == BookStatus.WANT_TO_READ }, TabTone.AMBER),
        LibraryTab(BookStatus.READ.key, stringResource(Res.string.library_read), books.count { it.status == BookStatus.READ }, TabTone.EMERALD),
    )
    val sortOptions = buildList {
        add(SelectOption(BookSort.CREATED.key, stringResource(Res.string.library_dateAdded)))
        add(SelectOption(BookSort.TITLE.key, stringResource(Res.string.library_titleAZ)))
        add(SelectOption(BookSort.AUTHOR.key, stringResource(Res.string.library_authorAZ)))
        if (activeStatus == BookStatus.READ) add(SelectOption(BookSort.RATING.key, stringResource(Res.string.library_ratingDesc)))
    }
    if (sortOptions.none { it.value == sortKey }) sortKey = BookSort.CREATED.key

    fun savePage(book: Book, page: Int?) {
        scope.launch { container.books.update(book.id, patchOf("current_page" to page)) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= TABLET_BREAKPOINT_DP.dp
        val columns = libraryColumns(maxWidth)
        CompositionLocalProvider(LocalWideLayout provides wide) {
            val pad = libraryPagePadding()
            val bleed = if (wide) Modifier else Modifier.horizontalBleed(pad)
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
                            actions = {
                                LibraryHeaderButton(LucideIcons.Search, stringResource(Res.string.library_search), active = searchOpen, onClick = {
                                    searchOpen = !searchOpen
                                    if (!searchOpen) query = ""
                                })
                            },
                        )
                        Spacer(Modifier.height(if (searchOpen) 16.dp else 20.dp))
                    }
                    if (searchOpen) {
                        item(key = "search") {
                            LibrarySearchField(query, { query = it }, stringResource(Res.string.library_search), onClose = { searchOpen = false; query = "" })
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    if (!searching) {
                        item(key = "tabs") {
                            LibraryStatusTabs(
                                tabs = tabs,
                                activeId = activeTab,
                                onSelect = { id -> activeTab = id; tabPicked = true; genreFilter = ""; authorFilter = "" },
                                modifier = bleed,
                            )
                            Spacer(Modifier.height(14.dp))
                            CollectionChipsRow(
                                categories = categories,
                                activeId = activeTab,
                                onSelect = { id -> activeTab = id; tabPicked = true },
                                onDeleteCategory = { deletingCategoryId = it },
                                newCategoryLabel = stringResource(Res.string.library_newCategory),
                                newCategoryPlaceholder = stringResource(Res.string.library_newCategoryPlaceholder),
                                onCreateCategory = { name -> scope.launch { container.bookCategories.create(name)?.let { activeTab = it.id; tabPicked = true } } },
                                modifier = bleed,
                            )
                            Spacer(Modifier.height(20.dp))
                        }
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
                    if (!searching && activeCategory != null) {
                        item(key = "category-toolbar") {
                            CategoryToolbar(
                                countText = pluralStringResource(Res.plurals.common_itemsCountBooks, activeCategory.itemIds.size, activeCategory.itemIds.size),
                                addLabel = stringResource(Res.string.library_addBooks),
                                onAdd = { pickerCategory = activeCategory },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (!searching && isStatusTab && tabBooks.isNotEmpty()) {
                        item(key = "toolbar") {
                            LibraryToolbar(
                                countText = pluralStringResource(Res.plurals.common_itemsCountBooks, filtered.size, filtered.size),
                                sortOptions = sortOptions,
                                sortKey = sortKey,
                                onSort = { sortKey = it },
                                filtersLabel = stringResource(Res.string.library_filters),
                                activeFilters = activeFilters,
                                onOpenFilters = if (genres.size > 1 || authors.size > 1 || activeFilters > 0) ({ filtersOpen = true }) else null,
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    when {
                        loading -> skeletonGrid(columns)
                        searching -> when {
                            searchResults.isEmpty() -> item(key = "empty-search") {
                                EmptyState(icon = LucideIcons.Search, title = stringResource(Res.string.library_noBooksFound))
                            }
                            viewMode == ViewMode.GRID -> gridRows(searchResults, columns, key = { it.id }) { book -> BookCard(book, onClick = { selected = book }) }
                            else -> listRows(searchResults, key = { it.id }) { book -> BookListRow(book, onClick = { selected = book }) }
                        }
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
                        activeStatus == BookStatus.READING && viewMode == ViewMode.GRID -> gridRows(filtered, if (wide) 2 else 1, key = { it.id }, spacing = 12.dp) { book ->
                            BookReadingCard(book, onClick = { selected = book }, onSavePage = { page -> savePage(book, page) })
                        }
                        viewMode == ViewMode.GRID -> gridRows(filtered, columns, key = { it.id }) { book -> BookCard(book, onClick = { selected = book }, showStatus = false) }
                        else -> listRows(filtered, key = { it.id }) { book -> BookListRow(book, onClick = { selected = book }, showStatus = false) }
                    }
                }
            }
        }
    }

    if (filtersOpen) {
        LibraryFiltersSheet(
            title = stringResource(Res.string.library_filters),
            groups = listOf(
                FilterGroup(stringResource(Res.string.library_genre), stringResource(Res.string.library_allGenres), genres, genreFilter) { genreFilter = it },
                FilterGroup(stringResource(Res.string.library_author), stringResource(Res.string.library_allAuthors), authors, authorFilter) { authorFilter = it },
            ),
            resetLabel = stringResource(Res.string.library_filtersReset),
            doneLabel = stringResource(Res.string.library_filtersDone),
            onReset = { genreFilter = ""; authorFilter = "" },
            onClose = { filtersOpen = false },
        )
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
