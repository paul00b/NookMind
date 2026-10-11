package fr.paulbr.nookmind.feature.social

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import fr.paulbr.nookmind.feature.tonight.TonightEntryCard
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.social_subtitle
import fr.paulbr.nookmind.resources.social_title
import org.jetbrains.compose.resources.stringResource

/** The Social tab: what is played and shared with friends, the same in every mode. */
@Composable
fun SocialScreen(container: AppContainer, contentPadding: PaddingValues, onOpenTonight: () -> Unit) {
    val colors = NookTheme.colors
    val wide = LocalWideLayout.current
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = if (wide) 32.dp else 16.dp, vertical = if (wide) 32.dp else 16.dp)) {
            Text(stringResource(Res.string.social_title), style = NookTheme.type.h1Serif, color = if (colors.isDark) Palette.Purple400 else Palette.Purple600)
            Text(stringResource(Res.string.social_subtitle), style = NookTheme.type.sm, color = colors.textSubtle)
            Spacer(Modifier.height(24.dp))
            TonightEntryCard(onClick = onOpenTonight)
        }
    }
}
