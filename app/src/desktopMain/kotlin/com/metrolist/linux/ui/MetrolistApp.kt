package com.metrolist.linux.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.shared.domain.MusicStore
import com.metrolist.shared.model.Album
import com.metrolist.shared.model.Track
import com.metrolist.shared.player.PlayerController
import com.metrolist.shared.player.PlayerState
import com.metrolist.shared.player.RepeatMode
import com.metrolist.shared.repository.YouTubeMusicRepository
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class NavItem(val label: String) {
    HOME("Home"),
    SEARCH("Search"),
    QUEUE("Queue"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

/** The player is owned by the caller so that things outside the window (tray, media keys) can use it too. */
@Composable
fun MetrolistApp(player: PlayerController) {
    val store = remember { MusicStore(YouTubeMusicRepository()) }
    var selected by remember { mutableStateOf(NavItem.HOME) }
    var query by remember { mutableStateOf("") }
    val results by store.results.collectAsState()
    val isSearching by store.isSearching.collectAsState()
    val error by store.error.collectAsState()
    val library by store.library.collectAsState()

    val playerState by player.state.collectAsState()

    LaunchedEffect(store) { store.load() }

    // Debounce: a new keystroke cancels the pending search and restarts the timer.
    LaunchedEffect(query) {
        delay(300)
        store.updateQuery(query)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                    NavigationRail {
                        NavItem.entries.forEach { item ->
                            val icon = when (item) {
                                NavItem.HOME -> Icons.Default.Home
                                NavItem.SEARCH -> Icons.Default.Search
                                NavItem.QUEUE -> Icons.Default.QueueMusic
                                NavItem.LIBRARY -> Icons.Default.LibraryMusic
                                NavItem.SETTINGS -> Icons.Default.Settings
                            }

                            NavigationRailItem(
                                selected = item == selected,
                                onClick = { selected = item },
                                icon = { Icon(icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }

                    VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))

                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        when (selected) {
                            NavItem.HOME -> HomeScreen(library)
                            NavItem.SEARCH -> SearchScreen(
                                query = query,
                                onQueryChange = { query = it },
                                tracks = results,
                                isSearching = isSearching,
                                error = error,
                                currentTrackId = playerState.current?.id,
                                onPlay = { track -> player.play(track, results) },
                                onAddToQueue = player::addToQueue,
                            )
                            NavItem.QUEUE -> QueueScreen(
                                state = playerState,
                                onPlayAt = player::playAt,
                                onRemove = player::removeAt,
                            )
                            NavItem.LIBRARY -> LibraryScreen(library)
                            NavItem.SETTINGS -> SettingsScreen()
                        }
                    }
                }

                PlayerBar(
                    state = playerState,
                    onPrevious = player::previous,
                    onTogglePlay = player::togglePlayPause,
                    onNext = player::next,
                    onSeek = player::seekTo,
                    onToggleShuffle = player::toggleShuffle,
                    onCycleRepeat = player::cycleRepeat,
                    onVolumeChange = player::setVolume,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(library: List<Album>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Metrolist Linux", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Native Linux desktop port foundation for the Metrolist ecosystem.")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status", style = MaterialTheme.typography.titleMedium)
                Text("The shared state layer is working. The next step is to port the actual music logic from the Android app.")
            }
        }

        Text("Library overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(library) { album ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(album.title, fontWeight = FontWeight.SemiBold)
                            Text(album.artist)
                        }
                        Text("Album")
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    tracks: List<Track>,
    isSearching: Boolean,
    error: String?,
    currentTrackId: String?,
    onPlay: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search music") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error)
        } else if (isSearching && tracks.isEmpty()) {
            Text("Searching…")
        } else if (tracks.isEmpty()) {
            Text(
                if (query.isBlank()) "Type to search YouTube Music."
                else "No results."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks) { track ->
                    val isCurrent = track.id == currentTrackId
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(track) }
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
                            IconButton(onClick = { onAddToQueue(track) }) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to queue")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(library: List<Album>) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (library.isEmpty()) {
            Text("No items loaded yet.")
        } else {
            library.forEach { album ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(album.title, fontWeight = FontWeight.SemiBold)
                            Text(album.artist)
                        }
                        Text("${album.year ?: "N/A"}")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Linux app settings and system integration are planned next.")
        Button(onClick = {}) {
            Text("Apply desktop configuration")
        }
    }
}

@Composable
private fun QueueScreen(
    state: PlayerState,
    onPlayAt: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Queue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (state.queue.isEmpty()) {
            Text("The queue is empty. Search for music, then play a track or add it to the queue.")
        } else {
            Text("${state.queue.size} tracks" + if (state.shuffle) " · shuffled" else "")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(state.queue) { index, track ->
                    val isCurrent = index == state.index
                    val highlight = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayAt(index) }
                                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("${index + 1}", modifier = Modifier.width(28.dp), color = highlight)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    fontWeight = FontWeight.SemiBold,
                                    color = highlight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(formatTime(track.durationSeconds.toDouble()))
                            // The track that is playing right now cannot be removed.
                            IconButton(onClick = { onRemove(index) }, enabled = !isCurrent) {
                                Icon(Icons.Default.Close, contentDescription = "Remove from queue")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerBar(
    state: PlayerState,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Double) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Int) -> Unit,
) {
    val track = state.current
    val error = state.error
    val duration = state.durationSeconds
    val canSeek = track != null && duration > 0
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction
        ?: if (duration > 0) (state.positionSeconds / duration).toFloat().coerceIn(0f, 1f) else 0f
    val shownPosition = if (dragFraction != null) fraction * duration else state.positionSeconds

    val activeTint = MaterialTheme.colorScheme.primary
    val idleTint = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(92.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    track?.title ?: "Nothing playing",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (error != null) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text(
                        track?.artist ?: "Search for a track and click it to play",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = if (state.shuffle) "Shuffle on" else "Shuffle off",
                        tint = if (state.shuffle) activeTint else idleTint,
                    )
                }
                IconButton(onClick = onPrevious, enabled = track != null) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }
                IconButton(onClick = onTogglePlay, enabled = track != null) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                    )
                }
                IconButton(onClick = onNext, enabled = state.hasNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        if (state.repeat == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = when (state.repeat) {
                            RepeatMode.OFF -> "Repeat off"
                            RepeatMode.ALL -> "Repeat all"
                            RepeatMode.ONE -> "Repeat one"
                        },
                        tint = if (state.repeat != RepeatMode.OFF) activeTint else idleTint,
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(formatTime(shownPosition), style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = fraction,
                    onValueChange = { dragFraction = it },
                    onValueChangeFinished = {
                        dragFraction?.let { onSeek(it * duration) }
                        dragFraction = null
                    },
                    enabled = canSeek,
                    modifier = Modifier.weight(1f),
                )
                Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
            }

            Row(
                modifier = Modifier.width(120.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (state.volume == 0) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Volume",
                    tint = idleTint,
                )
                Slider(
                    value = state.volume / 100f,
                    onValueChange = { onVolumeChange((it * 100).roundToInt()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun formatTime(totalSeconds: Double): String {
    val seconds = totalSeconds.toLong().coerceAtLeast(0)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, secs)
    else String.format("%d:%02d", minutes, secs)
}
