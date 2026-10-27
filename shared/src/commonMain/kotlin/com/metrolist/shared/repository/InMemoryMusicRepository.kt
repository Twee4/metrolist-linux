package com.metrolist.shared.repository

import com.metrolist.shared.model.Album
import com.metrolist.shared.model.Track

interface MusicRepository {
    suspend fun search(query: String): List<Track>
    suspend fun getLibrary(): List<Album>
}

/** Demo data source used until the real YouTube Music backend is ported. */
class InMemoryMusicRepository : MusicRepository {
    private val tracks = listOf(
        Track("1", "Desktop Example Track", "Metrolist", "Demo Album", 214),
        Track("2", "Another Demo Track", "Metrolist", "Demo Album", 186),
        Track("3", "Night Drive", "Metrolist", "Night Drive", 201),
    )

    override suspend fun search(query: String): List<Track> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()

        return tracks.filter {
            it.title.contains(normalized, ignoreCase = true) ||
                it.artist.contains(normalized, ignoreCase = true)
        }
    }

    override suspend fun getLibrary(): List<Album> = listOf(
        Album("a1", "Library Picks", "Various Artists"),
        Album("a2", "Night Drive", "Metrolist Demo"),
        Album("a3", "Deep Focus", "Metrolist Demo"),
    )
}
