package com.metrolist.shared.model

/** A playlist in the user's library. [browseId] is what the browse endpoint needs ("VL" + playlist id). */
data class Playlist(
    val browseId: String,
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
)

/** One page-through of a playlist: its tracks. */
data class PlaylistContents(val playlist: Playlist, val tracks: List<Track>)
