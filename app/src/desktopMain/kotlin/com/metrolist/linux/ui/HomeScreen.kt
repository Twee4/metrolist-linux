package com.metrolist.linux.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.shared.domain.HomeState
import com.metrolist.shared.model.Playlist
import com.metrolist.shared.model.Track

@Composable
internal fun HomeScreen(
    state: HomeState,
    signedIn: Boolean,
    currentTrackId: String?,
    onPlay: (Track, List<Track>) -> Unit,
    onOpen: (Playlist) -> Unit,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Home", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (!signedIn) {
            Text(
                "Sign in under Settings for recommendations based on your listening, and to see your library.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (state) {
            HomeState.Loading -> Text("Loading recommendations…")
            is HomeState.Failed -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = onRetry) { Text("Retry") }
            }
            is HomeState.Loaded -> if (state.shelves.isEmpty()) {
                Text("No recommendations found.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    items(state.shelves) { shelf ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(shelf.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(shelf.tracks, key = { it.id }) { track ->
                                    Card(
                                        title = track.title,
                                        subtitle = track.artist,
                                        highlighted = track.id == currentTrackId,
                                        onClick = { onPlay(track, shelf.tracks) },
                                    )
                                }
                                items(shelf.collections, key = { it.browseId }) { collection ->
                                    Card(
                                        title = collection.title,
                                        subtitle = collection.subtitle,
                                        highlighted = false,
                                        onClick = { onOpen(collection) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(title: String, subtitle: String, highlighted: Boolean, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(200.dp).clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) MaterialTheme.colorScheme.primary else Color.Unspecified,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
