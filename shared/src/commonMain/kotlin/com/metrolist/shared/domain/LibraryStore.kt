package com.metrolist.shared.domain

import com.metrolist.shared.model.Playlist
import com.metrolist.shared.model.Shelf
import com.metrolist.shared.model.Track
import com.metrolist.shared.repository.LibraryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface LibraryState {
    data object SignedOut : LibraryState
    data object Loading : LibraryState
    data class Loaded(val playlists: List<Playlist>) : LibraryState
    data class Failed(val message: String) : LibraryState
}

sealed interface PlaylistState {
    val playlist: Playlist

    data class Loading(override val playlist: Playlist) : PlaylistState
    data class Loaded(override val playlist: Playlist, val tracks: List<Track>) : PlaylistState
    data class Failed(override val playlist: Playlist, val message: String) : PlaylistState
}

sealed interface HomeState {
    data object Loading : HomeState
    data class Loaded(val shelves: List<Shelf>) : HomeState
    data class Failed(val message: String) : HomeState
}

/** UI state for the signed-in user's library and the playlist currently being viewed. */
class LibraryStore(private val repository: LibraryRepository) {
    private val _library = MutableStateFlow<LibraryState>(LibraryState.SignedOut)
    val library: StateFlow<LibraryState> = _library.asStateFlow()

    private val _playlist = MutableStateFlow<PlaylistState?>(null)
    val playlist: StateFlow<PlaylistState?> = _playlist.asStateFlow()

    private val _home = MutableStateFlow<HomeState>(HomeState.Loading)
    val home: StateFlow<HomeState> = _home.asStateFlow()

    /** Loads the recommendations. They also work signed out, just less personal. */
    suspend fun refreshHome() {
        _home.value = HomeState.Loading
        _home.value = try {
            HomeState.Loaded(repository.home())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            HomeState.Failed(e.message ?: "Could not load recommendations")
        }
    }

    /** Loads the library, or clears it when [signedIn] is false. Safe to call again to retry. */
    suspend fun refresh(signedIn: Boolean) {
        _playlist.value = null
        if (!signedIn) {
            _library.value = LibraryState.SignedOut
            return
        }
        _library.value = LibraryState.Loading
        _library.value = try {
            LibraryState.Loaded(repository.playlists())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LibraryState.Failed(e.message ?: "Could not load your library")
        }
    }

    suspend fun open(playlist: Playlist) {
        _playlist.value = PlaylistState.Loading(playlist)
        val next = try {
            PlaylistState.Loaded(playlist, repository.tracks(playlist))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PlaylistState.Failed(playlist, e.message ?: "Could not load this playlist")
        }
        // Ignore a slow result if the user has since closed it or opened another playlist.
        if (_playlist.value?.playlist == playlist) _playlist.value = next
    }

    fun close() {
        _playlist.value = null
    }
}
