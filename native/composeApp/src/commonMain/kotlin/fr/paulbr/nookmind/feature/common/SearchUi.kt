package fr.paulbr.nookmind.feature.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.Pill
import fr.paulbr.nookmind.core.designsystem.components.SectionHeader
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.components.StarRating
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.platform.logDebug
import fr.paulbr.nookmind.feature.shell.TABLET_BREAKPOINT_DP
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Search state of the three home screens (Home.tsx / MovieHome.tsx / SeriesHome.tsx):
 * 600 ms debounce, minimum 3 characters, dropdown visible while searching or when results exist.
 */
@Stable
class SearchController<T> internal constructor(private val minLength: Int) {
    var query by mutableStateOf("")
    var results by mutableStateOf<List<T>>(emptyList())
    var searching by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var dropdownOpen by mutableStateOf(false)

    val dropdownVisible: Boolean get() = (dropdownOpen || searching) && query.isNotEmpty()

    /** `handleCloseSearch`: clears the query and hides the dropdown. */
    fun close() {
        query = ""
        results = emptyList()
        dropdownOpen = false
    }

    fun hideDropdown() {
        dropdownOpen = false
    }

    /** Re-shows previous results when the field regains focus. */
    fun reopenIfResults() {
        if (results.isNotEmpty()) dropdownOpen = true
    }

    internal suspend fun run(q: String, search: suspend (String) -> List<T>, timeoutText: String) {
        if (q.trim().length < minLength) {
            results = emptyList()
            dropdownOpen = false
            return
        }
        searching = true
        error = null
        try {
            results = search(q)
            dropdownOpen = true
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            // The web app shows the same message whatever went wrong, so the real cause is logged:
            // a rejected API key reads exactly like a network timeout otherwise.
            logDebug("search", "search failed for \"$q\"", t)
            error = timeoutText
            dropdownOpen = false
        } finally {
            searching = false
        }
    }
}

@Composable
fun <T> rememberSearchController(
    timeoutText: String,
    debounceMs: Long = 600,
    minLength: Int = 3,
    search: suspend (String) -> List<T>,
): SearchController<T> {
    val controller = remember { SearchController<T>(minLength) }
    val latestSearch by rememberUpdatedState(search)
    val latestTimeout by rememberUpdatedState(timeoutText)
    LaunchedEffect(controller.query) {
        delay(debounceMs)
        controller.run(controller.query, latestSearch, latestTimeout)
    }
    return controller
}

/** One reorderable / hideable block of the home page (SearchSectionStack item). */
class HomeSection(val id: String, val visible: Boolean, val content: @Composable () -> Unit)

/** Search field + dropdown + sections layout shared by the three home screens. */
@Composable
fun <T> HomeScreenScaffold(
    contentPadding: PaddingValues,
    title: String,
    subtitle: String,
    titleColor: Color,
    search: SearchController<T>,
    searchPlaceholder: String,
    noResultsText: String,
    sections: List<HomeSection>,
    resultKey: (T) -> Any,
    resultRow: @Composable (T) -> Unit,
) {
    val density = LocalDensity.current
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var rootTop by remember { mutableFloatStateOf(0f) }
    var fieldBottom by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { rootTop = it.boundsInRoot().top }) {
        val wide = maxWidth >= TABLET_BREAKPOINT_DP.dp
        val navClearance = contentPadding.calculateBottomPadding()
        val minHeight = (maxHeight - navClearance) * 0.8f
        CompositionLocalProvider(LocalWideLayout provides wide) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll, enabled = !search.dropdownVisible)
                    .padding(contentPadding)
                    .padding(horizontal = 16.dp, vertical = 48.dp)
                    .heightIn(min = minHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HomeHeading(title, subtitle, titleColor, wide)
                Spacer(Modifier.height(40.dp))
                HomeSearchField(
                    query = search.query,
                    onQueryChange = { search.query = it },
                    placeholder = searchPlaceholder,
                    onClear = { search.close() },
                    onFocused = {
                        if (!wide) scope.launch { scroll.animateScrollTo(0) }
                        search.reopenIfResults()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 576.dp)
                        .onGloballyPositioned { fieldBottom = it.boundsInRoot().bottom },
                )
                search.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = NookTheme.type.sm, color = Palette.Red500, textAlign = TextAlign.Center)
                }
                SearchSectionStack(sections, Modifier.fillMaxWidth())
            }

            if (search.dropdownVisible) {
                // Tap outside the dropdown closes the search (web: mousedown outside dropdownRef).
                Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { search.close(); keyboard?.hide() } })
                val top = with(density) { (fieldBottom - rootTop).toDp() } + 8.dp
                val maxDropdownHeight = if (wide) maxHeight * 0.6f else (maxHeight - top - maxOf(navClearance, 16.dp)).coerceAtLeast(160.dp)
                Box(
                    Modifier.align(Alignment.TopCenter).offset(y = top).fillMaxWidth().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    SearchDropdownCard(search, maxDropdownHeight, noResultsText, resultKey, resultRow, onScrolled = { keyboard?.hide() })
                }
            }
        }
    }
}

/** `font-serif text-4xl md:text-5xl font-bold` + `text-lg text-gray-500 max-w-md`. */
@Composable
fun HomeHeading(title: String, subtitle: String, titleColor: Color, wide: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            title,
            style = if (wide) NookTheme.type.serif(48, FontWeight.Bold, 52) else NookTheme.type.displaySerif,
            color = titleColor,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            subtitle,
            style = NookTheme.type.lg,
            color = NookTheme.colors.textSubtle,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 448.dp),
        )
    }
}

/** `input rounded-full pl-11 pr-10 py-3.5 text-base shadow-xs` with the search icon and the clear cross. */
@Composable
fun HomeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit = {},
) {
    val colors = NookTheme.colors
    NookTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .shadow(1.dp, NookShapes.full, ambientColor = Palette.Black.alpha(0.08f), spotColor = Palette.Black.alpha(0.08f))
            .onFocusChanged { if (it.hasFocus) onFocused() },
        placeholder = placeholder,
        shape = NookShapes.full,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        leading = { Icon(LucideIcons.Search, null, Modifier.size(18.dp), tint = colors.textFaint) },
        trailing = if (query.isNotEmpty()) {
            { Icon(LucideIcons.X, null, Modifier.size(16.dp).clickable(onClick = onClear), tint = colors.textFaint) }
        } else null,
    )
}

/** The results card: skeleton rows while searching, "no results" text, or the result list. */
@Composable
fun <T> SearchDropdownCard(
    search: SearchController<T>,
    maxHeight: Dp,
    noResultsText: String,
    key: (T) -> Any,
    row: @Composable (T) -> Unit,
    onScrolled: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) onScrolled() }
    NookCard(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 576.dp)
            .heightIn(max = maxHeight)
            .shadow(24.dp, NookShapes.xl2, ambientColor = Palette.Black.alpha(0.25f), spotColor = Palette.Black.alpha(0.25f)),
    ) {
        when {
            search.searching -> SearchSkeletonRows()
            search.results.isEmpty() -> Text(
                noResultsText,
                style = NookTheme.type.sm,
                color = NookTheme.colors.textSubtle,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
            else -> LazyColumn(state = listState) {
                itemsIndexed(search.results, key = { _, item -> key(item) }) { index, item ->
                    if (index > 0) HairlineDivider(Modifier.padding(horizontal = 12.dp))
                    row(item)
                }
            }
        }
    }
}

/** Three pulsing placeholder rows (`p-3 space-y-2`). */
@Composable
fun SearchSkeletonRows() {
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBox(Modifier.size(40.dp, 56.dp), shape = NookShapes.lg)
                Column(Modifier.weight(1f).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(0.75f).height(14.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.5f).height(12.dp))
                }
            }
        }
    }
}

/** One dropdown result (`px-4 py-3 gap-3`, 40 x 56 thumbnail). */
@Composable
fun SearchResultRow(
    imageUrl: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholderIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = NookTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp, 56.dp).clip(NookShapes.lg).background(colors.surfaceMuted), contentAlignment = Alignment.Center) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(colors.surfaceMuted2), contentAlignment = Alignment.Center) {
                    if (placeholderIcon != null) Icon(placeholderIcon, null, Modifier.size(16.dp), tint = colors.textFaint)
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = NookTheme.type.xs, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

/** "In library" pill of the dropdown (`text-emerald-600 bg-emerald-500/10 px-2 py-1 rounded-full`). */
@Composable
fun AlreadyAddedPill(text: String) {
    Pill(
        text,
        background = Palette.Emerald500.alpha(0.10f),
        color = NookTheme.colors.emeraldText,
        icon = LucideIcons.CheckCircle2,
        iconSize = 12.dp,
        textStyle = NookTheme.type.sans(12, FontWeight.Medium, 16),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Port of SearchSectionStack.tsx: ordered sections, each hideable with a height + fade animation. */
@Composable
fun SearchSectionStack(sections: List<HomeSection>, modifier: Modifier = Modifier) {
    Column(modifier) {
        sections.forEach { section ->
            AnimatedVisibility(
                visible = section.visible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(40.dp))
                    section.content()
                }
            }
        }
    }
}

/** Horizontal poster slider with its uppercase section title. */
@Composable
fun PosterSlider(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    titleColor: Color = NookTheme.colors.textSubtle,
    trailing: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title, icon = icon, color = titleColor, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 8.dp)) { content() }
    }
}

/** One slider card: 2:3 poster, single-line title, optional stars, optional badge overlay. */
@Composable
fun PosterSlideCard(
    imageUrl: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = posterSlideWidth(),
    rating: Double? = null,
    mode: MediaMode = NookTheme.mode,
    titleLines: Int = 1,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    Column(modifier.width(width).clip(NookShapes.lg).clickable(onClick = onClick)) {
        MediaImage(imageUrl, title, Modifier.fillMaxWidth(), mode = mode, placeholderIconSize = 22.dp, overlay = badge)
        Spacer(Modifier.height(8.dp))
        Text(title, style = NookTheme.type.sans(12, FontWeight.Medium, 14), color = NookTheme.colors.textBody, maxLines = titleLines, overflow = TextOverflow.Ellipsis)
        if (rating != null) {
            Spacer(Modifier.height(2.dp))
            StarRating(rating, size = 10.dp)
        }
    }
}

/** Small corner badge on a slider poster (`text-[10px] font-bold text-white px-1.5 py-0.5 rounded-md`). */
@Composable
fun BoxScope.PosterCornerBadge(text: String, color: Color) {
    Text(
        text,
        style = NookTheme.type.sans(10, FontWeight.Bold, 14),
        color = Palette.White,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(6.dp)
            .clip(NookShapes.md)
            .background(color, NookShapes.md)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        maxLines = 1,
    )
}
