package fr.paulbr.nookmind.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.NookCard
import fr.paulbr.nookmind.core.designsystem.components.NookTextField
import fr.paulbr.nookmind.core.designsystem.components.OutlinedActionButton
import fr.paulbr.nookmind.core.designsystem.components.PasswordField
import fr.paulbr.nookmind.core.designsystem.components.PrimaryButton
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.core.network.JellyseerrApi.State
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.jellyseerr_available
import fr.paulbr.nookmind.resources.jellyseerr_checking
import fr.paulbr.nookmind.resources.jellyseerr_connected
import fr.paulbr.nookmind.resources.jellyseerr_failed
import fr.paulbr.nookmind.resources.jellyseerr_hint
import fr.paulbr.nookmind.resources.jellyseerr_keyPlaceholder
import fr.paulbr.nookmind.resources.jellyseerr_partial
import fr.paulbr.nookmind.resources.jellyseerr_pending
import fr.paulbr.nookmind.resources.jellyseerr_processing
import fr.paulbr.nookmind.resources.jellyseerr_request
import fr.paulbr.nookmind.resources.jellyseerr_requested
import fr.paulbr.nookmind.resources.jellyseerr_save
import fr.paulbr.nookmind.resources.jellyseerr_unreachable
import fr.paulbr.nookmind.resources.jellyseerr_urlPlaceholder
import fr.paulbr.nookmind.resources.jellyseerr_wrong
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * "Ask Jellyseerr" on a movie or series page, in the personal build once a server is set: shows
 * where the title stands there (asked, downloading, available) and asks for it in one tap.
 */
@Composable
fun JellyseerrButton(container: AppContainer, type: String, tmdbId: Int, modifier: Modifier = Modifier) {
    if (!container.personalFeatures || !container.jellyseerr.isConfigured) return
    val scope = rememberCoroutineScope()
    var state by remember(type, tmdbId) { mutableStateOf<State?>(null) }
    var checked by remember(type, tmdbId) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val requested = stringResource(Res.string.jellyseerr_requested)
    LaunchedEffect(type, tmdbId) {
        state = container.jellyseerr.state(type, tmdbId)
        checked = true
    }
    val text = when {
        !checked -> stringResource(Res.string.jellyseerr_checking)
        state == null -> stringResource(Res.string.jellyseerr_unreachable)
        state == State.NOT_REQUESTED -> stringResource(Res.string.jellyseerr_request)
        state == State.PENDING -> stringResource(Res.string.jellyseerr_pending)
        state == State.PROCESSING -> stringResource(Res.string.jellyseerr_processing)
        state == State.PARTIAL -> stringResource(Res.string.jellyseerr_partial)
        else -> stringResource(Res.string.jellyseerr_available)
    }
    // A series partly there can be asked again: the seasons still missing.
    val canAct = checked && (state == null || state == State.NOT_REQUESTED || state == State.PARTIAL)
    OutlinedActionButton(
        text = text,
        onClick = {
            if (busy || !canAct) return@OutlinedActionButton
            busy = true
            scope.launch {
                if (state == null) {
                    state = container.jellyseerr.state(type, tmdbId)
                } else {
                    val error = container.jellyseerr.request(type, tmdbId)
                    if (error == null) {
                        container.toasts.success(requested)
                        state = container.jellyseerr.state(type, tmdbId) ?: State.PENDING
                    } else {
                        container.toasts.error(Res.string.jellyseerr_failed, error)
                    }
                }
                busy = false
            }
        },
        modifier = modifier.fillMaxWidth(),
        icon = when (state) {
            State.AVAILABLE -> LucideIcons.Check
            State.PENDING, State.PROCESSING -> LucideIcons.Clock
            null -> if (checked) LucideIcons.RefreshCw else LucideIcons.Download
            else -> LucideIcons.Download
        },
        loading = busy || !checked,
        enabled = canAct || !checked,
    )
}

/** The server and key, in Settings of the personal build. Saved only once they answer. */
@Composable
fun JellyseerrSettingsCard(container: AppContainer) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(container.prefs.jellyseerrUrl) }
    var key by remember { mutableStateOf(container.prefs.jellyseerrKey) }
    var busy by remember { mutableStateOf(false) }
    NookCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.jellyseerr_hint), style = NookTheme.type.sm, color = NookTheme.colors.textSubtle)
            NookTextField(
                url, { url = it },
                placeholder = stringResource(Res.string.jellyseerr_urlPlaceholder),
                keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next,
                textStyle = NookTheme.type.sm,
            )
            PasswordField(key, { key = it }, placeholder = stringResource(Res.string.jellyseerr_keyPlaceholder))
            PrimaryButton(
                stringResource(Res.string.jellyseerr_save),
                onClick = {
                    if (busy) return@PrimaryButton
                    busy = true
                    scope.launch {
                        val name = container.jellyseerr.check(url, key)
                        if (name != null) {
                            container.prefs.jellyseerrUrl = url.trim()
                            container.prefs.jellyseerrKey = key.trim()
                            container.toasts.success(Res.string.jellyseerr_connected, name)
                        } else {
                            container.toasts.error(Res.string.jellyseerr_wrong)
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                loading = busy,
            )
        }
    }
}
