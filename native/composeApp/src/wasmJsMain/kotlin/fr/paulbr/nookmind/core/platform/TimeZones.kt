package fr.paulbr.nookmind.core.platform

// kotlinx-datetime reads the time zone rules from js-joda in the browser: without them, any
// conversion in the device's own zone (today's date, "this evening") fails. See its README.
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

private val jsJodaTz = JsJodaTimeZoneModule
