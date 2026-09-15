package fr.paulbr.nookmind.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.PillTab
import fr.paulbr.nookmind.core.designsystem.components.SectionHeader
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.network.TmdbApi
import kotlinx.coroutines.flow.distinctUntilChanged

/** One tab of the discover slider and the TMDB page fetcher behind it. */
class TrendingCategory<T>(val key: String, val label: String, val fetch: suspend (page: Int) -> TmdbApi.Page<T>)

/**
 * Port of TrendingMoviesSlider.tsx / TrendingSeriesSlider.tsx: amber "Discover" header, category
 * pills, horizontally paged TMDB results (up to 5 pages, loaded as the end comes into view), items
 * already in the library filtered out, pulsing skeletons while loading.
 */
@Composable
fun <T> TrendingSlider(
    title: String,
    categories: List<TrendingCategory<T>>,
    trackedIds: Set<Int>,
    idOf: (T) -> Int,
    titleOf: (T) -> String,
    posterOf: (T) -> String?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    mode: MediaMode = NookTheme.mode,
) {
    val colors = NookTheme.colors
    var categoryKey by rememberSaveable { mutableStateOf(categories.first().key) }
    val category = categories.firstOrNull { it.key == categoryKey } ?: categories.first()
    var items by remember { mutableStateOf<List<T>>(emptyList()) }
    var hasMore by remember { mutableStateOf(true) }
    var initialLoading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var nextPage by remember { mutableIntStateOf(1) }
    var loadRequests by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val width = posterSlideWidth()

    // Category change: reset and fetch the first page.
    LaunchedEffect(categoryKey) {
        items = emptyList()
        hasMore = true
        initialLoading = true
        loadingMore = false
        nextPage = 1
        listState.scrollToItem(0)
        val page = category.fetch(1)
        items = page.results.distinctBy(idOf)
        hasMore = page.hasMore
        nextPage = 2
        initialLoading = false
    }

    // Subsequent pages, triggered by the sentinel (last visible item near the end).
    LaunchedEffect(loadRequests) {
        if (loadRequests == 0 || !hasMore || initialLoading || loadingMore) return@LaunchedEffect
        loadingMore = true
        val page = category.fetch(nextPage)
        val known = items.map(idOf).toHashSet()
        items = items + page.results.filter { idOf(it) !in known }
        hasMore = page.hasMore
        nextPage += 1
        loadingMore = false
    }

    LaunchedEffect(listState, items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { last ->
                val visibleCount = items.count { idOf(it) !in trackedIds }
                if (last >= 0 && last >= visibleCount - 2 && hasMore && !loadingMore && !initialLoading) loadRequests += 1
            }
    }

    Column(modifier.fillMaxWidth()) {
        SectionHeader(title, icon = LucideIcons.Flame, color = if (colors.isDark) Palette.Amber400 else Palette.Amber500)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat -> PillTab(cat.label, selected = cat.key == categoryKey, onClick = { categoryKey = cat.key }) }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
            if (initialLoading) {
                items(8) { SliderSkeletonCard(width) }
            } else {
                items(items.filter { idOf(it) !in trackedIds }, key = idOf) { item ->
                    PosterSlideCard(posterOf(item), titleOf(item), onClick = { onSelect(item) }, width = width, mode = mode, titleLines = 2)
                }
                if (loadingMore) items(4) { SliderSkeletonCard(width) }
            }
        }
    }
}

/** Pulsing poster + title line placeholder of the sliders. */
@Composable
fun SliderSkeletonCard(width: Dp = posterSlideWidth()) {
    Column(Modifier.width(width)) {
        SkeletonBox(Modifier.fillMaxWidth().height(width * 1.5f), shape = NookShapes.xl, color = NookTheme.colors.surfaceMuted)
        Spacer(Modifier.height(8.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(12.dp), shape = NookShapes.sm, color = NookTheme.colors.surfaceMuted)
    }
}
