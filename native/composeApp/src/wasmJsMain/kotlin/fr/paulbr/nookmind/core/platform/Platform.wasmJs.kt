package fr.paulbr.nookmind.core.platform

import com.russhwolf.settings.Settings
import fr.paulbr.nookmind.web.WEB_VERSION_NAME
import kotlinx.browser.window
import kotlinx.datetime.LocalDate

actual val platformKind: PlatformKind = PlatformKind.WEB

/** Browser haptics are a no-op, so never degrade the mapping there, as on desktop. */
actual val hapticApiLevel: Int = Int.MAX_VALUE

/** `navigator.language`, e.g. `fr-FR`. */
actual fun deviceLanguageTag(): String = window.navigator.language.ifBlank { "en" }

private val monthsShortFr = listOf("janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.")
private val monthsLongFr = listOf("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre")
private val monthsShortEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val monthsLongEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

/** The same patterns as the JVM and iOS actuals, for the two languages the app speaks. */
actual fun formatLocalDate(date: LocalDate, style: DateStyle, languageTag: String): String {
    val fr = languageTag.lowercase().startsWith("fr")
    val m = date.month.ordinal
    val d = date.day
    val y = date.year
    val short = if (fr) monthsShortFr[m] else monthsShortEn[m]
    return when (style) {
        DateStyle.DAY_MONTH_SHORT -> "$d $short"
        DateStyle.DAY_MONTH_SHORT_YEAR -> "$d $short $y"
        DateStyle.DAY_MONTH_LONG_YEAR -> if (fr) "$d ${monthsLongFr[m]} $y" else "${monthsLongEn[m]} $d, $y"
        DateStyle.MONTH_SHORT_YEAR -> "$short $y"
        DateStyle.NUMERIC -> if (fr) "${d.toString().padStart(2, '0')}/${(m + 1).toString().padStart(2, '0')}/$y" else "${m + 1}/$d/$y"
    }
}

/** One prefix per store in the browser's localStorage, the counterpart of one file per name elsewhere. */
actual fun createPlatformSettings(name: String): Settings = LocalStorageSettings("nookmind.$name.")

/** A web page does not close itself. */
actual fun exitApplication() = Unit

actual fun logDebug(tag: String, message: String, throwable: Throwable?) {
    println("[$tag] $message")
    throwable?.printStackTrace()
}

actual fun openExternalUrl(url: String) {
    window.open(url, "_blank", "noopener")
}

/** The system share sheet where the browser has one (phones, Safari), else the clipboard. */
actual fun shareText(text: String) = jsShare(text)

private fun jsShare(text: String): Unit = js(
    """{
    if (navigator.share) { navigator.share({ text: text }).catch(function () {}); }
    else if (navigator.clipboard) { navigator.clipboard.writeText(text).catch(function () {}); }
}""",
)

actual fun appVersionName(): String? = WEB_VERSION_NAME
