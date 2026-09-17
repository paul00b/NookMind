package fr.paulbr.nookmind.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.data.AuthState
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.components.Spinner
import fr.paulbr.nookmind.core.designsystem.components.pulse
import fr.paulbr.nookmind.feature.auth.LoginScreen
import fr.paulbr.nookmind.feature.legal.LegalKind
import fr.paulbr.nookmind.feature.legal.LegalScreen
import fr.paulbr.nookmind.feature.onboarding.OnboardingScreen
import fr.paulbr.nookmind.feature.shell.MainScaffold
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.logo
import org.jetbrains.compose.resources.painterResource

/** Full-screen destinations that sit above the auth-driven flow (routes of App.tsx). */
sealed interface RootOverlay {
    data object Onboarding : RootOverlay
    data class Legal(val kind: LegalKind) : RootOverlay
}

/**
 * Port of the router of App.tsx:
 *  - `/onboarding` until the flag is set, then `/login`,
 *  - `/` protected by the session (loading spinner while Supabase restores it),
 *  - `/privacy` and `/terms` reachable from Settings.
 */
@Composable
fun AppRoot(container: AppContainer) {
    val authState by container.auth.state.collectAsState()
    val onboardingDone by container.prefs.onboardingCompleted.collectAsState()
    var overlay by remember { mutableStateOf<RootOverlay?>(null) }

    fun completeOnboarding() {
        container.prefs.setOnboardingCompleted(true)
        overlay = null
    }

    when (val current = overlay) {
        is RootOverlay.Legal -> LegalScreen(current.kind, onBack = { overlay = null })
        RootOverlay.Onboarding -> OnboardingScreen(onFinish = ::completeOnboarding)
        null -> when (authState) {
            AuthState.Loading -> LoadingScreen()
            AuthState.SignedOut -> {
                // signOut() resets the flag, exactly like the web app.
                if (!onboardingDone) {
                    OnboardingScreen(onFinish = ::completeOnboarding)
                } else {
                    LoginScreen(container)
                }
            }
            is AuthState.SignedIn -> MainScaffold(
                container = container,
                onOpenLegal = { overlay = RootOverlay.Legal(it) },
                onReplayOnboarding = {
                    container.prefs.setOnboardingCompleted(false)
                    overlay = RootOverlay.Onboarding
                },
            )
        }
    }
}

/** The `ProtectedRoute` loading state: pulsing logo + amber spinner on the page background. */
@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(NookTheme.colors.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            androidx.compose.foundation.Image(painterResource(Res.drawable.logo), contentDescription = "NookMind", modifier = Modifier.size(48.dp).pulse())
            Spinner(size = 20.dp, color = Palette.Amber500)
        }
    }
}
