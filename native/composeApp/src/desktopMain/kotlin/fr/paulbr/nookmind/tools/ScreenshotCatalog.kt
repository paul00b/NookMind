package fr.paulbr.nookmind.tools

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.App
import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.model.GoogleBookVolume
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.ThemeMode
import fr.paulbr.nookmind.feature.books.AddBookSheet
import fr.paulbr.nookmind.feature.books.BookDetailSheet
import fr.paulbr.nookmind.feature.common.HomeScreenScaffold
import fr.paulbr.nookmind.feature.common.HomeSection
import fr.paulbr.nookmind.feature.common.SearchResultRow
import fr.paulbr.nookmind.feature.common.rememberSearchController
import fr.paulbr.nookmind.feature.shell.MainScaffold
import fr.paulbr.nookmind.feature.shell.MainTab

class ScreenshotEntry(val name: String, val content: @Composable () -> Unit)

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
        ScreenshotEntry("books-home", shell(MediaMode.BOOKS, MainTab.SEARCH)),
        ScreenshotEntry("books-home-dark", shell(MediaMode.BOOKS, MainTab.SEARCH, dark = true)),
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
        ScreenshotEntry("books-library", shell(MediaMode.BOOKS, MainTab.LIBRARY)),
        ScreenshotEntry("books-library-dark", shell(MediaMode.BOOKS, MainTab.LIBRARY, dark = true)),
        ScreenshotEntry("books-nextup", shell(MediaMode.BOOKS, MainTab.NEXT_UP)),
        ScreenshotEntry("books-detail", shell(MediaMode.BOOKS, MainTab.LIBRARY) { BookDetailSheet(container, FakeData.books[0], onClose = {}) }),
        ScreenshotEntry("books-add", shell(MediaMode.BOOKS, MainTab.SEARCH) { AddBookSheet(container, FakeData.books[3].copy(id = "", rating = null, personalNote = null), onClose = {}) }),
    )
}
