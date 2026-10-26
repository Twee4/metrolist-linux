package com.metrolist.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val query: String,
    val tracks: List<Track>,
    val albums: List<Album>,
)
