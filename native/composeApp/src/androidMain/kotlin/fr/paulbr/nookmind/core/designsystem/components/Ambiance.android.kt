package fr.paulbr.nookmind.core.designsystem.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

actual fun noiseImageBitmap(size: Int, seed: Int): ImageBitmap {
    val random = Random(seed)
    val pixels = IntArray(size * size) {
        val v = random.nextInt(256)
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    val bitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    return bitmap.asImageBitmap()
}
