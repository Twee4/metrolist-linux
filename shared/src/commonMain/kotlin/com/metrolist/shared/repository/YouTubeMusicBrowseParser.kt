package com.metrolist.shared.repository

import com.metrolist.shared.model.Playlist
import com.metrolist.shared.model.Shelf
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

    /**
     * The home page's carousels. Items with a video id become songs; items pointing at an album ("MPRE...")
     * or playlist ("VL...") become collections. Artists, moods and other items are skipped, as are empty shelves.
     */
    fun parseHome(body: String): List<Shelf> =
        findAll(json.parseToJsonElement(body), "musicCarouselShelfRenderer").mapNotNull { shelf ->
            val title = shelf.obj("header")?.obj("musicCarouselShelfBasicHeaderRenderer")?.obj("title")
                ?.array("runs")?.firstOrNull()?.asObject()?.string("text")?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val tracks = mutableListOf<Track>()
            val collections = mutableListOf<Playlist>()
            for (item in shelf.array("contents").orEmpty()) {
                val two = item.asObject()?.obj("musicTwoRowItemRenderer")
                if (two != null) {
                    twoRowTrack(two)?.let(tracks::add) ?: toCollection(two)?.let(collections::add)
                } else {
                    tracks += YouTubeMusicSearchParser.parseTree(item)
                }
            }
            Shelf(title, tracks.distinctBy { it.id }, collections.distinctBy { it.browseId })
                .takeIf { it.tracks.isNotEmpty() || it.collections.isNotEmpty() }
        }.toList()

    private fun twoRowTrack(renderer: JsonObject): Track? {
        val videoId = renderer.obj("navigationEndpoint")?.obj("watchEndpoint")?.string("videoId") ?: return null
        val title = renderer.obj("title")?.array("runs")?.firstOrNull()?.asObject()?.string("text")
            ?.takeIf { it.isNotBlank() } ?: return null
        val artist = renderer.obj("subtitle")?.array("runs")
            ?.mapNotNull { it.asObject()?.string("text") }
            ?.filterNot { it == " • " || it.lowercase() in setOf("song", "video") }
            ?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
        return Track(id = videoId, title = title, artist = artist, durationSeconds = 0L, artUrl = thumbnail(renderer))
    }

    private fun thumbnail(renderer: JsonObject): String? =
        renderer.obj("thumbnailRenderer")?.obj("musicThumbnailRenderer")
            ?.obj("thumbnail")?.array("thumbnails")?.lastOrNull()?.asObject()?.string("url")

    private fun continuationToken(scope: JsonElement): String? =
        (scope.findFirst("nextContinuationData") as? JsonObject)?.string("continuation")
            ?: (scope.findFirst("continuationCommand") as? JsonObject)?.string("token")

    private fun toPlaylist(renderer: JsonObject): Playlist? = toCollection(renderer, albums = false)

    private fun toCollection(renderer: JsonObject, albums: Boolean = true): Playlist? {
        val browseId = renderer.obj("navigationEndpoint")?.obj("browseEndpoint")?.string("browseId")
            ?.takeIf { it.startsWith("VL") || (albums && it.startsWith("MPRE")) } ?: return null
        val title = renderer.obj("title")?.array("runs")?.firstOrNull()?.asObject()?.string("text")
            ?.takeIf { it.isNotBlank() } ?: return null
        val subtitle = renderer.obj("subtitle")?.array("runs")
            ?.joinToString("") { it.asObject()?.string("text").orEmpty() }.orEmpty()
        return Playlist(browseId, title, subtitle, thumbnail(renderer))
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
