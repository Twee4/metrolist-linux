package com.metrolist.shared.domain

import com.metrolist.shared.model.Album
import com.metrolist.shared.model.QueueEntry
import com.metrolist.shared.model.Track
import com.metrolist.shared.repository.InMemoryMusicRepository
import com.metrolist.shared.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Single source of truth for UI state. Swap the repository to change the data source. */
class MusicStore(
    private val repository: MusicRepository = InMemoryMusicRepository(),
) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Track>>(emptyList())
    val results: StateFlow<List<Track>> = _results.asStateFlow()

    private val _library = MutableStateFlow<List<Album>>(emptyList())
    val library: StateFlow<List<Album>> = _library.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    suspend fun updateQuery(nextQuery: String) {
        _query.value = nextQuery
        _results.value = if (nextQuery.isBlank()) emptyList() else repository.search(nextQuery)
    }

    suspend fun load() {
        _library.value = repository.getLibrary()
        _queue.value = repository.getQueue()
    }
}
