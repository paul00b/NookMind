package fr.paulbr.nookmind.core.platform

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.time.format.DateTimeFormatter
import java.util.Locale

actual fun deviceLanguageTag(): String = Locale.getDefault().toLanguageTag()

private fun LocalDate.toJava(): java.time.LocalDate = java.time.LocalDate.of(year, month.number, day)

actual fun formatLocalDate(date: LocalDate, style: DateStyle, languageTag: String): String {
    val locale = Locale.forLanguageTag(languageTag)
    val isFr = locale.language == "fr"
    val pattern = when (style) {
        DateStyle.DAY_MONTH_SHORT -> if (isFr) "d MMM" else "d MMM"
        DateStyle.DAY_MONTH_SHORT_YEAR -> if (isFr) "d MMM yyyy" else "d MMM yyyy"
        DateStyle.DAY_MONTH_LONG_YEAR -> if (isFr) "d MMMM yyyy" else "MMMM d, yyyy"
        DateStyle.MONTH_SHORT_YEAR -> if (isFr) "MMM yyyy" else "MMM yyyy"
        DateStyle.NUMERIC -> if (isFr) "dd/MM/yyyy" else "M/d/yyyy"
    }
    return DateTimeFormatter.ofPattern(pattern, locale).format(date.toJava())
}
