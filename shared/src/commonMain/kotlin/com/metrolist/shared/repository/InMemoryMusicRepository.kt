package com.metrolist.shared.repository

import com.metrolist.shared.model.Track

interface MusicRepository {
    suspend fun search(query: String): List<Track>
}

/** Demo search data, used only as the base for [YouTubeMusicRepository] and in tests. */
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
}
