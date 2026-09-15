package fr.paulbr.nookmind.core.domain

/** Background / foreground pair for an IMDb rating cell (port of `imdbRatingStyle.ts`). */
data class RatingStyle(val background: Long, val foreground: Long)

fun getRatingStyle(rating: Double?): RatingStyle = when {
    rating == null -> RatingStyle(0xFF374151, 0xFF6B7280)
    rating >= 9 -> RatingStyle(0xFF16A34A, 0xFFFFFFFF)
    rating >= 8 -> RatingStyle(0xFF4ADE80, 0xFF14532D)
    rating >= 7 -> RatingStyle(0xFFFACC15, 0xFF713F12)
    rating >= 6 -> RatingStyle(0xFFF97316, 0xFFFFFFFF)
    rating >= 5 -> RatingStyle(0xFFEF4444, 0xFFFFFFFF)
    else -> RatingStyle(0xFF7F1D1D, 0xFFFCA5A5)
}

/** Formats `7.8` style ratings with one decimal, like `toFixed(1)`. */
fun Double.toFixed1(): String {
    val rounded = kotlin.math.round(this * 10) / 10
    val intPart = rounded.toLong()
    val decimal = kotlin.math.abs(kotlin.math.round((rounded - intPart) * 10)).toInt()
    return "$intPart.$decimal"
}
