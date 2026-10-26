package com.metrolist.linux.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.shared.model.Album
import com.metrolist.shared.model.QueueEntry
import com.metrolist.shared.model.Track
import com.metrolist.shared.state.AppState

private enum class NavItem(val label: String) {
    HOME("Home"),
    SEARCH("Search"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

@Composable
fun MetrolistApp() {
    val appState = remember { AppState() }
    var selected by remember { mutableStateOf(NavItem.HOME) }
    var query by remember { mutableStateOf("") }
    val results = appState.results
    val queue = appState.queue
    val library = appState.library

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                    NavigationRail {
                        NavItem.entries.forEach { item ->
                            val icon = when (item) {
                                NavItem.HOME -> Icons.Default.Home
                                NavItem.SEARCH -> Icons.Default.Search
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

                    Divider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))

                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        when (selected) {
                            NavItem.HOME -> HomeScreen(library.value)
                            NavItem.SEARCH -> SearchScreen(
                                query = query,
                                onQueryChange = {
                                    query = it
                                    appState.updateQuery(it)
                                },
                                tracks = results.value,
                            )
                            NavItem.LIBRARY -> LibraryScreen(library.value)
                            NavItem.SETTINGS -> SettingsScreen()
                        }
                    }
                }

                PlayerBar(queue.value)
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
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search music") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (tracks.isEmpty()) {
            Text("Type to search. This is the native Linux desktop prototype.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks) { track ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(track.title, fontWeight = FontWeight.SemiBold)
                                Text(track.artist)
                            }
                            Text("${track.durationSeconds / 60}:${String.format("%02d", track.durationSeconds % 60)}")
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
private fun PlayerBar(queue: List<QueueEntry>) {
    val playing = queue.firstOrNull() ?: QueueEntry("none", "Nothing playing", "Nothing loaded", 0)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(92.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = MaterialTheme.colorScheme.primary)
                }
                Column {
                    Text(playing.title, fontWeight = FontWeight.SemiBold)
                    Text(playing.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous") }
                IconButton(onClick = {}) { Icon(Icons.Default.PlayArrow, contentDescription = "Play") }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, contentDescription = "Next") }
            }
        }
    }
}
