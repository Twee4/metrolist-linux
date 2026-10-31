package com.metrolist.shared.repository

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Small null-safe accessors for walking YouTube Music's loosely shaped JSON.

internal fun JsonElement.asObject(): JsonObject? = this as? JsonObject
internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
internal fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray
internal fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

/** The `text.runs` list used by most text fields. */
internal fun JsonObject.runs(): List<JsonObject> =
    obj("text")?.array("runs")?.mapNotNull { it.asObject() }.orEmpty()

/** Depth-first search for the first value stored under [key] anywhere below this element. */
internal fun JsonElement.findFirst(key: String): JsonElement? = when (this) {
    is JsonObject -> this[key] ?: values.firstNotNullOfOrNull { it.findFirst(key) }
    is JsonArray -> firstNotNullOfOrNull { it.findFirst(key) }
    else -> null
}
