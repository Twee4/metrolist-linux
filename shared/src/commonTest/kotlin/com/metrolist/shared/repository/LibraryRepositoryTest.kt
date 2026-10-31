package com.metrolist.shared.repository

import com.metrolist.shared.model.Playlist
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryRepositoryTest {
    private fun row(id: String) = """{ "musicResponsiveListItemRenderer": {
        "playlistItemData": { "videoId": "$id" },
        "flexColumns": [ { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [ { "text": "T$id" } ] } } } ] } }"""

    private fun page(ids: List<String>, token: String?) = """{ "contents": { "musicPlaylistShelfRenderer": {
        "contents": [ ${ids.joinToString(",") { row(it) }} ]
        ${if (token != null) """, "continuations": [ { "nextContinuationData": { "continuation": "$token" } } ]""" else ""}
    } } }"""

    private class FakeApi(val pages: Map<String, String>) : BrowseApi {
        val requested = mutableListOf<String>()
        override suspend fun browse(browseId: String) = pages.getValue(browseId).also { requested += browseId }
        override suspend fun browseContinuation(token: String) = pages.getValue(token).also { requested += token }
    }

    private val playlist = Playlist("VLPL1", "Mine")

    @Test
    fun followsContinuationsAndDropsDuplicates() = runTest {
        val api = FakeApi(mapOf(
            "VLPL1" to page(listOf("a", "b"), "t1"),
            "t1" to page(listOf("b", "c"), "t2"),
            "t2" to page(listOf("d"), null),
        ))
        val tracks = YouTubeMusicLibraryRepository(api).tracks(playlist)
        assertEquals(listOf("a", "b", "c", "d"), tracks.map { it.id })
        assertEquals(listOf("VLPL1", "t1", "t2"), api.requested)
    }

    @Test
    fun stopsWhenATokenRepeats() = runTest {
        val api = FakeApi(mapOf(
            "VLPL1" to page(listOf("a"), "loop"),
            "loop" to page(listOf("b"), "loop"),
        ))
        val tracks = YouTubeMusicLibraryRepository(api).tracks(playlist)
        assertEquals(listOf("a", "b"), tracks.map { it.id })
        assertEquals(listOf("VLPL1", "loop"), api.requested)
    }

    @Test
    fun likedSongsComesFirstAndIsNotDuplicated() = runTest {
        val grid = """{ "x": [
            { "musicTwoRowItemRenderer": { "title": { "runs": [ { "text": "Liked Music" } ] },
              "navigationEndpoint": { "browseEndpoint": { "browseId": "VLLM" } } } },
            { "musicTwoRowItemRenderer": { "title": { "runs": [ { "text": "Mine" } ] },
              "navigationEndpoint": { "browseEndpoint": { "browseId": "VLPL1" } } } } ] }"""
        val api = FakeApi(mapOf("FEmusic_liked_playlists" to grid))
        val result = YouTubeMusicLibraryRepository(api).playlists()
        assertEquals(listOf("VLLM", "VLPL1"), result.map { it.browseId })
        assertEquals("Liked songs", result.first().title)
    }
}
