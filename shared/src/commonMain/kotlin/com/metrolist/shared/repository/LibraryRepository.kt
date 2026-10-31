package com.metrolist.shared.repository

import com.metrolist.shared.model.Playlist
import com.metrolist.shared.model.Shelf
import com.metrolist.shared.model.Track

/** The signed-in user's own music. */
interface LibraryRepository {
    suspend fun playlists(): List<Playlist>
    suspend fun tracks(playlist: Playlist): List<Track>
    suspend fun home(): List<Shelf>
}

class YouTubeMusicLibraryRepository(private val api: BrowseApi) : LibraryRepository {

    override suspend fun playlists(): List<Playlist> {
        val fromLibrary = YouTubeMusicBrowseParser.parsePlaylists(api.browse(LIBRARY_PLAYLISTS_ID))
            .filter { it.browseId != LIKED_SONGS.browseId } // shown first, below
        return listOf(LIKED_SONGS) + fromLibrary
    }

    override suspend fun home(): List<Shelf> = YouTubeMusicBrowseParser.parseHome(api.browse(HOME_ID))

    override suspend fun tracks(playlist: Playlist): List<Track> {
        val seenIds = HashSet<String>()
        val seenTokens = HashSet<String>()
        val result = mutableListOf<Track>()

        var page = YouTubeMusicBrowseParser.parseTracks(api.browse(playlist.browseId))
        var pages = 1
        while (true) {
            // A video can appear twice in a playlist; the player queue is keyed by id, so keep the first.
            page.tracks.filterTo(result) { seenIds.add(it.id) }
            val next = page.continuation
            // Guards against a response that keeps handing back the same token, and against huge playlists.
            if (next == null || !seenTokens.add(next) || pages >= MAX_PAGES) break
            page = YouTubeMusicBrowseParser.parseTracks(api.browseContinuation(next))
            pages++
        }
        return result
    }

    companion object {
        const val LIBRARY_PLAYLISTS_ID = "FEmusic_liked_playlists"
        const val HOME_ID = "FEmusic_home"
        const val MAX_PAGES = 20

        /** "Liked songs" is a playlist with the fixed id LM, but is not always in the library grid. */
        val LIKED_SONGS = Playlist(browseId = "VLLM", title = "Liked songs", subtitle = "Auto playlist")
    }
}
