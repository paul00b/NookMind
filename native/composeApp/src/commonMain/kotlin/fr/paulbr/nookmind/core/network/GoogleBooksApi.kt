package fr.paulbr.nookmind.core.network

import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.GoogleBookVolume
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

/** Port of src/lib/googleBooks.ts */
class GoogleBooksApi(private val client: HttpClient) {
    @Serializable
    private data class VolumesResponse(val items: List<GoogleBookVolume>? = null)

    /** Throws on network/HTTP failure so the UI can show the "search timed out" message. */
    suspend fun searchBooks(query: String, maxResults: Int = 8): List<GoogleBookVolume> {
        if (query.isBlank()) return emptyList()
        return retryWhenBusy {
            val response = client.get(BASE_URL) {
                parameter("q", query)
                parameter("maxResults", maxResults)
                parameter("printType", "books")
                AppConfig.googleBooksApiKey?.let { parameter("key", it) }
                timeout { requestTimeoutMillis = 8_000 }
            }
            response.ensureOk()
            response.body<VolumesResponse>().items ?: emptyList()
        }
    }

    /** One volume by its id (a friend's book), or null when it cannot be read. */
    suspend fun fetchVolume(id: String): GoogleBookVolume? = runCatching {
        retryWhenBusy {
            val response = client.get("$BASE_URL/$id") {
                AppConfig.googleBooksApiKey?.let { parameter("key", it) }
                timeout { requestTimeoutMillis = 8_000 }
            }
            response.ensureOk()
            response.body<GoogleBookVolume>()
        }
    }.getOrNull()

    /**
     * The book behind a scanned barcode: Google Books first (`isbn:`), then Open Library, which knows
     * some editions Google does not. Null when neither has it.
     */
    suspend fun findByIsbn(code: String): Book? {
        val isbn = code.filter { it.isDigit() || it == 'X' }
        if (isbn.length != 10 && isbn.length != 13) return null
        runCatching { searchBooks("isbn:$isbn", 1).firstOrNull() }.getOrNull()?.let { return extractBookData(it) }
        return runCatching { openLibraryBook(isbn) }.getOrNull()
    }

    @Serializable
    private data class OpenLibrarySearch(val docs: List<OpenLibraryDoc> = emptyList())

    @Serializable
    private data class OpenLibraryDoc(
        val title: String = "",
        @kotlinx.serialization.SerialName("author_name") val authorName: List<String>? = null,
        @kotlinx.serialization.SerialName("cover_i") val coverId: Long? = null,
        @kotlinx.serialization.SerialName("number_of_pages_median") val pages: Int? = null,
        @kotlinx.serialization.SerialName("first_publish_year") val year: Int? = null,
    )

    private suspend fun openLibraryBook(isbn: String): Book? {
        val response = client.get("https://openlibrary.org/search.json") {
            parameter("isbn", isbn)
            parameter("fields", "title,author_name,cover_i,number_of_pages_median,first_publish_year")
            timeout { requestTimeoutMillis = 8_000 }
        }
        if (response.status.value !in 200..299) return null
        val doc = response.body<OpenLibrarySearch>().docs.firstOrNull()?.takeIf { it.title.isNotBlank() } ?: return null
        return Book(
            title = doc.title,
            author = doc.authorName?.joinToString(", ")?.ifBlank { null } ?: "Unknown Author",
            coverUrl = doc.coverId?.let { "https://covers.openlibrary.org/b/id/$it-L.jpg" },
            publishedDate = doc.year?.toString(),
            pageCount = doc.pages?.takeIf { it > 0 },
        )
    }

    private fun HttpResponse.ensureOk() {
        val code = status.value
        if (code == 429 || code in 500..599) throw BusyException(code)
        if (code !in 200..299) throw IllegalStateException("Failed to fetch books ($code)")
    }

    /** Google Books turned the request down for now (a 503 or a 429): the same request a moment later goes through. */
    internal class BusyException(val status: Int) : IllegalStateException("Google Books is busy ($status)")

    companion object {
        /**
         * Runs [block] again after a short pause while Google Books is busy. It answers a few percent
         * of requests with a 503 "backendFailed" whatever the query, and the search showed it as a
         * timeout. Only a busy answer is asked again: a timeout has already made the reader wait.
         */
        internal suspend fun <T> retryWhenBusy(attempts: Int = 3, pauseMs: Long = 300, block: suspend () -> T): T {
            repeat(attempts - 1) { attempt ->
                try {
                    return block()
                } catch (e: BusyException) {
                    delay(pauseMs * (attempt + 1))
                }
            }
            return block()
        }

        private const val BASE_URL = "https://www.googleapis.com/books/v1/volumes"

        /** `extractBookData` — the Book is not yet persisted (no id / user / created_at). */
        fun extractBookData(volume: GoogleBookVolume): Book {
            val info = volume.volumeInfo
            val thumbnail = info.imageLinks?.thumbnail ?: info.imageLinks?.smallThumbnail
            val coverUrl = thumbnail?.replace("http://", "https://")?.replace("zoom=1", "zoom=2")
            return Book(
                googleBooksId = volume.id,
                title = info.title.ifBlank { "Unknown Title" },
                author = info.authors?.joinToString(", ")?.ifBlank { null } ?: "Unknown Author",
                description = info.description?.ifBlank { null },
                coverUrl = coverUrl,
                publishedDate = info.publishedDate?.ifBlank { null },
                pageCount = info.pageCount?.takeIf { it > 0 },
                genre = info.categories?.firstOrNull()?.ifBlank { null },
            )
        }

        fun secureThumbnail(url: String?): String? = url?.replace("http://", "https://")
    }
}
