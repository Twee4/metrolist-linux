package com.metrolist.shared.domain

import com.metrolist.shared.model.Track
import com.metrolist.shared.repository.InMemoryMusicRepository
import com.metrolist.shared.repository.MusicRepository
import kotlinx.coroutines.CancellationException
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

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Runs a search. Cancelling the calling coroutine (e.g. on the next keystroke) aborts it. */
    suspend fun updateQuery(nextQuery: String) {
        _query.value = nextQuery
        _error.value = null
        if (nextQuery.isBlank()) {
            _results.value = emptyList()
            _isSearching.value = false
            return
        }

        _isSearching.value = true
        try {
            _results.value = repository.search(nextQuery)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _results.value = emptyList()
            _error.value = e.message ?: "Search failed"
        } finally {
            _isSearching.value = false
        }
    }
}
