package fr.paulbr.nookmind.core.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Builds the partial-update payload of [LibraryRepository.update] (the `Partial<T>` of the web app):
 * `patchOf("status" to "read", "rating" to null)`. Status enums must be passed as their `key`.
 */
fun patchOf(vararg pairs: Pair<String, Any?>): JsonObject = buildJsonObject {
    pairs.forEach { (key, value) -> put(key, value.toJsonElement()) }
}

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Enum<*> -> throw IllegalArgumentException("Pass the database key of the enum, not the enum itself")
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Map<*, *> -> JsonObject(entries.associate { (k, v) -> k.toString() to v.toJsonElement() })
    else -> JsonPrimitive(toString())
}
