package fr.paulbr.nookmind.feature.series

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.HairlineDivider
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons

/** Collapsible bordered section of the series sheets (`border rounded-xl` + chevron header). */
@Composable
fun AccordionSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyOpen: Boolean = false,
    onOpen: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = NookTheme.colors
    var open by remember { mutableStateOf(initiallyOpen) }
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    Column(modifier.fillMaxWidth().clip(NookShapes.xl).border(1.dp, colors.divider, NookShapes.xl)) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open; if (open) onOpen?.invoke() }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = NookTheme.type.sans(14, FontWeight.Medium, 20), color = colors.textBody2, modifier = Modifier.weight(1f))
            Icon(LucideIcons.ChevronDown, null, Modifier.size(16.dp).rotate(rotation), tint = colors.textFaint)
        }
        AnimatedVisibility(open) {
            Column(Modifier.fillMaxWidth()) {
                HairlineDivider()
                content()
            }
        }
    }
}
