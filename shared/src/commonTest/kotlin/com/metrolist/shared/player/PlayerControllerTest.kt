package com.metrolist.shared.player

import com.metrolist.shared.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.random.Random
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
    override fun setVolume(percent: Int) { calls += "volume:$percent" }
    override fun stop() { calls += "stop" }
    override fun close() { calls += "close" }
}

class PlayerControllerTest {
    private val a = Track("a", "A", "Artist", durationSeconds = 100)
    private val b = Track("b", "B", "Artist", durationSeconds = 200)
    private val c = Track("c", "C", "Artist", durationSeconds = 300)
    private val d = Track("d", "D", "Artist", durationSeconds = 400)

    private fun controller(): Pair<PlayerController, FakeBackend> {
        val backend = FakeBackend()
        return PlayerController(backend, Random(42)) to backend
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

    @Test
    fun volumeIsClampedAndForwardedToBackend() {
        val (player, backend) = controller()
        player.setVolume(150)
        player.setVolume(-5)
        player.setVolume(40)
        assertEquals(40, player.state.value.volume)
        assertEquals(listOf("volume:100", "volume:0", "volume:40"), backend.calls)
    }

    @Test
    fun shuffleKeepsCurrentTrackFirstAndRestoresOrderWhenTurnedOff() {
        val (player, backend) = controller()
        player.play(b, listOf(a, b, c, d))

        player.toggleShuffle()
        val shuffled = player.state.value
        assertTrue(shuffled.shuffle)
        assertEquals(b, shuffled.current)
        assertEquals(0, shuffled.index)
        assertEquals(4, shuffled.queue.size)
        assertEquals(setOf(a, b, c, d), shuffled.queue.toSet())
        assertEquals(listOf("play:b"), backend.calls) // the playing track is not restarted

        player.toggleShuffle()
        val restored = player.state.value
        assertFalse(restored.shuffle)
        assertEquals(listOf(a, b, c, d), restored.queue)
        assertEquals(1, restored.index)
        assertEquals(b, restored.current)
    }

    @Test
    fun shuffleChosenBeforePlayingAppliesToThePlayedQueue() {
        val (player, _) = controller()
        player.toggleShuffle()
        assertTrue(player.state.value.shuffle)

        player.play(c, listOf(a, b, c, d))
        val s = player.state.value
        assertEquals(c, s.current)
        assertEquals(0, s.index)
        assertEquals(setOf(a, b, c, d), s.queue.toSet())
    }

    @Test
    fun repeatCyclesOffAllOneOff() {
        val (player, _) = controller()
        assertEquals(RepeatMode.OFF, player.state.value.repeat)
        player.cycleRepeat()
        assertEquals(RepeatMode.ALL, player.state.value.repeat)
        player.cycleRepeat()
        assertEquals(RepeatMode.ONE, player.state.value.repeat)
        player.cycleRepeat()
        assertEquals(RepeatMode.OFF, player.state.value.repeat)
    }

    @Test
    fun repeatAllWrapsAroundOnNextAndWhenTheQueueEnds() {
        val (player, backend) = controller()
        player.play(b, listOf(a, b))
        assertFalse(player.state.value.hasNext)

        player.cycleRepeat() // ALL
        assertTrue(player.state.value.hasNext)

        player.next() // wraps from the last track to the first
        assertEquals(a, player.state.value.current)
        player.next()
        assertEquals(b, player.state.value.current)
        player.handle(BackendEvent.Ended) // natural end of the last track wraps too
        assertEquals(a, player.state.value.current)
        assertEquals(listOf("play:b", "play:a", "play:b", "play:a"), backend.calls)
    }

    @Test
    fun repeatOneReplaysOnEndButNextStillAdvances() {
        val (player, backend) = controller()
        player.play(a, listOf(a, b))
        player.cycleRepeat()
        player.cycleRepeat() // ONE

        player.handle(BackendEvent.Ended)
        assertEquals(a, player.state.value.current)
        assertTrue(player.state.value.isPlaying)
        assertEquals(listOf("play:a", "play:a"), backend.calls)

        player.next()
        assertEquals(b, player.state.value.current)
    }

    @Test
    fun addToQueueAppendsWithoutInterruptingPlayback() {
        val (player, backend) = controller()
        player.play(a)
        player.addToQueue(b)
        assertEquals(listOf(a, b), player.state.value.queue)
        assertEquals(0, player.state.value.index)
        assertEquals(listOf("play:a"), backend.calls)
        assertTrue(player.state.value.hasNext)
    }

    @Test
    fun addToQueueStartsPlayingWhenNothingIsPlaying() {
        val (player, backend) = controller()
        player.addToQueue(a)
        assertEquals(a, player.state.value.current)
        assertEquals(listOf("play:a"), backend.calls)
    }

    @Test
    fun tracksAddedWhileShuffledSurviveTurningShuffleOff() {
        val (player, _) = controller()
        player.play(a, listOf(a, b))
        player.toggleShuffle()
        player.addToQueue(c)
        player.toggleShuffle()
        assertEquals(listOf(a, b, c), player.state.value.queue)
        assertEquals(a, player.state.value.current)
    }

    @Test
    fun removeAtDropsOtherTracksButNeverTheCurrentOne() {
        val (player, backend) = controller()
        player.play(b, listOf(a, b, c, d))

        player.removeAt(1) // the playing track: ignored
        assertEquals(listOf(a, b, c, d), player.state.value.queue)

        player.removeAt(0) // before the current track: index shifts, playback unaffected
        assertEquals(listOf(b, c, d), player.state.value.queue)
        assertEquals(0, player.state.value.index)
        assertEquals(b, player.state.value.current)

        player.removeAt(2)
        assertEquals(listOf(b, c), player.state.value.queue)

        player.removeAt(9) // out of range: ignored
        assertEquals(listOf(b, c), player.state.value.queue)
        assertEquals(listOf("play:b"), backend.calls)
    }

    @Test
    fun removeWhileShuffledStillRestoresTheRestOfTheOriginalOrder() {
        val (player, _) = controller()
        player.play(a, listOf(a, b, c, d))
        player.toggleShuffle()

        val victim = player.state.value.queue[1]
        player.removeAt(1)
        player.toggleShuffle()

        assertEquals(listOf(a, b, c, d).filter { it != victim }, player.state.value.queue)
        assertEquals(a, player.state.value.current)
    }

    @Test
    fun playAtJumpsWithinTheQueue() {
        val (player, backend) = controller()
        player.play(a, listOf(a, b, c))
        player.playAt(2)
        assertEquals(c, player.state.value.current)
        player.playAt(9) // out of range: ignored
        assertEquals(c, player.state.value.current)
        assertEquals(listOf("play:a", "play:c"), backend.calls)
    }

    @Test
    fun stopKeepsVolumeShuffleAndRepeatSettings() {
        val (player, backend) = controller()
        player.setVolume(40)
        player.toggleShuffle()
        player.cycleRepeat()
        player.play(a)
        player.stop()

        assertEquals(
            PlayerState(volume = 40, shuffle = true, repeat = RepeatMode.ALL),
            player.state.value,
        )
        assertEquals(listOf("volume:40", "play:a", "stop"), backend.calls)
    }
}
