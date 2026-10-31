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

/** A titled row on the home page: songs you can play, and albums/playlists you can open. */
data class Shelf(
    val title: String,
    val tracks: List<Track> = emptyList(),
    val collections: List<Playlist> = emptyList(),
)
