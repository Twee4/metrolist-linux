package com.metrolist.shared.state

import com.metrolist.shared.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppState {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Track>>(emptyList())
    val results: StateFlow<List<Track>> = _results.asStateFlow()

    fun updateQuery(next: String) {
        _query.value = next
        _results.value = if (next.isBlank()) {
            emptyList()
        } else {
            listOf(
                Track("1", "Desktop Example Track", "Metrolist", 214, null),
                Track("2", "Another Demo Track", "Metrolist", 186, null),
            ).filter { it.title.contains(next, ignoreCase = true) || it.artist.contains(next, ignoreCase = true) }
        }
    }
}
