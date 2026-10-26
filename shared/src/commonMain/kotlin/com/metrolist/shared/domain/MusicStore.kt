package com.metrolist.shared.domain

import com.metrolist.shared.model.Album
import com.metrolist.shared.model.Track
import com.metrolist.shared.repository.InMemoryMusicRepository
import com.metrolist.shared.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicStore(
    private val repository: MusicRepository = InMemoryMusicRepository(),
) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Track>>(emptyList())
    val results: StateFlow<List<Track>> = _results.asStateFlow()

    private val _library = MutableStateFlow<List<Album>>(emptyList())
    val library: StateFlow<List<Album>> = _library.asStateFlow()

    suspend fun updateQuery(nextQuery: String) {
        _query.value = nextQuery
        if (nextQuery.isBlank()) {
            _results.value = emptyList()
            return
        }
        _results.value = repository.search(nextQuery)
    }

    suspend fun loadLibrary() {
        _library.value = repository.getLibrary()
    }
}
