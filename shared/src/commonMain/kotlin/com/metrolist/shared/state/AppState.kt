package com.metrolist.shared.state

import com.metrolist.shared.model.Album
import com.metrolist.shared.model.QueueEntry
import com.metrolist.shared.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppState {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Track>>(emptyList())
    val results: StateFlow<List<Track>> = _results.asStateFlow()

    private val _library = MutableStateFlow<List<Album>>(
        listOf(
            Album("a1", "Library Picks", "Various Artists"),
            Album("a2", "Night Drive", "Metrolist Demo"),
            Album("a3", "Deep Focus", "Metrolist Demo"),
        )
    )
    val library: StateFlow<List<Album>> = _library.asStateFlow()

    private val _queue = MutableStateFlow(
        listOf(
            QueueEntry("q1", "Midnight Echo", "Demo Artist", 214),
            QueueEntry("q2", "Neon Skyline", "Demo Artist", 193),
            QueueEntry("q3", "Amber Skies", "Demo Artist", 232),
        )
    )
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    fun updateQuery(next: String) {
        _query.value = next
        _results.value = if (next.isBlank()) {
            emptyList()
        } else {
            listOf(
                Track("1", "Desktop Example Track", "Metrolist", "Demo Album", 214, null),
                Track("2", "Another Demo Track", "Metrolist", "Demo Album", 186, null),
                Track("3", "Night Drive", "Metrolist", "Night Drive", 201, null),
            ).filter { it.title.contains(next, ignoreCase = true) || it.artist.contains(next, ignoreCase = true) }
        }
    }
}
