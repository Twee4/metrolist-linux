package com.metrolist.shared.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeMusicSearchParserTest {
    // Hand-built fixture that mirrors the structure of a real response (not captured live).
    private val fixture = """
    {
      "contents": {
        "tabbedSearchResultsRenderer": {
          "tabs": [{
            "tabRenderer": {
              "content": {
                "sectionListRenderer": {
                  "contents": [
                    {
                      "musicShelfRenderer": {
                        "contents": [
                          {
                            "musicResponsiveListItemRenderer": {
                              "playlistItemData": { "videoId": "abc123" },
                              "thumbnail": {
                                "musicThumbnailRenderer": {
                                  "thumbnail": {
                                    "thumbnails": [
                                      { "url": "https://img/small.jpg" },
                                      { "url": "https://img/large.jpg" }
                                    ]
                                  }
                                }
                              },
                              "flexColumns": [
                                { "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Night Drive" } ] } } },
                                { "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [
                                      { "text": "Song" }, { "text": " • " },
                                      { "text": "Some Artist" }, { "text": " • " },
                                      { "text": "Some Album" }, { "text": " • " },
                                      { "text": "3:34" }
                                    ] } } }
                              ]
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "playlistItemData": { "videoId": "def456" },
                              "flexColumns": [
                                { "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Long Mix" } ] } } },
                                { "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [
                                      { "text": "Video" }, { "text": " • " },
                                      { "text": "Mixer" }, { "text": " • " },
                                      { "text": "1:02:03" }
                                    ] } } }
                              ]
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "flexColumns": [
                                { "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "An Artist Result" } ] } } }
                              ]
                            }
                          },
                          {
                            "musicResponsiveListItemRenderer": {
                              "playlistItemData": { "videoId": "abc123" },
                              "flexColumns": [
                                { "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": { "runs": [ { "text": "Night Drive (duplicate)" } ] } } }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          }]
        }
      }
    }
    """.trimIndent()

    @Test
    fun parsesSongsAndVideosAndSkipsOtherItems() {
        val tracks = YouTubeMusicSearchParser.parse(fixture)
        assertEquals(listOf("abc123", "def456"), tracks.map { it.id })
    }

    @Test
    fun parsesSongFields() {
        val song = YouTubeMusicSearchParser.parse(fixture).first()
        assertEquals("Night Drive", song.title)
        assertEquals("Some Artist", song.artist)
        assertEquals("Some Album", song.album)
        assertEquals(214L, song.durationSeconds)
        assertEquals("https://img/large.jpg", song.artUrl)
    }

    @Test
    fun parsesHourLongDurationAndMissingAlbum() {
        val video = YouTubeMusicSearchParser.parse(fixture)[1]
        assertEquals(3723L, video.durationSeconds)
        assertEquals("Mixer", video.artist)
        assertEquals("Unknown Album", video.album)
        assertNull(video.artUrl)
    }

    @Test
    fun emptyResponseGivesNoTracks() {
        assertTrue(YouTubeMusicSearchParser.parse("{}").isEmpty())
    }
}
