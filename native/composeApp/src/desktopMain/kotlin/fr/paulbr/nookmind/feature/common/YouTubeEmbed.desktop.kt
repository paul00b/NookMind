package fr.paulbr.nookmind.feature.common

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.platform.openExternalUrl

/** No embedded browser on the desktop target: hand the trailer to the system browser. */
@Composable
actual fun YouTubeEmbed(videoKey: String, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        PrimaryButton("YouTube", onClick = { openExternalUrl("https://www.youtube.com/watch?v=$videoKey") })
    }
}
