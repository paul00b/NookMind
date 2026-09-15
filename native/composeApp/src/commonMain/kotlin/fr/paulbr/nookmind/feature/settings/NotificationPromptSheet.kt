package fr.paulbr.nookmind.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.data.PushEnableResult
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.alpha
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.NookSheet
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.components.SheetCloseButton
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.common_later
import fr.paulbr.nookmind.resources.notifPrompt_body
import fr.paulbr.nookmind.resources.notifPrompt_enable
import fr.paulbr.nookmind.resources.notifPrompt_enableFailed
import fr.paulbr.nookmind.resources.notifPrompt_enabled
import fr.paulbr.nookmind.resources.notifPrompt_enabling
import fr.paulbr.nookmind.resources.notifPrompt_subtitle
import fr.paulbr.nookmind.resources.notifPrompt_title
import fr.paulbr.nookmind.resources.notifPrompt_tokenError
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of NotificationPromptSheet.tsx: the one-time push opt-in shown 5 s after the first launch. */
@Composable
fun NotificationPromptSheet(container: AppContainer, onDismiss: () -> Unit) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    val enabledText = stringResource(Res.string.notifPrompt_enabled)
    val tokenErrorText = stringResource(Res.string.notifPrompt_tokenError)
    val failedText = stringResource(Res.string.notifPrompt_enableFailed)

    NookSheet(
        onClose = onDismiss,
        maxWidth = 384.dp,
        background = if (colors.isDark) Palette.NightCard else Palette.Cream,
    ) { controller ->
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(56.dp).clip(NookShapes.xl2).background(Palette.Teal500.alpha(0.10f)), contentAlignment = Alignment.Center) {
                        Icon(LucideIcons.Bell, null, Modifier.size(26.dp), tint = Palette.Teal500)
                    }
                    Column {
                        Text(stringResource(Res.string.notifPrompt_title), style = NookTheme.type.titleSerif, color = colors.textStrong)
                        Spacer(Modifier.height(2.dp))
                        Text(stringResource(Res.string.notifPrompt_subtitle), style = NookTheme.type.sans(12, androidx.compose.ui.text.font.FontWeight.Medium, 16), color = colors.tealText)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(Res.string.notifPrompt_body), style = NookTheme.type.sans(14, lineHeight = 23), color = colors.textMuted)
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostButton(stringResource(Res.string.common_later), onClick = { controller.close() }, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        if (loading) stringResource(Res.string.notifPrompt_enabling) else stringResource(Res.string.notifPrompt_enable),
                        onClick = {
                            loading = true
                            scope.launch {
                                when (val result = container.push.enable()) {
                                    PushEnableResult.Enabled -> { container.toasts.success(enabledText); controller.close() }
                                    PushEnableResult.PermissionDenied -> controller.close()
                                    PushEnableResult.TokenUnavailable -> container.toasts.error(tokenErrorText)
                                    is PushEnableResult.Failed -> container.toasts.error(result.message ?: failedText)
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        loading = loading,
                        icon = LucideIcons.Bell,
                        iconSize = 14.dp,
                        color = Palette.Teal500,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            SheetCloseButton(controller, Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 8.dp), size = 18.dp)
        }
    }
}
