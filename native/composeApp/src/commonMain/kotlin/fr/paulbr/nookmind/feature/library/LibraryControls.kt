package fr.paulbr.nookmind.feature.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.ChoiceChip
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.IconGhostButton
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SelectOption
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.designsystem.onFill
import fr.paulbr.nookmind.core.model.CollectionItem
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.common.SheetHeader

/**
 * Status tabs of a library: equal-width text tabs over a hairline, the active one underlined in its
 * status colour. Page tabs rather than pills, so they never read as the Series / Movies / Books
 * switch at the bottom of the screen. [activeId] matches no tab while a collection is open.
 */
@Composable
fun LibraryStatusTabs(tabs: List<LibraryTab>, activeId: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    val haptics = LocalNookHaptics.current
    val wide = LocalWideLayout.current
    Box(modifier.fillMaxWidth()) {
        HairlineDivider(Modifier.align(Alignment.BottomCenter))
        Row(Modifier.fillMaxWidth().padding(horizontal = if (wide) 0.dp else 16.dp)) {
            tabs.forEach { tab ->
                val active = tab.id == activeId
                val indicator by animateColorAsState(if (active) tab.tone.base() else Color.Transparent, label = "tabIndicator")
                Column(
                    Modifier
                        .weight(1f)
                        .clip(NookShapes.sm)
                        .selectable(selected = active, role = Role.Tab) {
                            if (!active) haptics.perform(HapticCue.TICK)
                            onSelect(tab.id)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(Modifier.padding(top = 10.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (tab.icon != null) {
                            Icon(tab.icon, null, Modifier.size(14.dp), tint = if (active) tab.tone.text() else colors.textSubtle)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            tab.label,
                            style = NookTheme.type.sans(15, if (active) FontWeight.SemiBold else FontWeight.Medium, 20),
                            color = if (active) colors.textStrong else colors.textSubtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            tab.count.toString(),
                            style = NookTheme.type.sans(12, FontWeight.SemiBold, 16),
                            color = if (active) tab.tone.text() else colors.textFaint,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth(0.64f)
                            .height(3.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(indicator),
                    )
                }
            }
        }
    }
}

/**
 * The collections of a library as chips under the status tabs: a personal way of sorting, kept
 * apart from the statuses. The open one gets its delete cross; the last chip creates a collection.
 */
@Composable
fun CollectionChipsRow(
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
    var hadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun submit() {
        val name = newName.trim()
        if (name.isEmpty()) return
        onCreateCategory(name)
        newName = ""
        creating = false
        hadFocus = false
    }

    LaunchedEffect(creating) { if (creating) runCatching { focusRequester.requestFocus() } }

    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = if (wide) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { cat ->
            val active = cat.id == activeId
            Row(
                Modifier
                    .clip(NookShapes.full)
                    .background(if (active) Palette.Amber500.alpha(0.12f) else Color.Transparent, NookShapes.full)
                    .border(1.dp, if (active) Palette.Amber500.alpha(0.6f) else colors.borderStrong, NookShapes.full)
                    .clickable { onSelect(cat.id) }
                    .padding(start = 12.dp, end = if (active) 6.dp else 12.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (active) LucideIcons.FolderOpen else LucideIcons.Folder, null, Modifier.size(14.dp), tint = if (active) colors.amberText else colors.textSubtle)
                Spacer(Modifier.width(6.dp))
                Text(cat.title, style = NookTheme.type.sans(13, FontWeight.Medium, 18), color = if (active) colors.amberText else colors.textBody2, maxLines = 1)
                Spacer(Modifier.width(6.dp))
                Text(cat.itemIds.size.toString(), style = NookTheme.type.sans(12, FontWeight.SemiBold, 16), color = if (active) colors.amberText else colors.textFaint)
                if (active) {
                    Spacer(Modifier.width(2.dp))
                    IconGhostButton(LucideIcons.X, contentDescription = null, onClick = { onDeleteCategory(cat.id) }, size = 13.dp, padding = 3.dp, tint = colors.textSubtle)
                }
            }
        }
        if (creating) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NookTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.width(168.dp).onFocusChanged {
                        if (it.hasFocus) hadFocus = true
                        else if (hadFocus && newName.isBlank()) { creating = false; hadFocus = false }
                    },
                    placeholder = newCategoryPlaceholder,
                    textStyle = NookTheme.type.sm,
                    shape = NookShapes.full,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    focusRequester = focusRequester,
                )
                IconGhostButton(LucideIcons.Check, contentDescription = null, onClick = { submit() }, size = 16.dp, padding = 4.dp, tint = colors.amberText)
                IconGhostButton(LucideIcons.X, contentDescription = null, onClick = { creating = false; newName = ""; hadFocus = false }, size = 16.dp, padding = 4.dp, tint = colors.textFaint)
            }
        } else {
            val dash = colors.borderStrong
            Row(
                Modifier
                    .clip(NookShapes.full)
                    .drawBehind {
                        drawRoundRect(
                            color = dash,
                            cornerRadius = CornerRadius(size.height / 2, size.height / 2),
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                        )
                    }
                    .clickable { creating = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LucideIcons.Plus, null, Modifier.size(14.dp), tint = colors.textSubtle)
                Spacer(Modifier.width(6.dp))
                Text(newCategoryLabel.removePrefix("+ "), style = NookTheme.type.sans(13, FontWeight.Medium, 18), color = colors.textSubtle, maxLines = 1)
            }
        }
    }
}

/** A square-ish icon button of the library header (`bg-gray-100 rounded-xl`), like the view toggle. */
@Composable
fun LibraryHeaderButton(icon: ImageVector, contentDescription: String?, onClick: () -> Unit, active: Boolean = false) {
    val colors = NookTheme.colors
    Box(
        Modifier
            .size(40.dp)
            .clip(NookShapes.xl)
            .background(colors.surfaceMuted, NookShapes.xl)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(17.dp), tint = if (active) colors.amberText else colors.textMuted)
    }
}

/** Search field of a library, under the header while its search button is on. */
@Composable
fun LibrarySearchField(query: String, onQuery: (String) -> Unit, placeholder: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    NookTextField(
        value = query,
        onValueChange = onQuery,
        modifier = modifier.fillMaxWidth(),
        placeholder = placeholder,
        shape = NookShapes.full,
        imeAction = ImeAction.Search,
        focusRequester = focus,
        leading = { Icon(LucideIcons.Search, null, Modifier.size(18.dp), tint = colors.textSubtle) },
        trailing = { IconGhostButton(LucideIcons.X, contentDescription = null, onClick = onClose, size = 16.dp, padding = 2.dp, tint = colors.textSubtle) },
    )
}

/**
 * Count of what is shown, with the sort menu and the filters button on the right: two controls in
 * place of a row of selects that ran off the screen. [onOpenFilters] null hides the filters button.
 */
@Composable
fun LibraryToolbar(
    countText: String,
    sortOptions: List<SelectOption>,
    sortKey: String,
    onSort: (String) -> Unit,
    filtersLabel: String,
    activeFilters: Int,
    onOpenFilters: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = NookTheme.colors
    var sortOpen by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(countText, style = NookTheme.type.sm, color = colors.textSubtle, modifier = Modifier.weight(1f), maxLines = 1)
        if (sortOptions.size > 1) {
            Box {
                ToolbarPill(LucideIcons.ArrowUpDown, sortOptions.firstOrNull { it.value == sortKey }?.label ?: sortOptions.first().label, onClick = { sortOpen = true })
                DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }, containerColor = colors.surface) {
                    sortOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label, style = NookTheme.type.sm, color = if (option.value == sortKey) colors.amberText else colors.textBody) },
                            onClick = { sortOpen = false; onSort(option.value) },
                        )
                    }
                }
            }
        }
        if (onOpenFilters != null) {
            Spacer(Modifier.width(8.dp))
            ToolbarPill(LucideIcons.SlidersHorizontal, filtersLabel, onClick = onOpenFilters, badge = activeFilters.takeIf { it > 0 })
        }
    }
}

@Composable
private fun ToolbarPill(icon: ImageVector, text: String, onClick: () -> Unit, badge: Int? = null) {
    val colors = NookTheme.colors
    Row(
        Modifier
            .height(34.dp)
            .clip(NookShapes.full)
            .background(colors.surfaceMuted, NookShapes.full)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = colors.textSubtle)
        Spacer(Modifier.width(6.dp))
        Text(text, style = NookTheme.type.sans(13, FontWeight.Medium, 18), color = colors.textBody2, maxLines = 1)
        if (badge != null) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(18.dp).clip(NookShapes.full).background(Palette.Amber500), contentAlignment = Alignment.Center) {
                Text(badge.toString(), style = NookTheme.type.sans(11, FontWeight.Bold, 14), color = onFill(Palette.Amber500))
            }
        }
    }
}

/** One criterion of [LibraryFiltersSheet]: "all" first, then each value as a chip. */
class FilterGroup(
    val label: String,
    val allLabel: String,
    val options: List<String>,
    val selected: String,
    val onSelect: (String) -> Unit,
)

/** The library filters (genre, author, director…) as chips in a sheet, out of the page itself. */
@Composable
fun LibraryFiltersSheet(
    title: String,
    groups: List<FilterGroup>,
    resetLabel: String,
    doneLabel: String,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = NookTheme.colors
    NookSheet(onClose = onClose, maxWidth = 560.dp, header = { controller -> SheetHeader(title, controller) }) { controller ->
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            groups.filter { it.options.isNotEmpty() }.forEach { group ->
                Column {
                    Text(group.label, style = NookTheme.type.sm, color = colors.textSubtle)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf("" to group.allLabel) + group.options.map { it to it }).forEach { (value, label) ->
                            ChoiceChip(
                                label,
                                selected = group.selected == value,
                                onClick = { group.onSelect(value) },
                                shape = NookShapes.full,
                                textStyle = NookTheme.type.sans(14, FontWeight.Medium, 20),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton(resetLabel, onClick = onReset, modifier = Modifier.weight(1f))
                PrimaryButton(doneLabel, onClick = { controller.close() }, modifier = Modifier.weight(1f))
            }
        }
    }
}
