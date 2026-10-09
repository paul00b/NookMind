package fr.paulbr.nookmind.core.network

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.intercept.Interceptor
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.size.Size
import io.ktor.client.HttpClient

/** The app's image loader: posters, covers and photos go through [httpClient]. */
fun nookImageLoader(context: PlatformContext, httpClient: HttpClient, crossfade: Boolean = true): ImageLoader =
    ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient))
            add(GoogleBooksCoverInterceptor())
        }
        .crossfade(crossfade)
        .build()

/**
 * Google Books answers a cover asked at zoom=2 or more with its "image not available" picture when
 * it only holds the small scan of a book, while the zoom=1 picture of that book is the real cover.
 * The covers are saved at zoom=2 (sharper when Google has it), so this swaps the placeholder for the
 * zoom=1 cover at display time, which also repairs the books saved before.
 */
class GoogleBooksCoverInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val fallback = (chain.request.data as? String)?.let(GoogleBooksCovers::zoom1Fallback) ?: return chain.proceed()
        // Decoded at full size: the placeholder is told apart by its exact dimensions.
        val result = chain.withSize(Size.ORIGINAL).proceed()
        if (result !is SuccessResult || !GoogleBooksCovers.isPlaceholder(result.image.width, result.image.height)) return result
        return chain.withRequest(chain.request.newBuilder().data(fallback).build()).proceed()
    }
}

object GoogleBooksCovers {
    private val host = Regex("""^https?://books\.google(usercontent)?\.[a-z.]+/""")
    private val zoom = Regex("""([?&]zoom=)(\d+)""")

    // The placeholder as served at zoom=2 and zoom=3 (PNG; the real covers are JPEGs).
    private val placeholderSizes = setOf(300 to 391, 575 to 750)

    /** The zoom=1 URL of a Google Books cover asked at another zoom, else null. */
    fun zoom1Fallback(url: String): String? {
        if (!host.containsMatchIn(url)) return null
        val level = zoom.find(url)?.groupValues?.get(2) ?: return null
        if (level == "1") return null
        return zoom.replace(url) { it.groupValues[1] + "1" }
    }

    fun isPlaceholder(width: Int, height: Int): Boolean = (width to height) in placeholderSizes
}
