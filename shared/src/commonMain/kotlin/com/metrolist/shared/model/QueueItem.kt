package com.metrolist.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class QueueItem(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long,
    val artUrl: String? = null,
)
