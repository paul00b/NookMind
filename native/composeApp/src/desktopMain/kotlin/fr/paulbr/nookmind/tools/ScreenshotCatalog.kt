package fr.paulbr.nookmind.tools

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.App
import fr.paulbr.nookmind.app.AppContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.components.ModeAmbianceBackground
import fr.paulbr.nookmind.core.model.GoogleBookVolume
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.ThemeMode
import fr.paulbr.nookmind.feature.books.AddBookSheet
import fr.paulbr.nookmind.feature.books.BookDetailSheet
import fr.paulbr.nookmind.feature.movies.AddMovieSheet
import fr.paulbr.nookmind.feature.movies.MovieDetailSheet
import fr.paulbr.nookmind.feature.series.AddSeriesSheet
import fr.paulbr.nookmind.feature.series.SeriesDetailSheet
import fr.paulbr.nookmind.feature.series.SeriesStatsSheet
import fr.paulbr.nookmind.feature.settings.NotificationPromptSheet
import fr.paulbr.nookmind.feature.settings.SettingsPanel
import fr.paulbr.nookmind.feature.common.HomeScreenScaffold
import fr.paulbr.nookmind.feature.common.HomeSection
import fr.paulbr.nookmind.feature.common.SearchResultRow
import fr.paulbr.nookmind.feature.common.rememberSearchController
import fr.paulbr.nookmind.feature.shell.MainScaffold
import fr.paulbr.nookmind.feature.shell.MainTab
import fr.paulbr.nookmind.feature.auth.LoginScreen
import fr.paulbr.nookmind.feature.legal.LegalKind
import fr.paulbr.nookmind.feature.legal.LegalScreen
import fr.paulbr.nookmind.feature.onboarding.OnboardingScreen

class ScreenshotEntry(
    val name: String,
    val widthDp: Int = Screenshots.WIDTH_DP,
    val heightDp: Int = Screenshots.HEIGHT_DP,
    val content: @Composable () -> Unit,
)

/** Screens rendered by the `screenshots` tool. Extended as features land. */
object ScreenshotCatalog {
    private val container by lazy { AppContainer().also(::seed) }

    /** Re-applied at render time: the repositories reset when the (absent) session resolves to signed-out. */
    private fun seed(c: AppContainer) {
        c.books.seedLocal(FakeData.books)
        c.bookCategories.seedLocal(FakeData.bookCategories)
        c.movies.seedLocal(FakeData.movies)
        c.movieCategories.seedLocal(FakeData.movieCategories)
        c.series.seedLocal(FakeData.series)
        c.seriesCategories.seedLocal(FakeData.seriesCategories)
    }

    /** The main shell on a given mode / tab, with an optional sheet on top. */
    private fun shell(mode: MediaMode, tab: MainTab, dark: Boolean = false, overlay: (@Composable () -> Unit)? = null): @Composable () -> Unit = {
        remember { container.prefs.setMediaMode(mode); seed(container) }
        NookTheme(if (dark) ThemeMode.DARK else ThemeMode.LIGHT, mode) {
            MainScaffold(container, onOpenLegal = {}, onReplayOnboarding = {}, initialTab = tab)
            overlay?.invoke()
        }
    }

    val entries: List<ScreenshotEntry> = listOf(
        ScreenshotEntry("smoke") { App(container) },
        // The mode icons, large, to check the generated Lucide vectors against lucide.dev.
        ScreenshotEntry("icons", widthDp = 256, heightDp = 120) {
            NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) {
                Box(Modifier.fillMaxSize().background(NookTheme.colors.background)) {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        MediaMode.entries.forEach { m ->
                            androidx.compose.material3.Icon(
                                fr.paulbr.nookmind.core.designsystem.components.modeIcon(m),
                                contentDescription = m.name,
                                modifier = Modifier.size(64.dp),
                                tint = NookTheme.colors.textStrong,
                            )
                        }
                    }
                }
            }
        },
        // Background layer alone, to compare the grain with the web app's `.mode-bg-*`.
        ScreenshotEntry("ambiance", widthDp = 256, heightDp = 256) {
            NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) {
                Box(Modifier.fillMaxSize().background(NookTheme.colors.background)) {
                    ModeAmbianceBackground(MediaMode.BOOKS)
                }
            }
        },
        ScreenshotEntry("books-home", content = shell(MediaMode.BOOKS, MainTab.SEARCH)),
        ScreenshotEntry("books-home-dark", content = shell(MediaMode.BOOKS, MainTab.SEARCH, dark = true)),
        ScreenshotEntry("books-home-dropdown") {
            NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) {
                val search = rememberSearchController<GoogleBookVolume>(timeoutText = "timeout") { FakeData.bookSearchResults }
                LaunchedEffect(Unit) { search.query = "Dune"; search.results = FakeData.bookSearchResults; search.dropdownOpen = true }
                HomeScreenScaffold(
                    contentPadding = PaddingValues(bottom = 144.dp),
                    title = "Reading tracker", subtitle = "Search for a book to track your reading journey (or add one manually)",
                    titleColor = NookTheme.colors.amberText, search = search, searchPlaceholder = "Search by title, author, or ISBN...",
                    noResultsText = "No books found", sections = emptyList<HomeSection>(), resultKey = { it.id },
                ) { vol -> SearchResultRow(null, vol.volumeInfo.title, vol.volumeInfo.authors?.joinToString() ?: "", onClick = {}) }
            }
        },
        ScreenshotEntry("books-library", content = shell(MediaMode.BOOKS, MainTab.LIBRARY)),
        ScreenshotEntry("books-library-dark", content = shell(MediaMode.BOOKS, MainTab.LIBRARY, dark = true)),
        ScreenshotEntry("books-nextup", content = shell(MediaMode.BOOKS, MainTab.NEXT_UP)),
        ScreenshotEntry("books-detail", content = shell(MediaMode.BOOKS, MainTab.LIBRARY) { BookDetailSheet(container, FakeData.books[0], onClose = {}) }),
        ScreenshotEntry("books-add", content = shell(MediaMode.BOOKS, MainTab.SEARCH) { AddBookSheet(container, FakeData.books[3].copy(id = "", rating = null, personalNote = null), onClose = {}) }),
        ScreenshotEntry("movies-home", content = shell(MediaMode.MOVIES, MainTab.SEARCH)),
        ScreenshotEntry("movies-home-dark", content = shell(MediaMode.MOVIES, MainTab.SEARCH, dark = true)),
        ScreenshotEntry("movies-library", content = shell(MediaMode.MOVIES, MainTab.LIBRARY)),
        ScreenshotEntry("movies-nextup", content = shell(MediaMode.MOVIES, MainTab.NEXT_UP)),
        ScreenshotEntry("movies-detail", content = shell(MediaMode.MOVIES, MainTab.LIBRARY) { MovieDetailSheet(container, FakeData.movies[0], onClose = {}) }),
        ScreenshotEntry("movies-add", content = shell(MediaMode.MOVIES, MainTab.SEARCH) { AddMovieSheet(container, FakeData.movies[2].copy(id = ""), onClose = {}) }),
        ScreenshotEntry("series-home", content = shell(MediaMode.SERIES, MainTab.SEARCH)),
        ScreenshotEntry("series-home-dark", content = shell(MediaMode.SERIES, MainTab.SEARCH, dark = true)),
        ScreenshotEntry("series-library", content = shell(MediaMode.SERIES, MainTab.LIBRARY)),
        ScreenshotEntry("series-library-dark", content = shell(MediaMode.SERIES, MainTab.LIBRARY, dark = true)),
        ScreenshotEntry("series-nextup", content = shell(MediaMode.SERIES, MainTab.NEXT_UP)),
        ScreenshotEntry("series-detail", content = shell(MediaMode.SERIES, MainTab.LIBRARY) { SeriesDetailSheet(container, FakeData.series[0], onClose = {}) }),
        ScreenshotEntry("series-add", content = shell(MediaMode.SERIES, MainTab.SEARCH) { AddSeriesSheet(container, FakeData.series[4].copy(id = ""), onClose = {}) }),
        ScreenshotEntry("series-stats", content = shell(MediaMode.SERIES, MainTab.LIBRARY) { SeriesStatsSheet(container, FakeData.series, onClose = {}) }),
        ScreenshotEntry("settings", content = shell(MediaMode.BOOKS, MainTab.SEARCH) { SettingsPanel(container, onClose = {}, onOpenLegal = {}, onReplayOnboarding = {}) }),
        ScreenshotEntry("settings-dark", content = shell(MediaMode.BOOKS, MainTab.SEARCH, dark = true) { SettingsPanel(container, onClose = {}, onOpenLegal = {}, onReplayOnboarding = {}) }),
        ScreenshotEntry("notif-prompt", content = shell(MediaMode.SERIES, MainTab.SEARCH) { NotificationPromptSheet(container, onDismiss = {}) }),
        ScreenshotEntry("onboarding") { NookTheme(ThemeMode.LIGHT, MediaMode.SERIES) { OnboardingScreen(onFinish = {}) } },
        ScreenshotEntry("login") { NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) { LoginScreen(container) } },
        ScreenshotEntry("login-dark") { NookTheme(ThemeMode.DARK, MediaMode.BOOKS) { LoginScreen(container) } },
        ScreenshotEntry("legal") { NookTheme(ThemeMode.LIGHT, MediaMode.BOOKS) { LegalScreen(LegalKind.PRIVACY, onBack = {}) } },
        ScreenshotEntry("books-library-list", content = shellWithView(MediaMode.BOOKS, MainTab.LIBRARY, list = true)),
        ScreenshotEntry("tablet-books-library", widthDp = 1024, heightDp = 768, content = shell(MediaMode.BOOKS, MainTab.LIBRARY)),
        ScreenshotEntry("tablet-series-home", widthDp = 1024, heightDp = 768, content = shell(MediaMode.SERIES, MainTab.SEARCH)),
    )

    /** Same as [shell] but forces the list view mode of the library. */
    private fun shellWithView(mode: MediaMode, tab: MainTab, list: Boolean): @Composable () -> Unit = {
        remember {
            container.prefs.setMediaMode(mode)
            container.prefs.setViewMode(mode, if (list) fr.paulbr.nookmind.core.data.ViewMode.LIST else fr.paulbr.nookmind.core.data.ViewMode.GRID)
            seed(container)
        }
        NookTheme(ThemeMode.LIGHT, mode) {
            MainScaffold(container, onOpenLegal = {}, onReplayOnboarding = {}, initialTab = tab)
        }
    }
}
