package com.metrolist.shared.player

import com.metrolist.shared.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerState(
    val queue: List<Track> = emptyList(),
    val index: Int = -1,
    val isPlaying: Boolean = false,
    val positionSeconds: Double = 0.0,
    /** Length reported by the backend, or the track's listed length until that arrives. */
    val durationSeconds: Double = 0.0,
    val error: String? = null,
) {
    val current: Track? get() = queue.getOrNull(index)
    val hasNext: Boolean get() = index in 0 until queue.lastIndex
    val hasPrevious: Boolean get() = index > 0
}

/** Owns the play queue and mirrors the backend's progress into [state]. */
class PlayerController(private val backend: AudioBackend) {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var collector: Job? = null

    /** Starts listening to backend events. Calling it again replaces the previous listener. */
    fun start(scope: CoroutineScope) {
        collector?.cancel()
        collector = scope.launch { backend.events.collect(::handle) }
    }

    /** Plays [track] and makes [queue] (which should contain it) the current queue. */
    fun play(track: Track, queue: List<Track> = listOf(track)) {
        val index = queue.indexOfFirst { it.id == track.id }
        if (index >= 0) startAt(queue, index) else startAt(listOf(track), 0)
    }

    fun togglePlayPause() {
        val s = _state.value
        if (s.current == null) return
        val nowPlaying = !s.isPlaying
        backend.setPaused(!nowPlaying)
        _state.update { it.copy(isPlaying = nowPlaying) }
    }

    fun next() {
        val s = _state.value
        if (s.hasNext) startAt(s.queue, s.index + 1)
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

    fun stop() {
        backend.stop()
        _state.value = PlayerState()
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
                if (s.hasNext) startAt(s.queue, s.index + 1)
                else _state.update { it.copy(isPlaying = false, positionSeconds = 0.0) }
            }
            is BackendEvent.Error -> _state.update { it.copy(isPlaying = false, error = event.message) }
        }
    }

    private fun startAt(queue: List<Track>, index: Int) {
        val track = queue[index]
        _state.value = PlayerState(
            queue = queue,
            index = index,
            isPlaying = true,
            durationSeconds = track.durationSeconds.toDouble(),
        )
        backend.play(track.id)
    }

    private companion object {
        const val RESTART_THRESHOLD_SECONDS = 3.0
    }
}
