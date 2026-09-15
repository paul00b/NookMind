package fr.paulbr.nookmind.feature.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.ChoiceChip
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.components.SheetController
import fr.paulbr.nookmind.core.designsystem.components.TextLink
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons

/** Sheet title row with the close cross and a hairline (`p-6 pb-4 border-b`). */
@Composable
fun SheetHeader(
    title: String,
    controller: SheetController,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
    titleStyle: TextStyle = NookTheme.type.h3Serif,
    divider: Boolean = true,
    subtitle: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(padding), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = titleStyle, color = NookTheme.colors.textStrong)
                if (subtitle != null) Text(subtitle, style = NookTheme.type.xs, color = NookTheme.colors.textSubtle)
            }
            SheetCloseButton(controller, size = 18.dp)
        }
        if (divider) HairlineDivider()
    }
}

/**
 * Status selector of the add forms and detail sheets. [compact] is the `flex-1 py-1.5 text-xs`
 * variant of the forms; otherwise `text-sm px-3 py-1.5` chips that wrap.
 */
@Composable
fun StatusChipRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    selectedColor: Color = Palette.Amber500,
) {
    if (compact) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) ->
                ChoiceChip(label, selected = key == selected, onClick = { onSelect(key) }, modifier = Modifier.weight(1f), selectedColor = selectedColor)
            }
        }
    } else {
        FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (key, label) ->
                ChoiceChip(
                    label,
                    selected = key == selected,
                    onClick = { onSelect(key) },
                    selectedColor = selectedColor,
                    textStyle = NookTheme.type.sans(14, FontWeight.Medium, 20),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** "Are you sure? Yes, delete / Cancel" inline confirmation. */
@Composable
fun ConfirmDeleteRow(question: String, yesText: String, cancelText: String, onConfirm: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(question, style = NookTheme.type.sm, color = NookTheme.colors.textMuted)
        TextLink(yesText, onClick = onConfirm, color = Palette.Red500, weight = FontWeight.Medium)
        TextLink(cancelText, onClick = onCancel, color = NookTheme.colors.textSubtle)
    }
}

/** Red ghost "Delete" button of the detail sheets. */
@Composable
fun DeleteButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GhostButton(text, onClick = onClick, modifier = modifier, icon = LucideIcons.Trash2, color = Palette.Red500, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp))
}

/** Thin progress bar (`h-1.5 bg-gray-100 rounded-full` with an amber fill). */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Palette.Amber500, height: Dp = 6.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress")
    Box(modifier.fillMaxWidth().height(height).clip(NookShapes.full).background(NookTheme.colors.surfaceMuted)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(NookShapes.full).background(color))
    }
}

/** Status selector of the movie form: `flex-1 py-2.5 rounded-full text-sm font-medium border`. */
@Composable
fun PillStatusRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Palette.Amber500,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            ChoiceChip(
                label,
                selected = key == selected,
                onClick = { onSelect(key) },
                modifier = Modifier.weight(1f),
                shape = NookShapes.full,
                selectedColor = selectedColor,
                textStyle = NookTheme.type.sans(14, FontWeight.Medium, 20),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}
