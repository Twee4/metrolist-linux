package com.metrolist.shared.repository

import com.metrolist.shared.model.Album
import com.metrolist.shared.model.Track

interface MusicRepository {
    suspend fun search(query: String): List<Track>
    suspend fun getLibrary(): List<Album>
}

class InMemoryMusicRepository : MusicRepository {
    override suspend fun search(query: String): List<Track> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()

        return listOf(
            Track(
                id = "1",
                title = "Sample track",
                artist = "Demo Artist",
                durationSeconds = 214,
                artUrl = null,
            ),
            Track(
                id = "2",
                title = "Another track",
                artist = "Demo Artist",
                durationSeconds = 188,
                artUrl = null,
            )
        ).filter { it.title.contains(normalized, ignoreCase = true) || it.artist.contains(normalized, ignoreCase = true) }
    }

    override suspend fun getLibrary(): List<Album> = listOf(
        Album(id = "a1", title = "Library Picks", artist = "Various Artists"),
        Album(id = "a2", title = "Night Drive", artist = "Demo Artist")
    )
}
