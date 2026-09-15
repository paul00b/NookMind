package fr.paulbr.nookmind.feature.common

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** YouTube embed in a WebView (the `<iframe>` of TrailerModal.tsx), autoplay enabled. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun YouTubeEmbed(videoKey: String, modifier: Modifier) {
    var webView: WebView? = null
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                setBackgroundColor(android.graphics.Color.BLACK)
                loadUrl("https://www.youtube.com/embed/$videoKey?autoplay=1&rel=0&playsinline=1")
                webView = this
            }
        },
    )
    DisposableEffect(videoKey) {
        onDispose {
            webView?.apply { loadUrl("about:blank"); destroy() }
            webView = null
        }
    }
}
