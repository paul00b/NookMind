package fr.paulbr.nookmind.core.network

import fr.paulbr.nookmind.core.config.AppConfig
import fr.paulbr.nookmind.core.model.Book
import fr.paulbr.nookmind.core.model.GoogleBookVolume
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/** Port of src/lib/googleBooks.ts */
class GoogleBooksApi(private val client: HttpClient) {
    @Serializable
    private data class VolumesResponse(val items: List<GoogleBookVolume>? = null)

    /** Throws on network/HTTP failure so the UI can show the "search timed out" message. */
    suspend fun searchBooks(query: String, maxResults: Int = 8): List<GoogleBookVolume> {
        if (query.isBlank()) return emptyList()
        val response = client.get(BASE_URL) {
            parameter("q", query)
            parameter("maxResults", maxResults)
            parameter("printType", "books")
            AppConfig.googleBooksApiKey?.let { parameter("key", it) }
            timeout { requestTimeoutMillis = 8_000 }
        }
        if (response.status.value !in 200..299) throw IllegalStateException("Failed to fetch books (${response.status.value})")
        return response.body<VolumesResponse>().items ?: emptyList()
    }

    companion object {
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
