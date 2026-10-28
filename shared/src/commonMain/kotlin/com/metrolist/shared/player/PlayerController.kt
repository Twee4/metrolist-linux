package com.metrolist.shared.player

import com.metrolist.shared.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class RepeatMode { OFF, ALL, ONE }

data class PlayerState(
    /** Tracks in the order they will play (already shuffled when [shuffle] is on). */
    val queue: List<Track> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionSeconds: Double = 0.0,
    /** Length reported by the backend, or the track's listed length until that arrives. */
    val durationSeconds: Double = 0.0,
    val error: String? = null,
    /** 0 to 100. */
    val volume: Int = 100,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
) {
    val current: Track? get() = queue.getOrNull(index)

    /** True if "next" would start something: a later track, or wrapping around with repeat-all. */
    val hasNext: Boolean get() = current != null && (index < queue.lastIndex || repeat == RepeatMode.ALL)
    val hasPrevious: Boolean get() = index > 0
}

/** Owns the play queue and mirrors the backend's progress into [state]. */
class PlayerController(
    private val backend: AudioBackend,
    private val random: Random = Random.Default,
) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var collector: Job? = null

    /**
     * The queue in its original (unshuffled) order, so turning shuffle off restores it.
     * Invariant: while shuffle is off, this equals `state.queue`.
     */
    private var original: List<Track> = emptyList()

    /** Starts listening to backend events. Calling it again replaces the previous listener. */
    fun start(scope: CoroutineScope) {
        collector?.cancel()
        collector = scope.launch { backend.events.collect(::handle) }
    }

    /** Plays [track] and makes [queue] (which should contain it) the current queue. */
    fun play(track: Track, queue: List<Track> = listOf(track)) {
        val list = if (queue.any { it.id == track.id }) queue else listOf(track)
        original = list
        if (_state.value.shuffle) {
            startAt(shuffledWithFirst(list, track), 0)
        } else {
            startAt(list, list.indexOfFirst { it.id == track.id })
        }
    }

    /** Jumps to the track at [index] in the current queue. */
    fun playAt(index: Int) {
        val s = _state.value
        if (index in s.queue.indices) startAt(s.queue, index)
    }

    fun togglePlayPause() {
        val s = _state.value
        if (s.current == null) return
        val nowPlaying = !s.isPlaying
        backend.setPaused(!nowPlaying)
        _state.update { it.copy(isPlaying = nowPlaying) }
    }

    /** Pauses if playing; does nothing otherwise. */
    fun pause() {
        if (_state.value.isPlaying) togglePlayPause()
    }

    /** Resumes if paused; does nothing otherwise. */
    fun resume() {
        val s = _state.value
        if (s.current != null && !s.isPlaying) togglePlayPause()
    }

    fun next() {
        val s = _state.value
        if (s.current == null) return
        when {
            s.index < s.queue.lastIndex -> startAt(s.queue, s.index + 1)
            s.repeat == RepeatMode.ALL -> startAt(s.queue, 0)
        }
    }

    /** Restarts the track if it has been playing for a few seconds, otherwise goes back one. */
    fun previous() {
        val s = _state.value
        if (s.current == null) return
        if (s.positionSeconds > RESTART_THRESHOLD_SECONDS || !s.hasPrevious) {
            seekTo(0.0)
        } else {
            startAt(s.queue, s.index - 1)
        }
    }

    fun seekTo(seconds: Double) {
        val s = _state.value
        if (s.current == null) return
        val target =
            if (s.durationSeconds > 0) seconds.coerceIn(0.0, s.durationSeconds)
            else seconds.coerceAtLeast(0.0)
        backend.seekTo(target)
        _state.update { it.copy(positionSeconds = target) }
    }

    fun setVolume(percent: Int) {
        val volume = percent.coerceIn(0, 100)
        backend.setVolume(volume)
        _state.update { it.copy(volume = volume) }
    }

    /** Turning shuffle on keeps the current track first and shuffles the rest; off restores the order. */
    fun toggleShuffle() {
        val s = _state.value
        val current = s.current
        val nowShuffled = !s.shuffle
        when {
            current == null -> _state.update { it.copy(shuffle = nowShuffled) }
            nowShuffled -> _state.update {
                it.copy(shuffle = true, queue = shuffledWithFirst(it.queue, current), index = 0)
            }
            else -> {
                val restoredIndex = original.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
                _state.update { it.copy(shuffle = false, queue = original, index = restoredIndex) }
            }
        }
    }

    fun setShuffle(enabled: Boolean) {
        if (_state.value.shuffle != enabled) toggleShuffle()
    }

    fun setRepeat(mode: RepeatMode) {
        _state.update { it.copy(repeat = mode) }
    }

    /** Cycles off -> repeat all -> repeat one -> off. */
    fun cycleRepeat() {
        _state.update {
            val next = when (it.repeat) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeat = next)
        }
    }

    /** Appends [track] to the queue, or starts playing it if nothing is playing. */
    fun addToQueue(track: Track) {
        if (_state.value.current == null) {
            play(track)
            return
        }
        original = original + track
        _state.update { it.copy(queue = it.queue + track) }
    }

    /** Removes the track at [index]. The track that is playing right now cannot be removed. */
    fun removeAt(index: Int) {
        val s = _state.value
        if (index !in s.queue.indices || index == s.index) return

        val removed = s.queue[index]
        val newQueue = s.queue.toMutableList().apply { removeAt(index) }
        val newIndex = if (index < s.index) s.index - 1 else s.index

        original = if (s.shuffle) {
            original.toMutableList().apply {
                val i = indexOfFirst { it.id == removed.id }
                if (i >= 0) removeAt(i)
            }
        } else {
            newQueue
        }
        _state.update { it.copy(queue = newQueue, index = newIndex) }
    }

    /** Stops playback and clears the queue. Volume, shuffle and repeat are kept. */
    fun stop() {
        backend.stop()
        original = emptyList()
        _state.update { PlayerState(volume = it.volume, shuffle = it.shuffle, repeat = it.repeat) }
    }

    fun close() {
        collector?.cancel()
        backend.close()
    }

    /** Applies one backend event. Public so the logic can be driven without a real backend. */
    fun handle(event: BackendEvent) {
        when (event) {
            is BackendEvent.Position -> _state.update { it.copy(positionSeconds = event.seconds) }
            is BackendEvent.Duration -> _state.update { it.copy(durationSeconds = event.seconds) }
            BackendEvent.Ended -> {
                val s = _state.value
                when {
                    s.current == null -> Unit
                    s.repeat == RepeatMode.ONE -> startAt(s.queue, s.index)
                    s.index < s.queue.lastIndex -> startAt(s.queue, s.index + 1)
                    s.repeat == RepeatMode.ALL -> startAt(s.queue, 0)
                    else -> _state.update { it.copy(isPlaying = false, positionSeconds = 0.0) }
                }
            }
            is BackendEvent.Error -> _state.update { it.copy(isPlaying = false, error = event.message) }
        }
    }

    private fun startAt(queue: List<Track>, index: Int) {
        val track = queue[index]
        _state.update {
            it.copy(
                queue = queue,
                index = index,
                isPlaying = true,
                positionSeconds = 0.0,
                durationSeconds = track.durationSeconds.toDouble(),
                error = null,
            )
        }
        backend.play(track.id)
    }

    private fun shuffledWithFirst(list: List<Track>, first: Track): List<Track> {
        val firstIndex = list.indexOfFirst { it.id == first.id }
        val rest = list.filterIndexed { i, _ -> i != firstIndex }.shuffled(random)
        return listOf(list[firstIndex]) + rest
    }

    private companion object {
        const val RESTART_THRESHOLD_SECONDS = 3.0
    }
}
