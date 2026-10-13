package fr.paulbr.nookmind.feature.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.platform.openExternalUrl

/** The canvas Compose draws in cannot host YouTube's player: the trailer opens in a new tab. */
@Composable
actual fun YouTubeEmbed(videoKey: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        PrimaryButton("YouTube", onClick = { openExternalUrl("https://www.youtube.com/watch?v=$videoKey") })
    }
}
