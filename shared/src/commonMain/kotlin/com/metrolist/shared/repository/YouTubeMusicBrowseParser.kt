package com.metrolist.shared.repository

import com.metrolist.shared.model.Playlist
import com.metrolist.shared.model.Track
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** One page of a playlist plus the token for the next page, if there is one. */
data class TrackPage(val tracks: List<Track>, val continuation: String?)

/** Parses `youtubei/v1/browse` responses for the library grid and for playlist pages. */
object YouTubeMusicBrowseParser {
    private val json = Json { ignoreUnknownKeys = true }

    /** The playlists shown in the library grid. Albums, artists and "New playlist" buttons are skipped. */
    fun parsePlaylists(body: String): List<Playlist> {
        val seen = HashSet<String>()
        return findAll(json.parseToJsonElement(body), "musicTwoRowItemRenderer")
            .mapNotNull(::toPlaylist)
            .filter { seen.add(it.browseId) }
            .toList()
    }

    /**
     * The tracks on a playlist page or continuation page. When the response has a playlist shelf only
     * that shelf is read, so "related" carousels further down the page are not mistaken for tracks.
     */
    fun parseTracks(body: String): TrackPage {
        val root = json.parseToJsonElement(body)
        val scope = root.findFirst("musicPlaylistShelfRenderer")
            ?: root.findFirst("musicPlaylistShelfContinuation")
            ?: root
        return TrackPage(YouTubeMusicSearchParser.parseTree(scope), continuationToken(scope))
    }

    private fun continuationToken(scope: JsonElement): String? =
        (scope.findFirst("nextContinuationData") as? JsonObject)?.string("continuation")
            ?: (scope.findFirst("continuationCommand") as? JsonObject)?.string("token")

    private fun toPlaylist(renderer: JsonObject): Playlist? {
        val browseId = renderer.obj("navigationEndpoint")?.obj("browseEndpoint")?.string("browseId")
            ?.takeIf { it.startsWith("VL") } ?: return null
        val title = renderer.obj("title")?.array("runs")?.firstOrNull()?.asObject()?.string("text")
            ?.takeIf { it.isNotBlank() } ?: return null
        val subtitle = renderer.obj("subtitle")?.array("runs")
            ?.joinToString("") { it.asObject()?.string("text").orEmpty() }.orEmpty()
        val thumbnail = renderer.obj("thumbnailRenderer")?.obj("musicThumbnailRenderer")
            ?.obj("thumbnail")?.array("thumbnails")?.lastOrNull()?.asObject()?.string("url")
        return Playlist(browseId, title, subtitle, thumbnail)
    }

    private fun findAll(node: JsonElement, key: String): Sequence<JsonObject> = sequence {
        when (node) {
            is JsonObject -> {
                val hit = node[key] as? JsonObject
                if (hit != null) yield(hit) else for (child in node.values) yieldAll(findAll(child, key))
            }
            is JsonArray -> for (child in node) yieldAll(findAll(child, key))
            else -> Unit
        }
    }
}
