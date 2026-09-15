package fr.paulbr.nookmind.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.data.ViewMode
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.CountBadge
import fr.paulbr.nookmind.core.designsystem.components.EmptyState
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.InlineBanner
import fr.paulbr.nookmind.core.designsystem.components.BannerTone
import fr.paulbr.nookmind.core.designsystem.components.MediaImage
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SkeletonBox
import fr.paulbr.nookmind.core.designsystem.components.TextLink
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.CollectionItem
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.common.LocalWideLayout

/** Colour of a status tab (`amber` default, `blue` watching, `emerald` watched). */
enum class TabTone { AMBER, BLUE, EMERALD }

@Composable
fun TabTone.base(): Color = when (this) {
    TabTone.AMBER -> Palette.Amber500
    TabTone.BLUE -> Palette.Blue500
    TabTone.EMERALD -> Palette.Emerald500
}

@Composable
fun TabTone.text(): Color = when (this) {
    TabTone.AMBER -> NookTheme.colors.amberText
    TabTone.BLUE -> NookTheme.colors.blueText
    TabTone.EMERALD -> NookTheme.colors.emeraldText
}

/** A built-in status tab of a library. */
data class LibraryTab(val id: String, val label: String, val count: Int, val tone: TabTone = TabTone.AMBER, val icon: ImageVector? = null)

/** `grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5`. */
fun libraryColumns(maxWidth: Dp): Int = when {
    maxWidth < 768.dp -> 2
    maxWidth < 1024.dp -> 3
    maxWidth < 1280.dp -> 4
    else -> 5
}

/** `p-4 md:p-8` page padding. */
@Composable
fun libraryPagePadding(): Dp = if (LocalWideLayout.current) 32.dp else 16.dp

/** Max content width of the library pages (`max-w-6xl`). */
val LIBRARY_MAX_WIDTH = 1152.dp

/** Title + count + view toggle (+ extra actions) of the library pages. */
@Composable
fun LibraryHeader(
    title: String,
    titleColor: Color,
    subtitle: String,
    viewMode: ViewMode,
    onViewMode: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, style = NookTheme.type.h1Serif, color = titleColor)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = NookTheme.type.sm, color = NookTheme.colors.textSubtle)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions?.invoke(this)
            ViewModeToggle(viewMode, onViewMode)
        }
    }
}

/** `bg-gray-100 rounded-xl p-1` segmented grid / list toggle. */
@Composable
fun ViewModeToggle(viewMode: ViewMode, onViewMode: (ViewMode) -> Unit, modifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    Row(
        modifier.clip(NookShapes.xl).background(colors.surfaceMuted, NookShapes.xl).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(ViewMode.GRID to LucideIcons.LayoutGrid, ViewMode.LIST to LucideIcons.List).forEach { (mode, icon) ->
            val active = viewMode == mode
            Box(
                Modifier
                    .then(if (active) Modifier.shadow(1.dp, NookShapes.lg, ambientColor = Palette.Black.alpha(0.1f), spotColor = Palette.Black.alpha(0.1f)) else Modifier)
                    .clip(NookShapes.lg)
                    .background(if (active) colors.surface else Color.Transparent, NookShapes.lg)
                    .clickable { onViewMode(mode) }
                    .padding(8.dp),
            ) {
                Icon(icon, contentDescription = mode.key, modifier = Modifier.size(16.dp), tint = if (active) colors.amberText else colors.textFaint)
            }
        }
    }
}

/**
 * Status tabs + collection tabs + "New collection" affordance, horizontally scrollable, over a
 * hairline. The delete cross of a collection shows on its active tab (the web shows it on hover).
 */
@Composable
fun LibraryTabsRow(
    tabs: List<LibraryTab>,
    categories: List<CollectionItem>,
    activeId: String,
    onSelect: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    newCategoryLabel: String,
    newCategoryPlaceholder: String,
    onCreateCategory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NookTheme.colors
    val wide = LocalWideLayout.current
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    fun submit() {
        val name = newName.trim()
        if (name.isEmpty()) return
        onCreateCategory(name)
        newName = ""
        creating = false
        hadFocus = false
    }

    LaunchedEffect(creating) { if (creating) runCatching { focusRequester.requestFocus() } }

    Box(modifier.fillMaxWidth()) {
        HairlineDivider(Modifier.align(Alignment.BottomCenter))
        Row(
            Modifier.horizontalScroll(scroll).padding(horizontal = if (wide) 0.dp else 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEach { tab ->
                val active = tab.id == activeId
                TabButton(active = active, onClick = { onSelect(tab.id) }, indicator = tab.tone.base()) {
                    if (tab.icon != null) {
                        Icon(tab.icon, null, Modifier.size(13.dp), tint = if (active) tab.tone.text() else colors.textSubtle)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(tab.label, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = if (active) tab.tone.text() else colors.textSubtle, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    CountBadge(tab.count, active, activeColor = tab.tone.base(), activeText = tab.tone.text())
                }
            }
            categories.forEach { cat ->
                val active = cat.id == activeId
                TabButton(active = active, onClick = { onSelect(cat.id) }, indicator = Palette.Amber500) {
                    Text(cat.title, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = if (active) colors.amberText else colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(IntrinsicWidthLimit))
                    Spacer(Modifier.width(6.dp))
                    CountBadge(cat.itemIds.size, active)
                    if (active) {
                        Spacer(Modifier.width(4.dp))
                        IconGhostButton(LucideIcons.X, contentDescription = null, onClick = { onDeleteCategory(cat.id) }, size = 12.dp, padding = 2.dp, tint = colors.textFaint)
                    }
                }
            }
            if (creating) {
                Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NookTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        modifier = Modifier.width(144.dp).onFocusChanged {
                            if (it.hasFocus) hadFocus = true
                            else if (hadFocus && newName.isBlank()) { creating = false; hadFocus = false }
                        },
                        placeholder = newCategoryPlaceholder,
                        textStyle = NookTheme.type.sm,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        focusRequester = focusRequester,
                    )
                    IconGhostButton(LucideIcons.Check, contentDescription = null, onClick = { submit() }, size = 16.dp, padding = 4.dp, tint = colors.amberText)
                    IconGhostButton(LucideIcons.X, contentDescription = null, onClick = { creating = false; newName = ""; hadFocus = false }, size = 16.dp, padding = 4.dp, tint = colors.textFaint)
                }
            } else {
                TabButton(active = false, onClick = { creating = true }, indicator = Color.Transparent) {
                    Icon(LucideIcons.Plus, null, Modifier.size(14.dp), tint = colors.textFaint)
                    Spacer(Modifier.width(4.dp))
                    Text(newCategoryLabel.removePrefix("+ "), style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textFaint, maxLines = 1)
                }
            }
        }
    }
}

/** Widest a collection tab label can be before it ellipsizes. */
private val IntrinsicWidthLimit = Dp.Unspecified

@Composable
private fun TabButton(active: Boolean, onClick: () -> Unit, indicator: Color, content: @Composable RowScope.() -> Unit) {
    Column(Modifier.clip(NookShapes.sm).clickable(onClick = onClick).padding(horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (active) indicator else Color.Transparent))
    }
}

/** Red confirmation banner shown above the content when deleting a collection. */
@Composable
fun DeleteCategoryBanner(text: String, yesText: String, cancelText: String, onConfirm: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    InlineBanner(
        text,
        modifier = modifier,
        tone = BannerTone.ERROR,
        icon = LucideIcons.Trash2,
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextLink(yesText, onClick = onConfirm, color = Palette.Red600, weight = FontWeight.Medium)
                TextLink(cancelText, onClick = onCancel, color = NookTheme.colors.textSubtle)
            }
        },
    )
}

/** "N items" + "Add …" primary button of a collection tab. */
@Composable
fun CategoryToolbar(countText: String, addLabel: String, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(countText, style = NookTheme.type.sm, color = NookTheme.colors.textSubtle, modifier = Modifier.weight(1f))
        PrimaryButton(addLabel, onClick = onAdd, icon = LucideIcons.Plus, iconSize = 15.dp, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp))
    }
}

/** Horizontally scrolling row of filter selects. */
@Composable
fun FilterRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val wide = LocalWideLayout.current
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = if (wide) 0.dp else 16.dp).padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Pulsing placeholder card of the loading grid. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    NookCard(modifier) {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(2f / 3f), shape = NookShapes.xl)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.8f).height(14.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(12.dp))
        }
    }
}

fun LazyListScope.skeletonGrid(columns: Int, count: Int = 8) {
    gridRows(List(count) { it }, columns, key = { "skeleton-$it" }) { SkeletonCard() }
}

/** Lays [items] out as grid rows of [columns] cells (`gap-4`). */
fun <T> LazyListScope.gridRows(items: List<T>, columns: Int, key: (T) -> Any, spacing: Dp = 16.dp, itemContent: @Composable (T) -> Unit) {
    val rows = items.chunked(columns)
    rows.forEachIndexed { rowIndex, chunk ->
        item(key = "row-" + key(chunk.first()).toString()) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = if (rowIndex < rows.lastIndex) spacing else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                chunk.forEach { Box(Modifier.weight(1f)) { itemContent(it) } }
                repeat(columns - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

enum class SegmentPosition { FIRST, MIDDLE, LAST, SINGLE }

/** Lays [items] out as the rows of one `card divide-y` container, lazily. */
fun <T> LazyListScope.listRows(items: List<T>, key: (T) -> Any, rowContent: @Composable (T) -> Unit) {
    items.forEachIndexed { index, item ->
        val position = when {
            items.size == 1 -> SegmentPosition.SINGLE
            index == 0 -> SegmentPosition.FIRST
            index == items.lastIndex -> SegmentPosition.LAST
            else -> SegmentPosition.MIDDLE
        }
        item(key = "list-" + key(item).toString()) {
            ListCardSegment(position) { rowContent(item) }
        }
    }
}

/** One slice of a card: background, side borders, rounded ends, hairline between rows. */
@Composable
fun ListCardSegment(position: SegmentPosition, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = NookTheme.colors
    val radius = 16.dp
    val shape = when (position) {
        SegmentPosition.SINGLE -> RoundedCornerShape(radius)
        SegmentPosition.FIRST -> RoundedCornerShape(topStart = radius, topEnd = radius)
        SegmentPosition.LAST -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        SegmentPosition.MIDDLE -> RoundedCornerShape(0.dp)
    }
    val border = colors.border
    val divider = colors.divider
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface, shape)
            .drawBehind {
                val stroke = 1.dp.toPx()
                val r = radius.toPx()
                val extend = r * 2
                val top = if (position == SegmentPosition.FIRST || position == SegmentPosition.SINGLE) 0f else -extend
                val bottom = if (position == SegmentPosition.LAST || position == SegmentPosition.SINGLE) size.height else size.height + extend
                drawRoundRect(
                    color = border,
                    topLeft = Offset(stroke / 2f, top + stroke / 2f),
                    size = Size(size.width - stroke, bottom - top - stroke),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(stroke),
                )
                if (position == SegmentPosition.MIDDLE || position == SegmentPosition.LAST) {
                    drawLine(divider, Offset(0f, stroke / 2f), Offset(size.width, stroke / 2f), stroke)
                }
            },
    ) {
        content()
    }
}

/**
 * Row of the list view: 40 x 56 thumbnail, serif title, subtitle, then [trailing] (genre, stars,
 * status pill) and the optional remove cross of a collection.
 */
@Composable
fun LibraryListRow(
    imageUrl: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mode: MediaMode = NookTheme.mode,
    onRemove: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    val colors = NookTheme.colors
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MediaImage(imageUrl, title, Modifier.width(40.dp), mode = mode, shape = NookShapes.lg, placeholderIconSize = 16.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = NookTheme.type.cardTitleSerif, color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = NookTheme.type.xs, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
        if (onRemove != null) {
            IconGhostButton(LucideIcons.X, contentDescription = null, onClick = onRemove, size = 14.dp, padding = 6.dp, tint = colors.textFaint, shape = NookShapes.lg)
        }
    }
}

/** Small "remove from collection" cross over a grid card (`top-2 left-2 bg-black/50 rounded-full`). */
@Composable
fun CardRemoveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.padding(8.dp).size(20.dp).clip(NookShapes.full).background(Palette.Black.alpha(0.5f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(LucideIcons.X, null, Modifier.size(11.dp), tint = Palette.White)
    }
}

/** Empty state of a collection tab, with the "Add …" call to action. */
@Composable
fun CategoryEmptyState(title: String, description: String, addLabel: String, onAdd: () -> Unit) {
    EmptyState(
        icon = LucideIcons.FolderOpen,
        title = title,
        description = description,
        action = { PrimaryButton(addLabel, onClick = onAdd, icon = LucideIcons.Plus, iconSize = 15.dp) },
    )
}
