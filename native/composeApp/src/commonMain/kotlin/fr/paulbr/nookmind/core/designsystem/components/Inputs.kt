package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.ui.HapticCue
import fr.paulbr.nookmind.core.ui.LocalNookHaptics

/**
 * `.input` — white / #1a1f2e field, 12 dp radius, hairline border (12 %), amber focus ring.
 * A [readOnly] field renders muted like the search-prefilled fields of the add forms.
 */
@Composable
fun NookTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = NookTheme.type.sans(16, FontWeight.Normal, 24),
    shape: Shape = NookShapes.xl,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    minHeight: Dp? = null,
) {
    val colors = NookTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val background = if (readOnly) colors.surfaceMuted else colors.surface
    val textColor = if (readOnly) colors.textSubtle else colors.textBody
    val borderColor = when {
        focused && !readOnly -> Palette.Amber500
        else -> colors.borderStrong
    }
    Box(
        modifier
            .then(if (focused && !readOnly) Modifier.border(3.dp, Palette.Amber500.copy(alpha = 0.4f), shape) else Modifier)
            .padding(if (focused && !readOnly) 1.dp else 0.dp)
            .clip(shape)
            .background(background, shape)
            .border(1.dp, borderColor, shape)
            .then(if (minHeight != null) Modifier.heightIn(min = minHeight) else Modifier),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            enabled = enabled && !readOnly,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = textStyle.copy(color = textColor, textAlign = textAlign ?: textStyle.textAlign),
            cursorBrush = SolidColor(Palette.Amber500),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interaction,
            decorationBox = { inner ->
                Row(Modifier.padding(contentPadding), verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(12.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = textStyle, color = colors.textFaint, maxLines = if (singleLine) 1 else 2, overflow = TextOverflow.Ellipsis)
                        }
                        inner()
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(8.dp))
                        trailing()
                    }
                }
            },
        )
    }
}

/** Multi-line `.input` used for notes (`resize-none h-20 text-sm`). */
@Composable
fun NookTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    readOnly: Boolean = false,
    height: Dp = 80.dp,
) {
    NookTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(height),
        placeholder = placeholder,
        readOnly = readOnly,
        singleLine = false,
        textStyle = NookTheme.type.sm,
        minHeight = height,
    )
}

/** Password field with the eye toggle of the login form. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "••••••••",
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by remember { mutableStateOf(false) }
    NookTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = {
            Icon(
                if (visible) LucideIcons.EyeOff else LucideIcons.Eye,
                contentDescription = null,
                modifier = Modifier.size(16.dp).clickable { visible = !visible },
                tint = NookTheme.colors.textFaint,
            )
        },
    )
}

/** Form label (`block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1`). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    Text(
        text,
        modifier = modifier.padding(bottom = 4.dp),
        style = if (muted) NookTheme.type.sm else NookTheme.type.sans(14, FontWeight.Medium, 20),
        color = if (muted) NookTheme.colors.textSubtle else NookTheme.colors.textBody2,
    )
}

@Composable
fun LabeledField(label: String, modifier: Modifier = Modifier, muted: Boolean = false, content: @Composable () -> Unit) {
    Column(modifier) {
        FieldLabel(label, muted = muted)
        content()
    }
}

data class SelectOption(val value: String, val label: String)

/** The `<select>` of the library filters: `.input py-2 pr-8 text-sm` with a chevron, opening a menu. */
@Composable
fun NookSelect(
    value: String,
    options: List<SelectOption>,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val colors = NookTheme.colors
    val current = options.firstOrNull { it.value == value } ?: options.firstOrNull()
    Box(modifier) {
        Row(
            Modifier
                .clip(NookShapes.xl)
                .background(colors.surface, NookShapes.xl)
                .border(1.dp, colors.borderStrong, NookShapes.xl)
                .clickable { open = true }
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current?.label ?: "", style = NookTheme.type.sm, color = colors.textBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Icon(LucideIcons.ChevronDown, null, Modifier.size(14.dp), tint = colors.textFaint)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = colors.surface) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, style = NookTheme.type.sm, color = if (option.value == value) NookTheme.colors.amberText else colors.textBody) },
                    onClick = { open = false; onChange(option.value) },
                )
            }
        }
    }
}

/** The teal iOS-like switch of the notification settings. */
@Composable
fun NookToggle(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = NookTheme.colors
    val haptics = LocalNookHaptics.current
    val knobOffset by androidx.compose.animation.core.animateDpAsState(if (checked) 22.dp else 4.dp, label = "knob")
    Box(
        modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(NookShapes.full)
            .background(if (checked) Palette.Teal500 else if (colors.isDark) Palette.White.copy(alpha = 0.10f) else Palette.Gray200, NookShapes.full)
            .border(1.dp, if (checked) Palette.Teal500 else colors.border, NookShapes.full)
            .clickable(enabled = enabled) {
                haptics.perform(if (checked) HapticCue.TOGGLE_OFF else HapticCue.TOGGLE_ON)
                onChange(!checked)
            },
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = knobOffset)
                .size(20.dp)
                .clip(NookShapes.full)
                .background(Palette.White, NookShapes.full),
        )
    }
}

/** Segmented control (`flex bg-gray-100 dark:bg-gray-800 rounded-full p-1`) — login mode toggle. */
@Composable
fun SegmentedPill(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = NookShapes.full,
    itemShape: Shape = NookShapes.full,
) {
    val colors = NookTheme.colors
    Row(modifier.clip(shape).background(colors.surfaceMuted, shape).padding(4.dp)) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .background(if (active) colors.surface else Color.Transparent, itemShape)
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = if (active) colors.textStrong else colors.textSubtle)
            }
        }
    }
}
