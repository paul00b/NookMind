package fr.paulbr.nookmind.core.designsystem.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.random.Random

actual fun noiseImageBitmap(size: Int, seed: Int): ImageBitmap {
    val random = Random(seed)
    val bytes = ByteArray(size * size * 4)
    var i = 0
    repeat(size * size) {
        val v = random.nextInt(256).toByte()
        bytes[i++] = v // B
        bytes[i++] = v // G
        bytes[i++] = v // R
        bytes[i++] = 0xFF.toByte()
    }
    val info = ImageInfo(size, size, ColorType.BGRA_8888, ColorAlphaType.PREMUL)
    return Image.makeRaster(info, bytes, size * 4).toComposeImageBitmap()
}
