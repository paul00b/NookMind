package fr.paulbr.nookmind.feature.settings

import androidx.compose.runtime.Composable
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.feature.legal.LegalKind

// Placeholder — replaced by the real panel in the settings step.
@Composable
fun SettingsPanel(container: AppContainer, onClose: () -> Unit, onOpenLegal: (LegalKind) -> Unit, onReplayOnboarding: () -> Unit) {
}

@Composable
fun NotificationPromptSheet(container: AppContainer, onDismiss: () -> Unit) {
}
