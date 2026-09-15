package fr.paulbr.nookmind.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.paulbr.nookmind.core.model.MediaMode
import fr.paulbr.nookmind.core.model.ThemeMode
import fr.paulbr.nookmind.resources.Res
import fr.paulbr.nookmind.resources.inter_bold
import fr.paulbr.nookmind.resources.inter_light
import fr.paulbr.nookmind.resources.inter_medium
import fr.paulbr.nookmind.resources.inter_regular
import fr.paulbr.nookmind.resources.inter_semibold
import fr.paulbr.nookmind.resources.playfair_bold
import fr.paulbr.nookmind.resources.playfair_italic
import fr.paulbr.nookmind.resources.playfair_regular
import fr.paulbr.nookmind.resources.playfair_semibold
import org.jetbrains.compose.resources.Font

/**
 * Semantic colours of the web app (Tailwind utilities + `.dark` variants), resolved for one theme.
 */
@Immutable
data class NookColors(
    val isDark: Boolean,
    /** `bg-[#f8f6f1] dark:bg-[#0f1117]` */
    val background: Color,
    /** `.card` — `bg-white dark:bg-[#1a1f2e]` */
    val surface: Color,
    /** `bg-gray-100 dark:bg-gray-800` (placeholders, segmented controls, read-only inputs) */
    val surfaceMuted: Color,
    /** `bg-gray-200 dark:bg-gray-700` (skeletons, dividers in cells) */
    val surfaceMuted2: Color,
    /** `bg-gray-50 dark:bg-gray-800/50` (episode tiles) */
    val surfaceSubtle: Color,
    /** `border-black/8 dark:border-white/8` */
    val border: Color,
    /** `border-black/12 dark:border-white/12` */
    val borderStrong: Color,
    /** `border-black/6 dark:border-white/6` */
    val divider: Color,
    /** `border-gray-200 dark:border-gray-700` */
    val borderNeutral: Color,
    /** `text-gray-900 dark:text-gray-100` */
    val textStrong: Color,
    /** `text-gray-800 dark:text-gray-200` */
    val textBody: Color,
    /** `text-gray-700 dark:text-gray-300` */
    val textBody2: Color,
    /** `text-gray-600 dark:text-gray-400` */
    val textMuted: Color,
    /** `text-gray-500 dark:text-gray-400` */
    val textSubtle: Color,
    /** `text-gray-400 dark:text-gray-500` */
    val textFaint: Color,
    /** `text-gray-300 dark:text-gray-600` (empty poster icons) */
    val textDisabled: Color,
    /** `text-amber-600 dark:text-amber-400` */
    val amberText: Color,
    /** `text-amber-700 dark:text-amber-400` */
    val amberTextStrong: Color,
    val indigoText: Color,
    val tealText: Color,
    val emeraldText: Color,
    val blueText: Color,
    val purpleText: Color,
    val redText: Color,
    /** Toast background `#1a1f2e` in both themes. */
    val toastBackground: Color,
    val toastText: Color,
)

val LightNookColors = NookColors(
    isDark = false,
    background = Palette.Cream,
    surface = Palette.White,
    surfaceMuted = Palette.Gray100,
    surfaceMuted2 = Palette.Gray200,
    surfaceSubtle = Palette.Gray50,
    border = Palette.Black.alpha(0.08f),
    borderStrong = Palette.Black.alpha(0.12f),
    divider = Palette.Black.alpha(0.06f),
    borderNeutral = Palette.Gray200,
    textStrong = Palette.Gray900,
    textBody = Palette.Gray800,
    textBody2 = Palette.Gray700,
    textMuted = Palette.Gray600,
    textSubtle = Palette.Gray500,
    textFaint = Palette.Gray400,
    textDisabled = Palette.Gray300,
    amberText = Palette.Amber600,
    amberTextStrong = Palette.Amber700,
    indigoText = Palette.Indigo600,
    tealText = Palette.Teal600,
    emeraldText = Palette.Emerald600,
    blueText = Palette.Blue500,
    purpleText = Palette.Purple500,
    redText = Palette.Red600,
    toastBackground = Palette.NightCard,
    toastText = Palette.Gray100,
)

val DarkNookColors = NookColors(
    isDark = true,
    background = Palette.Night,
    surface = Palette.NightCard,
    surfaceMuted = Palette.Gray800,
    surfaceMuted2 = Palette.Gray700,
    surfaceSubtle = Palette.Gray800.alpha(0.5f),
    border = Palette.White.alpha(0.08f),
    borderStrong = Palette.White.alpha(0.12f),
    divider = Palette.White.alpha(0.06f),
    borderNeutral = Palette.Gray700,
    textStrong = Palette.Gray100,
    textBody = Palette.Gray200,
    textBody2 = Palette.Gray300,
    textMuted = Palette.Gray400,
    textSubtle = Palette.Gray400,
    textFaint = Palette.Gray500,
    textDisabled = Palette.Gray600,
    amberText = Palette.Amber400,
    amberTextStrong = Palette.Amber400,
    indigoText = Palette.Indigo400,
    tealText = Palette.Teal400,
    emeraldText = Palette.Emerald400,
    blueText = Palette.Blue400,
    purpleText = Palette.Purple400,
    redText = Palette.Red400,
    toastBackground = Palette.NightCard,
    toastText = Palette.Gray100,
)

/** Per-mode accent (amber / indigo / teal). */
@Immutable
data class ModeAccent(val base: Color, val strong: Color, val text: Color, val subtle: Color)

fun NookColors.accent(mode: MediaMode): ModeAccent = when (mode) {
    MediaMode.BOOKS -> ModeAccent(Palette.Amber500, Palette.Amber600, amberText, Palette.Amber500.alpha(0.10f))
    MediaMode.MOVIES -> ModeAccent(Palette.Indigo500, Palette.Indigo600, indigoText, Palette.Indigo500.alpha(0.10f))
    MediaMode.SERIES -> ModeAccent(Palette.Teal500, Palette.Teal600, tealText, Palette.Teal500.alpha(0.10f))
}

/** Font families (Inter for UI, Playfair Display for headings) loaded from Compose resources. */
@Immutable
data class NookFonts(val sans: FontFamily, val serif: FontFamily)

/** Tailwind text scale as ready-to-use styles. */
@Immutable
data class NookTypography(
    private val fonts: NookFonts,
) {
    val sans: FontFamily get() = fonts.sans
    val serif: FontFamily get() = fonts.serif

    fun sans(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Int? = null): TextStyle =
        TextStyle(fontFamily = fonts.sans, fontSize = size.sp, fontWeight = weight, lineHeight = (lineHeight ?: defaultLeading(size)).sp)

    fun serif(size: Int, weight: FontWeight = FontWeight.Bold, lineHeight: Int? = null, italic: Boolean = false): TextStyle =
        TextStyle(
            fontFamily = fonts.serif, fontSize = size.sp, fontWeight = weight,
            lineHeight = (lineHeight ?: defaultLeading(size)).sp,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        )

    /** text-xs */ val xs: TextStyle get() = sans(12, lineHeight = 16)
    /** text-sm */ val sm: TextStyle get() = sans(14, lineHeight = 20)
    /** text-base */ val base: TextStyle get() = sans(16, lineHeight = 24)
    /** text-lg */ val lg: TextStyle get() = sans(18, lineHeight = 28)
    /** text-[10px] */ val micro: TextStyle get() = sans(10, lineHeight = 14)
    /** text-[11px] */ val tiny: TextStyle get() = sans(11, lineHeight = 14)

    /** font-serif text-4xl font-bold (page titles on the search screen) */
    val displaySerif: TextStyle get() = serif(36, lineHeight = 40)
    /** font-serif text-3xl font-bold */
    val h1Serif: TextStyle get() = serif(30, lineHeight = 36)
    /** font-serif text-2xl font-bold */
    val h2Serif: TextStyle get() = serif(24, lineHeight = 30)
    /** font-serif text-xl font-bold */
    val h3Serif: TextStyle get() = serif(20, lineHeight = 26)
    /** font-serif text-lg font-bold */
    val titleSerif: TextStyle get() = serif(18, lineHeight = 24)
    /** font-serif font-semibold text-sm (card titles) */
    val cardTitleSerif: TextStyle get() = serif(14, FontWeight.SemiBold, lineHeight = 17)

    private fun defaultLeading(size: Int): Int = when {
        size <= 12 -> 16
        size <= 14 -> 20
        size <= 16 -> 24
        size <= 18 -> 28
        size <= 20 -> 28
        size <= 24 -> 32
        size <= 30 -> 36
        size <= 36 -> 40
        else -> size
    }
}

object NookShapes {
    val sm = RoundedCornerShape(4.dp)
    val md = RoundedCornerShape(6.dp)
    val lg = RoundedCornerShape(8.dp)
    val xl = RoundedCornerShape(12.dp)
    val xl2 = RoundedCornerShape(16.dp)
    val xl3 = RoundedCornerShape(24.dp)
    val sheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val full = RoundedCornerShape(50)
}

val LocalNookColors = staticCompositionLocalOf { LightNookColors }
val LocalNookTypography = staticCompositionLocalOf<NookTypography> { error("NookTheme not provided") }
val LocalMediaMode = staticCompositionLocalOf { MediaMode.BOOKS }

object NookTheme {
    val colors: NookColors
        @Composable @ReadOnlyComposable get() = LocalNookColors.current
    val type: NookTypography
        @Composable @ReadOnlyComposable get() = LocalNookTypography.current
    val mode: MediaMode
        @Composable @ReadOnlyComposable get() = LocalMediaMode.current
    val accent: ModeAccent
        @Composable @ReadOnlyComposable get() = LocalNookColors.current.accent(LocalMediaMode.current)
}

@Composable
fun rememberNookFonts(): NookFonts {
    val sans = FontFamily(
        Font(Res.font.inter_light, FontWeight.Light),
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )
    val serif = FontFamily(
        Font(Res.font.playfair_regular, FontWeight.Normal),
        Font(Res.font.playfair_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.playfair_semibold, FontWeight.SemiBold),
        Font(Res.font.playfair_bold, FontWeight.Bold),
    )
    return NookFonts(sans, serif)
}

@Composable
fun resolveIsDark(themeMode: ThemeMode): Boolean = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

private fun materialScheme(colors: NookColors, accent: ModeAccent): ColorScheme {
    val base = if (colors.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent.base,
        onPrimary = Palette.White,
        background = colors.background,
        onBackground = colors.textBody,
        surface = colors.surface,
        onSurface = colors.textBody,
        surfaceVariant = colors.surfaceMuted,
        onSurfaceVariant = colors.textMuted,
        outline = colors.borderStrong,
        outlineVariant = colors.border,
        surfaceContainer = colors.surface,
        surfaceContainerHigh = colors.surface,
        surfaceContainerHighest = colors.surface,
        surfaceContainerLow = colors.surface,
        surfaceContainerLowest = colors.surface,
        scrim = Palette.Black,
        error = Palette.Red500,
    )
}

@Composable
fun NookTheme(
    themeMode: ThemeMode,
    mediaMode: MediaMode,
    content: @Composable () -> Unit,
) {
    val isDark = resolveIsDark(themeMode)
    val colors = if (isDark) DarkNookColors else LightNookColors
    val fonts = rememberNookFonts()
    val typography = NookTypography(fonts)
    val accent = colors.accent(mediaMode)
    val materialTypography = Typography(
        bodyLarge = typography.base,
        bodyMedium = typography.sm,
        bodySmall = typography.xs,
        labelLarge = typography.sans(14, FontWeight.Medium, 20),
        labelMedium = typography.sans(12, FontWeight.Medium, 16),
        labelSmall = typography.sans(11, FontWeight.Medium, 16),
        titleLarge = typography.h3Serif,
        titleMedium = typography.sans(16, FontWeight.SemiBold, 24),
        titleSmall = typography.sans(14, FontWeight.SemiBold, 20),
        headlineSmall = typography.h2Serif,
        headlineMedium = typography.h1Serif,
    )
    CompositionLocalProvider(
        LocalNookColors provides colors,
        LocalNookTypography provides typography,
        LocalMediaMode provides mediaMode,
    ) {
        MaterialTheme(
            colorScheme = materialScheme(colors, accent),
            typography = materialTypography,
            content = content,
        )
    }
}
