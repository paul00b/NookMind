package fr.paulbr.nookmind.feature.books

import fr.paulbr.nookmind.app.AppContainer
import fr.paulbr.nookmind.core.domain.normalizeTitle
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.BookStatus
import fr.paulbr.nookmind.core.platform.logDebug
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.home_scanAlreadyIn
import fr.paulbr.nookmind.resources.home_scanFailed
import fr.paulbr.nookmind.resources.home_scanNotFound
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Scan a book's barcode, find it, put it in the library and open it.
 *
 * Runs in the app's scope rather than the screen's: while the camera is up the screen can leave
 * the composition, and the code it read must not be lost with it. Every way it ends says so: the
 * book opens (added to "Want to read", or already there), or a message says why not.
 */
class BookScan(private val container: AppContainer) {
    private val _searching = MutableStateFlow(false)
    /** Between the code read and the answer: the books screen shows a spinner. */
    val searching: StateFlow<Boolean> = _searching

    private val _opened = MutableStateFlow<Book?>(null)
    /** The library book the scan led to, for the books screen to open. */
    val opened: StateFlow<Book?> = _opened

    private var job: Job? = null

    fun close() { _opened.value = null }

    fun start() {
        if (job?.isActive == true) return
        // The camera screens are UI: started from the main thread.
        job = container.scope.launch(Dispatchers.Main) {
            val code = try {
                container.barcodeScanner.scan()
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                logDebug("scan", "scanner failed", t)
                container.toasts.error(Res.string.home_scanFailed)
                return@launch
            }
            if (code.isNullOrBlank()) return@launch // Closed without a code.
            _searching.value = true
            try {
                val found = container.googleBooks.findByIsbn(code)
                if (found == null) {
                    container.toasts.error(Res.string.home_scanNotFound, code)
                    return@launch
                }
                val existing = container.books.items.value.firstOrNull { same(it, found) }
                if (existing != null) {
                    container.toasts.success(Res.string.home_scanAlreadyIn)
                    _opened.value = existing
                    return@launch
                }
                // The repository says "added" itself, or why it could not.
                _opened.value = container.books.add(found.copy(status = BookStatus.WANT_TO_READ)) ?: return@launch
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                logDebug("scan", "lookup failed", t)
                container.toasts.error(Res.string.home_scanNotFound, code)
            } finally {
                _searching.value = false
            }
        }
    }

    private fun same(a: Book, b: Book): Boolean =
        (a.googleBooksId != null && a.googleBooksId == b.googleBooksId) ||
            (normalizeTitle(a.title) == normalizeTitle(b.title) && normalizeTitle(a.author) == normalizeTitle(b.author))
}
