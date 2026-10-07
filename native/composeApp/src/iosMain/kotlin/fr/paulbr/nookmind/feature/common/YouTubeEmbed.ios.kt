package fr.paulbr.nookmind.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSBundle
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.WebKit.WKAudiovisualMediaTypeNone
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

/**
 * YouTube embed in a WKWebView (the `<iframe>` of TrailerModal.tsx), autoplay enabled.
 *
 * The video is loaded once, in the factory, and `key(videoKey)` recreates the view when the key
 * changes. Loading from `update` instead would restart playback on every unrelated recomposition,
 * since `update` runs whenever the composable does. Same lifecycle as the Android actual, which
 * loads in the factory and disposes on key change.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun YouTubeEmbed(videoKey: String, modifier: Modifier) {
    key(videoKey) {
        UIKitView(
            factory = {
                val configuration = WKWebViewConfiguration().apply {
                    // Without these two, iOS opens the player full screen and waits for a tap.
                    allowsInlineMediaPlayback = true
                    mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeNone
                }
                WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
                    opaque = false
                    backgroundColor = UIColor.blackColor
                    scrollView.scrollEnabled = false
                    scrollView.bounces = false
                    // YouTube refuses an embed whose request carries no Referer (error 153), and a
                    // WKWebView loading the URL directly sends none: same fix as on Android, the
                    // identity YouTube documents for apps, https://<bundle id>.
                    val referer = "https://${(NSBundle.mainBundle.bundleIdentifier ?: "fr.paulbr.nookmind").lowercase()}"
                    NSURL.URLWithString("https://www.youtube.com/embed/$videoKey?autoplay=1&rel=0&playsinline=1")
                        ?.let { url ->
                            val request = NSMutableURLRequest.requestWithURL(url)
                            request.setValue(referer, forHTTPHeaderField = "Referer")
                            loadRequest(request)
                        }
                }
            },
            modifier = modifier,
            onRelease = { webView ->
                webView.stopLoading()
                // Blank page rather than a lingering player: the audio would otherwise keep going.
                webView.loadHTMLString("", baseURL = null)
            },
        )
    }
}
