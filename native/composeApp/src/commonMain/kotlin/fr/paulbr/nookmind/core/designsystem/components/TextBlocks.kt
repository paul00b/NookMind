package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons

/** Port of ExpandableDescription.tsx: clamped paragraph with a "See more / See less" toggle. */
@Composable
fun ExpandableDescription(
    description: String,
    seeMoreText: String,
    seeLessText: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    clampLines: Int = 4,
    textStyle: androidx.compose.ui.text.TextStyle = NookTheme.type.sans(14, lineHeight = 22),
    textColor: androidx.compose.ui.graphics.Color = NookTheme.colors.textBody2,
) {
    var expanded by remember { mutableStateOf(false) }
    var truncated by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().clickable(enabled = truncated || expanded) { expanded = !expanded }) {
        if (label != null) {
            Text(label, style = NookTheme.type.sm, color = NookTheme.colors.textSubtle)
            Spacer(Modifier.height(4.dp))
        }
        Text(
            description,
            style = textStyle,
            color = textColor,
            maxLines = if (expanded) Int.MAX_VALUE else clampLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!expanded) truncated = result.hasVisualOverflow },
        )
        if (truncated || expanded) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown, null, Modifier.size(12.dp), tint = NookTheme.colors.amberText)
                Text(if (expanded) seeLessText else seeMoreText, style = NookTheme.type.xs, color = NookTheme.colors.amberText)
            }
        }
    }
}

/**
 * Port of EditableNote.tsx, without its pencil: the note sits in a box drawn like the text field it
 * turns into, so tapping it (empty or not) is the obvious way to write. Save / Cancel close it.
 */
@Composable
fun EditableNote(
    note: String?,
    labelText: String,
    placeholderText: String,
    saveText: String,
    cancelText: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NookTheme.colors
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    Column(modifier.fillMaxWidth()) {
        Text(labelText, style = NookTheme.type.sm, color = colors.textSubtle)
        Spacer(Modifier.height(8.dp))
        if (editing) {
            NookTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = placeholderText,
                singleLine = false,
                textStyle = NookTheme.type.sm,
                minHeight = 96.dp,
                focusRequester = focus,
            )
            LaunchedEffect(Unit) { focus.requestFocus() }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(saveText, onClick = { onSave(draft); editing = false }, icon = LucideIcons.Check, iconSize = 14.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 6.dp))
                GhostButton(cancelText, onClick = { editing = false }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp))
            }
        } else {
            EditableValueBox(note, placeholderText, onClick = { draft = note ?: ""; editing = true }, Modifier.fillMaxWidth())
        }
    }
}

/**
 * A saved value shown in a box drawn like the field that edits it (the note, the current page, the
 * date watched): tapping it opens the editor. Replaces the pencil icons, which read as one more
 * button next to the sheet's real ones. [text] null or blank shows [placeholder] instead.
 */
@Composable
fun EditableValueBox(text: String?, placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = NookTheme.colors
    Box(
        modifier
            .clip(NookShapes.xl)
            .background(colors.surface, NookShapes.xl)
            .border(1.dp, colors.borderStrong, NookShapes.xl)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        if (text.isNullOrBlank()) {
            Text(placeholder, style = NookTheme.type.sm, color = colors.textFaint)
        } else {
            Text(text, style = NookTheme.type.sans(14, lineHeight = 22), color = colors.textBody2)
        }
    }
}

/** Label / value block used across the detail sheets (`text-sm text-gray-500` label). */
@Composable
fun LabeledBlock(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier) {
        Text(label, style = NookTheme.type.sm, color = NookTheme.colors.textSubtle)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
fun RowSpacer(width: Int) = Spacer(Modifier.width(width.dp))

@Composable
fun ColumnSpacer(height: Int) = Spacer(Modifier.height(height.dp))
