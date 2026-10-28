package com.metrolist.shared.player

import com.metrolist.shared.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MprisModelTest {
    private val a = Track("abc-DEF_123", "Night Drive", "Some Artist", "Some Album", 200, "https://img/a.jpg")
    private val b = Track("zzz", "Other", "Someone", durationSeconds = 100)

    private fun state(
        queue: List<Track> = listOf(a, b),
        index: Int = 0,
        isPlaying: Boolean = true,
        position: Double = 0.0,
        duration: Double = 200.0,
        repeat: RepeatMode = RepeatMode.OFF,
        shuffle: Boolean = false,
        volume: Int = 100,
    ) = PlayerState(
        queue = queue, index = index, isPlaying = isPlaying, positionSeconds = position,
        durationSeconds = duration, repeat = repeat, shuffle = shuffle, volume = volume,
    )

    @Test
    fun trackPathContainsOnlyLegalCharacters() {
        val path = Mpris.trackPath(a)
        assertTrue(Regex("^/[A-Za-z0-9_/]+$").matches(path), path)
        assertEquals("/org/metrolist/Track/abc_002dDEF_005f123", path)
    }

    @Test
    fun differentIdsNeverShareATrackPath() {
        val ids = listOf("a-b", "a_b", "a_002db", "ab", "a-b-", "é", "_")
        val paths = ids.map { Mpris.trackPath(Track(it, "t", "a", durationSeconds = 1)) }
        assertEquals(ids.size, paths.toSet().size)
    }

    @Test
    fun loopStatusMapsBothWays() {
        for (mode in RepeatMode.entries) {
            assertEquals(mode, Mpris.repeatMode(Mpris.loopStatus(mode)))
        }
        assertEquals("None", Mpris.loopStatus(RepeatMode.OFF))
        assertEquals("Playlist", Mpris.loopStatus(RepeatMode.ALL))
        assertEquals("Track", Mpris.loopStatus(RepeatMode.ONE))
        assertNull(Mpris.repeatMode("Bogus"))
    }

    @Test
    fun playbackStatusFollowsState() {
        assertEquals("Stopped", MprisProps.from(PlayerState()).playbackStatus)
        assertEquals("Playing", MprisProps.from(state(isPlaying = true)).playbackStatus)
        assertEquals("Paused", MprisProps.from(state(isPlaying = false)).playbackStatus)
    }

    @Test
    fun emptyPlayerCannotDoAnything() {
        val p = MprisProps.from(PlayerState())
        assertNull(p.metadata)
        assertFalse(p.canPlay)
        assertFalse(p.canPause)
        assertFalse(p.canSeek)
        assertFalse(p.canGoNext)
        assertFalse(p.canGoPrevious)
    }

    @Test
    fun metadataDescribesTheCurrentTrack() {
        val m = MprisProps.from(state(duration = 198.5)).metadata!!
        assertEquals(Mpris.trackPath(a), m.trackId)
        assertEquals("Night Drive", m.title)
        assertEquals(listOf("Some Artist"), m.artists)
        assertEquals("Some Album", m.album)
        assertEquals(198_500_000L, m.lengthMicros)
        assertEquals("https://img/a.jpg", m.artUrl)
    }

    @Test
    fun capabilitiesFollowQueuePosition() {
        assertTrue(MprisProps.from(state(index = 0)).canGoNext)
        assertFalse(MprisProps.from(state(index = 1)).canGoNext)
        assertTrue(MprisProps.from(state(index = 1, repeat = RepeatMode.ALL)).canGoNext)
        assertTrue(MprisProps.from(state(index = 0)).canGoPrevious) // restarts the track
        assertFalse(MprisProps.from(state(duration = 0.0)).canSeek)
        assertTrue(MprisProps.from(state(duration = 10.0)).canSeek)
    }

    @Test
    fun volumeIsAFractionOfOne() {
        assertEquals(0.4, MprisProps.from(state(volume = 40)).volume)
        assertEquals(1.0, MprisProps.from(state(volume = 100)).volume)
    }

    @Test
    fun positionChangesAloneAnnounceNothing() {
        val before = MprisProps.from(state(position = 1.0))
        val after = MprisProps.from(state(position = 50.0))
        assertEquals(before, after)
        assertTrue(after.changedFrom(before).isEmpty())
    }

    @Test
    fun firstSnapshotAnnouncesEveryProperty() {
        assertEquals(
            setOf(
                "PlaybackStatus", "LoopStatus", "Shuffle", "Volume", "Metadata",
                "CanGoNext", "CanGoPrevious", "CanPlay", "CanPause", "CanSeek",
            ),
            MprisProps.from(state()).changedFrom(null),
        )
    }

    @Test
    fun pausingChangesOnlyPlaybackStatus() {
        val playing = MprisProps.from(state(isPlaying = true))
        val paused = MprisProps.from(state(isPlaying = false))
        assertEquals(setOf("PlaybackStatus"), paused.changedFrom(playing))
    }

    @Test
    fun changingTrackChangesMetadataAndNavigation() {
        val first = MprisProps.from(state(index = 0))
        val second = MprisProps.from(state(index = 1, duration = 100.0))
        assertEquals(setOf("Metadata", "CanGoNext"), second.changedFrom(first))
    }

    @Test
    fun settingsChangesAreReportedIndividually() {
        val base = MprisProps.from(state())
        assertEquals(setOf("Shuffle"), MprisProps.from(state(shuffle = true)).changedFrom(base))
        assertEquals(setOf("LoopStatus"), MprisProps.from(state(repeat = RepeatMode.ONE)).changedFrom(base))
        assertEquals(setOf("Volume"), MprisProps.from(state(volume = 30)).changedFrom(base))
        assertNotEquals(base, MprisProps.from(state(volume = 30)))
    }
}
