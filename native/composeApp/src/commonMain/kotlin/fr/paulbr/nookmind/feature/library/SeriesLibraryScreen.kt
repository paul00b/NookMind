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
import androidx.compose.foundation.lazy.LazyListScope
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
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.EmptyState
import fr.paulbr.nookmind.core.designsystem.components.SectionHeader
import fr.paulbr.nookmind.core.designsystem.components.SelectOption
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.getEffectiveSeriesStatus
import fr.paulbr.nookmind.core.domain.isSeriesWaiting
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Series
import fr.paulbr.nookmind.core.model.SeriesCategory
import fr.paulbr.nookmind.core.model.SeriesStatus
import fr.paulbr.nookmind.feature.collections.CategoryItemPickerSheet
import fr.paulbr.nookmind.feature.collections.PickerItem
import fr.paulbr.nookmind.feature.collections.PickerLabels
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.horizontalBleed
import fr.paulbr.nookmind.feature.series.SeriesCard
import fr.paulbr.nookmind.feature.series.SeriesDetailSheet
import fr.paulbr.nookmind.feature.series.SeriesListRow
import fr.paulbr.nookmind.feature.series.SeriesStatsSheet
import fr.paulbr.nookmind.feature.shell.TABLET_BREAKPOINT_DP
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_collectionDeleted
import fr.paulbr.nookmind.resources.common_itemsCountSeries
import fr.paulbr.nookmind.resources.library_filters
import fr.paulbr.nookmind.resources.library_filtersDone
import fr.paulbr.nookmind.resources.library_filtersReset
import fr.paulbr.nookmind.resources.library_genre
import fr.paulbr.nookmind.resources.seriesLibrary_addNSeries
import fr.paulbr.nookmind.resources.seriesLibrary_addSeries
import fr.paulbr.nookmind.resources.seriesLibrary_addSeriesTo
import fr.paulbr.nookmind.resources.seriesLibrary_allCreators
import fr.paulbr.nookmind.resources.seriesLibrary_allGenres
import fr.paulbr.nookmind.resources.seriesLibrary_alreadyInCategory
import fr.paulbr.nookmind.resources.seriesLibrary_cancel
import fr.paulbr.nookmind.resources.seriesLibrary_categoryEmpty
import fr.paulbr.nookmind.resources.seriesLibrary_categoryEmptyDesc
import fr.paulbr.nookmind.resources.seriesLibrary_confirmAdd
import fr.paulbr.nookmind.resources.seriesLibrary_confirmDeleteCategory
import fr.paulbr.nookmind.resources.seriesLibrary_creator
import fr.paulbr.nookmind.resources.seriesLibrary_creatorAZ
import fr.paulbr.nookmind.resources.seriesLibrary_dateAdded
import fr.paulbr.nookmind.resources.seriesLibrary_newCategory
import fr.paulbr.nookmind.resources.seriesLibrary_newCategoryPlaceholder
import fr.paulbr.nookmind.resources.seriesLibrary_noSeriesFound
import fr.paulbr.nookmind.resources.seriesLibrary_noSeriesWatched
import fr.paulbr.nookmind.resources.seriesLibrary_ratingDesc
import fr.paulbr.nookmind.resources.seriesLibrary_search
import fr.paulbr.nookmind.resources.seriesLibrary_searchSeries
import fr.paulbr.nookmind.resources.seriesLibrary_title
import fr.paulbr.nookmind.resources.seriesLibrary_titleAZ
import fr.paulbr.nookmind.resources.seriesLibrary_waitingNextSeason
import fr.paulbr.nookmind.resources.seriesLibrary_wantToWatch
import fr.paulbr.nookmind.resources.seriesLibrary_watched
import fr.paulbr.nookmind.resources.seriesLibrary_watching
import fr.paulbr.nookmind.resources.seriesLibrary_yesDelete
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private enum class SeriesSort(val key: String) { CREATED("created_at"), TITLE("title"), CREATOR("creator"), RATING("rating") }

/** Port of SeriesLibrary.tsx: tabs with icons, watching split into active / waiting, stats sheet. */
@Composable
fun SeriesLibraryScreen(container: AppContainer, contentPadding: PaddingValues) {
    val series by container.series.items.collectAsState()
    val loading by container.series.loading.collectAsState()
    val categories by container.seriesCategories.items.collectAsState()
    val scope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf(SeriesStatus.WATCHING.key) }
    var genreFilter by rememberSaveable { mutableStateOf("") }
    var creatorFilter by rememberSaveable { mutableStateOf("") }
    var sortKey by rememberSaveable { mutableStateOf(SeriesSort.CREATED.key) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var filtersOpen by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(container.prefs.viewMode(MediaMode.SERIES)) }
    var selected by remember { mutableStateOf<Series?>(null) }
    var pickerCategory by remember { mutableStateOf<SeriesCategory?>(null) }
    var deletingCategoryId by remember { mutableStateOf<String?>(null) }
    var showStats by remember { mutableStateOf(false) }

    val activeCategory = categories.firstOrNull { it.id == activeTab }
    val activeStatus = SeriesStatus.entries.firstOrNull { it.key == activeTab }
    val isStatusTab = activeStatus != null

    fun matchesTab(s: Series, status: SeriesStatus): Boolean {
        val effective = getEffectiveSeriesStatus(s)
        return when (status) {
            SeriesStatus.WATCHING -> effective == SeriesStatus.WATCHING || isSeriesWaiting(s)
            SeriesStatus.WATCHED -> effective == SeriesStatus.WATCHED && !isSeriesWaiting(s)
            SeriesStatus.WANT_TO_WATCH -> effective == SeriesStatus.WANT_TO_WATCH
        }
    }

    val tabSeries = if (activeStatus != null) series.filter { matchesTab(it, activeStatus) } else emptyList()
    val searching = searchOpen && query.isNotBlank()
    val genres = tabSeries.mapNotNull { it.genre?.takeIf(String::isNotBlank) }.distinct().sorted()
    val creators = tabSeries.map { it.creator }.filter { it.isNotBlank() }.distinct().sorted()
    val filtered = tabSeries
        .filter { genreFilter.isEmpty() || it.genre == genreFilter }
        .filter { creatorFilter.isEmpty() || it.creator == creatorFilter }
        .let { list ->
            when (SeriesSort.entries.first { it.key == sortKey }) {
                SeriesSort.CREATED -> list.sortedByDescending { it.createdAt }
                SeriesSort.TITLE -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                SeriesSort.CREATOR -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.creator })
                SeriesSort.RATING -> list.sortedByDescending { it.rating ?: 0.0 }
            }
        }
    val categorySeries = activeCategory?.let { cat -> series.filter { it.id in cat.itemIds } } ?: emptyList()
    val searchResults = if (searching) {
        val q = query.trim()
        series.filter { it.title.contains(q, ignoreCase = true) || it.creator.contains(q, ignoreCase = true) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    } else emptyList()
    val activeFilters = listOf(genreFilter, creatorFilter).count { it.isNotEmpty() }

    val tabs = listOf(
        LibraryTab(SeriesStatus.WATCHING.key, stringResource(Res.string.seriesLibrary_watching), series.count { matchesTab(it, SeriesStatus.WATCHING) }, TabTone.BLUE, LucideIcons.Play),
        LibraryTab(SeriesStatus.WANT_TO_WATCH.key, stringResource(Res.string.seriesLibrary_wantToWatch), series.count { matchesTab(it, SeriesStatus.WANT_TO_WATCH) }, TabTone.AMBER, LucideIcons.Bookmark),
        LibraryTab(SeriesStatus.WATCHED.key, stringResource(Res.string.seriesLibrary_watched), series.count { matchesTab(it, SeriesStatus.WATCHED) }, TabTone.EMERALD, LucideIcons.CheckCheck),
    )
    val sortOptions = buildList {
        add(SelectOption(SeriesSort.CREATED.key, stringResource(Res.string.seriesLibrary_dateAdded)))
        add(SelectOption(SeriesSort.TITLE.key, stringResource(Res.string.seriesLibrary_titleAZ)))
        add(SelectOption(SeriesSort.CREATOR.key, stringResource(Res.string.seriesLibrary_creatorAZ)))
        if (activeStatus == SeriesStatus.WATCHED) add(SelectOption(SeriesSort.RATING.key, stringResource(Res.string.seriesLibrary_ratingDesc)))
    }
    if (sortOptions.none { it.value == sortKey }) sortKey = SeriesSort.CREATED.key

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
                            title = stringResource(Res.string.seriesLibrary_title),
                            titleColor = NookTheme.colors.tealText,
                            subtitle = pluralStringResource(Res.plurals.common_itemsCountSeries, series.size, series.size),
                            viewMode = viewMode,
                            onViewMode = { viewMode = it; container.prefs.setViewMode(MediaMode.SERIES, it) },
                            actions = {
                                LibraryHeaderButton(LucideIcons.BarChart2, contentDescription = null, onClick = { showStats = true })
                                LibraryHeaderButton(LucideIcons.Search, stringResource(Res.string.seriesLibrary_search), active = searchOpen, onClick = {
                                    searchOpen = !searchOpen
                                    if (!searchOpen) query = ""
                                })
                            },
                        )
                        Spacer(Modifier.height(if (searchOpen) 16.dp else 20.dp))
                    }
                    if (searchOpen) {
                        item(key = "search") {
                            LibrarySearchField(query, { query = it }, stringResource(Res.string.seriesLibrary_search), onClose = { searchOpen = false; query = "" })
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    if (!searching) {
                        item(key = "tabs") {
                            LibraryStatusTabs(
                                tabs = tabs,
                                activeId = activeTab,
                                onSelect = { id -> activeTab = id; genreFilter = ""; creatorFilter = "" },
                                modifier = bleed,
                            )
                            Spacer(Modifier.height(14.dp))
                            CollectionChipsRow(
                                categories = categories,
                                activeId = activeTab,
                                onSelect = { id -> activeTab = id },
                                onDeleteCategory = { deletingCategoryId = it },
                                newCategoryLabel = stringResource(Res.string.seriesLibrary_newCategory),
                                newCategoryPlaceholder = stringResource(Res.string.seriesLibrary_newCategoryPlaceholder),
                                onCreateCategory = { name -> scope.launch { container.seriesCategories.create(name)?.let { activeTab = it.id } } },
                                modifier = bleed,
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    deletingCategoryId?.let { id ->
                        val cat = categories.firstOrNull { it.id == id }
                        if (cat != null) item(key = "delete-banner") {
                            DeleteCategoryBanner(
                                text = stringResource(Res.string.seriesLibrary_confirmDeleteCategory, cat.title),
                                yesText = stringResource(Res.string.seriesLibrary_yesDelete),
                                cancelText = stringResource(Res.string.seriesLibrary_cancel),
                                onConfirm = {
                                    if (activeTab == id) activeTab = SeriesStatus.WATCHING.key
                                    deletingCategoryId = null
                                    scope.launch { if (container.seriesCategories.delete(id)) container.toasts.success(Res.string.common_collectionDeleted, cat.title) }
                                },
                                onCancel = { deletingCategoryId = null },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (!searching && activeCategory != null) {
                        item(key = "category-toolbar") {
                            CategoryToolbar(
                                countText = pluralStringResource(Res.plurals.common_itemsCountSeries, activeCategory.itemIds.size, activeCategory.itemIds.size),
                                addLabel = stringResource(Res.string.seriesLibrary_addSeries),
                                onAdd = { pickerCategory = activeCategory },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (!searching && isStatusTab && tabSeries.isNotEmpty()) {
                        item(key = "toolbar") {
                            LibraryToolbar(
                                countText = pluralStringResource(Res.plurals.common_itemsCountSeries, filtered.size, filtered.size),
                                sortOptions = sortOptions,
                                sortKey = sortKey,
                                onSort = { sortKey = it },
                                filtersLabel = stringResource(Res.string.library_filters),
                                activeFilters = activeFilters,
                                onOpenFilters = if (genres.size > 1 || creators.size > 1 || activeFilters > 0) ({ filtersOpen = true }) else null,
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    val onRemoveFromCategory: ((Series) -> Unit)? = activeCategory?.let { cat -> { s -> scope.launch { container.seriesCategories.removeMappedItem(cat.id, s.id) } } }
                    when {
                        loading -> skeletonGrid(columns)
                        searching -> when {
                            searchResults.isEmpty() -> item(key = "empty-search") {
                                EmptyState(icon = LucideIcons.Search, title = stringResource(Res.string.seriesLibrary_noSeriesFound))
                            }
                            else -> seriesRows(searchResults, viewMode, columns, onSelect = { selected = it }, onRemove = null, showStatus = true)
                        }
                        activeCategory != null -> when {
                            categorySeries.isEmpty() -> item(key = "empty-category") {
                                CategoryEmptyState(
                                    title = stringResource(Res.string.seriesLibrary_categoryEmpty),
                                    description = stringResource(Res.string.seriesLibrary_categoryEmptyDesc),
                                    addLabel = stringResource(Res.string.seriesLibrary_addSeries),
                                    onAdd = { pickerCategory = activeCategory },
                                )
                            }
                            else -> seriesRows(categorySeries, viewMode, columns, onSelect = { selected = it }, onRemove = onRemoveFromCategory, showStatus = true)
                        }
                        filtered.isEmpty() -> item(key = "empty") { EmptyState(icon = LucideIcons.Tv, title = stringResource(Res.string.seriesLibrary_noSeriesWatched)) }
                        activeStatus == SeriesStatus.WATCHING -> {
                            val active = filtered.filter { !isSeriesWaiting(it) }
                            val waiting = filtered.filter { isSeriesWaiting(it) }.sortedWith(compareBy(nullsLast()) { it.nextAirDate })
                            if (active.isNotEmpty()) {
                                if (waiting.isNotEmpty()) item(key = "watching-header") {
                                    SectionHeader(stringResource(Res.string.seriesLibrary_watching), icon = LucideIcons.Play, color = NookTheme.colors.blueText)
                                    Spacer(Modifier.height(16.dp))
                                }
                                seriesRows(active, viewMode, columns, keyPrefix = "active", onSelect = { selected = it }, onRemove = null, showStatus = false)
                            }
                            if (waiting.isNotEmpty()) {
                                item(key = "waiting-header") {
                                    Spacer(Modifier.height(if (active.isNotEmpty()) 32.dp else 0.dp))
                                    SectionHeader(stringResource(Res.string.seriesLibrary_waitingNextSeason), icon = LucideIcons.Clock, color = NookTheme.colors.purpleText)
                                    Spacer(Modifier.height(16.dp))
                                }
                                seriesRows(waiting, viewMode, columns, keyPrefix = "waiting", onSelect = { selected = it }, onRemove = null, showStatus = false)
                            }
                        }
                        else -> seriesRows(filtered, viewMode, columns, onSelect = { selected = it }, onRemove = null, showStatus = false)
                    }
                }
            }
        }
    }

    if (filtersOpen) {
        LibraryFiltersSheet(
            title = stringResource(Res.string.library_filters),
            groups = listOf(
                FilterGroup(stringResource(Res.string.library_genre), stringResource(Res.string.seriesLibrary_allGenres), genres, genreFilter) { genreFilter = it },
                FilterGroup(stringResource(Res.string.seriesLibrary_creator), stringResource(Res.string.seriesLibrary_allCreators), creators, creatorFilter) { creatorFilter = it },
            ),
            resetLabel = stringResource(Res.string.library_filtersReset),
            doneLabel = stringResource(Res.string.library_filtersDone),
            onReset = { genreFilter = ""; creatorFilter = "" },
            onClose = { filtersOpen = false },
        )
    }

    selected?.let { SeriesDetailSheet(container, it, onClose = { selected = null }) }
    if (showStats) SeriesStatsSheet(container, series, onClose = { showStats = false })
    pickerCategory?.let { category ->
        CategoryItemPickerSheet(
            existingIds = category.itemIds,
            items = series.map { PickerItem(it.id, it.title, it.creator, it.posterUrl) },
            labels = PickerLabels(
                header = stringResource(Res.string.seriesLibrary_addSeriesTo, category.title),
                searchPlaceholder = stringResource(Res.string.seriesLibrary_searchSeries),
                noItemsFound = stringResource(Res.string.seriesLibrary_noSeriesFound),
                alreadyInCategory = stringResource(Res.string.seriesLibrary_alreadyInCategory),
                cancel = stringResource(Res.string.seriesLibrary_cancel),
                confirmLabel = { n -> if (n > 0) stringResource(Res.string.seriesLibrary_addNSeries, n) else stringResource(Res.string.seriesLibrary_confirmAdd) },
            ),
            mode = MediaMode.SERIES,
            onConfirm = { ids -> scope.launch { container.seriesCategories.addMappedItems(category.id, ids); pickerCategory = null } },
            onClose = { pickerCategory = null },
        )
    }
}

/** Series as grid cards or list rows; [showStatus] only where statuses mix (a collection, a search). */
private fun LazyListScope.seriesRows(list: List<Series>, viewMode: ViewMode, columns: Int, keyPrefix: String = "", onSelect: (Series) -> Unit, onRemove: ((Series) -> Unit)?, showStatus: Boolean) {
    if (viewMode == ViewMode.GRID) {
        gridRows(list, columns, key = { keyPrefix + it.id }) { s -> SeriesCard(s, onClick = { onSelect(s) }, onRemove = onRemove?.let { r -> { r(s) } }, showStatus = showStatus) }
    } else {
        listRows(list, key = { keyPrefix + it.id }) { s -> SeriesListRow(s, onClick = { onSelect(s) }, onRemove = onRemove?.let { r -> { r(s) } }, showStatus = showStatus) }
    }
}
