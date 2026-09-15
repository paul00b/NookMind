package fr.paulbr.nookmind.core.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/** Today in the device time zone (equivalent of `new Date(); setHours(0,0,0,0)`). */
fun todayLocal(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

/**
 * Parses an ISO `YYYY-MM-DD` date (TMDB / Supabase). Tolerates a trailing time part and
 * returns null for blanks or garbage instead of throwing.
 */
fun parseDateOnly(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    val head = value.trim().take(10)
    return runCatching { LocalDate.parse(head) }.getOrNull()
}

/** Whole days from [today] to [target] (negative when in the past). */
fun daysUntil(target: LocalDate, today: LocalDate = todayLocal()): Int = today.daysUntil(target)

fun daysUntil(dateStr: String, today: LocalDate = todayLocal()): Int? =
    parseDateOnly(dateStr)?.let { daysUntil(it, today) }

fun isFutureDate(dateStr: String?, today: LocalDate = todayLocal()): Boolean {
    val date = parseDateOnly(dateStr) ?: return false
    return date > today
}

fun isTodayOrPast(dateStr: String, today: LocalDate = todayLocal()): Boolean {
    val date = parseDateOnly(dateStr) ?: return false
    return date <= today
}

/** `YYYY` prefix of an ISO date, or null. */
fun yearOf(dateStr: String?): String? = dateStr?.takeIf { it.length >= 4 }?.take(4)

fun yearInt(dateStr: String?): Int? = yearOf(dateStr)?.toIntOrNull()

/** Today as `YYYY-MM-DD` (used as default watched date). */
fun todayIso(): String = todayLocal().toString()
