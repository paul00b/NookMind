package fr.paulbr.nookmind.core.platform

import com.russhwolf.settings.Settings
import kotlinx.datetime.LocalDate

enum class PlatformKind { ANDROID, IOS, DESKTOP }

/** Which platform the shared code runs on (`Capacitor.getPlatform()` equivalent). */
expect val platformKind: PlatformKind

val isAndroid: Boolean get() = platformKind == PlatformKind.ANDROID
val isIos: Boolean get() = platformKind == PlatformKind.IOS

/** BCP-47 tag of the device language, e.g. `fr-FR` (`navigator.language`). */
expect fun deviceLanguageTag(): String

fun isFrenchDevice(): Boolean = deviceLanguageTag().lowercase().startsWith("fr")

/** Date rendering styles used by the web app's `toLocaleDateString` calls. */
enum class DateStyle {
    /** `15 avr.` / `15 Apr` */
    DAY_MONTH_SHORT,
    /** `15 avr. 2026` / `15 Apr 2026` */
    DAY_MONTH_SHORT_YEAR,
    /** `15 avril 2026` / `April 15, 2026` */
    DAY_MONTH_LONG_YEAR,
    /** `avr. 2026` / `Apr 2026` */
    MONTH_SHORT_YEAR,
    /** `15/04/2026` / `4/15/2026` — default `toLocaleDateString()` */
    NUMERIC,
}

/** Locale-aware date formatting (java.time on JVM, NSDateFormatter on iOS). */
expect fun formatLocalDate(date: LocalDate, style: DateStyle, languageTag: String = deviceLanguageTag()): String

/** Persistent key-value store (SharedPreferences / NSUserDefaults / java.util.prefs). */
expect fun createPlatformSettings(name: String): Settings

/** Terminates the app (Android hardware back at the root). No-op where not applicable. */
expect fun exitApplication()

/** Debug logging that reaches Logcat / the console. */
expect fun logDebug(tag: String, message: String, throwable: Throwable? = null)

/** Opens [url] in the system browser / the app registered for it (`window.open` / `<a target=_blank>`). */
expect fun openExternalUrl(url: String)
