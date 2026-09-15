package fr.paulbr.nookmind.feature.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.modeIcon
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.feature.common.SheetHeader

/** One selectable row of the picker. */
data class PickerItem(val id: String, val title: String, val subtitle: String, val imageUrl: String?)

/** Copy of the picker, provided per media type (books / movies / series). */
class PickerLabels(
    val header: String,
    val searchPlaceholder: String,
    val noItemsFound: String,
    val alreadyInCategory: String,
    val cancel: String,
    val confirmLabel: @Composable (newCount: Int) -> String,
)

/**
 * Port of CategoryItemPickerModal.tsx: multi-select of library items to add to a collection,
 * with a text filter, "Already added" markers and a count on the confirm button.
 */
@Composable
fun CategoryItemPickerSheet(
    existingIds: List<String>,
    items: List<PickerItem>,
    labels: PickerLabels,
    mode: MediaMode,
    onConfirm: (newIds: List<String>) -> Unit,
    onClose: () -> Unit,
) {
    val colors = NookTheme.colors
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(existingIds.toSet()) }
    val focusRequester = remember { FocusRequester() }
    val existing = remember(existingIds) { existingIds.toSet() }
    val filtered = remember(items, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) items else items.filter { it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q) }
    }
    val newIds = selected.filter { it !in existing }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    NookSheet(onClose = onClose, scrollable = false, maxWidth = 512.dp) { controller ->
        SheetHeader(
            labels.header, controller,
            padding = PaddingValues(20.dp),
            titleStyle = NookTheme.type.titleSerif,
        )
        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            NookTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = labels.searchPlaceholder,
                textStyle = NookTheme.type.sm,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                leading = { Icon(LucideIcons.Search, null, Modifier.size(15.dp), tint = colors.textFaint) },
                focusRequester = focusRequester,
            )
        }
        if (filtered.isEmpty()) {
            Text(
                labels.noItemsFound,
                style = NookTheme.type.sm,
                color = colors.textFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            )
        } else {
            LazyColumn(Modifier.weight(1f, fill = false).padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                items(filtered, key = { it.id }) { item ->
                    val isSelected = item.id in selected
                    val alreadyIn = item.id in existing
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(NookShapes.xl)
                            .background(if (isSelected) Palette.Amber500.alpha(if (colors.isDark) 0.15f else 0.10f) else Color.Transparent, NookShapes.xl)
                            .clickable { selected = if (isSelected) selected - item.id else selected + item.id }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(36.dp, 48.dp).clip(NookShapes.lg).background(colors.surfaceMuted), contentAlignment = Alignment.Center) {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(model = item.imageUrl, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(modeIcon(mode), null, Modifier.size(14.dp), tint = colors.textFaint)
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textStrong, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.subtitle, style = NookTheme.type.xs, color = colors.textSubtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (alreadyIn) {
                            Text(labels.alreadyInCategory, style = NookTheme.type.sans(12, FontWeight.Medium, 16), color = colors.emeraldText)
                        } else {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(NookShapes.md)
                                    .background(if (isSelected) Palette.Amber500 else Color.Transparent, NookShapes.md)
                                    .border(2.dp, if (isSelected) Palette.Amber500 else if (colors.isDark) Palette.Gray600 else Palette.Gray300, NookShapes.md),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) Icon(LucideIcons.Check, null, Modifier.size(12.dp), tint = Palette.White)
                            }
                        }
                    }
                }
            }
        }
        HairlineDivider()
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GhostButton(labels.cancel, onClick = { controller.close() }, modifier = Modifier.weight(1f))
            PrimaryButton(labels.confirmLabel(newIds.size), onClick = { onConfirm(newIds) }, modifier = Modifier.weight(1f), enabled = newIds.isNotEmpty())
        }
    }
}
