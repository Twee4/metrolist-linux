package com.metrolist.shared.player

import com.metrolist.shared.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeBackend : AudioBackend {
    override val events: Flow<BackendEvent> = emptyFlow()
    val calls = mutableListOf<String>()

    override fun play(videoId: String) { calls += "play:$videoId" }
    override fun setPaused(paused: Boolean) { calls += "paused:$paused" }
    override fun seekTo(seconds: Double) { calls += "seek:$seconds" }
    override fun stop() { calls += "stop" }
    override fun close() { calls += "close" }
}

class PlayerControllerTest {
    private val a = Track("a", "A", "Artist", durationSeconds = 100)
    private val b = Track("b", "B", "Artist", durationSeconds = 200)
    private val c = Track("c", "C", "Artist", durationSeconds = 300)

    private fun controller(): Pair<PlayerController, FakeBackend> {
        val backend = FakeBackend()
        return PlayerController(backend) to backend
    }

    @Test
    fun playStartsTrackAndSetsQueue() {
        val (player, backend) = controller()
        player.play(b, listOf(a, b, c))

        val s = player.state.value
        assertEquals(1, s.index)
        assertEquals(b, s.current)
        assertTrue(s.isPlaying)
        assertEquals(200.0, s.durationSeconds)
        assertEquals(listOf("play:b"), backend.calls)
    }

    @Test
    fun playTrackMissingFromQueueFallsBackToSingleTrackQueue() {
        val (player, _) = controller()
        player.play(c, listOf(a, b))
        assertEquals(listOf(c), player.state.value.queue)
        assertEquals(0, player.state.value.index)
    }

    @Test
    fun togglePauseFlipsStateAndTellsBackend() {
        val (player, backend) = controller()
        player.play(a)
        player.togglePlayPause()
        assertFalse(player.state.value.isPlaying)
        player.togglePlayPause()
        assertTrue(player.state.value.isPlaying)
        assertEquals(listOf("play:a", "paused:true", "paused:false"), backend.calls)
    }

    @Test
    fun controlsDoNothingWithoutATrack() {
        val (player, backend) = controller()
        player.togglePlayPause()
        player.next()
        player.previous()
        player.seekTo(10.0)
        assertTrue(backend.calls.isEmpty())
        assertNull(player.state.value.current)
    }

    @Test
    fun nextAdvancesAndStopsAtEndOfQueue() {
        val (player, backend) = controller()
        player.play(b, listOf(a, b, c))
        player.next()
        assertEquals(c, player.state.value.current)
        player.next()
        assertEquals(c, player.state.value.current)
        assertEquals(listOf("play:b", "play:c"), backend.calls)
    }

    @Test
    fun previousRestartsWhenPastThresholdOtherwiseGoesBack() {
        val (player, backend) = controller()
        player.play(b, listOf(a, b, c))

        player.handle(BackendEvent.Position(10.0))
        player.previous()
        assertEquals(b, player.state.value.current)
        assertEquals(0.0, player.state.value.positionSeconds)

        player.handle(BackendEvent.Position(1.0))
        player.previous()
        assertEquals(a, player.state.value.current)
        assertEquals(listOf("play:b", "seek:0.0", "play:a"), backend.calls)
    }

    @Test
    fun previousOnFirstTrackRestartsIt() {
        val (player, backend) = controller()
        player.play(a, listOf(a, b))
        player.handle(BackendEvent.Position(1.0))
        player.previous()
        assertEquals(a, player.state.value.current)
        assertEquals(listOf("play:a", "seek:0.0"), backend.calls)
    }

    @Test
    fun endedAutoAdvancesThenStopsAtLastTrack() {
        val (player, backend) = controller()
        player.play(a, listOf(a, b))

        player.handle(BackendEvent.Ended)
        assertEquals(b, player.state.value.current)
        assertTrue(player.state.value.isPlaying)

        player.handle(BackendEvent.Ended)
        assertEquals(b, player.state.value.current)
        assertFalse(player.state.value.isPlaying)
        assertEquals(listOf("play:a", "play:b"), backend.calls)
    }

    @Test
    fun backendDurationOverridesListedDuration() {
        val (player, _) = controller()
        player.play(a)
        player.handle(BackendEvent.Duration(98.5))
        assertEquals(98.5, player.state.value.durationSeconds)
    }

    @Test
    fun seekIsClampedToDuration() {
        val (player, backend) = controller()
        player.play(a)
        player.seekTo(999.0)
        player.seekTo(-5.0)
        assertEquals(listOf("play:a", "seek:100.0", "seek:0.0"), backend.calls)
    }

    @Test
    fun errorStopsPlaybackAndIsReportedThenClearedByNextPlay() {
        val (player, _) = controller()
        player.play(a, listOf(a, b))
        player.handle(BackendEvent.Error("boom"))
        assertEquals("boom", player.state.value.error)
        assertFalse(player.state.value.isPlaying)

        player.next()
        assertNull(player.state.value.error)
        assertTrue(player.state.value.isPlaying)
    }

    @Test
    fun stopClearsState() {
        val (player, backend) = controller()
        player.play(a)
        player.stop()
        assertEquals(PlayerState(), player.state.value)
        assertEquals(listOf("play:a", "stop"), backend.calls)
    }
}
