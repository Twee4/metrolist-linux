package com.metrolist.linux.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.shared.domain.LibraryState
import com.metrolist.shared.domain.PlaylistState
import com.metrolist.shared.model.Playlist
import com.metrolist.shared.model.Track

/** A clickable song row: click plays, the button on the right queues it. */
@Composable
internal fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlay)
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track.title,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatTime(track.durationSeconds.toDouble()))
            IconButton(onClick = onAddToQueue) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to queue")
            }
        }
    }
}

@Composable
internal fun LibraryScreen(
    state: LibraryState,
    onOpen: (Playlist) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        when (state) {
            LibraryState.SignedOut -> {
                Text("Sign in to see your YouTube Music playlists and liked songs.")
                Button(onClick = onOpenSettings) { Text("Go to Settings") }
            }
            LibraryState.Loading -> Text("Loading your library…")
            is LibraryState.Failed -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRetry) { Text("Retry") }
                    OutlinedButton(onClick = onOpenSettings) { Text("Settings") }
                }
            }
            is LibraryState.Loaded -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.playlists, key = { it.browseId }) { playlist ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(playlist) },
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(playlist.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (playlist.subtitle.isNotBlank()) {
                                Text(
                                    playlist.subtitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlaylistScreen(
    state: PlaylistState,
    currentTrackId: String?,
    onBack: () -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onAddToQueue: (Track) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to library") }
            Text(
                state.playlist.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when (state) {
            is PlaylistState.Loading -> Text("Loading…")
            is PlaylistState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
            is PlaylistState.Loaded -> {
                if (state.tracks.isEmpty()) {
                    Text("This playlist has no playable songs.")
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onPlay(state.tracks.first(), state.tracks) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("Play all")
                        }
                        Text("${state.tracks.size} songs")
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.tracks, key = { it.id }) { track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                onPlay = { onPlay(track, state.tracks) },
                                onAddToQueue = { onAddToQueue(track) },
                            )
                        }
                    }
                }
            }
        }
    }
}
