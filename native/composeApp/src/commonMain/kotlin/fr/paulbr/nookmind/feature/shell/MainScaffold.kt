package fr.paulbr.nookmind.feature.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.ModeAmbianceBackground
import fr.paulbr.nookmind.core.designsystem.components.ToastHost
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.platform.exitApplication
import fr.paulbr.nookmind.feature.home.BooksHomeScreen
import fr.paulbr.nookmind.feature.home.MoviesHomeScreen
import fr.paulbr.nookmind.feature.home.SeriesHomeScreen
import fr.paulbr.nookmind.feature.legal.LegalKind
import fr.paulbr.nookmind.feature.library.BooksLibraryScreen
import fr.paulbr.nookmind.feature.library.MoviesLibraryScreen
import fr.paulbr.nookmind.feature.library.SeriesLibraryScreen
import fr.paulbr.nookmind.feature.nextup.NextUpBooksScreen
import fr.paulbr.nookmind.feature.nextup.NextUpMoviesScreen
import fr.paulbr.nookmind.feature.nextup.NextUpSeriesScreen
import fr.paulbr.nookmind.feature.settings.NotificationPromptSheet
import fr.paulbr.nookmind.feature.settings.SettingsPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class MainTab { SEARCH, LIBRARY, NEXT_UP }

/** Swipe order of the web app (`MODES = ['series', 'movies', 'books']`). */
private val SWIPE_MODES = listOf(MediaMode.SERIES, MediaMode.MOVIES, MediaMode.BOOKS)

/** Breakpoint of Tailwind's `md:` (768 px): sidebar layout above, mobile layout below. */
const val TABLET_BREAKPOINT_DP = 768

/** Port of AppLayout.tsx + BottomNav + MobileTopBar + Sidebar, plus the in-app "router". */
@Composable
fun MainScaffold(
    container: AppContainer,
    onOpenLegal: (LegalKind) -> Unit,
    onReplayOnboarding: () -> Unit,
) {
    val mode by container.prefs.mediaMode.collectAsState()
    var tab by rememberSaveable { mutableStateOf(MainTab.SEARCH) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var notifPromptOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Series metadata refresh (24 h) starts once the library is available.
    LaunchedEffect(Unit) { container.seriesRefresh.start() }

    // Notification prompt after 5 s, once, when push is supported and not yet enabled.
    LaunchedEffect(Unit) {
        if (!container.push.isSupported || container.prefs.notificationPrompted) return@LaunchedEffect
        container.push.refresh()
        if (container.push.subscribed.value) return@LaunchedEffect
        delay(5_000)
        notifPromptOpen = true
    }

    // Deep routes coming from a notification tap.
    val pendingRoute by container.pendingRoute.collectAsState()
    LaunchedEffect(pendingRoute) {
        when (pendingRoute) {
            null -> Unit
            "/library" -> tab = MainTab.LIBRARY
            "/discover" -> tab = MainTab.NEXT_UP
            else -> tab = MainTab.SEARCH
        }
        if (pendingRoute != null) container.pendingRoute.value = null
    }

    // Hardware back: close settings, then go back to Search, then leave the app.
    BackHandler(enabled = settingsOpen || tab != MainTab.SEARCH) {
        when {
            settingsOpen -> settingsOpen = false
            tab != MainTab.SEARCH -> tab = MainTab.SEARCH
            else -> exitApplication()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(NookTheme.colors.background)) {
        val wide = maxWidth >= TABLET_BREAKPOINT_DP.dp
        ModeAmbianceBackground(mode)

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                Sidebar(container = container, tab = tab, onTab = { tab = it }, onOpenSettings = { settingsOpen = true })
                Box(Modifier.weight(1f)) {
                    ModeContent(container, mode, tab, bottomPadding = 0.dp)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                MobileTopBar(container = container, onOpenSettings = { settingsOpen = true })
                SwipeableModeArea(
                    mode = mode,
                    onModeChange = { container.prefs.setMediaMode(it) },
                    modifier = Modifier.weight(1f),
                ) {
                    ModeContent(container, mode, tab, bottomPadding = BOTTOM_NAV_CLEARANCE.dp)
                }
            }
            BottomNav(
                mode = mode,
                onMode = { container.prefs.setMediaMode(it) },
                tab = tab,
                onTab = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        ToastHost(
            controller = container.toasts,
            modifier = Modifier.align(Alignment.BottomCenter),
            bottomOffset = if (wide) 24.dp else 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        )

        if (settingsOpen) {
            SettingsPanel(
                container = container,
                onClose = { settingsOpen = false },
                onOpenLegal = { settingsOpen = false; onOpenLegal(it) },
                onReplayOnboarding = { settingsOpen = false; onReplayOnboarding() },
            )
        }

        if (notifPromptOpen) {
            NotificationPromptSheet(
                container = container,
                onDismiss = {
                    notifPromptOpen = false
                    container.prefs.notificationPrompted = true
                },
            )
        }
    }
    // Keep the coroutine scope referenced for future shell-level work.
    remember { scope }
}

/** Space reserved under the content for the floating bottom navigation (`pb-36` ≈ 144 px). */
const val BOTTOM_NAV_CLEARANCE = 144

@Composable
private fun ModeContent(container: AppContainer, mode: MediaMode, tab: MainTab, bottomPadding: androidx.compose.ui.unit.Dp) {
    val contentPadding = PaddingValues(bottom = bottomPadding)
    when (tab) {
        MainTab.SEARCH -> when (mode) {
            MediaMode.BOOKS -> BooksHomeScreen(container, contentPadding)
            MediaMode.MOVIES -> MoviesHomeScreen(container, contentPadding)
            MediaMode.SERIES -> SeriesHomeScreen(container, contentPadding)
        }
        MainTab.LIBRARY -> when (mode) {
            MediaMode.BOOKS -> BooksLibraryScreen(container, contentPadding)
            MediaMode.MOVIES -> MoviesLibraryScreen(container, contentPadding)
            MediaMode.SERIES -> SeriesLibraryScreen(container, contentPadding)
        }
        MainTab.NEXT_UP -> when (mode) {
            MediaMode.BOOKS -> NextUpBooksScreen(container, contentPadding)
            MediaMode.MOVIES -> NextUpMoviesScreen(container, contentPadding)
            MediaMode.SERIES -> NextUpSeriesScreen(container, contentPadding)
        }
    }
}

/**
 * Horizontal swipe on the main area switches the media mode (Series ↔ Movies ↔ Books) with the
 * same feel as the web: the content follows the finger at 25 %, snaps back, and a swipe of at
 * least 60 px that is clearly horizontal changes the mode. Horizontal sliders inside the content
 * consume their own drags first, so they keep working.
 */
@Composable
fun SwipeableModeArea(
    mode: MediaMode,
    onModeChange: (MediaMode) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .pointerInput(mode) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        total += dragAmount
                        scope.launch { offset.snapTo(total * 0.25f) }
                    },
                    onDragEnd = {
                        scope.launch { offset.animateTo(0f, tween(250)) }
                        if (abs(total) >= 60f) {
                            val index = SWIPE_MODES.indexOf(mode)
                            val step = if (total > 0) -1 else 1
                            val next = SWIPE_MODES[(index + step + SWIPE_MODES.size) % SWIPE_MODES.size]
                            onModeChange(next)
                        }
                        total = 0f
                    },
                    onDragCancel = {
                        scope.launch { offset.animateTo(0f, tween(250)) }
                        total = 0f
                    },
                )
            }
            .graphicsLayer { translationX = offset.value },
    ) {
        content()
    }
}

/** Sidebar column width of the desktop layout (`w-60`). */
val SIDEBAR_WIDTH = 240.dp

@Composable
fun Modifier.sidebarWidth(): Modifier = this.width(SIDEBAR_WIDTH)
