// GENERATED FILE — do not edit by hand. Run: node native/tools/gen-icons.mjs <lucide icons dir>
// Icons: Lucide (https://lucide.dev) — ISC License.
@file:Suppress("unused", "ObjectPropertyName", "MaxLineLength")

package fr.paulbr.nookmind.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Lucide icon set used by the web app, as Compose ImageVectors (24x24, 2px round stroke). */
object LucideIcons {

    private fun lucide(name: String, paths: List<String>): ImageVector {
        val builder = ImageVector.Builder(
            name = "Lucide.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                pathFillType = PathFillType.NonZero,
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }

    val Search: ImageVector by lazy { lucide("Search", listOf("m21 21-4.34-4.34", "M 3 11 A 8 8 0 1 0 19 11 A 8 8 0 1 0 3 11 Z")) }
    val X: ImageVector by lazy { lucide("X", listOf("M18 6 6 18", "m6 6 12 12")) }
    val BookOpen: ImageVector by lazy { lucide("BookOpen", listOf("M12 5v16", "M20.001 19A 2 2 0 0 0 22 17 V5a 2 2 0 0 0 -1.999 -2 L16 3.002A 5 5 0 0 0 12 5 a 5 5 0 0 0 -4 -2 H4a 2 2 0 0 0 -2 2 v12a 2 2 0 0 0 1.999 2 H8a 5 5 0 0 1 4 2 5 5 0 0 1 4 -2 z")) }
    val CheckCircle2: ImageVector by lazy { lucide("CheckCircle2", listOf("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12 Z", "m16 9-5.5 5.5L8 12")) }
    val Film: ImageVector by lazy { lucide("Film", listOf("M 5 3 H 19 A 2 2 0 0 1 21 5 V 19 A 2 2 0 0 1 19 21 H 5 A 2 2 0 0 1 3 19 V 5 A 2 2 0 0 1 5 3 Z", "M7 3v18", "M3 7.5h4", "M3 12h18", "M3 16.5h4", "M17 3v18", "M17 7.5h4", "M17 16.5h4")) }
    val Bookmark: ImageVector by lazy { lucide("Bookmark", listOf("M17 3a 2 2 0 0 1 2 2 v15a 1 1 0 0 1 -1.496 .868 l-4.512-2.578a 2 2 0 0 0 -1.984 0 l-4.512 2.578A 1 1 0 0 1 5 20 V5a 2 2 0 0 1 2 -2 z")) }
    val CheckCheck: ImageVector by lazy { lucide("CheckCheck", listOf("M18 6 7 17l-5-5", "m22 10-7.5 7.5L13 16")) }
    val Tv: ImageVector by lazy { lucide("Tv", listOf("m17 2-5 5-5-5", "M 4 7 H 20 A 2 2 0 0 1 22 9 V 20 A 2 2 0 0 1 20 22 H 4 A 2 2 0 0 1 2 20 V 9 A 2 2 0 0 1 4 7 Z")) }
    val Eye: ImageVector by lazy { lucide("Eye", listOf("M2.062 12.348a 1 1 0 0 1 0 -.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1 -19.876 0", "M 9 12 A 3 3 0 1 0 15 12 A 3 3 0 1 0 9 12 Z")) }
    val EyeOff: ImageVector by lazy { lucide("EyeOff", listOf("M10.733 5.076a 10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1 -1.444 2.49", "M14.084 14.158a 3 3 0 0 1 -4.242 -4.242", "M17.479 17.499a 10.75 10.75 0 0 1 -15.417 -5.151 1 1 0 0 1 0 -.696 10.75 10.75 0 0 1 4.446 -5.143", "m2 2 20 20")) }
    val Play: ImageVector by lazy { lucide("Play", listOf("M5 5a 2 2 0 0 1 3.008 -1.728 l11.997 6.998a 2 2 0 0 1 .003 3.458 l-12 7A 2 2 0 0 1 5 19 z")) }
    val Clock: ImageVector by lazy { lucide("Clock", listOf("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12 Z", "M12 6v6l4 2")) }
    val Clock3: ImageVector by lazy { lucide("Clock3", listOf("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12 Z", "M12 6v6h4")) }
    val CalendarDays: ImageVector by lazy { lucide("CalendarDays", listOf("M8 2v3", "M16 2v3", "M 5 3 H 19 A 2 2 0 0 1 21 5 V 19 A 2 2 0 0 1 19 21 H 5 A 2 2 0 0 1 3 19 V 5 A 2 2 0 0 1 5 3 Z", "M3 9h18", "M8 13h.01", "M12 13h.01", "M16 13h.01", "M8 17h.01", "M12 17h.01", "M16 17h.01")) }
    val Check: ImageVector by lazy { lucide("Check", listOf("M20 6 9 17l-5-5")) }
    val Star: ImageVector by lazy { lucide("Star", listOf("M11.525 2.295a .53 .53 0 0 1 .95 0 l2.31 4.679a 2.123 2.123 0 0 0 1.595 1.16 l5.166.756a .53 .53 0 0 1 .294 .904 l-3.736 3.638a 2.123 2.123 0 0 0 -.611 1.878 l.882 5.14a .53 .53 0 0 1 -.771 .56 l-4.618-2.428a 2.122 2.122 0 0 0 -1.973 0 L6.396 21.01a .53 .53 0 0 1 -.77 -.56 l.881-5.139a 2.122 2.122 0 0 0 -.611 -1.879 L2.16 9.795a .53 .53 0 0 1 .294 -.906 l5.165-.755a 2.122 2.122 0 0 0 1.597 -1.16 z")) }
    val ChevronDown: ImageVector by lazy { lucide("ChevronDown", listOf("m6 9 6 6 6-6")) }
    val ChevronUp: ImageVector by lazy { lucide("ChevronUp", listOf("m18 15-6-6-6 6")) }
    val ChevronLeft: ImageVector by lazy { lucide("ChevronLeft", listOf("m15 18-6-6 6-6")) }
    val ChevronRight: ImageVector by lazy { lucide("ChevronRight", listOf("m9 18 6-6-6-6")) }
    val LayoutGrid: ImageVector by lazy { lucide("LayoutGrid", listOf("M 4 3 H 9 A 1 1 0 0 1 10 4 V 9 A 1 1 0 0 1 9 10 H 4 A 1 1 0 0 1 3 9 V 4 A 1 1 0 0 1 4 3 Z", "M 15 3 H 20 A 1 1 0 0 1 21 4 V 9 A 1 1 0 0 1 20 10 H 15 A 1 1 0 0 1 14 9 V 4 A 1 1 0 0 1 15 3 Z", "M 15 14 H 20 A 1 1 0 0 1 21 15 V 20 A 1 1 0 0 1 20 21 H 15 A 1 1 0 0 1 14 20 V 15 A 1 1 0 0 1 15 14 Z", "M 4 14 H 9 A 1 1 0 0 1 10 15 V 20 A 1 1 0 0 1 9 21 H 4 A 1 1 0 0 1 3 20 V 15 A 1 1 0 0 1 4 14 Z")) }
    val List: ImageVector by lazy { lucide("List", listOf("M3 5h.01", "M3 12h.01", "M3 19h.01", "M8 5h13", "M8 12h13", "M8 19h13")) }
    val Plus: ImageVector by lazy { lucide("Plus", listOf("M5 12h14", "M12 5v14")) }
    val Trash2: ImageVector by lazy { lucide("Trash2", listOf("M10 11v6", "M14 11v6", "M19 6v14a 2 2 0 0 1 -2 2 H7a 2 2 0 0 1 -2 -2 V6", "M3 6h18", "M8 6V4a 2 2 0 0 1 2 -2 h4a 2 2 0 0 1 2 2 v2")) }
    val FolderOpen: ImageVector by lazy { lucide("FolderOpen", listOf("m6 14 1.5-2.9A 2 2 0 0 1 9.24 10 H20a 2 2 0 0 1 1.94 2.5 l-1.54 6a 2 2 0 0 1 -1.95 1.5 H4a 2 2 0 0 1 -2 -2 V5a 2 2 0 0 1 2 -2 h3.9a 2 2 0 0 1 1.69 .9 l.81 1.2a 2 2 0 0 0 1.67 .9 H18a 2 2 0 0 1 2 2 v2")) }
    val BarChart2: ImageVector by lazy { lucide("BarChart2", listOf("M3 3v16a 2 2 0 0 0 2 2 h16", "M18 17V9", "M13 17V5", "M8 17v-3")) }
    val Pencil: ImageVector by lazy { lucide("Pencil", listOf("M21.174 6.812a 1 1 0 0 0 -3.986 -3.987 L3.842 16.174a 2 2 0 0 0 -.5 .83 l-1.321 4.352a .5 .5 0 0 0 .623 .622 l4.353-1.32a 2 2 0 0 0 .83 -.497 z", "m15 5 4 4")) }
    val FolderPlus: ImageVector by lazy { lucide("FolderPlus", listOf("M12 10v6", "M9 13h6", "M20 20a 2 2 0 0 0 2 -2 V8a 2 2 0 0 0 -2 -2 h-7.9a 2 2 0 0 1 -1.69 -.9 L9.6 3.9A 2 2 0 0 0 7.93 3 H4a 2 2 0 0 0 -2 2 v13a 2 2 0 0 0 2 2 Z")) }
    val FolderMinus: ImageVector by lazy { lucide("FolderMinus", listOf("M9 13h6", "M20 20a 2 2 0 0 0 2 -2 V8a 2 2 0 0 0 -2 -2 h-7.9a 2 2 0 0 1 -1.69 -.9 L9.6 3.9A 2 2 0 0 0 7.93 3 H4a 2 2 0 0 0 -2 2 v13a 2 2 0 0 0 2 2 Z")) }
    val ArrowLeftRight: ImageVector by lazy { lucide("ArrowLeftRight", listOf("M8 3 4 7l4 4", "M4 7h16", "m16 21 4-4-4-4", "M20 17H4")) }
    val ArrowLeft: ImageVector by lazy { lucide("ArrowLeft", listOf("m12 19-7-7 7-7", "M19 12H5")) }
    val Loader2: ImageVector by lazy { lucide("Loader2", listOf("M21 12a 9 9 0 1 1 -6.219 -8.56")) }
    val User: ImageVector by lazy { lucide("User", listOf("M19 21v-2a 4 4 0 0 0 -4 -4 H9a 4 4 0 0 0 -4 4 v2", "M 8 7 A 4 4 0 1 0 16 7 A 4 4 0 1 0 8 7 Z")) }
    val Sun: ImageVector by lazy { lucide("Sun", listOf("M 8 12 A 4 4 0 1 0 16 12 A 4 4 0 1 0 8 12 Z", "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41")) }
    val Moon: ImageVector by lazy { lucide("Moon", listOf("M20.985 12.486a 9 9 0 1 1 -9.473 -9.472 c.405-.022.617.46.402.803a 6 6 0 0 0 8.268 8.268 c.344-.215.825-.004.803.401")) }
    val Monitor: ImageVector by lazy { lucide("Monitor", listOf("M 4 3 H 20 A 2 2 0 0 1 22 5 V 15 A 2 2 0 0 1 20 17 H 4 A 2 2 0 0 1 2 15 V 5 A 2 2 0 0 1 4 3 Z", "M 0 0 L 0 0", "M 0 0 L 0 0")) }
    val RefreshCw: ImageVector by lazy { lucide("RefreshCw", listOf("M3 12a 9 9 0 0 1 9 -9 9.75 9.75 0 0 1 6.74 2.74 L21 8", "M21 3v5h-5", "M21 12a 9 9 0 0 1 -9 9 9.75 9.75 0 0 1 -6.74 -2.74 L3 16", "M8 16H3v5")) }
    val RotateCcw: ImageVector by lazy { lucide("RotateCcw", listOf("M3 12a 9 9 0 1 0 9 -9 9.75 9.75 0 0 0 -6.74 2.74 L3 8", "M3 3v5h5")) }
    val Bell: ImageVector by lazy { lucide("Bell", listOf("M10.268 21a 2 2 0 0 0 3.464 0", "M3.262 15.326A 1 1 0 0 0 4 17 h16a 1 1 0 0 0 .74 -1.673 C19.41 13.956 18 12.499 18 8A 6 6 0 0 0 6 8 c0 4.499-1.411 5.956-2.738 7.326")) }
    val BellOff: ImageVector by lazy { lucide("BellOff", listOf("M10.268 21a 2 2 0 0 0 3.464 0", "M17 17H4a 1 1 0 0 1 -.74 -1.673 C4.59 13.956 6 12.499 6 8a 6 6 0 0 1 .258 -1.742", "m2 2 20 20", "M8.668 3.01A 6 6 0 0 1 18 8 c0 2.687.77 4.653 1.707 6.05")) }
    val Clapperboard: ImageVector by lazy { lucide("Clapperboard", listOf("m12.296 3.464 3.02 3.956", "M20.2 6 3 11l-.9-2.4c-.3-1.1.3-2.2 1.3-2.5l13.5-4c1.1-.3 2.2.3 2.5 1.3z", "M3 11h18v8a 2 2 0 0 1 -2 2 H5a 2 2 0 0 1 -2 -2 z", "m6.18 5.276 3.1 3.899")) }
    val Send: ImageVector by lazy { lucide("Send", listOf("M14.536 21.686a .5 .5 0 0 0 .937 -.024 l6.5-19a .496 .496 0 0 0 -.635 -.635 l-19 6.5a .5 .5 0 0 0 -.024 .937 l7.93 3.18a 2 2 0 0 1 1.112 1.11 z", "m21.854 2.147-10.94 10.939")) }
    val GripVertical: ImageVector by lazy { lucide("GripVertical", listOf("M 8 12 A 1 1 0 1 0 10 12 A 1 1 0 1 0 8 12 Z", "M 8 5 A 1 1 0 1 0 10 5 A 1 1 0 1 0 8 5 Z", "M 8 19 A 1 1 0 1 0 10 19 A 1 1 0 1 0 8 19 Z", "M 14 12 A 1 1 0 1 0 16 12 A 1 1 0 1 0 14 12 Z", "M 14 5 A 1 1 0 1 0 16 5 A 1 1 0 1 0 14 5 Z", "M 14 19 A 1 1 0 1 0 16 19 A 1 1 0 1 0 14 19 Z")) }
    val Library: ImageVector by lazy { lucide("Library", listOf("m16 6 4 14", "M12 6v14", "M8 8v12", "M4 4v16")) }
    val Compass: ImageVector by lazy { lucide("Compass", listOf("M 2 12 A 10 10 0 1 0 22 12 A 10 10 0 1 0 2 12 Z", "m16.24 7.76-1.804 5.411a 2 2 0 0 1 -1.265 1.265 L7.76 16.24l1.804-5.411a 2 2 0 0 1 1.265 -1.265 z")) }
    val Settings: ImageVector by lazy { lucide("Settings", listOf("M9.671 4.136a 2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1 -2.33 4.033 2.34 2.34 0 0 0 -3.319 1.915 2.34 2.34 0 0 1 -4.659 0 2.34 2.34 0 0 0 -3.32 -1.915 2.34 2.34 0 0 1 -2.33 -4.033 2.34 2.34 0 0 0 0 -3.831 A 2.34 2.34 0 0 1 6.35 6.051 a 2.34 2.34 0 0 0 3.319 -1.915", "M 9 12 A 3 3 0 1 0 15 12 A 3 3 0 1 0 9 12 Z")) }
    val Flame: ImageVector by lazy { lucide("Flame", listOf("M12 3q1 4 4 6.5t3 5.5a 1 1 0 0 1 -14 0 5 5 0 0 1 1 -3 1 1 0 0 0 5 0 c0-2-1.5-3-1.5-5q0-2 2.5-4")) }
    val AlertTriangle: ImageVector by lazy { lucide("AlertTriangle", listOf("m21.73 18-8-14a 2 2 0 0 0 -3.48 0 l-8 14A 2 2 0 0 0 4 21 h16a 2 2 0 0 0 1.73 -3", "M12 9v4", "M12 17h.01")) }
    val Download: ImageVector by lazy { lucide("Download", listOf("M12 15V3", "M21 15v4a 2 2 0 0 1 -2 2 H5a 2 2 0 0 1 -2 -2 v-4", "m7 10 5 5 5-5")) }
    val Share: ImageVector by lazy { lucide("Share", listOf("M12 2v13", "m16 6-4-4-4 4", "M4 12v8a 2 2 0 0 0 2 2 h12a 2 2 0 0 0 2 -2 v-8")) }
    val Tag: ImageVector by lazy { lucide("Tag", listOf("M12.586 2.586A 2 2 0 0 0 11.172 2 H4a 2 2 0 0 0 -2 2 v7.172a 2 2 0 0 0 .586 1.414 l8.704 8.704a 2.426 2.426 0 0 0 3.42 0 l6.58-6.58a 2.426 2.426 0 0 0 0 -3.42 z", "M 7 7.5 A 0.5 0.5 0 1 0 8 7.5 A 0.5 0.5 0 1 0 7 7.5 Z")) }
    val Drama: ImageVector by lazy { lucide("Drama", listOf("M10 11h.01", "M14 6h.01", "M18 6h.01", "M6.5 13.1h.01", "M22 5c0 9-4 12-6 12s-6-3-6-12c0-2 2-3 6-3s6 1 6 3", "M17.4 9.9c-.8.8-2 .8-2.8 0", "M10.1 7.1C9 7.2 7.7 7.7 6 8.6c-3.5 2-4.7 3.9-3.7 5.6 4.5 7.8 9.5 8.4 11.2 7.4.9-.5 1.9-2.1 1.9-4.7", "M9.1 16.5c.3-1.1 1.4-1.7 2.4-1.4")) }
    val Satellite: ImageVector by lazy { lucide("Satellite", listOf("m13.5 6.5-3.148-3.148a 1.205 1.205 0 0 0 -1.704 0 L6.352 5.648a 1.205 1.205 0 0 0 0 1.704 L9.5 10.5", "M16.5 7.5 19 5", "m17.5 10.5 3.148 3.148a 1.205 1.205 0 0 1 0 1.704 l-2.296 2.296a 1.205 1.205 0 0 1 -1.704 0 L13.5 14.5", "M9 21a 6 6 0 0 0 -6 -6", "M9.352 10.648a 1.205 1.205 0 0 0 0 1.704 l2.296 2.296a 1.205 1.205 0 0 0 1.704 0 l4.296-4.296a 1.205 1.205 0 0 0 0 -1.704 l-2.296-2.296a 1.205 1.205 0 0 0 -1.704 0 z")) }
    val ExternalLink: ImageVector by lazy { lucide("ExternalLink", listOf("M15 3h6v6", "M10 14 21 3", "M18 13v6a 2 2 0 0 1 -2 2 H5a 2 2 0 0 1 -2 -2 V8a 2 2 0 0 1 2 -2 h6")) }
}
