package com.metrolist.shared.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Pure helpers for mpv's JSON IPC (one JSON object per line over a Unix socket).
 * See https://mpv.io/manual/master/#json-ipc
 */
object MpvProtocol {
    const val PROP_POSITION_ID = 1
    const val PROP_DURATION_ID = 2

    private val json = Json { ignoreUnknownKeys = true }

    fun watchUrl(videoId: String): String = "https://music.youtube.com/watch?v=$videoId"

    fun loadFile(url: String) = command(JsonPrimitive("loadfile"), JsonPrimitive(url))

    /** `pause` is a property that survives across files, so it must be set explicitly on each play. */
    fun setPause(paused: Boolean) =
        command(JsonPrimitive("set_property"), JsonPrimitive("pause"), JsonPrimitive(paused))

    fun setVolume(percent: Int) =
        command(JsonPrimitive("set_property"), JsonPrimitive("volume"), JsonPrimitive(percent))

    fun seekAbsolute(seconds: Double) =
        command(JsonPrimitive("seek"), JsonPrimitive(seconds), JsonPrimitive("absolute"))

    fun stop() = command(JsonPrimitive("stop"))

    fun observeProperty(id: Int, name: String) =
        command(JsonPrimitive("observe_property"), JsonPrimitive(id), JsonPrimitive(name))

    /** Turns one line from mpv into an event, or null for anything we don't care about. */
    fun parseEvent(line: String): BackendEvent? {
        val obj = try {
            json.parseToJsonElement(line) as? JsonObject
        } catch (e: IllegalArgumentException) {
            null
        } ?: return null

        return when (obj.string("event")) {
            "property-change" -> {
                val data = obj["data"] as? JsonPrimitive
                val value = data?.takeIf { !it.isString }?.doubleOrNull ?: return null
                when (obj.string("name")) {
                    "time-pos" -> BackendEvent.Position(value)
                    "duration" -> BackendEvent.Duration(value)
                    else -> null
                }
            }
            "end-file" -> when (obj.string("reason")) {
                "eof" -> BackendEvent.Ended
                "error" -> BackendEvent.Error(
                    "Could not play this track (${obj.string("file_error") ?: "unknown error"}). " +
                        "Make sure yt-dlp is installed and up to date."
                )
                // "stop" fires for the old file whenever a new one replaces it; "quit" on shutdown.
                else -> null
            }
            else -> null
        }
    }

    private fun command(vararg parts: JsonElement): String = buildJsonObject {
        put("command", buildJsonArray { parts.forEach { add(it) } })
    }.toString()

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
