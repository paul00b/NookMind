package fr.paulbr.nookmind.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

/**
 * Headless renderer used to snapshot screens to PNG without a window (design review, CI).
 *
 * Usage (desktop): `./gradlew :composeApp:screenshots -PoutDir=build/screenshots`
 * Every entry of [ScreenshotCatalog.entries] is rendered at phone size (390x844 @ 3x).
 */
object Screenshots {
    const val WIDTH_DP = 390
    const val HEIGHT_DP = 844

    @OptIn(ExperimentalComposeUiApi::class)
    fun render(
        outFile: File,
        widthDp: Int = WIDTH_DP,
        heightDp: Int = HEIGHT_DP,
        scale: Float = 2f,
        settleFrames: Int = 30,
        content: @Composable () -> Unit,
    ) {
        val density = Density(scale)
        ImageComposeScene(
            width = (widthDp * scale).toInt(),
            height = (heightDp * scale).toInt(),
            density = density,
            content = content,
        ).use { scene ->
            // Let LaunchedEffects, async image loads and animations settle.
            var time = 0L
            repeat(settleFrames) {
                time += 16_666_667L
                scene.render(time)
                Thread.sleep(16)
            }
            val image = scene.render(time + 16_666_667L)
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG encoding failed")
            outFile.parentFile?.mkdirs()
            outFile.writeBytes(data.bytes)
        }
    }
}

fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "build/screenshots")
    val only = args.drop(1).toSet()
    val entries = ScreenshotCatalog.entries.filter { only.isEmpty() || it.name in only }
    entries.forEach { entry ->
        val file = File(outDir, "${entry.name}.png")
        Screenshots.render(file, content = entry.content)
        println("wrote ${file.path}")
    }
    println("done: ${entries.size} screenshot(s) in ${outDir.path}")
}
