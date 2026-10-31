package com.metrolist.shared.repository

import com.metrolist.shared.model.Playlist
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class YouTubeMusicBrowseParserTest {
    private fun grid(id: String?, title: String) = """
        { "musicTwoRowItemRenderer": {
            "title": { "runs": [ { "text": "$title" } ] },
            "subtitle": { "runs": [ { "text": "Playlist" }, { "text": " • " }, { "text": "12 songs" } ] },
            "thumbnailRenderer": { "musicThumbnailRenderer": { "thumbnail": { "thumbnails": [
                { "url": "https://img/small" }, { "url": "https://img/big" } ] } } }
            ${if (id != null) """, "navigationEndpoint": { "browseEndpoint": { "browseId": "$id" } }""" else ""}
        } }"""

    @Test
    fun parsesPlaylistGridAndSkipsOtherItems() {
        val body = """{ "contents": { "items": [
            ${grid(null, "New playlist")},
            ${grid("VLPL111", "Road trip")},
            ${grid("MPREb_album", "Some album")},
            ${grid("VLPL111", "Road trip again")},
            ${grid("VLPL222", "Focus")}
        ] } }"""
        assertEquals(
            listOf(
                Playlist("VLPL111", "Road trip", "Playlist • 12 songs", "https://img/big"),
                Playlist("VLPL222", "Focus", "Playlist • 12 songs", "https://img/big"),
            ),
            YouTubeMusicBrowseParser.parsePlaylists(body),
        )
    }

    private fun row(id: String, title: String, artist: String, album: String, duration: String) = """
        { "musicResponsiveListItemRenderer": {
            "playlistItemData": { "videoId": "$id" },
            "flexColumns": [
              { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [ { "text": "$title" } ] } } },
              { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [ { "text": "$artist" } ] } } },
              { "musicResponsiveListItemFlexColumnRenderer": { "text": { "runs": [ { "text": "$album" } ] } } }
            ],
            "fixedColumns": [
              { "musicResponsiveListItemFixedColumnRenderer": { "text": { "runs": [ { "text": "$duration" } ] } } }
            ]
        } }"""

    @Test
    fun parsesPlaylistRowsWithAlbumAndFixedDuration() {
        val body = """{ "contents": { "musicPlaylistShelfRenderer": {
            "contents": [ ${row("v1", "Song One", "Artist A", "Album X", "3:21")} ],
            "continuations": [ { "nextContinuationData": { "continuation": "TOKEN1" } } ]
        } } }"""
        val page = YouTubeMusicBrowseParser.parseTracks(body)
        val t = page.tracks.single()
        assertEquals("v1", t.id)
        assertEquals("Song One", t.title)
        assertEquals("Artist A", t.artist)
        assertEquals("Album X", t.album)
        assertEquals(201L, t.durationSeconds)
        assertEquals("TOKEN1", page.continuation)
    }

    @Test
    fun ignoresRelatedShelvesOutsidePlaylistShelf() {
        val body = """{ "contents": [
            { "musicPlaylistShelfRenderer": { "contents": [ ${row("v1", "Mine", "A", "B", "1:00")} ] } },
            { "musicCarouselShelfRenderer": { "contents": [ ${row("v2", "Suggested", "A", "B", "1:00")} ],
               "continuations": [ { "nextContinuationData": { "continuation": "WRONG" } } ] } }
        ] }"""
        val page = YouTubeMusicBrowseParser.parseTracks(body)
        assertEquals(listOf("v1"), page.tracks.map { it.id })
        assertNull(page.continuation)
    }

    @Test
    fun parsesNewStyleContinuationResponse() {
        val body = """{ "onResponseReceivedActions": [ { "appendContinuationItemsAction": { "continuationItems": [
            ${row("v3", "Three", "A", "B", "2:00")},
            { "continuationItemRenderer": { "continuationEndpoint": { "continuationCommand": { "token": "NEXT" } } } }
        ] } } ] }"""
        val page = YouTubeMusicBrowseParser.parseTracks(body)
        assertEquals(listOf("v3"), page.tracks.map { it.id })
        assertEquals("NEXT", page.continuation)
    }

    @Test
    fun parsesHomeShelves() {
        val body = """{ "contents": [
          { "musicCarouselShelfRenderer": {
              "header": { "musicCarouselShelfBasicHeaderRenderer": { "title": { "runs": [ { "text": "Listen again" } ] } } },
              "contents": [
                { "musicTwoRowItemRenderer": { "title": { "runs": [ { "text": "Some Song" } ] },
                  "subtitle": { "runs": [ { "text": "Song" }, { "text": " • " }, { "text": "Band" } ] },
                  "navigationEndpoint": { "watchEndpoint": { "videoId": "vid1" } } } },
                { "musicTwoRowItemRenderer": { "title": { "runs": [ { "text": "Some Album" } ] },
                  "subtitle": { "runs": [ { "text": "Album • Band" } ] },
                  "navigationEndpoint": { "browseEndpoint": { "browseId": "MPREb_x" } } } },
                { "musicTwoRowItemRenderer": { "title": { "runs": [ { "text": "An Artist" } ] },
                  "navigationEndpoint": { "browseEndpoint": { "browseId": "UCabc" } } } }
              ] } },
          { "musicCarouselShelfRenderer": {
              "header": { "musicCarouselShelfBasicHeaderRenderer": { "title": { "runs": [ { "text": "Empty" } ] } } },
              "contents": [] } },
          { "musicCarouselShelfRenderer": {
              "contents": [ { "musicTwoRowItemRenderer": { "title": { "runs": [ { "text": "x" } ] },
                  "navigationEndpoint": { "watchEndpoint": { "videoId": "no-title-shelf" } } } } ] } }
        ] }"""
        val shelves = YouTubeMusicBrowseParser.parseHome(body)
        val shelf = shelves.single()
        assertEquals("Listen again", shelf.title)
        assertEquals(listOf("vid1"), shelf.tracks.map { it.id })
        assertEquals("Band", shelf.tracks.single().artist)
        assertEquals(listOf("MPREb_x"), shelf.collections.map { it.browseId })
    }
}
