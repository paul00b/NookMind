package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun noiseImageBitmap(size: Int, seed: Int): ImageBitmap {
    val pixels = noisePixels(size, seed)
    val bytes = ByteArray(size * size * 4)
    var i = 0
    for (argb in pixels) {
        bytes[i++] = (argb and 0xFF).toByte() // B
        bytes[i++] = ((argb shr 8) and 0xFF).toByte() // G
        bytes[i++] = ((argb shr 16) and 0xFF).toByte() // R
        bytes[i++] = ((argb ushr 24) and 0xFF).toByte() // A
    }
    val info = ImageInfo(size, size, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)
    return Image.makeRaster(info, bytes, size * 4).toComposeImageBitmap()
}
