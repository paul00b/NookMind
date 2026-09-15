package fr.paulbr.nookmind.feature.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.paulbr.nookmind.core.designsystem.NookShapes
import fr.paulbr.nookmind.core.designsystem.NookTheme
import fr.paulbr.nookmind.core.designsystem.Palette
import fr.paulbr.nookmind.core.designsystem.icons.LucideIcons
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.logo
import fr.paulbr.nookmind.resources.onboarding_getStarted
import fr.paulbr.nookmind.resources.onboarding_skip
import fr.paulbr.nookmind.resources.onboarding_slide1Body
import fr.paulbr.nookmind.resources.onboarding_slide1Title
import fr.paulbr.nookmind.resources.onboarding_slide2Body
import fr.paulbr.nookmind.resources.onboarding_slide2Title
import fr.paulbr.nookmind.resources.onboarding_slide3Body
import fr.paulbr.nookmind.resources.onboarding_slide3Title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val SLIDE_ACCENTS = listOf(Palette.Amber500, Palette.Indigo500, Palette.Teal500)

/** Port of Onboarding.tsx: three swipeable slides on the dark background, dots, Skip, Get started. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val titles = listOf(Res.string.onboarding_slide1Title, Res.string.onboarding_slide2Title, Res.string.onboarding_slide3Title)
    val bodies = listOf(Res.string.onboarding_slide1Body, Res.string.onboarding_slide2Body, Res.string.onboarding_slide3Body)

    Box(Modifier.fillMaxSize().background(Palette.Night)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            OnboardingSlide(
                title = stringResource(titles[page]),
                body = stringResource(bodies[page]),
                gradientColor = SLIDE_ACCENTS[page],
            ) {
                when (page) {
                    0 -> Slide1Illustration()
                    1 -> Slide2Illustration()
                    else -> Slide3Illustration()
                }
            }
        }

        // Skip
        Text(
            stringResource(Res.string.onboarding_skip),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
                .clip(NookShapes.lg)
                .clickable(onClick = onFinish)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = NookTheme.type.sm,
            color = Palette.White.copy(alpha = 0.7f),
        )

        // Bottom: Get started + dots
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (pagerState.currentPage == 2) {
                Box(
                    Modifier
                        .shadow(12.dp, NookShapes.full, ambientColor = Palette.Teal500.copy(alpha = 0.27f), spotColor = Palette.Teal500.copy(alpha = 0.27f))
                        .clip(NookShapes.full)
                        .background(Brush.linearGradient(listOf(Palette.Teal500, Palette.Teal600)), NookShapes.full)
                        .clickable(onClick = onFinish)
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(Res.string.onboarding_getStarted), style = NookTheme.type.sans(14, FontWeight.SemiBold, 20), color = Palette.White)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    val active = pagerState.currentPage == i
                    val width by androidx.compose.animation.core.animateDpAsState(if (active) 20.dp else 6.dp, tween(300), label = "dot")
                    val color by androidx.compose.animation.animateColorAsState(if (active) SLIDE_ACCENTS[i] else Palette.Gray600, tween(300), label = "dotColor")
                    Box(Modifier.width(width).height(6.dp).clip(NookShapes.full).background(color, NookShapes.full))
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlide(title: String, body: String, gradientColor: Color, illustration: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Palette.Night)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val radiusX = w * 0.8f
            val center = Offset(w / 2f, -h * 0.1f)
            scale(scaleX = 1f, scaleY = (h * 0.4f) / radiusX, pivot = center) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(gradientColor.copy(alpha = 0.55f), gradientColor.copy(alpha = 0f)), center, radiusX),
                    radius = radiusX,
                    center = center,
                )
            }
        }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.padding(bottom = 32.dp)) { illustration() }
            Text(
                title,
                style = NookTheme.type.sans(24, FontWeight.Bold, 32),
                color = Palette.Gray100,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp).padding(bottom = 12.dp),
            )
            Text(
                body,
                style = NookTheme.type.sans(16, lineHeight = 26),
                color = Palette.Gray400,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
    }
}

/** Gentle float animation (the `float1/2/3` keyframes). */
@Composable
private fun floatOffset(durationMs: Int, dx: Float, dy: Float, delayMs: Int = 0): Pair<Float, Float> {
    val transition = rememberInfiniteTransition(label = "float")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(durationMs, delayMillis = delayMs, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    val wave = kotlin.math.sin(t * 2f * kotlin.math.PI.toFloat()) * 0.5f + 0.5f
    return dx * wave to dy * wave
}

@Composable
private fun Slide1Illustration() {
    val (x1, y1) = floatOffset(6000, 4f, -6f)
    val (x2, y2) = floatOffset(7000, -5f, 5f)
    val (x3, y3) = floatOffset(8000, 3f, 4f)
    Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Text("🎬", style = NookTheme.type.sans(30), modifier = Modifier.align(Alignment.TopStart).offset(x1.dp, y1.dp).alpha(0.15f))
        Text("📺", style = NookTheme.type.sans(24), modifier = Modifier.align(Alignment.TopEnd).offset(x2.dp, (16 + y2).dp).alpha(0.12f))
        Text("📖", style = NookTheme.type.sans(24), modifier = Modifier.align(Alignment.BottomStart).offset((16 + x3).dp, (-8 + y3).dp).alpha(0.10f))
        Image(
            painterResource(Res.drawable.logo),
            contentDescription = "NookMind",
            modifier = Modifier.size(80.dp).shadow(24.dp, NookShapes.xl3, ambientColor = Palette.Amber500.copy(alpha = 0.27f), spotColor = Palette.Amber500.copy(alpha = 0.27f)),
        )
    }
}

@Composable
private fun Slide2Illustration() {
    val icons: List<ImageVector> = listOf(LucideIcons.Star, LucideIcons.Bell, LucideIcons.Drama, LucideIcons.Satellite)
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        icons.forEachIndexed { i, icon ->
            val (x, y) = floatOffset(6000 + i * 1000, if (i % 3 == 1) -5f else 4f, if (i % 3 == 0) -6f else 5f, delayMs = i * 1500)
            Box(
                Modifier
                    .offset(x.dp, y.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.Indigo500.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                    .border(1.dp, Palette.Indigo500.copy(alpha = 0.20f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(24.dp), tint = Palette.Indigo400)
            }
        }
    }
}

@Composable
private fun Slide3Illustration() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
        repeat(3) { i ->
            val middle = i == 1
            val start = Palette.Teal500.copy(alpha = if (middle) 0.30f else 0.20f)
            val end = Palette.Teal500.copy(alpha = if (middle) 0.10f else 0.05f)
            Box(
                Modifier
                    .size(width = 48.dp, height = 68.dp)
                    .scale(if (middle) 1.1f else 1f)
                    .clip(NookShapes.lg)
                    .background(Brush.linearGradient(listOf(start, end)), NookShapes.lg)
                    .border(1.dp, Palette.Teal500.copy(alpha = if (middle) 0.30f else 0.20f), NookShapes.lg),
            )
        }
    }
    Spacer(Modifier.height(0.dp))
}
