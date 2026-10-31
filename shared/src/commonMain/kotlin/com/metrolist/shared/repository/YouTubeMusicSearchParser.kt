package com.metrolist.shared.repository

import com.metrolist.shared.model.Track
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Parses the JSON returned by YouTube Music's internal `youtubei/v1/search` endpoint.
 *
 * The response is deeply nested and undocumented, so this walks the tree looking for
 * `musicResponsiveListItemRenderer` nodes that carry a video id (songs and videos) instead of
 * hard-coding one path. Items without a video id (artists, albums, playlists) are skipped.
 */
object YouTubeMusicSearchParser {
    private val json = Json { ignoreUnknownKeys = true }
    private const val SEPARATOR = " • "
    private val durationRegex = Regex("""^(?:(\d+):)?(\d{1,2}):(\d{2})$""")
    private val typeLabels = setOf("song", "video", "episode")

    fun parse(body: String): List<Track> = parseTree(json.parseToJsonElement(body))

    /** Tracks found anywhere under [root], without duplicates. Also used for playlist pages. */
    internal fun parseTree(root: JsonElement): List<Track> {
        val seen = HashSet<String>()
        return findRenderers(root)
            .mapNotNull(::toTrack)
            .filter { seen.add(it.id) }
            .toList()
    }

    private fun findRenderers(node: JsonElement): Sequence<JsonObject> = sequence {
        when (node) {
            is JsonObject -> {
                val renderer = node["musicResponsiveListItemRenderer"] as? JsonObject
                if (renderer != null) {
                    yield(renderer)
                } else {
                    for (child in node.values) yieldAll(findRenderers(child))
                }
            }
            is JsonArray -> for (child in node) yieldAll(findRenderers(child))
            else -> Unit
        }
    }

    private fun toTrack(renderer: JsonObject): Track? {
        val videoId = renderer.obj("playlistItemData")?.string("videoId")
            ?: renderer.firstVideoId()
            ?: return null

        val columns = renderer.array("flexColumns")
            ?.mapNotNull { it.asObject()?.obj("musicResponsiveListItemFlexColumnRenderer") }
            ?: return null

        val title = columns.getOrNull(0)?.runs()?.firstOrNull()?.string("text")?.takeIf { it.isNotBlank() }
            ?: return null

        val groups = splitBySeparator(columns.getOrNull(1)?.runs().orEmpty().mapNotNull { it.string("text") })
            .filterNot { it.size == 1 && it[0].lowercase() in typeLabels }
        val details = groups.map { it.joinToString("") }

        // Playlist rows keep the duration in a fixed column and the album in a third flex column;
        // search rows put everything in the second column, separated by " • ".
        val fixedDuration = renderer.array("fixedColumns")?.firstOrNull()?.asObject()
            ?.obj("musicResponsiveListItemFixedColumnRenderer")?.runs()?.firstOrNull()
            ?.string("text")?.trim()?.takeIf { durationRegex.matches(it) }
        val durationText = fixedDuration ?: details.lastOrNull { durationRegex.matches(it) }
        val rest = details.filterNot { it == durationText }
        val thirdColumn = columns.getOrNull(2)?.runs()?.joinToString("") { it.string("text").orEmpty() }
            ?.trim()?.takeIf { it.isNotEmpty() }
        val artist = rest.getOrNull(0) ?: "Unknown Artist"
        val album = thirdColumn ?: rest.getOrNull(1) ?: "Unknown Album"

        val artUrl = renderer.obj("thumbnail")
            ?.obj("musicThumbnailRenderer")
            ?.obj("thumbnail")
            ?.array("thumbnails")
            ?.lastOrNull()?.asObject()
            ?.string("url")

        return Track(
            id = videoId,
            title = title,
            artist = artist,
            album = album,
            durationSeconds = durationText?.let(::parseDuration) ?: 0L,
            artUrl = artUrl,
        )
    }

    /** Fallback: the play overlay carries the video id when playlistItemData is absent. */
    private fun JsonObject.firstVideoId(): String? =
        obj("overlay")?.obj("musicItemThumbnailOverlayRenderer")?.obj("content")
            ?.obj("musicPlayButtonRenderer")?.obj("playNavigationEndpoint")
            ?.obj("watchEndpoint")?.string("videoId")

    private fun splitBySeparator(texts: List<String>): List<List<String>> {
        val groups = mutableListOf<MutableList<String>>(mutableListOf())
        for (text in texts) {
            if (text == SEPARATOR) groups.add(mutableListOf()) else groups.last().add(text)
        }
        return groups.filter { it.isNotEmpty() }
    }

    private fun parseDuration(text: String): Long {
        val match = durationRegex.matchEntire(text) ?: return 0L
        val hours = match.groupValues[1].toLongOrNull() ?: 0L
        val minutes = match.groupValues[2].toLong()
        val seconds = match.groupValues[3].toLong()
        return hours * 3600 + minutes * 60 + seconds
    }

}
