package fr.paulbr.nookmind.core.designsystem.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun noiseImageBitmap(size: Int, seed: Int): ImageBitmap {
    // createBitmap(IntArray) takes non-premultiplied ARGB and premultiplies internally.
    val bitmap = Bitmap.createBitmap(noisePixels(size, seed), size, size, Bitmap.Config.ARGB_8888)
    return bitmap.asImageBitmap()
}
