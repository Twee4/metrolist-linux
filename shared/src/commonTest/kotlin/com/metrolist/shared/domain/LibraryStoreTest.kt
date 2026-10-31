package com.metrolist.shared.domain

import com.metrolist.shared.model.Playlist
import com.metrolist.shared.model.Track
import com.metrolist.shared.repository.LibraryRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LibraryStoreTest {
    private val mine = Playlist("VLPL1", "Mine")
    private val track = Track("v1", "Song", "Artist", "Album", 100)

    private class FakeRepo(var fail: Boolean = false, val list: List<Playlist>, val songs: List<Track>) : LibraryRepository {
        override suspend fun playlists(): List<Playlist> = if (fail) error("boom") else list
        override suspend fun tracks(playlist: Playlist): List<Track> = if (fail) error("nope") else songs
    }

    @Test
    fun signedOutShowsNothingAndDoesNotCallTheRepository() = runTest {
        val store = LibraryStore(FakeRepo(fail = true, list = emptyList(), songs = emptyList()))
        store.refresh(signedIn = false)
        assertEquals(LibraryState.SignedOut, store.library.value)
    }

    @Test
    fun loadsPlaylistsAndReportsFailures() = runTest {
        val repo = FakeRepo(list = listOf(mine), songs = listOf(track))
        val store = LibraryStore(repo)
        store.refresh(true)
        assertEquals(LibraryState.Loaded(listOf(mine)), store.library.value)

        repo.fail = true
        store.refresh(true)
        assertEquals(LibraryState.Failed("boom"), store.library.value)
    }

    @Test
    fun opensAndClosesAPlaylist() = runTest {
        val store = LibraryStore(FakeRepo(list = listOf(mine), songs = listOf(track)))
        store.open(mine)
        assertEquals(PlaylistState.Loaded(mine, listOf(track)), store.playlist.value)
        store.close()
        assertNull(store.playlist.value)
    }

    @Test
    fun playlistFailureIsReported() = runTest {
        val store = LibraryStore(FakeRepo(fail = true, list = emptyList(), songs = emptyList()))
        store.open(mine)
        assertIs<PlaylistState.Failed>(store.playlist.value)
    }
}
