package fr.paulbr.nookmind.feature.common

import fr.paulbr.nookmind.core.domain.parseDateOnly
import fr.paulbr.nookmind.core.platform.DateStyle
import fr.paulbr.nookmind.core.platform.formatLocalDate

/** Formats an ISO `YYYY-MM-DD` string in the device locale, or null when it cannot be parsed. */
fun formatIsoDate(iso: String?, style: DateStyle): String? = parseDateOnly(iso)?.let { formatLocalDate(it, style) }
