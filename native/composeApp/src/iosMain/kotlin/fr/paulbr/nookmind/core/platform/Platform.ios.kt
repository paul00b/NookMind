package fr.paulbr.nookmind.core.platform

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
// Declared in the NSLocaleGeneralInfo category of NSLocale, which Kotlin/Native exposes as
// extension members: they resolve only when imported by name, unlike the class's own members.
import platform.Foundation.languageCode
import platform.Foundation.preferredLanguages
import platform.UIKit.UIApplication

actual val platformKind: PlatformKind = PlatformKind.IOS

/**
 * The tiers in `HapticCue.toFeedbackType()` are Android API levels: which `HapticFeedbackType`
 * constants a given Android release actually vibrates for. iOS has no such gradient, Compose maps
 * every type to a UIKit feedback generator, so it never degrades, like desktop.
 */
actual val hapticApiLevel: Int = Int.MAX_VALUE

/** First entry of the user's preferred languages, e.g. `fr-FR`: the `navigator.language` of iOS. */
actual fun deviceLanguageTag(): String =
    (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"

actual fun formatLocalDate(date: LocalDate, style: DateStyle, languageTag: String): String {
    val locale = NSLocale(localeIdentifier = languageTag)
    val isFr = locale.languageCode == "fr"
    // Same Unicode date patterns as the JVM actual: NSDateFormatter and java.time both follow TR35.
    val pattern = when (style) {
        DateStyle.DAY_MONTH_SHORT -> "d MMM"
        DateStyle.DAY_MONTH_SHORT_YEAR -> "d MMM yyyy"
        DateStyle.DAY_MONTH_LONG_YEAR -> if (isFr) "d MMMM yyyy" else "MMMM d, yyyy"
        DateStyle.MONTH_SHORT_YEAR -> "MMM yyyy"
        DateStyle.NUMERIC -> if (isFr) "dd/MM/yyyy" else "M/d/yyyy"
    }
    // Built and formatted in the same (system) calendar and time zone, so the day never shifts.
    val nsDate = NSCalendar.currentCalendar.dateFromComponents(date.toNSDateComponents()) ?: return date.toString()
    val formatter = NSDateFormatter().apply {
        this.locale = locale
        dateFormat = pattern
    }
    return formatter.stringFromDate(nsDate)
}

/** One `NSUserDefaults` suite per store, the counterpart of one SharedPreferences file per name on Android. */
actual fun createPlatformSettings(name: String): Settings =
    NSUserDefaultsSettings(NSUserDefaults(suiteName = "fr.paulbr.nookmind.$name"))

/**
 * Nothing to do: iOS has no hardware back button to leave the app with, and Apple rejects apps
 * that terminate themselves. The root back gesture simply has no effect.
 */
actual fun exitApplication() = Unit

actual fun logDebug(tag: String, message: String, throwable: Throwable?) {
    println("[$tag] $message")
    throwable?.printStackTrace()
}

actual fun openExternalUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: run {
        println("[platform] cannot open $url: not a valid URL")
        return
    }
    UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
}
