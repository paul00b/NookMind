package fr.paulbr.nookmind.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.GhostButton
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.settings_deleteAccountCancel
import fr.paulbr.nookmind.resources.settings_deleteAccountConfirmBody
import fr.paulbr.nookmind.resources.settings_deleteAccountConfirmCta
import fr.paulbr.nookmind.resources.settings_deleteAccountConfirmTitle
import fr.paulbr.nookmind.resources.settings_deleteAccountFailed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Port of DeleteAccountDialog.tsx: confirmation before wiping the account and every row. */
@Composable
fun DeleteAccountDialog(container: AppContainer, onClose: () -> Unit, onDeleted: () -> Unit) {
    val colors = NookTheme.colors
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val failedText = stringResource(Res.string.settings_deleteAccountFailed)

    Dialog(onDismissRequest = { if (!busy) onClose() }) {
        NookCard(
            Modifier.fillMaxWidth().widthIn(max = 384.dp),
            background = if (colors.isDark) Palette.NightCard else Palette.Cream,
            contentPadding = PaddingValues(24.dp),
        ) {
            Text(stringResource(Res.string.settings_deleteAccountConfirmTitle), style = NookTheme.type.titleSerif, color = colors.textStrong)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(Res.string.settings_deleteAccountConfirmBody), style = NookTheme.type.sm, color = colors.textMuted)
            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = NookTheme.type.sm, color = colors.redText)
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostButton(stringResource(Res.string.settings_deleteAccountCancel), onClick = { if (!busy) onClose() }, modifier = Modifier.weight(1f), enabled = !busy)
                PrimaryButton(
                    stringResource(Res.string.settings_deleteAccountConfirmCta),
                    onClick = {
                        busy = true
                        errorMessage = null
                        scope.launch {
                            container.auth.deleteAccount(failedText)
                                .onSuccess { onClose(); onDeleted() }
                                .onFailure { errorMessage = it.message?.takeIf { m -> m.isNotBlank() } ?: failedText; busy = false }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    loading = busy,
                    color = Palette.Red600,
                    textStyle = NookTheme.type.sans(14, androidx.compose.ui.text.font.FontWeight.SemiBold, 20),
                )
            }
        }
    }
}
