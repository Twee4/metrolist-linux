package com.metrolist.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val durationSeconds: Long,
    val artUrl: String? = null,
)

@Serializable
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int? = null,
)
