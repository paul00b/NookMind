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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import fr.paulbr.nookmind.feature.common.LocalWideLayout
import androidx.compose.runtime.CompositionLocalProvider
import fr.paulbr.nookmind.feature.home.BooksHomeScreen
import fr.paulbr.nookmind.feature.home.MoviesHomeScreen
import fr.paulbr.nookmind.feature.home.SeriesHomeScreen
import fr.paulbr.nookmind.feature.legal.LegalKind
import fr.paulbr.nookmind.feature.library.BooksLibraryScreen
import fr.paulbr.nookmind.feature.library.MoviesLibraryScreen
import fr.paulbr.nookmind.feature.library.SeriesLibraryScreen
import fr.paulbr.nookmind.feature.nextup.NextUpBooksScreen
import fr.paulbr.nookmind.feature.nextup.NextUpMoviesScreen
import fr.paulbr.nookmind.feature.social.SocialScreen
import fr.paulbr.nookmind.feature.tonight.TonightScreen
import fr.paulbr.nookmind.feature.nextup.NextUpSeriesScreen
import fr.paulbr.nookmind.feature.settings.NotificationPromptSheet
import fr.paulbr.nookmind.feature.settings.SettingsPanel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import fr.paulbr.nookmind.core.domain.ShareLinks
import fr.paulbr.nookmind.feature.common.ItemSheetsHost
import fr.paulbr.nookmind.feature.common.rememberItemSheets
import fr.paulbr.nookmind.resources.share_notFound
import org.jetbrains.compose.resources.stringResource
import fr.paulbr.nookmind.resources.Res

enum class MainTab { SEARCH, LIBRARY, NEXT_UP, SOCIAL }

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
    initialTab: MainTab = MainTab.SEARCH,
) {
    val mode by container.prefs.mediaMode.collectAsState()
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var notifPromptOpen by remember { mutableStateOf(false) }
    var tonightOpen by rememberSaveable { mutableStateOf(false) }
    // The online evening a friend invited me to, opened from the Social tab or a notification.
    var tonightJoin by rememberSaveable { mutableStateOf<String?>(null) }
    val openTonight: (String?) -> Unit = { session ->
        tonightJoin = session
        tonightOpen = true
    }
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

    // An invitation or a friend request waiting puts a dot on the Social tab.
    val invitations by container.social.invitations.collectAsState()
    val friends by container.social.friends.collectAsState()
    val socialBadge = tab != MainTab.SOCIAL && (invitations.isNotEmpty() || friends.any { it.incoming && !it.accepted })

    // A shared link (nookmind://add/movie/…, or the https page) opens the item, to add it.
    val linkSheets = rememberItemSheets(container)
    val linkMissing = stringResource(Res.string.share_notFound)

    // Deep routes coming from a notification tap.
    val pendingRoute by container.pendingRoute.collectAsState()
    LaunchedEffect(pendingRoute) {
        val route = pendingRoute
        when {
            route == null -> Unit
            route == "/library" -> tab = MainTab.LIBRARY
            route == "/discover" -> tab = MainTab.NEXT_UP
            route == "/social" -> tab = MainTab.SOCIAL
            route.startsWith("/add/") -> ShareLinks.parseRoute(route)?.let { (kind, key) ->
                linkSheets.open(kind, key, onMissing = { container.toasts.error(linkMissing) })
            }
            route.startsWith("/friend/") -> ShareLinks.parseFriendRoute(route)?.let { code ->
                tab = MainTab.SOCIAL
                container.pendingFriendCode.value = code
            }
            route.startsWith("/tonight/") -> {
                tab = MainTab.SOCIAL
                openTonight(route.removePrefix("/tonight/").ifBlank { null })
            }
            else -> tab = MainTab.SEARCH
        }
        if (pendingRoute != null) container.pendingRoute.value = null
    }

    // Pull to refresh, on every tab: my library and my friends again from Supabase, and the screens
    // that read TMDB or Google Books rebuilt on fresh data (their caches dropped).
    var refreshing by remember { mutableStateOf(false) }
    var refreshRound by remember { mutableIntStateOf(0) }
    val onRefresh: () -> Unit = {
        if (!refreshing) {
            scope.launch {
                refreshing = true
                refreshTab(container, tab, mode)
                if (tab == MainTab.SEARCH || tab == MainTab.NEXT_UP) refreshRound++
                refreshing = false
            }
        }
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
        // Everything the floating navigation blurs behind itself is drawn inside this source layer:
        // the mode halo and the scrolling content. The navigation itself sits outside it, otherwise
        // it would blur its own reflection.
        val hazeState = rememberHazeState()

        CompositionLocalProvider(LocalWideLayout provides wide) {
        Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
            ModeAmbianceBackground(mode)
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Sidebar(container = container, tab = tab, onTab = { tab = it }, onOpenSettings = { settingsOpen = true }, socialBadge = socialBadge)
                    Box(Modifier.weight(1f)) {
                        RefreshableContent(refreshing, onRefresh) {
                            key(refreshRound) { ModeContent(container, mode, tab, bottomPadding = 0.dp, onOpenTonight = openTonight) }
                        }
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
                        RefreshableContent(refreshing, onRefresh) {
                            key(refreshRound) { ModeContent(container, mode, tab, bottomPadding = BOTTOM_NAV_CLEARANCE.dp, onOpenTonight = openTonight) }
                        }
                    }
                }
            }
        }
        if (!wide) {
            BottomNav(
                mode = mode,
                onMode = { container.prefs.setMediaMode(it) },
                tab = tab,
                onTab = { tab = it },
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter),
                socialBadge = socialBadge,
            )
        }

        if (tonightOpen) {
            key(tonightJoin) {
                TonightScreen(container, onClose = { tonightOpen = false; tonightJoin = null }, joinSessionId = tonightJoin)
            }
        }

        ItemSheetsHost(container, linkSheets)

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
    }
    // Keep the coroutine scope referenced for future shell-level work.
    remember { scope }
}

/** What a pull refreshes on each tab. Lasts at least half a second, so the spinner never just blinks. */
private suspend fun refreshTab(container: AppContainer, tab: MainTab, mode: MediaMode) = coroutineScope {
    launch { delay(500) }
    val library = when (mode) {
        MediaMode.BOOKS -> container.books
        MediaMode.MOVIES -> container.movies
        MediaMode.SERIES -> container.series
    }
    when (tab) {
        MainTab.SOCIAL -> container.social.refresh()
        MainTab.LIBRARY -> library.refetch()
        MainTab.SEARCH, MainTab.NEXT_UP -> {
            container.clearCaches()
            library.refetch()
        }
    }
}

/** The tab's content under a pull-to-refresh, with the spinner in the colours of the navigation. */
@Composable
private fun RefreshableContent(refreshing: Boolean, onRefresh: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = NookTheme.colors.surface,
                color = NookTheme.colors.amberText,
            )
        },
    ) {
        content()
    }
}

/** Space reserved under the content for the floating bottom navigation (`pb-36` ≈ 144 px). */
const val BOTTOM_NAV_CLEARANCE = 144

@Composable
private fun ModeContent(container: AppContainer, mode: MediaMode, tab: MainTab, bottomPadding: androidx.compose.ui.unit.Dp, onOpenTonight: (String?) -> Unit) {
    val contentPadding = PaddingValues(bottom = bottomPadding)
    when (tab) {
        MainTab.SEARCH -> when (mode) {
            MediaMode.BOOKS -> BooksHomeScreen(container, contentPadding)
            MediaMode.MOVIES -> MoviesHomeScreen(container, contentPadding, onOpenTonight = { onOpenTonight(null) })
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
        MainTab.SOCIAL -> SocialScreen(container, contentPadding, onOpenTonight)
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
