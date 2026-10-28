package com.metrolist.shared.player

import com.metrolist.shared.model.Track

/**
 * Platform-independent view of the player as MPRIS (the Linux media-player D-Bus standard) sees it.
 * The D-Bus plumbing lives elsewhere; keeping the mapping here makes it unit-testable.
 * Spec: https://specifications.freedesktop.org/mpris-spec/latest/
 */
object Mpris {
    const val BUS_NAME = "org.mpris.MediaPlayer2.metrolist"
    const val OBJECT_PATH = "/org/mpris/MediaPlayer2"
    const val ROOT_INTERFACE = "org.mpris.MediaPlayer2"
    const val PLAYER_INTERFACE = "org.mpris.MediaPlayer2.Player"

    /** The spec's placeholder for "there is no current track". */
    const val NO_TRACK_PATH = "/org/mpris/MediaPlayer2/TrackList/NoTrack"

    private const val TRACK_PATH_PREFIX = "/org/metrolist/Track/"

    /**
     * D-Bus object path elements may only contain `[A-Za-z0-9_]`, but YouTube ids also use `-`.
     * Every other character becomes `_` plus exactly four hex digits (and `_` itself is escaped),
     * so different ids can never produce the same path.
     */
    fun trackPath(track: Track): String = TRACK_PATH_PREFIX + buildString {
        for (c in track.id) {
            if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9') append(c)
            else append('_').append(c.code.toString(16).padStart(4, '0'))
        }
    }

    fun loopStatus(mode: RepeatMode): String = when (mode) {
        RepeatMode.OFF -> "None"
        RepeatMode.ALL -> "Playlist"
        RepeatMode.ONE -> "Track"
    }

    fun repeatMode(loopStatus: String): RepeatMode? = when (loopStatus) {
        "None" -> RepeatMode.OFF
        "Playlist" -> RepeatMode.ALL
        "Track" -> RepeatMode.ONE
        else -> null
    }

    fun toMicros(seconds: Double): Long = (seconds * 1_000_000).toLong()
}

data class MprisMetadata(
    val trackId: String,
    val title: String,
    val artists: List<String>,
    val album: String,
    val lengthMicros: Long,
    val artUrl: String?,
)

/**
 * Everything MPRIS clients are told about via PropertiesChanged. Playback position is deliberately
 * left out: the spec says it must be read on demand and never announced as a property change.
 */
data class MprisProps(
    val playbackStatus: String,
    val loopStatus: String,
    val shuffle: Boolean,
    val volume: Double,
    val metadata: MprisMetadata?,
    val canGoNext: Boolean,
    val canGoPrevious: Boolean,
    val canPlay: Boolean,
    val canPause: Boolean,
    val canSeek: Boolean,
) {
    /** Names of the properties whose value differs from [old]; all of them when [old] is null. */
    fun changedFrom(old: MprisProps?): Set<String> = buildSet {
        if (old == null || playbackStatus != old.playbackStatus) add("PlaybackStatus")
        if (old == null || loopStatus != old.loopStatus) add("LoopStatus")
        if (old == null || shuffle != old.shuffle) add("Shuffle")
        if (old == null || volume != old.volume) add("Volume")
        if (old == null || metadata != old.metadata) add("Metadata")
        if (old == null || canGoNext != old.canGoNext) add("CanGoNext")
        if (old == null || canGoPrevious != old.canGoPrevious) add("CanGoPrevious")
        if (old == null || canPlay != old.canPlay) add("CanPlay")
        if (old == null || canPause != old.canPause) add("CanPause")
        if (old == null || canSeek != old.canSeek) add("CanSeek")
    }

    companion object {
        fun from(state: PlayerState): MprisProps {
            val track = state.current
            return MprisProps(
                playbackStatus = when {
                    track == null -> "Stopped"
                    state.isPlaying -> "Playing"
                    else -> "Paused"
                },
                loopStatus = Mpris.loopStatus(state.repeat),
                shuffle = state.shuffle,
                volume = state.volume / 100.0,
                metadata = track?.let {
                    MprisMetadata(
                        trackId = Mpris.trackPath(it),
                        title = it.title,
                        artists = listOf(it.artist),
                        album = it.album,
                        lengthMicros = Mpris.toMicros(state.durationSeconds),
                        artUrl = it.artUrl,
                    )
                },
                canGoNext = state.hasNext,
                // "Previous" restarts the track when it is the first one, so it works whenever a track is loaded.
                canGoPrevious = track != null,
                canPlay = track != null,
                canPause = track != null,
                canSeek = track != null && state.durationSeconds > 0,
            )
        }
    }
}
