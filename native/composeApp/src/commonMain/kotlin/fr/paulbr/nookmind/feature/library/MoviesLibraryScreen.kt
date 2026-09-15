package fr.paulbr.nookmind.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.ViewMode
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.EmptyState
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.NookSelect
import fr.paulbr.nookmind.core.designsystem.components.SelectOption
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.domain.yearOf
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.Movie
import fr.paulbr.nookmind.core.model.MovieCategory
import fr.paulbr.nookmind.core.model.MovieStatus
import fr.paulbr.nookmind.feature.collections.CategoryItemPickerSheet
import fr.paulbr.nookmind.feature.collections.PickerItem
import fr.paulbr.nookmind.feature.collections.PickerLabels
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.horizontalBleed
import fr.paulbr.nookmind.feature.movies.MovieCard
import fr.paulbr.nookmind.feature.movies.MovieDetailSheet
import fr.paulbr.nookmind.feature.movies.MovieListRow
import fr.paulbr.nookmind.feature.shell.TABLET_BREAKPOINT_DP
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_collectionDeleted
import fr.paulbr.nookmind.resources.common_itemsCountMovies
import fr.paulbr.nookmind.resources.movieLibrary_addMovies
import fr.paulbr.nookmind.resources.movieLibrary_addMoviesTo
import fr.paulbr.nookmind.resources.movieLibrary_addNMovies
import fr.paulbr.nookmind.resources.movieLibrary_allDirectors
import fr.paulbr.nookmind.resources.movieLibrary_allGenres
import fr.paulbr.nookmind.resources.movieLibrary_alreadyInCategory
import fr.paulbr.nookmind.resources.movieLibrary_cancel
import fr.paulbr.nookmind.resources.movieLibrary_categoryEmpty
import fr.paulbr.nookmind.resources.movieLibrary_categoryEmptyDesc
import fr.paulbr.nookmind.resources.movieLibrary_confirmAdd
import fr.paulbr.nookmind.resources.movieLibrary_confirmDeleteCategory
import fr.paulbr.nookmind.resources.movieLibrary_dateAdded
import fr.paulbr.nookmind.resources.movieLibrary_directorAZ
import fr.paulbr.nookmind.resources.movieLibrary_moviesCount
import fr.paulbr.nookmind.resources.movieLibrary_newCategory
import fr.paulbr.nookmind.resources.movieLibrary_newCategoryPlaceholder
import fr.paulbr.nookmind.resources.movieLibrary_noDateGroup
import fr.paulbr.nookmind.resources.movieLibrary_noMoviesFound
import fr.paulbr.nookmind.resources.movieLibrary_noMoviesWatched
import fr.paulbr.nookmind.resources.movieLibrary_ratingDesc
import fr.paulbr.nookmind.resources.movieLibrary_searchMovies
import fr.paulbr.nookmind.resources.movieLibrary_title
import fr.paulbr.nookmind.resources.movieLibrary_titleAZ
import fr.paulbr.nookmind.resources.movieLibrary_wantToWatch
import fr.paulbr.nookmind.resources.movieLibrary_watched
import fr.paulbr.nookmind.resources.movieLibrary_watchedDate
import fr.paulbr.nookmind.resources.movieLibrary_yearCount
import fr.paulbr.nookmind.resources.movieLibrary_yesDelete
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private enum class MovieSort(val key: String) { CREATED("created_at"), TITLE("title"), DIRECTOR("director"), RATING("rating"), WATCHED_DATE("watched_date") }

/** Port of MovieLibrary.tsx: status / collection tabs, filters, grid or list, year groups for watched movies. */
@Composable
fun MoviesLibraryScreen(container: AppContainer, contentPadding: PaddingValues) {
    val movies by container.movies.items.collectAsState()
    val loading by container.movies.loading.collectAsState()
    val categories by container.movieCategories.items.collectAsState()
    val scope = rememberCoroutineScope()

    var activeTab by rememberSaveable { mutableStateOf(MovieStatus.WANT_TO_WATCH.key) }
    var genreFilter by rememberSaveable { mutableStateOf("") }
    var directorFilter by rememberSaveable { mutableStateOf("") }
    var sortKey by rememberSaveable { mutableStateOf(MovieSort.CREATED.key) }
    var viewMode by remember { mutableStateOf(container.prefs.viewMode(MediaMode.MOVIES)) }
    var selected by remember { mutableStateOf<Movie?>(null) }
    var pickerCategory by remember { mutableStateOf<MovieCategory?>(null) }
    var deletingCategoryId by remember { mutableStateOf<String?>(null) }

    val activeCategory = categories.firstOrNull { it.id == activeTab }
    val activeStatus = MovieStatus.entries.firstOrNull { it.key == activeTab }
    val isStatusTab = activeStatus != null
    val tabMovies = if (activeStatus != null) movies.filter { it.status == activeStatus } else emptyList()
    val genres = tabMovies.mapNotNull { it.genre?.takeIf(String::isNotBlank) }.distinct()
    val directors = tabMovies.map { it.director }.filter { it.isNotBlank() }.distinct()
    val sort = MovieSort.entries.first { it.key == sortKey }
    val filtered = tabMovies
        .filter { genreFilter.isEmpty() || it.genre == genreFilter }
        .filter { directorFilter.isEmpty() || it.director == directorFilter }
        .let { list ->
            when (sort) {
                MovieSort.CREATED -> list.sortedByDescending { it.createdAt }
                MovieSort.TITLE -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                MovieSort.DIRECTOR -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.director })
                MovieSort.RATING -> list.sortedByDescending { it.rating ?: 0.0 }
                MovieSort.WATCHED_DATE -> list.sortedWith(compareByDescending<Movie> { it.watchedDate != null }.thenByDescending { it.watchedDate ?: "" })
            }
        }
    val categoryMovies = activeCategory?.let { cat -> movies.filter { it.id in cat.itemIds } } ?: emptyList()

    val tabs = listOf(
        LibraryTab(MovieStatus.WANT_TO_WATCH.key, stringResource(Res.string.movieLibrary_wantToWatch), movies.count { it.status == MovieStatus.WANT_TO_WATCH }),
        LibraryTab(MovieStatus.WATCHED.key, stringResource(Res.string.movieLibrary_watched), movies.count { it.status == MovieStatus.WATCHED }),
    )
    val sortOptions = buildList {
        if (activeStatus == MovieStatus.WATCHED) add(SelectOption(MovieSort.WATCHED_DATE.key, stringResource(Res.string.movieLibrary_watchedDate)))
        add(SelectOption(MovieSort.CREATED.key, stringResource(Res.string.movieLibrary_dateAdded)))
        add(SelectOption(MovieSort.TITLE.key, stringResource(Res.string.movieLibrary_titleAZ)))
        add(SelectOption(MovieSort.DIRECTOR.key, stringResource(Res.string.movieLibrary_directorAZ)))
        if (activeStatus == MovieStatus.WATCHED) add(SelectOption(MovieSort.RATING.key, stringResource(Res.string.movieLibrary_ratingDesc)))
    }
    if (sortOptions.none { it.value == sortKey }) sortKey = MovieSort.CREATED.key
    val noDateGroup = stringResource(Res.string.movieLibrary_noDateGroup)

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
                            title = stringResource(Res.string.movieLibrary_title),
                            titleColor = NookTheme.colors.indigoText,
                            subtitle = pluralStringResource(Res.plurals.movieLibrary_moviesCount, movies.size, movies.size),
                            viewMode = viewMode,
                            onViewMode = { viewMode = it; container.prefs.setViewMode(MediaMode.MOVIES, it) },
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
                                MovieStatus.entries.firstOrNull { it.key == id }?.let { status ->
                                    genreFilter = ""; directorFilter = ""
                                    sortKey = if (status == MovieStatus.WATCHED) MovieSort.WATCHED_DATE.key else MovieSort.CREATED.key
                                }
                            },
                            onDeleteCategory = { deletingCategoryId = it },
                            newCategoryLabel = stringResource(Res.string.movieLibrary_newCategory),
                            newCategoryPlaceholder = stringResource(Res.string.movieLibrary_newCategoryPlaceholder),
                            onCreateCategory = { name -> scope.launch { container.movieCategories.create(name)?.let { activeTab = it.id } } },
                            modifier = if (wide) Modifier else Modifier.horizontalBleed(pad),
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                    deletingCategoryId?.let { id ->
                        val cat = categories.firstOrNull { it.id == id }
                        if (cat != null) item(key = "delete-banner") {
                            DeleteCategoryBanner(
                                text = stringResource(Res.string.movieLibrary_confirmDeleteCategory, cat.title),
                                yesText = stringResource(Res.string.movieLibrary_yesDelete),
                                cancelText = stringResource(Res.string.movieLibrary_cancel),
                                onConfirm = {
                                    if (activeTab == id) activeTab = MovieStatus.WANT_TO_WATCH.key
                                    deletingCategoryId = null
                                    scope.launch { if (container.movieCategories.delete(id)) container.toasts.success(Res.string.common_collectionDeleted, cat.title) }
                                },
                                onCancel = { deletingCategoryId = null },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (activeCategory != null) {
                        item(key = "category-toolbar") {
                            CategoryToolbar(
                                countText = pluralStringResource(Res.plurals.common_itemsCountMovies, activeCategory.itemIds.size, activeCategory.itemIds.size),
                                addLabel = stringResource(Res.string.movieLibrary_addMovies),
                                onAdd = { pickerCategory = activeCategory },
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (isStatusTab && tabMovies.isNotEmpty()) {
                        item(key = "filters") {
                            FilterRow(if (wide) Modifier else Modifier.horizontalBleed(pad)) {
                                NookSelect(genreFilter, listOf(SelectOption("", stringResource(Res.string.movieLibrary_allGenres))) + genres.map { SelectOption(it, it) }, onChange = { genreFilter = it })
                                NookSelect(directorFilter, listOf(SelectOption("", stringResource(Res.string.movieLibrary_allDirectors))) + directors.map { SelectOption(it, it) }, onChange = { directorFilter = it })
                                NookSelect(sortKey, sortOptions, onChange = { sortKey = it })
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    val onRemoveFromCategory: ((Movie) -> Unit)? = activeCategory?.let { cat -> { m -> scope.launch { container.movieCategories.removeMappedItem(cat.id, m.id) } } }
                    when {
                        loading -> skeletonGrid(columns)
                        activeCategory != null -> when {
                            categoryMovies.isEmpty() -> item(key = "empty-category") {
                                CategoryEmptyState(
                                    title = stringResource(Res.string.movieLibrary_categoryEmpty),
                                    description = stringResource(Res.string.movieLibrary_categoryEmptyDesc),
                                    addLabel = stringResource(Res.string.movieLibrary_addMovies),
                                    onAdd = { pickerCategory = activeCategory },
                                )
                            }
                            else -> movieRows(categoryMovies, viewMode, columns, onSelect = { selected = it }, onRemove = onRemoveFromCategory)
                        }
                        filtered.isEmpty() -> item(key = "empty") { EmptyState(icon = LucideIcons.Film, title = stringResource(Res.string.movieLibrary_noMoviesWatched)) }
                        activeStatus == MovieStatus.WATCHED && sort == MovieSort.WATCHED_DATE -> {
                            val groups = LinkedHashMap<String, MutableList<Movie>>()
                            filtered.forEach { m -> groups.getOrPut(yearOf(m.watchedDate) ?: noDateGroup) { ArrayList() } += m }
                            groups.entries.forEachIndexed { index, (year, list) ->
                                item(key = "year-$year") {
                                    if (index > 0) Spacer(Modifier.height(32.dp))
                                    YearDivider(year, list.size)
                                }
                                movieRows(list, viewMode, columns, keyPrefix = year, onSelect = { selected = it }, onRemove = null)
                            }
                        }
                        else -> movieRows(filtered, viewMode, columns, onSelect = { selected = it }, onRemove = null)
                    }
                }
            }
        }
    }

    selected?.let { MovieDetailSheet(container, it, onClose = { selected = null }) }
    pickerCategory?.let { category ->
        CategoryItemPickerSheet(
            existingIds = category.itemIds,
            items = movies.map { PickerItem(it.id, it.title, it.director, it.posterUrl) },
            labels = PickerLabels(
                header = stringResource(Res.string.movieLibrary_addMoviesTo, category.title),
                searchPlaceholder = stringResource(Res.string.movieLibrary_searchMovies),
                noItemsFound = stringResource(Res.string.movieLibrary_noMoviesFound),
                alreadyInCategory = stringResource(Res.string.movieLibrary_alreadyInCategory),
                cancel = stringResource(Res.string.movieLibrary_cancel),
                confirmLabel = { n -> if (n > 0) stringResource(Res.string.movieLibrary_addNMovies, n) else stringResource(Res.string.movieLibrary_confirmAdd) },
            ),
            mode = MediaMode.MOVIES,
            onConfirm = { ids -> scope.launch { container.movieCategories.addMappedItems(category.id, ids); pickerCategory = null } },
            onClose = { pickerCategory = null },
        )
    }
}

private fun LazyListScope.movieRows(list: List<Movie>, viewMode: ViewMode, columns: Int, keyPrefix: String = "", onSelect: (Movie) -> Unit, onRemove: ((Movie) -> Unit)?) {
    if (viewMode == ViewMode.GRID) {
        gridRows(list, columns, key = { keyPrefix + it.id }) { m -> MovieCard(m, onClick = { onSelect(m) }, onRemove = onRemove?.let { r -> { r(m) } }) }
    } else {
        listRows(list, key = { keyPrefix + it.id }) { m -> MovieListRow(m, onClick = { onSelect(m) }, onRemove = onRemove?.let { r -> { r(m) } }) }
    }
}

/** `YearDivider` of MovieLibrary.tsx: year · count and a hairline. */
@Composable
fun YearDivider(year: String, count: Int) {
    val colors = NookTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(year, style = NookTheme.type.sans(12, FontWeight.SemiBold, 16).copy(letterSpacing = 0.3.sp), color = colors.textFaint)
        Text("·", style = NookTheme.type.xs, color = colors.textDisabled)
        Text(pluralStringResource(Res.plurals.movieLibrary_yearCount, count, count), style = NookTheme.type.xs, color = colors.textFaint)
        HairlineDivider(Modifier.weight(1f))
    }
}
