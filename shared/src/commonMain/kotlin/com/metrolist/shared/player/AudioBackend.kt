package com.metrolist.shared.player

import kotlinx.coroutines.flow.Flow

/** Something that can turn a YouTube video id into sound. Implementations are platform specific. */
interface AudioBackend {
    val events: Flow<BackendEvent>

    /** Starts playing [videoId], replacing whatever is playing. Never blocks on the network. */
    fun play(videoId: String)

    fun setPaused(paused: Boolean)

    fun seekTo(seconds: Double)

    /** Stops playback but keeps the backend usable. */
    fun stop()

    /** Releases all resources. The backend cannot be used afterwards. */
    fun close()
}

sealed interface BackendEvent {
    data class Position(val seconds: Double) : BackendEvent
    data class Duration(val seconds: Double) : BackendEvent

    /** The current track played to its natural end. */
    data object Ended : BackendEvent

    data class Error(val message: String) : BackendEvent
}
