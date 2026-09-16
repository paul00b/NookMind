package fr.paulbr.nookmind

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.app.AppRoot
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.ui.LocalNookHaptics
import fr.paulbr.nookmind.core.ui.rememberNookHaptics

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }

/** Entry composable shared by Android, desktop and (later) iOS. */
@Composable
fun App(container: AppContainer) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(container.httpClient)) }
            .crossfade(true)
            .build()
    }
    val theme by container.prefs.theme.collectAsState()
    val mode by container.prefs.mediaMode.collectAsState()
    val hapticsEnabled by container.prefs.hapticsEnabled.collectAsState()
    CompositionLocalProvider(LocalAppContainer provides container) {
        NookTheme(themeMode = theme, mediaMode = mode) {
            CompositionLocalProvider(LocalNookHaptics provides rememberNookHaptics(hapticsEnabled)) {
                AppRoot(container)
            }
        }
    }
}
